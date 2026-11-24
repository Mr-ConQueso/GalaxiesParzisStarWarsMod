package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.item.FilledDrinkItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class DrinkItems
{
	public static final TagKey<Item> DRINK_CONTAINER = TagKey.create(Registries.ITEM, Galaxies.id("drink_container"));

	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final ResourceKey<CreativeModeTab> FOOD_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Galaxies.id("food_items"));

	public static final List<Container> GLASSES = IntStream.rangeClosed(1, 10).mapToObj(i -> registerContainer("glass_" + i)).toList();
	public static final List<Container> GLASS_BOTTLES = IntStream.rangeClosed(1, 3).mapToObj(i -> registerContainer("glass_bottle_" + i)).toList();

	public static final Container DURASTEEL_CUP = registerContainer("durasteel_cup", new Item.Properties().component(CoreComponents.METAL_COMPONENT, 1), 1);
	public static final Container DESH_CUP = registerContainer("desh_cup", new Item.Properties().component(CoreComponents.METAL_COMPONENT, 1), 1);
	public static final List<Container> CERAMIC_CUPS = Arrays.stream(DyeColor.values()).map(color -> registerContainer(color.getName() + "_cup")).toList();
	public static final List<Container> PLASTIC_BOTTLES = IntStream.rangeClosed(1, 2)
			.mapToObj(i -> registerContainer("plastic_bottle_" + i, new Item.Properties(), 0)).toList();

	public static final List<Container> CONTAINERS = Stream.of(GLASSES, GLASS_BOTTLES, List.of(DURASTEEL_CUP, DESH_CUP), CERAMIC_CUPS, PLASTIC_BOTTLES)
			.flatMap(List::stream).toList();

	public static void register(IEventBus bus)
	{
		ITEMS.register(bus);
		bus.addListener(DrinkItems::populateFoodTab);
	}

	private static void populateFoodTab(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey().equals(FOOD_TAB))
			for (var container : CONTAINERS)
				event.accept(container.empty().get());
	}

	private static Container registerContainer(String id)
	{
		return registerContainer(id, new Item.Properties(), 1);
	}

	private static Container registerContainer(String id, Item.Properties properties, int tintLayer)
	{
		var empty = ITEMS.registerSimpleItem(id, properties);
		var filled = ITEMS.register(id + "_filled", () -> new FilledDrinkItem(empty,
				new Item.Properties().component(DataComponents.POTION_CONTENTS, PotionContents.EMPTY)));
		return new Container(empty, filled, tintLayer);
	}

	public record Container(DeferredItem<Item> empty, DeferredItem<FilledDrinkItem> filled, int tintLayer)
	{
	}

	private DrinkItems()
	{
	}
}
