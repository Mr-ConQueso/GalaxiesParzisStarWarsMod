package dev.pswg.item;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;

public final class DoorInsertItem extends Item
{
	private final DyeColor color;

	public DoorInsertItem(DyeColor color, Properties properties)
	{
		super(properties);
		this.color = color;
	}

	public DyeColor getColor()
	{
		return color;
	}
}
