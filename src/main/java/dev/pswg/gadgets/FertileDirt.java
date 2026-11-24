package dev.pswg.gadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

final class FertileDirt extends Block
{
	FertileDirt(Properties properties)
	{
		super(properties);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		var target = pos.above();
		var plant = level.getBlockState(target);
		if (plant.getBlock() instanceof BonemealableBlock fertilizable && fertilizable.isValidBonemealTarget(level, target, plant)
				&& fertilizable.isBonemealSuccess(level, random, target, plant))
			fertilizable.performBonemeal(level, random, target, plant);
	}
}
