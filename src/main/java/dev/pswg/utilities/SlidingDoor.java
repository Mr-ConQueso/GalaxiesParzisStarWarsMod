package dev.pswg.utilities;

import dev.pswg.item.DoorInsertItem;
import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class SlidingDoor extends WaterloggedUtility
{
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
	public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
	public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
	public static final IntegerProperty INSERT = IntegerProperty.create("door_color", 0, 16);
	private static final VoxelShape[][][] OUTLINE = geometry(true);
	private static final VoxelShape[][][] COLLISION = geometry(false);

	public SlidingDoor(Properties properties)
	{
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(HALF, DoubleBlockHalf.LOWER)
				.setValue(OPEN, true).setValue(POWERED, false).setValue(INSERT, 0));
	}

	private static VoxelShape[][][] geometry(boolean outline)
	{
		var shapes = new VoxelShape[2][2][4];
		var posts = Shapes.or(Block.box(4, 0, 0, 12, 16, 1), Block.box(4, 0, 15, 12, 16, 16));
		for (var half : DoubleBlockHalf.values())
		{
			boolean upper = half == DoubleBlockHalf.UPPER;
			var trim = Block.box(4, upper ? 15 : 0, 0, 12, upper ? 16 : 1, 16);
			var frame = outline ? Shapes.or(posts, trim) : posts;
			for (int open = 0; open <= 1; open++)
			{
				var panel = Block.box(6, upper ? 0 : 1, open == 0 ? 1 : 14.5, 10, upper ? 15 : 16, 15);
				var shape = Shapes.or(frame, panel);
				for (int rotation = 0; rotation < 4; rotation++)
					shapes[half.ordinal()][open][rotation] = VoxelShapeUtil.rotate(shape, rotation);
			}
		}
		return shapes;
	}

	public static DyeColor color(BlockState state)
	{
		int color = state.getValue(INSERT);
		return color == 0 ? null : DyeColor.byId(color - 1);
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(FACING, HALF, OPEN, POWERED, INSERT); }
	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return shape(state, OUTLINE); }
	@Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return shape(state, COLLISION); }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new Animation(pos, state) : null; }
	@Override protected boolean isPathfindable(BlockState state, PathComputationType path) { return state.getValue(OPEN) && path != PathComputationType.WATER; }

	private static VoxelShape shape(BlockState state, VoxelShape[][][] shapes)
	{
		int rotation = (state.getValue(FACING).get2DDataValue() + 1) % 4;
		return shapes[state.getValue(HALF).ordinal()][state.getValue(OPEN) ? 1 : 0][rotation];
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos)
	{
		var below = pos.below();
		return state.getValue(HALF) == DoubleBlockHalf.UPPER ? level.getBlockState(below).is(this)
				: level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		var pos = context.getClickedPos();
		var world = context.getLevel();
		if (pos.getY() >= world.getMaxBuildHeight() - 1 || !world.getBlockState(pos.above()).canBeReplaced(context))
			return null;
		return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection())
				.setValue(POWERED, world.hasNeighborSignal(pos) || world.hasNeighborSignal(pos.above()));
	}

	@Override
	public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack)
	{
		var top = pos.above();
		world.setBlock(top, state.setValue(HALF, DoubleBlockHalf.UPPER).setValue(WATERLOGGED, world.getFluidState(top).is(Fluids.WATER)), Block.UPDATE_ALL);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction face, BlockState neighbor, LevelAccessor world, BlockPos pos, BlockPos neighborPos)
	{
		var updated = super.updateShape(state, face, neighbor, world, pos, neighborPos);
		boolean lower = state.getValue(HALF) == DoubleBlockHalf.LOWER;
		if (face == (lower ? Direction.UP : Direction.DOWN))
		{
			if (neighbor.is(this) && state.getValue(HALF) != neighbor.getValue(HALF))
				return neighbor.setValue(HALF, state.getValue(HALF)).setValue(WATERLOGGED, state.getValue(WATERLOGGED));
			return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		}
		if (lower && face == Direction.DOWN && !state.canSurvive(world, pos))
			return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		return updated;
	}

	@Override
	public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player)
	{
		if (!world.isClientSide && state.getValue(HALF) == DoubleBlockHalf.UPPER && (player.isCreative() || !player.hasCorrectToolForDrops(state)))
		{
			var bottom = pos.below();
			var lower = world.getBlockState(bottom);
			if (lower.is(this) && lower.getValue(HALF) == DoubleBlockHalf.LOWER)
				world.setBlock(bottom, lower.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(),
						Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
		}
		return super.playerWillDestroy(world, pos, state, player);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		if (color(state) != null || !(stack.getItem() instanceof DoorInsertItem insert))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (!world.isClientSide && player.mayBuild())
		{
			var bottom = lower(state, pos);
			var lower = world.getBlockState(bottom);
			if (lower.is(this) && color(lower) == null)
			{
				world.setBlock(bottom, lower.setValue(INSERT, insert.getColor().getId() + 1).setValue(OPEN, lower.getValue(POWERED)), Block.UPDATE_ALL);
				stack.consume(1, player);
			}
		}
		return ItemInteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if (color(state) == null)
			return InteractionResult.PASS;
		if (!world.isClientSide)
		{
			var bottom = lower(state, pos);
			var lower = world.getBlockState(bottom);
			if (lower.is(this)) change(world, bottom, lower, !lower.getValue(OPEN), lower.getValue(POWERED), player);
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected void neighborChanged(BlockState state, Level world, BlockPos pos, Block source, BlockPos sourcePos, boolean moving)
	{
		if (world.isClientSide || source == this) return;
		var bottom = lower(state, pos);
		var lower = world.getBlockState(bottom);
		if (!lower.is(this)) return;
		boolean powered = world.hasNeighborSignal(bottom) || world.hasNeighborSignal(bottom.above());
		if (powered != lower.getValue(POWERED)) change(world, bottom, lower, color(lower) == null || powered, powered, null);
	}

	private static BlockPos lower(BlockState state, BlockPos pos) { return state.getValue(HALF) == DoubleBlockHalf.LOWER ? pos : pos.below(); }

	private static void change(Level world, BlockPos pos, BlockState state, boolean open, boolean powered, Player player)
	{
		boolean moving = state.getValue(OPEN) != open;
		if (moving && world.getBlockEntity(pos) instanceof Animation animation)
			animation.begin(open);
		world.setBlock(pos, state.setValue(OPEN, open).setValue(POWERED, powered), Block.UPDATE_ALL);
		if (moving)
		{
			world.playSound(null, pos, open ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE, SoundSource.BLOCKS, 0.65F, 1.15F);
			world.gameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
		}
	}

	public static final class Animation extends BlockEntity
	{
		private long start = Long.MIN_VALUE;
		private float from;
		private boolean opening;

		public Animation(BlockPos pos, BlockState state) { super(LegacyUtilities.DOOR_ENTITY.get(), pos, state); }

		void begin(boolean open)
		{
			from = openness(0);
			opening = open;
			start = level.getGameTime();
			setChanged();
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}

		public float openness(float partial)
		{
			if (level == null || start == Long.MIN_VALUE || level.getGameTime() - start >= 10)
				return getBlockState().getValue(OPEN) ? 1 : 0;
			float t = Mth.clamp((level.getGameTime() - start + partial) / 10, 0, 1);
			return Mth.lerp(1 - (1 - t) * (1 - t) * (1 - t), from, opening ? 1 : 0);
		}

		@Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.saveAdditional(tag, registries);
			if (start != Long.MIN_VALUE)
			{
				tag.putLong("start", start);
				tag.putFloat("from", from);
				tag.putBoolean("opening", opening);
			}
		}
		@Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.loadAdditional(tag, registries);
			start = tag.contains("start") ? tag.getLong("start") : Long.MIN_VALUE;
			from = tag.getFloat("from");
			opening = tag.getBoolean("opening");
		}
		@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
		@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	}
}
