package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ScaffoldingBlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class MetalScaffoldBlock extends ScaffoldingBlock
{
	private static final MapCodec<ScaffoldingBlock> CODEC = simpleCodec(MetalScaffoldBlock::new);
	private static final VoxelShape FRAME = Shapes.or(box(0, 0, 0, 2, 16, 2), box(0, 0, 14, 2, 16, 16),
			box(14, 0, 0, 16, 16, 2), box(14, 0, 14, 16, 16, 16), box(1, 14, 2, 2, 15, 14),
			box(14, 14, 2, 15, 15, 14), box(2, 14, 1, 14, 15, 2), box(2, 14, 14, 14, 15, 15));

	public MetalScaffoldBlock(Properties properties)
	{
		super(properties);
	}

	@Override public MapCodec<ScaffoldingBlock> codec() { return CODEC; }
	@Override public boolean isScaffolding(BlockState state, LevelReader level, BlockPos pos, LivingEntity entity) { return true; }

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return context.isHoldingItem(asItem()) ? Shapes.block() : FRAME;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		var level = context.getLevel();
		var pos = context.getClickedPos();
		return withDistance(defaultBlockState().setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER)), level, pos, distance(level, pos));
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		int distance = distance(level, pos);
		if (distance == STABILITY_MAX_DISTANCE)
		{
			if (state.getValue(DISTANCE) == STABILITY_MAX_DISTANCE)
				FallingBlockEntity.fall(level, pos, state);
			else
				level.destroyBlock(pos, true);
			return;
		}
		var updated = withDistance(state, level, pos, distance);
		if (updated != state)
			level.setBlockAndUpdate(pos, updated);
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
	{
		return distance(level, pos) < STABILITY_MAX_DISTANCE;
	}

	private BlockState withDistance(BlockState state, BlockGetter level, BlockPos pos, int distance)
	{
		return state.setValue(DISTANCE, distance).setValue(BOTTOM, distance > 0 && !level.getBlockState(pos.below()).is(this));
	}

	private int distance(BlockGetter level, BlockPos pos)
	{
		var below = pos.below();
		var support = level.getBlockState(below);
		int distance = STABILITY_MAX_DISTANCE;
		if (support.is(this))
			distance = support.getValue(DISTANCE);
		else if (support.isFaceSturdy(level, below, Direction.UP))
			return 0;

		for (var side : Direction.Plane.HORIZONTAL)
		{
			var neighbor = level.getBlockState(pos.relative(side));
			if (neighbor.is(this))
				distance = Math.min(distance, neighbor.getValue(DISTANCE) + 1);
		}
		return distance;
	}

	public static final class Item extends ScaffoldingBlockItem
	{
		public Item(MetalScaffoldBlock block, Properties properties)
		{
			super(block, properties);
		}

		@Override
		public BlockPlaceContext updatePlacementContext(BlockPlaceContext context)
		{
			var scaffold = (MetalScaffoldBlock) getBlock();
			var level = context.getLevel();
			var pos = context.getClickedPos();
			if (level.getBlockState(pos).is(scaffold))
				return super.updatePlacementContext(context);
			return scaffold.distance(level, pos) < STABILITY_MAX_DISTANCE ? context : null;
		}
	}
}
