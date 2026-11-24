package dev.pswg.gadgets.scrapping;

import dev.pswg.gadgets.WorkstationMenu;
import dev.pswg.gadgets.Workstations;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;

public final class ScrappingMenu extends WorkstationMenu
{
	private final ContainerData data;

	public ScrappingMenu(int id, Inventory player)
	{
		this(id, player, new SimpleContainer(ScrappingTable.SIZE), new SimpleContainerData(ScrappingTable.TOOL_COUNT));
		for (int i = 0; i < ScrappingTable.TOOL_COUNT; i++)
			data.set(i, -1);
	}

	ScrappingMenu(int id, Inventory player, Container inventory, ContainerData data)
	{
		super(Workstations.SCRAPPING_MENU.get(), id, inventory);
		this.data = data;
		for (int tool = 0; tool < ScrappingTable.TOOL_COUNT; tool++)
		{
			int index = tool;
			addMachineSlot(tool, 6, 46 + tool * 22, 1, stack -> ScrappingTable.isTool(index, stack));
		}
		addMachineSlot(ScrappingTable.INPUT, 81, 28, 1, stack -> true);
		for (int slot = ScrappingTable.FIRST_OUTPUT; slot < ScrappingTable.SIZE; slot++)
		{
			int offset = slot - ScrappingTable.FIRST_OUTPUT;
			addMachineSlot(slot, 129 + offset % 2 * 21, 46 + offset / 2 * 22, 64, stack -> false);
		}
		addPlayerSlots(player, 124);
		addDataSlots(data);
	}

	public ItemStack input()
	{
		return inventory.getItem(ScrappingTable.INPUT);
	}

	public int progress(int tool)
	{
		return data.get(tool);
	}

	@Override
	public boolean clickMenuButton(Player player, int tool)
	{
		return inventory instanceof ScrappingTable table && !player.isSpectator()
				&& player.containerMenu == this && stillValid(player) && table.work(tool);
	}
}
