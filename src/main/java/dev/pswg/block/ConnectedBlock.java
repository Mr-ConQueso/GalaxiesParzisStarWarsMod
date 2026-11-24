package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ConnectedBlock extends Block
{
	public static final MapCodec<ConnectedBlock> CODEC = simpleCodec(ConnectedBlock::new);

	public ConnectedBlock(Properties properties)
	{
		super(properties);
		var state = defaultBlockState();
		for (var direction : Direction.values())
			state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), false);
		registerDefaultState(state);
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		for (var property : PipeBlock.PROPERTY_BY_DIRECTION.values())
			builder.add(property);
	}

	public static boolean isConnected(BlockState state, Direction direction)
	{
		return state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		var state = defaultBlockState();
		for (var direction : Direction.values())
			state = withNeighbor(state, direction, context.getLevel().getBlockState(context.getClickedPos().relative(direction)));
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor world,
	                                 BlockPos pos, BlockPos neighborPos)
	{
		return withNeighbor(state, direction, neighbor);
	}

	private BlockState withNeighbor(BlockState state, Direction direction, BlockState neighbor)
	{
		return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighbor.is(this));
	}

	@Override
	protected boolean skipRendering(BlockState state, BlockState adjacent, Direction direction)
	{
		return adjacent.is(this) || super.skipRendering(state, adjacent, direction);
	}

	public static class Glass extends ConnectedBlock
	{
		public static final MapCodec<Glass> CODEC = simpleCodec(Glass::new);

		public Glass(Properties properties)
		{
			super(properties);
		}

		@Override
		protected MapCodec<? extends Block> codec()
		{
			return CODEC;
		}

		@Override
		protected VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
		{
			return Shapes.empty();
		}

		@Override
		protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos)
		{
			return 1.0F;
		}

		@Override
		protected boolean propagatesSkylightDown(BlockState state, BlockGetter world, BlockPos pos)
		{
			return true;
		}
	}
}
