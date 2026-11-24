package dev.pswg.gadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class WorkstationInventory extends BaseContainerBlockEntity implements WorldlyContainer
{
	private static final int[] NO_AUTOMATION_SLOTS = {};
	private NonNullList<ItemStack> items;

	protected WorkstationInventory(BlockEntityType<?> type, BlockPos pos, BlockState state, int size)
	{
		super(type, pos, state);
		items = NonNullList.withSize(size, ItemStack.EMPTY);
	}

	@Override
	public int getContainerSize()
	{
		return items.size();
	}

	@Override
	protected NonNullList<ItemStack> getItems()
	{
		return items;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> items)
	{
		this.items = items;
	}

	@Override
	public int[] getSlotsForFace(Direction side)
	{
		return NO_AUTOMATION_SLOTS;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side)
	{
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side)
	{
		return false;
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		items.clear();
		ContainerHelper.loadAllItems(tag, items, registries);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		ContainerHelper.saveAllItems(tag, items, registries);
	}
}
