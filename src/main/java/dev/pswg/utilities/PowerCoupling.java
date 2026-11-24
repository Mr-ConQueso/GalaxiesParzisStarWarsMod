package dev.pswg.utilities;

import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PowerCoupling extends WaterloggedUtility
{
	public static final DirectionProperty FACING = BlockStateProperties.FACING;
	public static final IntegerProperty POWER = BlockStateProperties.POWER;
	private static final VoxelShape SHAPE = Block.box(0, 5.5, 5.5, 4, 10.5, 10.5);

	public PowerCoupling(Properties properties)
	{
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST).setValue(POWER, 0));
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(FACING, POWER); }
	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return VoxelShapeUtil.rotateToFace(SHAPE, state.getValue(FACING)); }
	@Override protected boolean isSignalSource(BlockState state) { return true; }
	@Override protected int getSignal(BlockState state, BlockGetter world, BlockPos pos, Direction face) { return face == state.getValue(FACING).getOpposite() ? state.getValue(POWER) : 0; }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Links(pos, state); }
	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type)
	{
		return world.isClientSide ? null : createTickerHelper(type, LegacyUtilities.COUPLING_ENTITY.get(), Links::tick);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		var state = super.getStateForPlacement(context).setValue(FACING, context.getClickedFace());
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos)
	{
		var direction = state.getValue(FACING);
		var support = pos.relative(direction.getOpposite());
		return world.getBlockState(support).isFaceSturdy(world, support, direction);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor world, BlockPos pos, BlockPos neighborPos)
	{
		var updated = super.updateShape(state, direction, neighbor, world, pos, neighborPos);
		if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(world, pos))
			return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
		return updated;
	}

	public static final class Cable extends Item
	{
		Cable() { super(new Properties()); }

		@Override
		public InteractionResult useOn(UseOnContext context)
		{
			var world = context.getLevel();
			var pos = context.getClickedPos();
			var player = context.getPlayer();
			if (!world.getBlockState(pos).is(LegacyUtilities.COUPLING.get()))
				return InteractionResult.PASS;
			if (world instanceof ServerLevel server && player != null && player.mayBuild())
			{
				var cable = context.getItemInHand();
				var component = LegacyUtilities.CABLE_SOURCE.get();
				var selected = cable.remove(component);
				if (selected == null)
					cable.set(component, GlobalPos.of(world.dimension(), pos));
				else if (selected.dimension() == world.dimension() && !selected.pos().equals(pos) && server.hasChunkAt(selected.pos())
						&& server.mayInteract(player, selected.pos()) && server.getBlockEntity(selected.pos()) instanceof Links source
						&& server.getBlockEntity(pos) instanceof Links target)
				{
					if (player.isSecondaryUseActive()) source.disconnect(target);
					else source.connect(target);
				}
				world.playSound(null, pos, SoundEvents.COPPER_HIT, SoundSource.BLOCKS, 0.65F, 1.2F);
			}
			return InteractionResult.sidedSuccess(world.isClientSide);
		}

		@Override public boolean isFoil(ItemStack stack) { return stack.has(LegacyUtilities.CABLE_SOURCE.get()); }

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
		{
			var selected = stack.get(LegacyUtilities.CABLE_SOURCE.get());
			if (selected != null)
				tooltip.add(Component.translatable("tooltip.pswg.cable_source", selected.pos().getX(), selected.pos().getY(), selected.pos().getZ()));
		}
	}

	public static final class Links extends BlockEntity
	{
		private static final int MAX_LINKS = 32;
		private static final int MAX_NETWORK_SIZE = 256;
		private static final int MAX_SPAN = 64;
		private final Set<BlockPos> outgoing = new LinkedHashSet<>();
		private final Set<BlockPos> incoming = new LinkedHashSet<>();

		public Links(BlockPos pos, BlockState state) { super(LegacyUtilities.COUPLING_ENTITY.get(), pos, state); }
		public Set<BlockPos> outgoing() { return Collections.unmodifiableSet(outgoing); }

		public boolean connect(Links target)
		{
			if (level == null || level.isClientSide || level != target.level || target == this || outgoing.size() >= MAX_LINKS
					|| target.incoming.size() >= MAX_LINKS || worldPosition.distSqr(target.worldPosition) > MAX_SPAN * MAX_SPAN
					|| target.outgoing.contains(worldPosition) || !outgoing.add(target.worldPosition))
				return false;
			target.incoming.add(worldPosition);
			changed();
			target.changed();
			return true;
		}

		public void disconnect(Links target)
		{
			if (outgoing.remove(target.worldPosition))
			{
				target.incoming.remove(worldPosition);
				changed();
				target.changed();
			}
		}

		private void changed()
		{
			setChanged();
			if (level != null)
				level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}

		public static void tick(Level world, BlockPos pos, BlockState state, Links node)
		{
			if (world.getGameTime() % 4 != Math.floorMod(pos.getX() + pos.getZ(), 4))
				return;
			boolean changed = node.outgoing.removeIf(target -> world.hasChunkAt(target) && !(world.getBlockEntity(target) instanceof Links));
			changed |= node.incoming.removeIf(source -> world.hasChunkAt(source) && !(world.getBlockEntity(source) instanceof Links links && links.outgoing.contains(pos)));
			if (changed) node.changed();
			int power = node.networkPower(world);
			if (power != state.getValue(POWER))
				world.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_ALL);
		}

		private int networkPower(Level world)
		{
			int max = 0;
			var seen = new HashSet<BlockPos>();
			var pending = new ArrayDeque<Links>();
			seen.add(worldPosition);
			pending.add(this);
			while (!pending.isEmpty() && max < 15)
			{
				var node = pending.removeFirst();
				var front = node.getBlockState().getValue(FACING);
				for (var side : Direction.values())
				{
					var neighbor = node.worldPosition.relative(side);
					if (side != front && world.hasChunkAt(neighbor) && !world.getBlockState(neighbor).is(LegacyUtilities.COUPLING.get()))
						max = Math.max(max, world.getSignal(neighbor, side));
				}
				for (var source : node.incoming)
					if (seen.size() < MAX_NETWORK_SIZE && seen.add(source) && world.hasChunkAt(source)
							&& world.getBlockEntity(source) instanceof Links upstream && upstream.outgoing.contains(node.worldPosition))
						pending.addLast(upstream);
			}
			return max;
		}

		@Override
		protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.saveAdditional(tag, registries);
			tag.putLongArray("targets", outgoing.stream().mapToLong(BlockPos::asLong).toArray());
			tag.putLongArray("sources", incoming.stream().mapToLong(BlockPos::asLong).toArray());
		}

		@Override
		protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.loadAdditional(tag, registries);
			readLinks(tag.getLongArray("targets"), outgoing);
			readLinks(tag.getLongArray("sources"), incoming);
		}

		private void readLinks(long[] packed, Set<BlockPos> into)
		{
			into.clear();
			for (long value : packed)
			{
				if (into.size() == MAX_LINKS) break;
				var target = BlockPos.of(value);
				if (!worldPosition.equals(target) && worldPosition.distSqr(target) <= MAX_SPAN * MAX_SPAN)
					into.add(target);
			}
		}

		@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
		@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	}
}
