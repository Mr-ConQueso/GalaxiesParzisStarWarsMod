package dev.pswg.rendering;

import dev.pswg.container.DrinkItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.FastColor.ARGB32;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.DyedItemColor;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

public final class DrinkColors
{
	private static final int DEFAULT_COLOR = -13083194;

	public static void register(RegisterColorHandlersEvent.Item event)
	{
		for (var container : DrinkItems.CONTAINERS)
			event.register((stack, layer) -> layer == container.tintLayer() ? tint(stack) : -1, container.filled().get());
	}

	private static int tint(ItemStack stack)
	{
		var dye = stack.get(DataComponents.DYED_COLOR);
		if (dye != null)
			return DyedItemColor.getOrDefault(stack, DEFAULT_COLOR);
		var potion = stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
		return ARGB32.opaque(potion.customColor().orElseGet(() -> mixColors(potion)));
	}

	private static int mixColors(PotionContents potion)
	{
		int red = 0;
		int green = 0;
		int blue = 0;
		int weight = 0;
		for (var effect : potion.getAllEffects())
		{
			int color = effect.getEffect().value().getColor();
			int count = effect.getAmplifier() + 1;
			red += count * ARGB32.red(color);
			green += count * ARGB32.green(color);
			blue += count * ARGB32.blue(color);
			weight += count;
		}
		return weight == 0 ? DEFAULT_COLOR : ARGB32.color(red / weight, green / weight, blue / weight);
	}

	private DrinkColors()
	{
	}
}
