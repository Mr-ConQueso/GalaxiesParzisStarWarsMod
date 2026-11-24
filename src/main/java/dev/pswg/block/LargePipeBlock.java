package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.UnaryOperator;

public final class LargePipeBlock extends WaterloggableBlock
{
	private static final MapCodec<LargePipeBlock> CODEC = simpleCodec(LargePipeBlock::new);
	private static final VoxelShape SHAPE = box(2, 2, 2, 14, 14, 14);

	public LargePipeBlock(Properties properties)
	{
		super(properties);
	}

	@Override protected MapCodec<? extends Block> codec() { return CODEC; }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(PipeBlock.PROPERTY_BY_DIRECTION.values().toArray(BooleanProperty[]::new));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		var state = super.getStateForPlacement(context);
		for (var direction : Direction.values())
			state = state.setValue(port(direction), context.getLevel().getBlockState(context.getClickedPos().relative(direction)).is(this));
		return state;
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction face, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
	{
		var updated = super.updateShape(state, face, neighbor, level, pos, neighborPos);
		return neighbor.is(this) && neighbor.getValue(port(face.getOpposite())) ? updated.setValue(port(face), true) : updated;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		if (!player.mayBuild() || player.isShiftKeyDown())
			return InteractionResult.PASS;
		if (!level.isClientSide)
			level.setBlockAndUpdate(pos, state.cycle(port(hit.getDirection())));
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return transform(state, rotation::rotate); }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return transform(state, mirror::mirror); }

	private static BlockState transform(BlockState source, UnaryOperator<Direction> facing)
	{
		var result = source;
		for (var direction : Direction.values())
			result = result.setValue(port(facing.apply(direction)), source.getValue(port(direction)));
		return result;
	}

	private static BooleanProperty port(Direction direction)
	{
		return PipeBlock.PROPERTY_BY_DIRECTION.get(direction);
	}
}
