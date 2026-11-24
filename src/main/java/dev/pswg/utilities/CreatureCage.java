package dev.pswg.utilities;

import com.mojang.serialization.MapCodec;
import dev.pswg.mobs.Creatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;

public abstract class CreatureCage extends BaseEntityBlock
{
	protected CreatureCage(Properties properties) { super(properties); }
	@Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
	@Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Contents(pos, state); }

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
	{
		if (!(world.getBlockEntity(pos) instanceof Contents cage))
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		if (stack.is(Items.LEAD) && !cage.occupied())
		{
			var nearby = world.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(7), mob -> mob.getLeashHolder() == player && mob.isAlive() && !mob.isVehicle() && !mob.isPassenger());
			var nearest = nearby.stream().min(Comparator.comparingDouble(mob -> mob.distanceToSqr(Vec3.atCenterOf(pos)))).orElse(null);
			if (nearest != null)
			{
				if (world instanceof ServerLevel && cage.capture(nearest))
				{
					nearest.dropLeash(true, true);
					nearest.discard();
				}
				return ItemInteractionResult.sidedSuccess(world.isClientSide);
			}
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if (!(world.getBlockEntity(pos) instanceof Contents cage) || !cage.occupied() || !player.mayBuild() || !player.getMainHandItem().isEmpty())
			return InteractionResult.PASS;
		if (world instanceof ServerLevel server)
		{
			if (player.isSecondaryUseActive())
			{
				var carried = cage.carriedStack();
				var stored = cage.data;
				cage.clear();
				if (server.removeBlock(pos, false))
				{
					if (!player.addItem(carried)) player.drop(carried, false);
				}
				else
				{
					cage.data = stored;
					cage.update();
					return InteractionResult.FAIL;
				}
			}
			else if (!cage.release(server, false))
				return InteractionResult.FAIL;
		}
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState replacement, boolean moving)
	{
		if (!state.is(replacement.getBlock()) && world instanceof ServerLevel server && world.getBlockEntity(pos) instanceof Contents cage && cage.occupied())
		{
			if (!cage.release(server, true))
				Block.popResource(world, pos, cage.carriedStack());
		}
		super.onRemove(state, world, pos, replacement, moving);
	}

	public static final class Metal extends CreatureCage implements SimpleWaterloggedBlock
	{
		public Metal(Properties properties)
		{
			super(properties);
			registerDefaultState(defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, false));
		}

		@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(BlockStateProperties.WATERLOGGED); }
		@Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, context.getLevel().getFluidState(context.getClickedPos()).is(Fluids.WATER)); }
		@Override protected FluidState getFluidState(BlockState state) { return state.getValue(BlockStateProperties.WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state); }
		@Override
		protected BlockState updateShape(BlockState state, Direction face, BlockState neighbor, LevelAccessor world, BlockPos pos, BlockPos neighborPos)
		{
			if (state.getValue(BlockStateProperties.WATERLOGGED)) world.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(world));
			return super.updateShape(state, face, neighbor, world, pos, neighborPos);
		}
	}

	public static final class Terrarium extends CreatureCage
	{
		public static final IntegerProperty WATER = IntegerProperty.create("water", 0, 9);
		public Terrarium(Properties properties) { super(properties); registerDefaultState(defaultBlockState().setValue(WATER, 0)); }
		@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(WATER); }

		@Override
		protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
		{
			int water = state.getValue(WATER);
			if ((stack.is(Creatures.FAA_BUCKET.get()) || stack.is(Creatures.LAA_BUCKET.get())) && world.getBlockEntity(pos) instanceof Contents cage && !cage.occupied())
			{
				if (world instanceof ServerLevel server)
				{
					var fish = (stack.is(Creatures.FAA_BUCKET.get()) ? Creatures.FAA.get() : Creatures.LAA.get()).create(server);
					if (fish == null) return ItemInteractionResult.FAIL;
					fish.loadFromBucketTag(stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag());
					if (stack.has(DataComponents.CUSTOM_NAME)) fish.setCustomName(stack.get(DataComponents.CUSTOM_NAME));
					fish.setFromBucket(true);
					if (!cage.capture(fish)) return ItemInteractionResult.FAIL;
				}
				return transfer(stack, new ItemStack(Items.BUCKET), state.setValue(WATER, 9), world, pos, player, hand, SoundEvents.BUCKET_EMPTY_FISH);
			}
			if (stack.is(Items.WATER_BUCKET) && water == 0)
				return transfer(stack, new ItemStack(Items.BUCKET), state.setValue(WATER, 9), world, pos, player, hand, SoundEvents.BUCKET_EMPTY);
			if (stack.is(Items.BUCKET) && water == 9)
				return transfer(stack, new ItemStack(Items.WATER_BUCKET), state.setValue(WATER, 0), world, pos, player, hand, SoundEvents.BUCKET_FILL);
			if (stack.is(Items.POTION) && stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).is(Potions.WATER) && water <= 6)
				return transfer(stack, new ItemStack(Items.GLASS_BOTTLE), state.setValue(WATER, water + 3), world, pos, player, hand, SoundEvents.BOTTLE_EMPTY);
			if (stack.is(Items.GLASS_BOTTLE) && water >= 3)
				return transfer(stack, PotionContents.createItemStack(Items.POTION, Potions.WATER), state.setValue(WATER, water - 3), world, pos, player, hand, SoundEvents.BOTTLE_FILL);
			return super.useItemOn(stack, state, world, pos, player, hand, hit);
		}

		private static ItemInteractionResult transfer(ItemStack input, ItemStack output, BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, SoundEvent sound)
		{
			if (!world.isClientSide)
			{
				player.awardStat(Stats.ITEM_USED.get(input.getItem()));
				player.setItemInHand(hand, ItemUtils.createFilledResult(input, player, output));
				world.setBlockAndUpdate(pos, state);
				world.playSound(null, pos, sound, SoundSource.BLOCKS, 1, 1);
				world.gameEvent(player, GameEvent.FLUID_PLACE, pos);
			}
			return ItemInteractionResult.sidedSuccess(world.isClientSide);
		}
	}

	public static final class Contents extends BlockEntity
	{
		private CompoundTag data;
		private Mob preview;

		public Contents(BlockPos pos, BlockState state) { super(LegacyUtilities.CAGE_ENTITY.get(), pos, state); }
		public boolean occupied() { return data != null; }

		boolean capture(Mob mob)
		{
			if (occupied() || level == null || level.isClientSide || mob.level() != level || !mob.isAlive() || mob.isPassenger() || mob.isVehicle())
				return false;
			var tag = new CompoundTag();
			if (!mob.saveAsPassenger(tag)) return false;
			for (String key : new String[] { "UUID", "Leash", "leash", "Passengers", "Pos", "Motion", "Rotation", "PortalCooldown" })
				tag.remove(key);
			data = tag;
			update();
			return true;
		}

		public Mob preview()
		{
			if (preview == null && level != null && data != null && level.isClientSide)
			{
				preview = createMob(level);
				if (preview != null)
				{
					preview.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
					preview.setNoAi(true);
				}
			}
			return preview;
		}

		private Mob createMob(Level level)
		{
			return EntityType.create(data, level).filter(Mob.class::isInstance).map(Mob.class::cast).orElse(null);
		}

		public boolean release(ServerLevel world, boolean blockRemoved)
		{
			if (!occupied()) return false;
			var mob = createMob(world);
			if (mob == null) return false;
			var pos = worldPosition;
			for (var candidate : new BlockPos[] { blockRemoved ? pos : pos.above(), pos.north(), pos.south(), pos.east(), pos.west(), pos.above(2) })
			{
				if (!world.hasChunkAt(candidate) || !world.getWorldBorder().isWithinBounds(candidate)) continue;
				mob.moveTo(Vec3.atBottomCenterOf(candidate));
				if (world.noCollision(mob) && world.addFreshEntity(mob))
				{
					clear();
					return true;
				}
			}
			return false;
		}

		private ItemStack carriedStack()
		{
			var state = getBlockState();
			var stack = new ItemStack(state.getBlock());
			BlockItem.setBlockEntityData(stack, getType(), saveWithoutMetadata(level.registryAccess()));
			if (state.hasProperty(Terrarium.WATER))
				stack.set(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY.with(Terrarium.WATER, state));
			return stack;
		}

		private void clear() { data = null; preview = null; update(); }
		private void update()
		{
			setChanged();
			if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
		}

		@Override
		protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.saveAdditional(tag, registries);
			if (data != null) tag.put("containedEntity", data.copy());
		}

		@Override
		protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.loadAdditional(tag, registries);
			data = tag.contains("containedEntity", CompoundTag.TAG_COMPOUND) ? tag.getCompound("containedEntity").copy() : null;
			preview = null;
		}

		@Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
		@Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
	}
}
