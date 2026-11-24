package dev.pswg.screen;

import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import dev.pswg.container.Crates;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class CorrugatedCrateMenu extends AbstractContainerMenu
{
	private final Container crate;
	private final int columns;

	public CorrugatedCrateMenu(int id, Inventory playerInventory)
	{
		this(id, playerInventory, new SimpleContainer(CorrugatedCrateBlockEntity.SIZE));
	}

	public CorrugatedCrateMenu(int id, Inventory playerInventory, Container crate)
	{
		this(Crates.MENU.get(), id, playerInventory, crate, 5);
	}

	public CorrugatedCrateMenu(MenuType<CorrugatedCrateMenu> type, int id, Inventory playerInventory, Container crate, int columns)
	{
		super(type, id);
		checkContainerSize(crate, columns * 3);
		this.crate = crate;
		this.columns = columns;
		crate.startOpen(playerInventory.player);
		int crateX = columns == 13 ? 8 : 44;
		int playerX = columns == 13 ? 44 : 8;
		for (int row = 0; row < 3; row++)
			for (int column = 0; column < columns; column++)
				addSlot(new Slot(crate, column + row * columns, column * 18 + crateX, row * 18 + 18));

		for (int row = 0; row < 3; row++)
			for (int column = 0; column < 9; column++)
				addSlot(new Slot(playerInventory, column + row * 9 + 9, column * 18 + playerX, row * 18 + 86));

		for (int column = 0; column < 9; column++)
			addSlot(new Slot(playerInventory, column, column * 18 + playerX, 144));
	}

	public int columns() { return columns; }

	@Override
	public boolean stillValid(Player player)
	{
		return crate.stillValid(player);
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index)
	{
		if (index < 0 || index >= slots.size())
			return ItemStack.EMPTY;
		var slot = slots.get(index);
		if (!slot.hasItem())
			return ItemStack.EMPTY;

		var stack = slot.getItem();
		var original = stack.copy();
		int size = crate.getContainerSize();
		if (index < size ? !moveItemStackTo(stack, size, slots.size(), true) : !moveItemStackTo(stack, 0, size, false))
			return ItemStack.EMPTY;

		if (stack.isEmpty())
			slot.setByPlayer(ItemStack.EMPTY);
		else
			slot.setChanged();
		slot.onTake(player, stack);
		return original;
	}

	@Override
	public void removed(Player player)
	{
		super.removed(player);
		crate.stopOpen(player);
	}
}
