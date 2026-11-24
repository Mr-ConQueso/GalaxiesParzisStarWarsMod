package dev.pswg.particle.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pswg.Galaxies;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.Workstations;
import dev.pswg.particle.GalaxiesParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CherryParticle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = Galaxies.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ParticleClient
{
	@SubscribeEvent
	public static void register(RegisterParticleProvidersEvent event)
	{
		event.registerSpriteSet(GalaxiesParticles.SCORCH.get(), sprites -> SurfaceDecalParticle.factory(sprites, 0.1F, 200, 200, Mth.TWO_PI));
		event.registerSpriteSet(Demolitions.SCORCH.get(), sprites -> SurfaceDecalParticle.factory(sprites, 0.25F, 100, 200, Mth.HALF_PI));
		event.registerSpriteSet(Demolitions.DENSE_SCORCH.get(), sprites -> SurfaceDecalParticle.factory(sprites, 0.25F, 100, 200, Mth.HALF_PI));
		event.registerSpriteSet(Workstations.LASER_CUT.get(), sprites -> SurfaceDecalParticle.factory(sprites, 1 / 32F, 350, 400, 0F));

		event.registerSpriteSet(GalaxiesParticles.SPARK.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
				new SparkParticle(level, x, y, z, dx, dy, dz, sprites, SparkParticle.Style.BLASTER));
		event.registerSpriteSet(Demolitions.FRAGMENTATION_SPARK.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
				new SparkParticle(level, x, y, z, dx, dy, dz, sprites, SparkParticle.Style.FRAGMENTATION));
		event.registerSpriteSet(Demolitions.WIRE_LASER.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
				new SparkParticle(level, x, y, z, dx, dy, dz, sprites, SparkParticle.Style.TRIPWIRE));

		event.registerSpriteSet(GalaxiesParticles.SLUG_TRAIL.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
				new SlugTrail(level, x, y, z, dx, dy, dz, sprites));
		event.registerSpecial(GalaxiesParticles.WAKE.get(), (option, level, x, y, z, dx, dy, dz) ->
				option.getState().isAir() ? null : new WakeParticles.TerrainWake(option, level, x, y, z, dx, dy, dz));
		event.registerSpriteSet(GalaxiesParticles.WATER_WAKE.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
				new WakeParticles.WaterWake(level, x, y, z, dx, dy, dz, sprites));

		event.registerSpriteSet(GalaxiesParticles.SMALL_FLASH.get(), sprites -> (color, level, x, y, z, dx, dy, dz) ->
				new SmallFlash(level, x, y, z, color, sprites));
		event.registerSpriteSet(GalaxiesParticles.SHORT_FLAME.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
				new ShortFlame(level, x, y, z, dx, dy, dz, sprites, 1));
		event.registerSpriteSet(GalaxiesParticles.SMALL_SHORT_FLAME.get(), sprites -> (type, level, x, y, z, dx, dy, dz) ->
				new ShortFlame(level, x, y, z, dx, dy, dz, sprites, 0.5F));
		event.registerSpriteSet(GalaxiesParticles.SEQUOIA_LEAF.get(), sprites -> (color, level, x, y, z, dx, dy, dz) ->
				new SequoiaLeaf(level, x, y, z, sprites, color));
	}

	private static final class SlugTrail extends TextureSheetParticle
	{
		private final SpriteSet sprites;

		private SlugTrail(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites)
		{
			super(level, x, y, z);
			this.sprites = sprites;
			friction = 0.92F;
			quadSize = 0.2F + random.nextFloat() * 0.2F;
			lifetime = (int) (quadSize * 12 / (random.nextFloat() * 0.8 + 0.2));
			setColor(0.6F, 0.6F, 0.6F);
			setParticleSpeed(dx, dy, dz);
			setSpriteFromAge(sprites);
		}

		@Override
		public void tick()
		{
			super.tick();
			if (isAlive())
				setSpriteFromAge(sprites);
		}

		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
	}

	private static final class SmallFlash extends TextureSheetParticle
	{
		private SmallFlash(ClientLevel level, double x, double y, double z, ColorParticleOption color, SpriteSet sprites)
		{
			super(level, x, y, z);
			lifetime = 4;
			hasPhysics = false;
			setColor(color.getRed(), color.getGreen(), color.getBlue());
			pickSprite(sprites);
		}

		@Override
		public void render(VertexConsumer output, Camera camera, float partial)
		{
			setAlpha(0.6F - (age + partial - 1) * 0.125F);
			super.render(output, camera, partial);
		}

		@Override public float getQuadSize(float partial) { return 7 * Mth.sin(Math.max(0, age + partial - 1) * 0.05F * Mth.PI); }
		@Override public int getLightColor(float partial) { return LightTexture.FULL_BRIGHT; }
		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
	}

	private static final class ShortFlame extends RisingParticle
	{
		private ShortFlame(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, float size)
		{
			super(level, x, y, z, dx, dy, dz);
			lifetime = random.nextIntBetweenInclusive(2, 5);
			hasPhysics = false;
			scale(size);
			pickSprite(sprites);
		}

		@Override
		public float getQuadSize(float partial)
		{
			float progress = (age + partial) / lifetime;
			return quadSize * (1 - progress * progress * 0.5F);
		}

		@Override public int getLightColor(float partial) { return LightTexture.FULL_BRIGHT; }
		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
	}

	private static final class SequoiaLeaf extends CherryParticle
	{
		private SequoiaLeaf(ClientLevel level, double x, double y, double z, SpriteSet sprites, ColorParticleOption color)
		{
			super(level, x, y, z, sprites);
			setColor(color.getRed(), color.getGreen(), color.getBlue());
		}
	}

	private ParticleClient() {}
}
