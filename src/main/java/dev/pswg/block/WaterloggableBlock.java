package dev.pswg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public class WaterloggableBlock extends Block implements SimpleWaterloggedBlock
{
	public WaterloggableBlock(Properties settings)
	{
		super(settings);
		this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.WATERLOGGED, false));
	}

	@Override
	public FluidState getFluidState(BlockState state)
	{
		return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(BlockStateProperties.WATERLOGGED);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos)
	{
		if (state.getValue(BlockStateProperties.WATERLOGGED))
			world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));

		return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext ctx)
	{
		var fluidState = ctx.getLevel().getFluidState(ctx.getClickedPos());
		return super.getStateForPlacement(ctx).setValue(BlockStateProperties.WATERLOGGED, fluidState.getType() == Fluids.WATER);
	}
}
