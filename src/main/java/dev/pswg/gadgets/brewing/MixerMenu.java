package dev.pswg.gadgets.brewing;

import dev.pswg.gadgets.WorkstationMenu;
import dev.pswg.gadgets.Workstations;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.crafting.RecipeType;

public final class MixerMenu extends WorkstationMenu
{
	private static final int DATA_SIZE = 23;
	private final ContainerData data;

	public MixerMenu(int id, Inventory playerInventory)
	{
		this(id, playerInventory, new SimpleContainer(3), new SimpleContainerData(DATA_SIZE));
		data.set(0, 2560);
		data.set(1, 2560);
		for (int i = 5; i < DATA_SIZE; i += 2)
			data.set(i, -1);
	}

	MixerMenu(int id, Inventory playerInventory, Mixer mixer)
	{
		this(id, playerInventory, mixer, new SimpleContainerData(DATA_SIZE)
		{
			@Override
			public int get(int index)
			{
				return mixer.menuData(index);
			}
		});
	}

	private MixerMenu(int id, Inventory playerInventory, Container inventory, ContainerData data)
	{
		super(Workstations.MIXER_MENU.get(), id, inventory);
		this.data = data;
		addMachineSlot(Mixer.FUEL, 146, 122, 64, stack -> stack.getBurnTime(RecipeType.SMELTING) > 0);
		addMachineSlot(Mixer.INGREDIENT, 146, 69, 64, Mixer::isIngredient);
		addMachineSlot(Mixer.VESSEL, 146, 29, 1, Mixer::isVessel);
		addPlayerSlots(playerInventory, 174);
		addDataSlots(data);
	}

	@Override
	public boolean clickMenuButton(Player player, int action)
	{
		return inventory instanceof Mixer mixer && !player.isSpectator()
				&& player.containerMenu == this && stillValid(player) && mixer.act(player, action);
	}

	public float mapX() { return data.get(0) / 10F; }
	public float mapY() { return data.get(1) / 10F; }
	public int fuel() { return data.get(2); }
	public int pressure() { return data.get(3); }

	public int swatch(int group, int index)
	{
		int offset = 5 + (group * 3 + index) * 2;
		int high = data.get(offset);
		return high < 0 ? 0 : 0xFF000000 | high << 16 | (data.get(offset + 1) & 0xFFFF);
	}

	public boolean buttonEnabled(int action)
	{
		if (!Mixer.isVessel(inventory.getItem(Mixer.VESSEL)))
			return false;
		return switch (action)
		{
			case 0 -> fuel() > 0 && pressure() > 5;
			case 1, 2, 3 -> (data.get(4) & (1 << (action - 1))) != 0;
			default -> false;
		};
	}
}
