package dev.pswg.blasters;

import dev.pswg.particle.GalaxiesParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

final class BlasterEffects
{
	static void impact(Level level, Vec3 contact, Vec3 incident, BlockPos block, Direction face)
	{
		var normal = Vec3.atLowerCornerOf(face.getNormal());
		var point = contact.add(normal.scale(0.005 + level.random.nextDouble() * 0.0005));
		var state = level.getBlockState(block);
		if (state.isAir() || state.liquid())
			return;

		spawn(level, GalaxiesParticles.SCORCH.get(), point, normal);
		var reflection = incident.subtract(normal.scale(2 * incident.dot(normal)));
		var debris = new BlockParticleOption(ParticleTypes.BLOCK, state).setPos(block);
		for (int i = 0; i < 16; i++)
		{
			var scatter = randomVector(level, 0.03);
			spawn(level, GalaxiesParticles.SPARK.get(), point, scattered(level, reflection, scatter, 0.3));
			if (i % 3 == 0)
				spawn(level, debris, point, scattered(level, reflection, scatter, 0.15));
			if (i % 2 == 0)
				spawn(level, ParticleTypes.SMOKE, point, scattered(level, reflection, scatter, 0.08));
		}
	}

	static void slugTrail(Level level, Vec3 from, Vec3 to)
	{
		for (int i = 0; i < 10; i++)
			spawn(level, GalaxiesParticles.SLUG_TRAIL.get(), from.lerp(to, i / 10D), randomVector(level, 0.01));
	}

	static void muzzle(Level level, Vec3 point, Vec3 forward)
	{
		for (int i = 0; i < 25; i++)
		{
			var scatter = randomVector(level, 0.03);
			spawn(level, GalaxiesParticles.SPARK.get(), point, scattered(level, forward, scatter, 0.6));
			if (i % 3 == 0)
				spawn(level, ParticleTypes.SMOKE, point, scattered(level, forward, scatter, 0.05));
		}
	}

	private static Vec3 randomVector(Level level, double scale)
	{
		return new Vec3(level.random.nextGaussian() * scale, level.random.nextGaussian() * scale, level.random.nextGaussian() * scale);
	}

	private static Vec3 scattered(Level level, Vec3 forward, Vec3 scatter, double speed)
	{
		return forward.scale(speed * (0.5 + 0.5 * level.random.nextDouble())).add(scatter);
	}

	private static void spawn(Level level, ParticleOptions particle, Vec3 point, Vec3 velocity)
	{
		level.addParticle(particle, point.x, point.y, point.z, velocity.x, velocity.y, velocity.z);
	}

	private BlasterEffects() {}
}
