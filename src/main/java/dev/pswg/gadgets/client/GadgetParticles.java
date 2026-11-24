package dev.pswg.gadgets.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.pswg.Galaxies;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.GasParticleOptions;
import dev.pswg.gadgets.SmokeCloud;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class GadgetParticles
{
	private static final Map<SmokeCloud, Map<BlockPos, Integer>> VISIBLE_CELLS = new WeakHashMap<>();

	// Gas uses overlapping translucent billboards. They must depth-test against the world
	// without hiding the cells behind them.
	private static final ParticleRenderType GAS_SHEET = new ParticleRenderType()
	{
		@Override
		public BufferBuilder begin(Tesselator tesselator, TextureManager textures)
		{
			RenderSystem.depthMask(false);
			RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES);
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
		}

		@Override public String toString() { return "pswg_gadgets:gas"; }
	};

	@EventBusSubscriber(modid = Galaxies.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void register(RegisterParticleProvidersEvent event)
		{
			event.registerSpriteSet(Demolitions.EXPLOSION_SMOKE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
					new ExplosionSmoke(level, x, y, z, dx, dy, dz, sprites));
			event.registerSpriteSet(Demolitions.FRAGMENTATION_WAVE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
					new FragmentationWave(level, x, y, z, sprites));
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
							pos.getX() + 0.025 + 0.95 * random.nextDouble(), pos.getY() + 0.025 + 0.95 * random.nextDouble(),
							pos.getZ() + 0.025 + 0.95 * random.nextDouble(), 0, 0, 0);
				}
			}
		}
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
			lifetime = 1400;
			pickSprite(sprites);
		}

		@Override
		public void tick()
		{
			xo = x;
			yo = y;
			zo = z;
			xd += random.nextDouble() * (random.nextBoolean() ? 0.002 : -0.002);
			zd += random.nextDouble() * (random.nextBoolean() ? 0.002 : -0.002);
			yd += 0.001;
			move(xd, yd, zd);
			if (++age > 50)
				setAlpha(Math.max(0, alpha - 0.0025F));
			if (age >= lifetime || alpha <= 0)
				remove();
		}

		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
	}

	private static final class FragmentationWave extends TextureSheetParticle
	{
		FragmentationWave(ClientLevel level, double x, double y, double z, SpriteSet sprites)
		{
			super(level, x, y, z);
			lifetime = 25;
			hasPhysics = false;
			quadSize = 0.95F + random.nextInt(6) / 5F;
			pickSprite(sprites);
		}

		@Override
		public AABB getRenderBoundingBox(float partial)
		{
			return getBoundingBox().inflate(16);
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
				height = quadSize * (float) Math.abs(1 - Math.pow((time - 4) / 6, 2));
			}
			else
			{
				width = height = 0.5F * (float) Math.pow(time / 15, 6);
			}
			float opacity = time <= 15 ? 1F : Mth.clamp(1 - 1.5F * (float) Math.pow(time / lifetime, 3), 0, 1);
			var center = new Vector3f((float) (x - camera.getPosition().x), (float) (y - camera.getPosition().y),
					(float) (z - camera.getPosition().z));
			for (int i = 0; i < 4; i++)
			{
				boolean right = i < 2;
				boolean top = i == 1 || i == 2;
				var vertex = new Vector3f(right ? width : -width, top ? height : -height, 0).rotate(camera.rotation()).add(center);
				output.addVertex(vertex.x, vertex.y, vertex.z).setUv(right ? getU1() : getU0(), top ? getV0() : getV1())
						.setColor(1F, 1F, 1F, opacity).setLight(LightTexture.FULL_BRIGHT);
			}
		}

		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
	}

	private static final class GasParticle extends TextureSheetParticle
	{
		private final SmokeCloud cloud;
		private final BlockPos cell;
		private final int slot;
		private final float spin;

		private GasParticle(ClientLevel level, double x, double y, double z, GasParticleOptions type, SpriteSet sprites)
		{
			super(level, x, y, z);
			cloud = level.getEntity(type.cloudId()) instanceof SmokeCloud gas ? gas : null;
			cell = BlockPos.containing(x, y, z);
			slot = type.slot();
			spin = (random.nextBoolean() ? 1 : -1) * (0.001F + random.nextFloat() * 0.003F);
			quadSize = 0.6F + random.nextFloat() * 0.4F;
			lifetime = cloud == null ? 120 : SmokeCloud.MAX_AGE;
			hasPhysics = false;
			setAlpha(0);
			if (type.getType() == Demolitions.NERVE_PARTICLES.get())
				setColor(1, 0.9F, 0.6F);
			pickSprite(sprites);
		}

		@Override
		public void tick()
		{
			int remaining = cloud == null ? lifetime - age : SmokeCloud.MAX_AGE - cloud.cloudAge();
			if (remaining <= 0 || cloud != null && (cloud.isRemoved() || cloud.visualCells().getOrDefault(cell, 0) <= slot))
			{
				remove();
				return;
			}
			age++;
			oRoll = roll;
			roll += spin;
			setAlpha(0.3F * Math.min(1, Math.min(age / 20F, remaining / 90F)));
		}

		@Override
		public int getLightColor(float partial)
		{
			int light = super.getLightColor(partial);
			return LightTexture.pack(Math.max(5, LightTexture.block(light)), LightTexture.sky(light));
		}

		@Override public ParticleRenderType getRenderType() { return GAS_SHEET; }
	}

	private GadgetParticles() {}
}
