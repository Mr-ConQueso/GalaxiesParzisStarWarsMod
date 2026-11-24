package dev.pswg.gadgets.brewing;

import com.mojang.datafixers.util.Pair;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.container.GalaxiesItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.Map;

final class MixerIngredients
{
	static final TagKey<Item> FOOD = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("pswg_gadgets", "mixable_food"));

	private static final Map<Item, List<Pair<Float, Float>>> PATHS = Map.ofEntries(
			Map.entry(GalaxiesItems.STRIPPED_JAPOR_BRANCH.get(), List.of(Pair.of((float)Math.PI * 7 / 4f, 20f), Pair.of((float)Math.PI * 1 / 3f, 65f))),
			Map.entry(GalaxiesItems.CHASUKA_LEAF.get(), List.of(Pair.of((float)Math.PI * 7 / 12f, 25f), Pair.of((float)Math.PI * 3 / 4f, 20f), Pair.of((float)Math.PI * 11 / 12f, 15f))),
			Map.entry(GalaxiesBlocks.HKAK_BEAN.get(), List.of(Pair.of((float)Math.PI * 11 / 6f, 36f), Pair.of((float)Math.PI * 19 / 12f, 50f), Pair.of((float)Math.PI * 5 / 4f, 50f))),
			Map.entry(GalaxiesItems.CORPSE_OF_GORG.get(), List.of(Pair.of((float)Math.PI * 7 / 4f, 100f), Pair.of((float)Math.PI * 5 / 4f, 25f))),
			Map.entry(GalaxiesItems.EYE_OF_SKETTO.get(), List.of(Pair.of((float)Math.PI * 7 / 6f, 25f), Pair.of((float)Math.PI * 5 / 6f, 30f), Pair.of((float)Math.PI * 7 / 6f, 20f))),
			Map.entry(GalaxiesItems.KREETLE_HUSK.get(), List.of(Pair.of((float)Math.PI / -4f, 45f), Pair.of((float)Math.PI / 6f, 45f))),
			Map.entry(GalaxiesItems.LIZARD_GIZZARD.get(), List.of(Pair.of((float)Math.PI * 10 / 7f, 45f), Pair.of((float)Math.PI * 9f / 8f, 22f),  Pair.of((float)Math.PI / -9f, 12f))),
			Map.entry(GalaxiesItems.SQUILL_LIVER.get(), List.of(Pair.of((float)Math.PI * 11 / 12f, 30f), Pair.of((float)Math.PI * 3 / 4f, 40f))),
			Map.entry(GalaxiesItems.TONGUE_OF_WORRT.get(), List.of(Pair.of((float)Math.PI * 2 / 3f, 26f), Pair.of((float)Math.PI / 6f, 34f)))
	);
	private static final Map<Item, Integer> COLORS = Map.ofEntries(
			Map.entry(Items.APPLE, 0xFFff1c2b),
			Map.entry(Items.BEETROOT, 0xFF71160d),
			Map.entry(Items.CARROT, 0xFFff8e09),
			Map.entry(Items.GLOW_BERRIES, 0xFFf7e26b),
			Map.entry(Items.MELON_SLICE, 0xFF848920),
			Map.entry(Items.SWEET_BERRIES, 0xFFa50700),
			Map.entry(GalaxiesItems.BLACK_MELON.get(), 0xFF42443b),
			Map.entry(GalaxiesItems.BLUE_MILK.get(), 0xFF4f69a6),
			Map.entry(GalaxiesItems.DEB_DEB.get(), 0xFFd37e1a),
			Map.entry(GalaxiesItems.DESERT_PLUMS.get(), 0xFF404766),
			Map.entry(GalaxiesItems.JOGAN_FRUIT.get(), 0xFF57254d),
			Map.entry(GalaxiesItems.MEILOORUN.get(), 0xFFff442b),
			Map.entry(GalaxiesItems.PALLIE_FRUIT.get(), 0xFF5c6f36),
			Map.entry(GalaxiesItems.PIKA_FRUIT.get(), 0xFF567d8a)
	);

	static List<Pair<Float, Float>> path(ItemStack stack)
	{
		return PATHS.getOrDefault(stack.getItem(), List.of());
	}

	static int color(ItemStack stack)
	{
		return COLORS.getOrDefault(stack.getItem(), 0xFF808080);
	}

	private MixerIngredients()
	{
	}
}
