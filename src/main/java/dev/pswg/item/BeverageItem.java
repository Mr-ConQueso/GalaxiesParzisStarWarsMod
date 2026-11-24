package dev.pswg.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;

public final class BeverageItem extends Item
{
	public BeverageItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.DRINK;
	}
}
