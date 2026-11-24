package dev.pswg.blockEntity;

import dev.pswg.container.Crates;
import dev.pswg.screen.CorrugatedCrateMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

public final class CorrugatedCrateBlockEntity extends RandomizableContainerBlockEntity
{
	public static final int SIZE = 15;
	private final Supplier<MenuType<CorrugatedCrateMenu>> menu;
	private final String name;
	private final int columns;
	private NonNullList<ItemStack> inventory;

	public CorrugatedCrateBlockEntity(BlockPos pos, BlockState state)
	{
		this(Crates.ENTITY.get(), Crates.MENU, pos, state, "corrugated_crate", 5);
	}

	public CorrugatedCrateBlockEntity(BlockEntityType<?> type, Supplier<MenuType<CorrugatedCrateMenu>> menu,
	                                  BlockPos pos, BlockState state, String name, int columns)
	{
		super(type, pos, state);
		this.menu = menu;
		this.name = name;
		this.columns = columns;
		inventory = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
	}

	@Override
	public int getContainerSize()
	{
		return columns * 3;
	}

	@Override
	protected Component getDefaultName()
	{
		return Component.translatable("container.pswg." + name);
	}

	@Override
	protected NonNullList<ItemStack> getItems()
	{
		return inventory;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items)
	{
		inventory = items;
	}

	@Override
	protected CorrugatedCrateMenu createMenu(int id, Inventory playerInventory)
	{
		return new CorrugatedCrateMenu(menu.get(), id, playerInventory, this, columns);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		if (!trySaveLootTable(tag))
			ContainerHelper.saveAllItems(tag, inventory, registries);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		inventory = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
		if (!tryLoadLootTable(tag))
			ContainerHelper.loadAllItems(tag, inventory, registries);
	}
}
