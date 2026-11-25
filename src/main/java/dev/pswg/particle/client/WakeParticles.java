package dev.pswg.particle.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.BlockParticleOption;

final class WakeParticles
{
	private static float growth(int age, int life)
	{
		float progress = Math.min(1, 20F * age / life);
		return progress * progress * progress * progress;
	}

	static final class TerrainWake extends TerrainParticle
	{
		private final float maxSize;

		TerrainWake(BlockParticleOption option, ClientLevel level, double x, double y, double z, double horizontal, double vertical, double depth)
		{
			super(level, x, y, z, 0, 0, 0, option.getState());
			updateSprite(option.getState(), option.getPos());
			maxSize = 0.04F * (1 + random.nextFloat() * random.nextFloat() * 6);
			quadSize = 0;
			gravity = 0.5F;
			friction = 0.98F;
			setParticleSpeed(horizontal * (2 * random.nextDouble() - 1), vertical * (0.9 + 0.2 * random.nextDouble()),
					depth * (2 * random.nextDouble() - 1));
		}

		@Override
		public void tick()
		{
			quadSize = maxSize * growth(age, lifetime);
			super.tick();
		}
	}

	static final class WaterWake extends TextureSheetParticle
	{
		private final SpriteSet sprites;
		private final float maxSize;

		WaterWake(ClientLevel level, double x, double y, double z, double horizontal, double vertical, double depth, SpriteSet sprites)
		{
			super(level, x, y, z);
			this.sprites = sprites;
			float shade = 0.7F + random.nextFloat() * 0.3F;
			setColor(shade, shade, shade);
			maxSize = 0.1F * (1 + random.nextFloat() * random.nextFloat() * 6);
			quadSize = 0;
			lifetime = (int) (16 / (random.nextFloat() * 0.8 + 0.2)) + 2;
			gravity = 0.5F;
			friction = 0.98F;
			setParticleSpeed(horizontal * (2 * random.nextDouble() - 1), vertical * (0.9 + 0.2 * random.nextDouble()),
					depth * (2 * random.nextDouble() - 1));
			setSpriteFromAge(sprites);
		}

		@Override
		public void tick()
		{
			quadSize = maxSize * growth(age, lifetime);
			super.tick();
			if (isAlive())
				setSpriteFromAge(sprites);
		}

		@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_OPAQUE; }
	}

	private WakeParticles() {}
}
