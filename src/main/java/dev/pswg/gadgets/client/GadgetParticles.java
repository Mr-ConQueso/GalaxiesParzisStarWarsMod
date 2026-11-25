package dev.pswg.gadgets.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pswg.Galaxies;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.GasParticleOptions;
import dev.pswg.gadgets.SmokeCloud;
import dev.pswg.mixin.client.ParticleEngineAccessor;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class GadgetParticles
{
	private static final Map<SmokeCloud, Map<BlockPos, Integer>> VISIBLE_CELLS = new WeakHashMap<>();

	// Keep vanilla particle ticking and eviction, and draw this group through the sorted world geometry layer.
	private static final ParticleRenderType WORLD_PARTICLES = (tesselator, textures) -> null;
	private static final RenderType WORLD_TRANSLUCENT = RenderType.entityTranslucent(TextureAtlas.LOCATION_PARTICLES, false);
	private static final MultiBufferSource.BufferSource WORLD_BUFFER = MultiBufferSource.immediate(new ByteBufferBuilder(4096));

	@EventBusSubscriber(modid = Galaxies.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void register(RegisterParticleProvidersEvent event)
		{
			event.registerSpriteSet(Demolitions.EXPLOSION_SMOKE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
					new ExplosionSmoke(level, x, y, z, dx, dy, dz, sprites));
			event.registerSpriteSet(Demolitions.FRAGMENTATION_WAVE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
					new FragmentationWave(level, x, y, z, dx, dy, dz, sprites));
			for (var gas : List.of(Demolitions.SMOKE_PARTICLES, Demolitions.NERVE_PARTICLES))
				event.registerSpriteSet(gas.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
						new GasParticle(level, x, y, z, type, sprites));
		}

		@SubscribeEvent
		public static void reload(RegisterClientReloadListenersEvent event)
		{
			event.registerReloadListener((ResourceManagerReloadListener) resources -> VISIBLE_CELLS.clear());
		}
	}

	@SubscribeEvent
	public static void logout(ClientPlayerNetworkEvent.LoggingOut event)
	{
		VISIBLE_CELLS.clear();
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event)
	{
		var client = Minecraft.getInstance();
		if (client.level == null || client.isPaused())
			return;
		for (var entity : client.level.entitiesForRendering())
		{
			if (!(entity instanceof SmokeCloud cloud))
				continue;
			var cells = cloud.visualCells();
			var previous = VISIBLE_CELLS.put(cloud, cells);
			if (cells == previous)
				continue;
			var type = (cloud.isNerveGas() ? Demolitions.NERVE_PARTICLES : Demolitions.SMOKE_PARTICLES).get();
			for (var entry : cells.entrySet())
			{
				var pos = entry.getKey();
				int present = previous == null ? 0 : previous.getOrDefault(pos, 0);
				for (int i = present; i < entry.getValue(); i++)
				{
					var random = cloud.getRandom();
					client.particleEngine.createParticle(new GasParticleOptions(type, cloud.getId(), i),
							pos.getX() + 0.5 + random.nextIntBetweenInclusive(-475, 475) / 1000F,
							pos.getY() + 0.5 + random.nextIntBetweenInclusive(-475, 475) / 1000F,
							pos.getZ() + 0.5 + random.nextIntBetweenInclusive(-475, 475) / 1000F, 0, 0, 0);
				}
			}
		}
	}

	@SubscribeEvent
	public static void render(RenderLevelStageEvent event)
	{
		boolean fabulous = Minecraft.useShaderTransparency();
		var stage = event.getStage();
		boolean beforeWater = stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
		if (fabulous ? stage != RenderLevelStageEvent.Stage.AFTER_PARTICLES
				: !beforeWater && stage != RenderLevelStageEvent.Stage.AFTER_WEATHER)
			return;
		var engine = (ParticleEngineAccessor) Minecraft.getInstance().particleEngine;
		var particles = engine.pswg$particleLayers().get(WORLD_PARTICLES);
		if (particles == null || particles.isEmpty())
			return;
		float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		var vertices = WORLD_BUFFER.getBuffer(WORLD_TRANSLUCENT);
		for (var particle : particles)
			if ((fabulous || drawsBeforeWater(particle) == beforeWater)
					&& event.getFrustum().isVisible(particle.getRenderBoundingBox(partial)))
				particle.render(vertices, event.getCamera(), partial);

		// Fabulous sorts the particles target against water and clouds by depth. In Fast and Fancy,
		// submerged gas receives the water tint, and airborne gas blends over distant clouds.
		if (!fabulous)
			RenderSystem.depthMask(false);
		try
		{
			WORLD_BUFFER.endBatch(WORLD_TRANSLUCENT);
		}
		finally
		{
			if (beforeWater)
				RenderSystem.depthMask(true);
		}
	}

	private static boolean drawsBeforeWater(Particle particle)
	{
		return !(particle instanceof GasParticle gas) || gas.inWater;
	}

	private static final class ExplosionSmoke extends TextureSheetParticle
	{
		private ExplosionSmoke(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites)
		{
			super(level, x, y, z);
			scale(7.5F);
			setSize(0.25F, 0.25F);
			setParticleSpeed(dx, dy + random.nextFloat() / 500, dz);
			setAlpha(0.9F);
			lifetime = 1280 + random.nextInt(120);
			setSprite(sprites.get(0, 1));
		}

		@Override
		public void tick()
		{
			xo = x;
			yo = y;
			zo = z;
			if (++age >= lifetime || alpha <= 0)
			{
				remove();
				return;
			}
			xd += random.nextFloat() / 500F * (random.nextBoolean() ? 1 : -1);
			zd += random.nextFloat() / 500F * (random.nextBoolean() ? 1 : -1);
			yd += 0.001;
			move(xd, yd, zd);
			if (age >= 50 && alpha > 0.005F)
				setAlpha(alpha - 0.0025F);
		}

		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
	}

	private abstract static class WorldParticle extends TextureSheetParticle
	{
		WorldParticle(ClientLevel level, double x, double y, double z)
		{
			super(level, x, y, z);
			hasPhysics = false;
		}

		void billboard(VertexConsumer output, Camera camera, Quaternionf rotation, float width, float height, int light)
		{
			if (alpha * 255 < 1)
				return;
			var eye = camera.getPosition();
			var center = new Vector3f((float) (x - eye.x), (float) (y - eye.y), (float) (z - eye.z));
			var normal = rotation.transform(new Vector3f(0, 1, 0));
			var vertex = new Vector3f();
			for (int i = 0; i < 4; i++)
			{
				boolean right = i == 1 || i == 2;
				boolean top = i >= 2;
				vertex.set(right ? width : -width, top ? height : -height, 0).rotate(rotation).add(center);
				output.addVertex(vertex.x, vertex.y, vertex.z).setUv(right ? getU1() : getU0(), top ? getV0() : getV1())
						.setColor(rCol, gCol, bCol, alpha).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
						.setNormal(normal.x, normal.y, normal.z);
			}
		}

		@Override public ParticleRenderType getRenderType() { return WORLD_PARTICLES; }
	}

	private static final class FragmentationWave extends WorldParticle
	{
		FragmentationWave(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites)
		{
			super(level, x, y, z);
			lifetime = 25;
			quadSize = 0.95F + random.nextInt(6) / 5F;
			setParticleSpeed(dx, dy + random.nextFloat() / 500F, dz);
			setSprite(sprites.get(0, 1));
			setColor(Math.min(1, 0.97F + random.nextInt(4) / 3F),
					Math.min(1, 0.98F + random.nextInt(4) / 3F), 1F);
		}

		@Override
		public void tick()
		{
			// Fabric advances the same wave in its custom group and the vanilla quad group.
			super.tick();
			if (isAlive())
				super.tick();
		}

		@Override
		public AABB getRenderBoundingBox(float partial)
		{
			return getBoundingBox().inflate(25);
		}

		@Override
		public void render(VertexConsumer output, Camera camera, float partial)
		{
			float time = age + partial;
			float width;
			float height;
			if (time <= 15)
			{
				width = quadSize * (1 + 8 * (float) Math.max(0, Math.pow(time / 10 - 0.5F, 3)));
				height = quadSize * (float) (1 - Math.pow((time - 4) / 6, 2));
				if (height <= 0)
					return;
			}
			else
			{
				width = height = 0.5F * (float) Math.pow(time / 15, 6);
			}
			setAlpha(time <= 15 ? 1F : Math.max(0.001F, 1 - 1.5F * (float) Math.pow(time / lifetime, 3)));
			billboard(output, camera, camera.rotation(), width, height, LightTexture.pack(15, 0));
		}
	}

	private static final class GasParticle extends WorldParticle
	{
		private final SmokeCloud cloud;
		private final BlockPos cell;
		private final int slot;
		private final float billowing;
		private boolean inWater;

		private GasParticle(ClientLevel level, double x, double y, double z, GasParticleOptions type, SpriteSet sprites)
		{
			super(level, x, y, z);
			cloud = level.getEntity(type.cloudId()) instanceof SmokeCloud gas ? gas : null;
			cell = BlockPos.containing(x, y, z);
			slot = type.slot();
			billowing = (float) Math.toRadians(random.nextIntBetweenInclusive(1, 10) / 2500F);
			quadSize = random.nextIntBetweenInclusive(75, 125) / 125F;
			lifetime = cloud == null ? 120 : SmokeCloud.MAX_AGE * 2;
			age = cloud == null ? 0 : cloud.cloudAge();
			setAlpha(0);
			if (type.getType() == Demolitions.NERVE_PARTICLES.get())
				setColor(1, 0.9F, 0.6F);
			setSprite(sprites.get(0, 1));
		}

		@Override
		public void tick()
		{
			// Fabric also ticks gas in both its custom group and the vanilla no-render group.
			age += cloud == null ? 1 : 2;
			if (age >= lifetime || cloud != null && (cloud.isRemoved() || cloud.visualCells().getOrDefault(cell, 0) <= slot))
			{
				remove();
				return;
			}
			float progress = (float) age / lifetime;
			float fadeIn = Math.min(1, progress * 10);
			setAlpha(0.3F * fadeIn * fadeIn * Math.min(1, (1 - progress) * 10));
			inWater = level.getFluidState(cell).is(FluidTags.WATER);
		}

		@Override
		public int getLightColor(float partial)
		{
			return Math.max(80, super.getLightColor(partial));
		}

		@Override
		public void render(VertexConsumer output, Camera camera, float partial)
		{
			if (alpha > 0)
				billboard(output, camera, new Quaternionf(camera.rotation()).rotateZ(billowing), quadSize, quadSize, getLightColor(partial));
		}
	}

	private GadgetParticles() {}
}
