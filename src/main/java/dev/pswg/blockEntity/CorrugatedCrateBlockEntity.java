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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CorrugatedCrateBlockEntity extends RandomizableContainerBlockEntity
{
	public static final int SIZE = 15;
	private NonNullList<ItemStack> inventory = NonNullList.withSize(SIZE, ItemStack.EMPTY);

	public CorrugatedCrateBlockEntity(BlockPos pos, BlockState state)
	{
		super(Crates.ENTITY.get(), pos, state);
	}

	@Override
	public int getContainerSize()
	{
		return SIZE;
	}

	@Override
	protected Component getDefaultName()
	{
		return Component.translatable("container.pswg.corrugated_crate");
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
		return new CorrugatedCrateMenu(id, playerInventory, this);
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
		inventory = NonNullList.withSize(SIZE, ItemStack.EMPTY);
		if (!tryLoadLootTable(tag))
			ContainerHelper.loadAllItems(tag, inventory, registries);
	}
}
