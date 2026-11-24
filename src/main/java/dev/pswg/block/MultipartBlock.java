package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Supplier;

/** One inventory and one drop for an assembly whose parts have their own shape and water state. */
public abstract class MultipartBlock<T extends BlockEntity> extends BaseEntityBlock implements SimpleWaterloggedBlock
{
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	private static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

	private final List<Part> parts;
	private final VoxelShape[][] shapes;
	private final Supplier<BlockEntityType<T>> entityType;
	private final BlockEntityTicker<T> ticker;

	protected MultipartBlock(Properties properties, List<Part> parts, Supplier<BlockEntityType<T>> entityType, BlockEntityTicker<T> ticker)
	{
		super(properties.pushReaction(PushReaction.BLOCK));
		this.parts = List.copyOf(parts);
		this.entityType = entityType;
		this.ticker = ticker;
		shapes = new VoxelShape[parts.size()][4];
		for (int i = 0; i < parts.size(); i++)
			for (var direction : Direction.Plane.HORIZONTAL)
				shapes[i][direction.get2DDataValue()] = VoxelShapeUtil.rotateToFace(parts.get(i).shape(), direction);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(partProperty(), 0).setValue(WATERLOGGED, false));
	}

	protected abstract IntegerProperty partProperty();

	public final int partIndex(BlockState state) { return state.getValue(partProperty()); }
	public final int partCount() { return parts.size(); }
	public final BlockPos eastFacingOffset(int part) { return parts.get(part).offset(); }

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(FACING, WATERLOGGED, partProperty());
	}

	@Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
	@Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return shapes[partIndex(state)][state.getValue(FACING).get2DDataValue()];
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		var level = context.getLevel();
		var anchor = context.getClickedPos();
		var state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
		var properties = context.getItemInHand().getOrDefault(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY);
		state = properties.apply(state).setValue(partProperty(), 0);
		var player = context.getPlayer();
		var collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
		for (int i = 1; i < parts.size(); i++)
		{
			var pos = anchor.offset(offset(state, i));
			if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.hasChunkAt(pos)
					|| (player != null && !level.mayInteract(player, pos))
					|| !level.getBlockState(pos).canBeReplaced(BlockPlaceContext.at(context, pos, context.getClickedFace()))
					|| !level.isUnobstructed(placedPart(state, level, pos, i), pos, collision))
				return null;
		}
		return placedPart(state, level, anchor, 0);
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		var anchor = state.setValue(partProperty(), 0);
		if (anchor != state)
			level.setBlock(pos, anchor, Block.UPDATE_CLIENTS);
		for (int i = 1; i < parts.size(); i++)
		{
			var part = pos.offset(offset(anchor, i));
			level.setBlock(part, placedPart(anchor, level, part, i), Block.UPDATE_ALL);
		}
	}

	private BlockState placedPart(BlockState state, Level level, BlockPos pos, int index)
	{
		return state.setValue(partProperty(), index).setValue(WATERLOGGED, level.getFluidState(pos).is(Fluids.WATER));
	}

	private BlockPos offset(BlockState state, int index)
	{
		return eastFacingOffset(index).rotate(switch (state.getValue(FACING))
		{
			case NORTH -> Rotation.COUNTERCLOCKWISE_90;
			case SOUTH -> Rotation.CLOCKWISE_90;
			case WEST -> Rotation.CLOCKWISE_180;
			default -> Rotation.NONE;
		});
	}

	private boolean matches(BlockState other, BlockState state, int index)
	{
		return other.is(this) && partIndex(other) == index && other.getValue(FACING) == state.getValue(FACING);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return entityType != null && partIndex(state) == 0 ? entityType.get().create(pos, state) : null;
	}

	@Override
	public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type)
	{
		return ticker == null || level.isClientSide || partIndex(state) != 0 ? null : createTickerHelper(type, entityType.get(), ticker);
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos)
	{
		var anchor = pos.subtract(offset(state, partIndex(state)));
		return matches(level.getBlockState(anchor), state, 0) && level.getBlockEntity(anchor) instanceof MenuProvider menu ? menu : null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		var menu = getMenuProvider(state, level, pos);
		if (menu == null)
			return InteractionResult.PASS;
		if (!level.isClientSide)
			player.openMenu(menu);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving)
	{
		if (!replacement.is(this) && !level.isClientSide && !level.restoringBlockSnapshots)
		{
			if (partIndex(state) == 0)
			{
				Containers.dropContentsOnDestroy(state, replacement, level, pos);
				for (int i = 1; i < parts.size(); i++)
					removePart(level, pos.offset(offset(state, i)), state, i);
			}
			else
				removePart(level, pos.subtract(offset(state, partIndex(state))), state, 0);
		}
		super.onRemove(state, level, pos, replacement, moving);
	}

	private void removePart(Level level, BlockPos pos, BlockState state, int index)
	{
		var part = level.getBlockState(pos);
		if (matches(part, state, index))
			level.setBlock(pos, part.getFluidState().createLegacyBlock(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
	{
		if (state.getValue(WATERLOGGED))
			level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
		return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
	}

	@Override
	protected FluidState getFluidState(BlockState state)
	{
		return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
	}

	public record Part(BlockPos offset, VoxelShape shape) {}
}
