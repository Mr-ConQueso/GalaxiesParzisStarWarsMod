package dev.pswg.utilities;

import dev.pswg.gadgets.WorkstationInventory;
import dev.pswg.gadgets.WorkstationMenu;
import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Objects;
import java.util.Optional;

public final class MoistureVaporator extends WaterloggedUtility
{
	private static final VoxelShape SHAPE = VoxelShapeUtil.getCenteredCube(10, 40);
	private static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public MoistureVaporator(Properties properties)
	{
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.EAST));
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { super.createBlockStateDefinition(builder); builder.add(FACING); }
	@Override public BlockState getStateForPlacement(BlockPlaceContext context) { return super.getStateForPlacement(context).setValue(FACING, context.getHorizontalDirection()); }
	@Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
	@Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
	@Override protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) { return SHAPE; }
	@Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new Inventory(pos, state); }
	@Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type)
	{
		return world.isClientSide ? null : createTickerHelper(type, LegacyUtilities.VAPORATOR_ENTITY.get(), Inventory::tick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit)
	{
		if (!world.isClientSide && world.getBlockEntity(pos) instanceof Inventory machine)
			player.openMenu(machine);
		return InteractionResult.sidedSuccess(world.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level world, BlockPos pos, BlockState replacement, boolean moving)
	{
		if (!state.is(replacement.getBlock()) && !world.isClientSide)
			Containers.dropContentsOnDestroy(state, replacement, world, pos);
		super.onRemove(state, world, pos, replacement, moving);
	}

	private static Optional<RecipeHolder<VaporatorRecipe>> findRecipe(Level world, ItemStack stack)
	{
		return world == null || stack.isEmpty() ? Optional.empty() : world.getRecipeManager().getRecipeFor(LegacyUtilities.VAPORATING.get(), new SingleRecipeInput(stack), world);
	}

	public static final class Inventory extends WorkstationInventory
	{
		private static final int[] INPUT = { 0 };
		private static final int[] OUTPUT = { 1 };
		private ResourceLocation recipeId;
		private int progress;
		private int duration;

		public Inventory(BlockPos pos, BlockState state) { super(LegacyUtilities.VAPORATOR_ENTITY.get(), pos, state, 2); }
		@Override protected Component getDefaultName() { return Component.translatable("container.pswg.moisture_vaporator.gx8"); }
		@Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUTPUT : INPUT; }
		@Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && findRecipe(level, stack).isPresent(); }
		@Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN && canPlaceItem(slot, stack); }
		@Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side == Direction.DOWN && slot == 1; }

		public int progress() { return progress; }

		@Override
		protected AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory slots)
		{
			return new Menu(id, slots, this, new DataSlot()
			{
				@Override public int get() { return duration == 0 ? 0 : (int) (1000L * progress / duration); }
				@Override public void set(int value) {}
			});
		}

		public static void tick(Level world, BlockPos pos, BlockState state, Inventory unit)
		{
			var input = unit.getItem(0);
			var match = findRecipe(world, input).orElse(null);
			if (match == null)
			{
				unit.reset();
				return;
			}
			var output = unit.getItem(1);
			var produced = match.value().assemble(new SingleRecipeInput(input), world.registryAccess());
			if (!output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, produced) || output.getCount() + produced.getCount() > output.getMaxStackSize()))
			{
				unit.reset();
				return;
			}
			int duration = match.value().duration();
			if (!Objects.equals(unit.recipeId, match.id()) || unit.duration != duration)
			{
				unit.recipeId = match.id();
				unit.duration = duration;
				unit.progress = 0;
			}
			if (++unit.progress >= duration)
			{
				unit.removeItem(0, 1);
				unit.setItem(1, produced.copyWithCount(output.getCount() + produced.getCount()));
				unit.progress = 0;
			}
			unit.setChanged();
		}

		private void reset()
		{
			if (recipeId != null)
			{
				recipeId = null;
				progress = duration = 0;
				setChanged();
			}
		}

		@Override
		protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.saveAdditional(tag, registries);
			if (recipeId != null)
			{
				tag.putString("recipe", recipeId.toString());
				tag.putInt("progress", progress);
				tag.putInt("duration", duration);
			}
		}

		@Override
		protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
		{
			super.loadAdditional(tag, registries);
			recipeId = ResourceLocation.tryParse(tag.getString("recipe"));
			duration = Math.max(0, tag.getInt("duration"));
			progress = Math.clamp(tag.getInt("progress"), 0, Math.max(0, duration - 1));
		}
	}

	public static final class Menu extends WorkstationMenu
	{
		private final DataSlot progress;
		public Menu(int id, net.minecraft.world.entity.player.Inventory player) { this(id, player, new SimpleContainer(2), DataSlot.standalone()); }

		private Menu(int id, net.minecraft.world.entity.player.Inventory player, Container machine, DataSlot progress)
		{
			super(LegacyUtilities.VAPORATOR_MENU.get(), id, machine);
			this.progress = addDataSlot(progress);
			addMachineSlot(0, 31, 35, 64, stack -> findRecipe(player.player.level(), stack).isPresent());
			addMachineSlot(1, 129, 35, 64, stack -> false);
			addPlayerSlots(player, 84);
		}

		public float progress() { return progress.get() * 0.001F; }
	}
}
