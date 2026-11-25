package dev.pswg.block.plant;

import com.mojang.serialization.MapCodec;
import dev.pswg.particle.GalaxiesParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class SequoiaLeavesBlock extends LeavesBlock
{
	public static final MapCodec<SequoiaLeavesBlock> CODEC = simpleCodec(SequoiaLeavesBlock::new);
	// Neutralizes the pink palette of the 1.21.1 cherry sprite for Sequoia's falling leaves.
	private static final int FALLING_LEAF_TINT = 0xFF6FAD85;

	public SequoiaLeavesBlock(Properties properties)
	{
		super(properties);
	}

	@Override public MapCodec<? extends LeavesBlock> codec() { return CODEC; }

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
	{
		super.animateTick(state, level, pos, random);
		if (random.nextFloat() < 0.01F)
		{
			var below = pos.below();
			if (!isFaceFull(level.getBlockState(below).getCollisionShape(level, below), Direction.UP))
				ParticleUtils.spawnParticleBelow(level, pos, random, ColorParticleOption.create(GalaxiesParticles.SEQUOIA_LEAF.get(), FALLING_LEAF_TINT));
		}
	}
}
