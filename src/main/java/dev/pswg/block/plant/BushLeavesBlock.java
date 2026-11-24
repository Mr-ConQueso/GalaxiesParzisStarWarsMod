package dev.pswg.block.plant;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BushLeavesBlock extends LeavesBlock
{
	public static final MapCodec<BushLeavesBlock> CODEC = simpleCodec(BushLeavesBlock::new);

	@Override
	public MapCodec<BushLeavesBlock> codec()
	{
		return CODEC;
	}

	static
	{
		FACING = BlockStateProperties.FACING;
	}

	public static final EnumProperty<Direction> FACING;
	protected final VoxelShape NORTH_SHAPE;
	protected final VoxelShape SOUTH_SHAPE;
	protected final VoxelShape EAST_SHAPE;
	protected final VoxelShape WEST_SHAPE;
	protected final VoxelShape UP_SHAPE;
	protected final VoxelShape DOWN_SHAPE;
	
	public BushLeavesBlock(BlockBehaviour.Properties settings)
	{
		super(settings);
		this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.UP));
		int height = 8;
		int xzOffset = 3;
		this.UP_SHAPE = Block.box(xzOffset, 0.0D, xzOffset, 16 - xzOffset, height, 16 - xzOffset);
		this.DOWN_SHAPE = Block.box(xzOffset, 16 - height, xzOffset, 16 - xzOffset, 16.0D, 16 - xzOffset);
		this.NORTH_SHAPE = Block.box(xzOffset, xzOffset, 16 - height, 16 - xzOffset, 16 - xzOffset, 16.0D);
		this.SOUTH_SHAPE = Block.box(xzOffset, xzOffset, 0.0D, 16 - xzOffset, 16 - xzOffset, height);
		this.EAST_SHAPE = Block.box(0.0D, xzOffset, xzOffset, height, 16 - xzOffset, 16 - xzOffset);
		this.WEST_SHAPE = Block.box(16 - height, xzOffset, xzOffset, 16.0D, 16 - xzOffset, 16 - xzOffset);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
	{
		var direction = state.getValue(FACING);
		return switch (direction)
		{
			case NORTH -> this.NORTH_SHAPE;
			case SOUTH -> this.SOUTH_SHAPE;
			case EAST -> this.EAST_SHAPE;
			case WEST -> this.WEST_SHAPE;
			case DOWN -> this.DOWN_SHAPE;
			default -> this.UP_SHAPE;
		};
	}

	@Override
	public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos)
	{
		var direction = state.getValue(FACING);
		var blockPos = pos.relative(direction.getOpposite());
		return world.getBlockState(blockPos).isFaceSturdy(world, blockPos, direction);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor world, BlockPos pos, BlockPos neighborPos)
	{
		return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(world, pos) ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighborState, world, pos, neighborPos);
	}

	@Override
	@Nullable
	public BlockState getStateForPlacement(BlockPlaceContext ctx)
	{
		return super.getStateForPlacement(ctx).setValue(FACING, ctx.getClickedFace());
	}

	@Override
	public BlockState rotate(BlockState state, Rotation rotation)
	{
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState state, Mirror mirror)
	{
		return state.rotate(mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}
}
