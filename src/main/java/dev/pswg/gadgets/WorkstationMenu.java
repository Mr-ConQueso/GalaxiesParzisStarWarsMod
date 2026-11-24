package dev.pswg.gadgets;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public abstract class WorkstationMenu extends AbstractContainerMenu
{
	protected final Container inventory;

	protected WorkstationMenu(MenuType<?> type, int id, Container inventory)
	{
		super(type, id);
		this.inventory = inventory;
	}

	protected void addMachineSlot(int index, int x, int y, int limit, Predicate<ItemStack> filter)
	{
		addSlot(new Slot(inventory, index, x, y)
		{
			@Override
			public boolean mayPlace(ItemStack stack)
			{
				return filter.test(stack);
			}

			@Override
			public int getMaxStackSize()
			{
				return limit;
			}
		});
	}

	protected void addPlayerSlots(Inventory playerInventory, int y)
	{
		addPlayerSlots(playerInventory, 8, y);
	}

	protected void addPlayerSlots(Inventory playerInventory, int x, int y)
	{
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < 9; column++)
				addSlot(new Slot(playerInventory, 9 + row * 9 + column, x + column * 18, y + row * 18));
		for (int column = 0; column < 9; column++)
			addSlot(new Slot(playerInventory, column, x + column * 18, y + 58));
	}

	@Override
	public boolean stillValid(Player player)
	{
		return inventory.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		if (index < 0 || index >= slots.size() || !slots.get(index).hasItem())
			return ItemStack.EMPTY;
		var slot = slots.get(index);
		var stack = slot.getItem();
		var original = stack.copy();
		int size = inventory.getContainerSize();
		boolean moved = index < size ? moveItemStackTo(stack, size, slots.size(), true) : moveToMachine(stack, size);
		if (!moved)
			return ItemStack.EMPTY;
		if (stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}

	private boolean moveToMachine(ItemStack stack, int size)
	{
		boolean moved = false;
		for (int i = 0; i < size && !stack.isEmpty(); i++)
			if (slots.get(i).mayPlace(stack))
				moved |= moveItemStackTo(stack, i, i + 1, false);
		return moved;
	}
}
