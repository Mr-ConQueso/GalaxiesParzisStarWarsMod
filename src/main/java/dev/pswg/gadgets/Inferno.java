package dev.pswg.gadgets;

import dev.pswg.particle.GalaxiesParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

final class Inferno
{
	private static final int MAX_RADIUS = 8;
	private static final int FIRE_WAVE_TICKS = 15;
	private final Set<BlockPos> reached = new HashSet<>();

	void tick(ServerLevel level, GadgetCharge charge, int age)
	{
		if (age == 1 || age == 12)
		{
			int reach = age == 1 ? 4 : MAX_RADIUS;
			for (var entity : level.getEntitiesOfClass(LivingEntity.class, charge.getBoundingBox().inflate(reach),
					entity -> !entity.fireImmune() && entity.distanceToSqr(charge) <= reach * reach))
			{
				entity.igniteForTicks(age == 1 ? 100 : 300);
				entity.hurt(level.damageSources().inFire(), age == 1 ? 8 : 4);
			}
		}
		var center = charge.blockPosition();
		if (reached.add(center))
			burn(level, center, true);
		if (age % 2 == 0)
			spread(level, charge, center);
		if (age <= FIRE_WAVE_TICKS)
			flameWave(level, charge.position(), (double) age * MAX_RADIUS / FIRE_WAVE_TICKS);
	}

	private void spread(ServerLevel level, GadgetCharge charge, BlockPos center)
	{
		var next = new HashSet<BlockPos>();
		for (var visited : reached)
			for (var side : Direction.values())
			{
				var pos = visited.relative(side);
				if (center.distSqr(pos) < MAX_RADIUS * MAX_RADIUS && !reached.contains(pos) && level.hasChunkAt(pos))
					next.add(pos);
			}
		for (var pos : next)
		{
			var sides = new ArrayList<Direction>(6);
			for (var side : Direction.values())
				if (level.getBlockState(pos.relative(side)).isAir())
					sides.add(side);
			if (sides.isEmpty())
				continue;
			reached.add(pos);
			var state = level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty())
				continue;
			boolean inner = pos.closerToCenterThan(charge.position(), 4);
			burn(level, pos, inner);
			if (!level.isEmptyBlock(pos))
				for (var side : sides)
					scorch(level, pos, side, inner);
		}
	}

	private void flameWave(ServerLevel level, Vec3 origin, double radius)
	{
		for (var pos : reached)
		{
			var state = level.getBlockState(pos);
			if (state.isAir() || !state.getFluidState().isEmpty() || !pos.closerToCenterThan(origin, radius + 1)
					|| pos.closerToCenterThan(origin, radius))
				continue;
			for (var side : Direction.values())
				if (level.isEmptyBlock(pos.relative(side)))
					flames(level, pos, side);
		}
	}

	private static void flames(ServerLevel level, BlockPos pos, Direction side)
	{
		var normal = Vec3.atLowerCornerOf(side.getNormal());
		var at = Vec3.atCenterOf(pos).add(normal);
		double x = 1 - Math.abs(normal.x);
		double y = 1 - Math.abs(normal.y);
		double z = 1 - Math.abs(normal.z);
		level.sendParticles(GalaxiesParticles.SHORT_FLAME.get(), at.x, at.y, at.z, 2, x * 0.5, y * 0.5, z * 0.5, 0);
		level.sendParticles(GalaxiesParticles.SMALL_SHORT_FLAME.get(), at.x, at.y, at.z, 3, x, y, z, 0);
	}

	private static void burn(ServerLevel level, BlockPos pos, boolean inner)
	{
		var state = level.getBlockState(pos);
		if (state.is(Demolitions.INFERNO_CHAR))
			level.setBlockAndUpdate(pos, Demolitions.CHARRED.get().defaultBlockState());
		else if (state.is(Demolitions.INFERNO_DESTROY))
		{
			level.destroyBlock(pos, false);
			var at = Vec3.atCenterOf(pos);
			level.sendParticles(ParticleTypes.SMOKE, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.05);
		}
		else if (inner && state.is(BlockTags.DIRT))
			level.setBlockAndUpdate(pos, Demolitions.FERTILE.get().defaultBlockState());
	}

	private static void scorch(ServerLevel level, BlockPos pos, Direction side, boolean inner)
	{
		var normal = Vec3.atLowerCornerOf(side.getNormal());
		var tangent = Vec3.atLowerCornerOf((side.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP).getNormal());
		var bitangent = normal.cross(tangent);
		var center = Vec3.atCenterOf(pos).add(normal.scale(0.503));
		var particle = inner ? Demolitions.DENSE_SCORCH.get() : Demolitions.SCORCH.get();
		for (int i = 0; i < 4; i++)
		{
			var at = center.add(tangent.scale((i & 1) == 0 ? 0.25 : -0.25)).add(bitangent.scale((i & 2) == 0 ? 0.25 : -0.25));
			level.sendParticles(particle, at.x, at.y, at.z, 0, normal.x, normal.y, normal.z, 1);
		}
	}

	long[] save() { return reached.stream().mapToLong(BlockPos::asLong).toArray(); }

	void load(long[] packed, BlockPos center)
	{
		reached.clear();
		for (long value : packed)
		{
			var pos = BlockPos.of(value);
			if (pos.distSqr(center) < MAX_RADIUS * MAX_RADIUS) reached.add(pos);
		}
	}
}
