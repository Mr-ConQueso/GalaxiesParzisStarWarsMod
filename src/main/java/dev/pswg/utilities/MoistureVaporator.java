package dev.pswg.utilities;

import dev.pswg.block.MultipartBlock;
import dev.pswg.gadgets.WorkstationInventory;
import dev.pswg.gadgets.WorkstationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class MoistureVaporator extends MultipartBlock<MoistureVaporator.Inventory>
{
	// The original GX-8 mesh is just over eight blocks tall. Only occupied cells are reserved.
	private static final List<Part> PARTS = List.of(
			part(0, 0, 0,  0, 0, 0, 16, 16, 16),
			part(-1, 0, -1,  9, 0, 9, 16, 6, 16),
			part(-1, 0, 0,  5, 0, 0, 16, 16, 16),
			part(-1, 0, 1,  9, 0, 0, 16, 6, 7),
			part(0, 0, -1,  0, 0, 5.5, 16, 16, 16),
			part(0, 0, 1,  0, 0, 0, 16, 16, 10.5),
			part(1, 0, -1,  0, 0, 4, 7, 16, 16),
			part(1, 0, 0,  0, 0, 0, 7, 16, 16),
			part(1, 0, 1,  0, 0, 0, 7, 16, 12),
			part(-1, 1, -1,  14, 6, 14, 16, 16, 16),
			part(-1, 1, 0,  5, 0, 0, 16, 16, 16),
			part(-1, 1, 1,  14, 6, 0, 16, 16, 2),
			part(0, 1, -1,  0, 0, 5.5, 16, 16, 16),
			part(0, 1, 0,  0, 0, 0, 16, 6, 16),
			part(0, 1, 1,  0, 0, 0, 16, 16, 10.5),
			part(1, 1, -1,  0, 0, 4, 4.5, 16, 16),
			part(1, 1, 0,  0, 0, 0, 4, 16, 16),
			part(1, 1, 1,  0, 0, 0, 4.5, 16, 12),
			part(-1, 2, -1,  14, 0, 14, 16, 6, 16),
			part(-1, 2, 0,  5, 0, 0, 16, 16, 16),
			part(-1, 2, 1,  14, 0, 0, 16, 6, 2),
			part(0, 2, -1,  0, 0, 5.5, 16, 16, 16),
			part(0, 2, 0,  0, 5.5, 0, 16, 16, 16),
			part(0, 2, 1,  0, 0, 0, 16, 16, 10.5),
			part(1, 2, -1,  0, 0, 4, 4.5, 16, 16),
			part(1, 2, 0,  0, 0, 0, 4, 16, 16),
			part(1, 2, 1,  0, 0, 0, 4.5, 16, 12),
			part(-1, 3, -1,  15, 5.5, 15, 16, 13, 16),
			part(-1, 3, 0,  5, 0, 0, 16, 16, 16),
			part(-1, 3, 1,  15, 5.5, 0, 16, 13, 1),
			part(0, 3, -1,  0, 0, 5.5, 16, 16, 16),
			part(0, 3, 0,  0, 0, 0, 16, 16, 16),
			part(0, 3, 1,  0, 0, 0, 16, 16, 10.5),
			part(1, 3, -1,  0, 0, 4, 4.5, 16, 16),
			part(1, 3, 0,  0, 0, 0, 1, 13, 16),
			part(1, 3, 1,  0, 0, 0, 4.5, 16, 12),
			part(-1, 4, 0,  5, 0, 6, 9.5, 10.5, 10),
			part(0, 4, -1,  14.5, 0, 5.5, 16, 10, 8.5),
			part(0, 4, 0,  2, 0, 2, 14, 16, 14),
			part(0, 4, 1,  14.5, 0, 7.5, 16, 10, 10.5),
			part(1, 4, -1,  0, 0, 4, 4.5, 10.5, 10),
			part(1, 4, 1,  0, 0, 6, 4.5, 10.5, 12),
			part(0, 5, 0,  4, 0, 4, 13, 16, 12),
			part(0, 6, 0,  5, 0, 5, 11, 16, 11),
			part(-1, 7, 0,  13.5, 11, 7, 16, 16, 9),
			part(0, 7, -1,  11, 11, 13.5, 14.5, 16, 16),
			part(0, 7, 0,  0, 0, 0, 14, 16, 16),
			part(0, 7, 1,  11, 11, 0, 14.5, 16, 2.5),
			part(-1, 8, 0,  13.5, 0, 7, 16, 1, 9),
			part(0, 8, -1,  11, 0, 13.5, 14.5, 1, 16),
			part(0, 8, 0,  0, 0, 0, 14, 3, 16),
			part(0, 8, 1,  11, 0, 0, 14.5, 1, 2.5)
	);
	private static final IntegerProperty PART = IntegerProperty.create("part", 0, PARTS.size() - 1);

	public MoistureVaporator(Properties properties)
	{
		super(properties, PARTS, () -> LegacyUtilities.VAPORATOR_ENTITY.get(), Inventory::tick);
	}

	@Override protected IntegerProperty partProperty() { return PART; }

	public static boolean providesMoisture(BlockState state)
	{
		return state.getBlock() instanceof MoistureVaporator vaporator && vaporator.partIndex(state) == 0;
	}

	private static Part part(int x, int y, int z, double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
	{
		return new Part(new BlockPos(x, y, z), box(minX, minY, minZ, maxX, maxY, maxZ));
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
