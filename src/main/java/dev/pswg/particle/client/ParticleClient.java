package dev.pswg.particle.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pswg.Galaxies;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.Workstations;
import dev.pswg.particle.GalaxiesParticles;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.RisingParticle;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
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
		event.registerSpriteSet(GalaxiesParticles.SCORCH.get(), SurfaceDecalParticle::blasterFactory);
		event.registerSpriteSet(Demolitions.SCORCH.get(), sprites -> SurfaceDecalParticle.factory(sprites, 0.25F, 100, 200, 4));
		event.registerSpriteSet(Demolitions.DENSE_SCORCH.get(), sprites -> SurfaceDecalParticle.factory(sprites, 0.25F, 100, 200, 4));
		event.registerSpriteSet(Workstations.LASER_CUT.get(), sprites -> SurfaceDecalParticle.factory(sprites, 1 / 32F, 350, 400, 1));

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

	private static final class SlugTrail extends SimpleAnimatedParticle
	{
		private SlugTrail(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites)
		{
			super(level, x, y, z, sprites, 0F);
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
				setAlpha(1F);
		}
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

		@Override public float getQuadSize(float partial) { return 7 * Mth.sin((age + partial - 1) * 0.05F * Mth.PI); }
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

		@Override
		public int getLightColor(float partial)
		{
			int light = super.getLightColor(partial);
			int boost = (int) (240 * Mth.clamp((age + partial) / lifetime, 0, 1));
			return Math.min(240, (light & 255) + boost) | (light & 0xFF0000);
		}

		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
	}

	private static final class SequoiaLeaf extends TextureSheetParticle
	{
		private static final int LIFETIME = 300;
		private float spin = (float) Math.toRadians(random.nextBoolean() ? -30 : 30);
		private final float spinAcceleration = (float) Math.toRadians(random.nextBoolean() ? -5 : 5);
		private final double swirlPeriod = Math.toRadians(1000 + random.nextFloat() * 3000);

		private SequoiaLeaf(ClientLevel level, double x, double y, double z, SpriteSet sprites, ColorParticleOption color)
		{
			super(level, x, y, z);
			lifetime = LIFETIME;
			quadSize = random.nextBoolean() ? 0.1F : 0.15F;
			setSize(quadSize, quadSize);
			yd = -0.021F;
			setColor(color.getRed(), color.getGreen(), color.getBlue());
			pickSprite(sprites);
		}

		@Override
		public void tick()
		{
			xo = x;
			yo = y;
			zo = z;
			if (age++ >= lifetime)
			{
				remove();
				return;
			}
			float progress = (float) age / LIFETIME;
			xd += progress * Math.cos(progress * swirlPeriod) * 0.025F;
			zd += progress * Math.sin(progress * swirlPeriod) * 0.025F;
			yd -= 0.07F * 1.2F * 0.0025F;
			spin += spinAcceleration / 20;
			oRoll = roll;
			roll += spin / 20;
			move(xd, yd, zd);
			if (onGround || age > 1 && (xd == 0 || zd == 0))
				remove();
		}

		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }
	}

	private ParticleClient() {}
}
