package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.item.MetalPipeItem;
import dev.pswg.item.BeverageItem;
import dev.pswg.item.DoorInsertItem;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public final class GalaxiesItems
{
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Galaxies.MODID);
	private static final ResourceKey<CreativeModeTab> FOOD_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Galaxies.id("food_items"));
	private static final List<DeferredItem<Item>> FOODS = new ArrayList<>();

	private static final ToolTier BESKAR = new ToolTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2000, 10.0F, 5.0F, 16, "beskar");
	private static final ToolTier DURASTEEL = new ToolTier(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 500, 7.0F, 2.5F, 10, "durasteel");
	private static final ToolTier TITANIUM = new ToolTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 1000, 8.0F, 3.5F, 12, "titanium");

	public static final DeferredItem<Item> BROKEN_SMALL_POWER_PACK_ITEM = ingredient("broken_small_power_pack", () -> new Item.Properties().component(CoreComponents.METAL_COMPONENT, 1).component(CoreComponents.ENERGY_COMPONENT, 3).component(CoreComponents.TECH_COMPONENT, 2).component(CoreComponents.SCRAP, true));
	public static final DeferredItem<Item> ELECTRIC_MOTOR = ingredient("electric_motor", () -> new Item.Properties().component(CoreComponents.METAL_COMPONENT, 1).component(CoreComponents.TECH_COMPONENT, 2).component(CoreComponents.ENERGY_COMPONENT, 3).component(CoreComponents.SCRAP, false));
	public static final DeferredItem<Item> TURBINE = ingredient("turbine", () -> new Item.Properties().component(CoreComponents.METAL_COMPONENT, 1).component(CoreComponents.TECH_COMPONENT, 2));
	public static final DeferredItem<Item> BALL_BEARING = ingredient("ball_bearing", () -> new Item.Properties().component(CoreComponents.METAL_COMPONENT, 2));
	public static final DeferredItem<Item> DESH_WIRE = ingredient("desh_wire", () -> new Item.Properties().component(CoreComponents.METAL_COMPONENT, 1).component(CoreComponents.ENERGY_COMPONENT, 1));
	public static final DeferredItem<Item> DESH_COIL = ingredient("desh_coil", () -> new Item.Properties().component(CoreComponents.METAL_COMPONENT, 2).component(CoreComponents.PLASTIC_COMPONENT, 2).component(CoreComponents.ENERGY_COMPONENT, 2).component(CoreComponents.SCRAP, false));
	public static final DeferredItem<Item> LIGHT_PANEL = ingredient("light_panel", () -> new Item.Properties().component(CoreComponents.PLASTIC_COMPONENT, 2).component(CoreComponents.TECH_COMPONENT, 2).component(CoreComponents.SCRAP, false));
	public static final DeferredItem<Item> DISPLAY_PANEL = ingredient("display_panel", () -> new Item.Properties().component(CoreComponents.PLASTIC_COMPONENT, 1).component(CoreComponents.TECH_COMPONENT, 2).component(CoreComponents.ENERGY_COMPONENT, 2).component(CoreComponents.SCRAP, false));
	public static final DeferredItem<Item> PLASTEEL_ROD = ingredient("plasteel_rod", () -> new Item.Properties().component(CoreComponents.PLASTIC_COMPONENT, 1));
	public static final DeferredItem<Item> BESKAR_RAW = ingredient("raw_beskar");
	public static final DeferredItem<Item> BESKAR_INGOT = ingredient("beskar_ingot");
	public static final DeferredItem<Item> CHROMIUM_RAW = ingredient("raw_chromium");
	public static final DeferredItem<Item> CHROMIUM_INGOT = ingredient("chromium_ingot");
	public static final DeferredItem<Item> CHROMIUM_NUGGET = ingredient("chromium_nugget");
	public static final DeferredItem<Item> CORTOSIS_RAW = ingredient("raw_cortosis");
	public static final DeferredItem<Item> CORTOSIS_INGOT = ingredient("cortosis_ingot");
	public static final DeferredItem<Item> DESH_RAW = ingredient("raw_desh");
	public static final DeferredItem<Item> DESH_INGOT = ingredient("desh_ingot");
	public static final DeferredItem<Item> DESH_NUGGET = ingredient("desh_nugget");
	public static final DeferredItem<Item> DIATIUM_RAW = ingredient("raw_diatium");
	public static final DeferredItem<Item> DIATIUM_INGOT = ingredient("diatium_ingot");
	public static final DeferredItem<Item> DIATIUM_NUGGET = ingredient("diatium_nugget");
	public static final DeferredItem<Item> DURASTEEL_INGOT = ingredient("durasteel_ingot");
	public static final DeferredItem<Item> DURASTEEL_NUGGET = ingredient("durasteel_nugget");
	public static final DeferredItem<Item> EXONIUM_CRYSTAL = ingredient("exonium");
	public static final DeferredItem<Item> HELICITE_CRYSTAL = ingredient("helicite_crystal");
	public static final DeferredItem<Item> HELICITE_DUST = ingredient("helicite_dust");
	public static final DeferredItem<Item> IONITE_RAW = ingredient("raw_ionite");
	public static final DeferredItem<Item> IONITE_INGOT = ingredient("ionite_ingot");
	public static final DeferredItem<Item> IONITE_NUGGET = ingredient("ionite_nugget");
	public static final DeferredItem<Item> KELERIUM_RAW = ingredient("raw_kelerium");
	public static final DeferredItem<Item> KELERIUM_INGOT = ingredient("kelerium_ingot");
	public static final DeferredItem<Item> LOMMITE_CRYSTAL = ingredient("lommite_crystal");
	public static final DeferredItem<Item> LOMMITE_DUST = ingredient("lommite_dust");
	public static final DeferredItem<Item> PLASTEEL_INGOT = ingredient("plasteel_ingot");
	public static final DeferredItem<Item> PLASTEEL_NUGGET = ingredient("plasteel_nugget");
	public static final DeferredItem<Item> RUBINDUM_RAW = ingredient("raw_rubindum");
	public static final DeferredItem<Item> RUBINDUM_SHARD = ingredient("rubindum_shard");
	public static final DeferredItem<Item> THORILIDE_CRYSTAL = ingredient("thorilide_crystal");
	public static final DeferredItem<Item> THORILIDE_DUST = ingredient("thorilide_dust");
	public static final DeferredItem<Item> TITANIUM_RAW = ingredient("raw_titanium");
	public static final DeferredItem<Item> TITANIUM_INGOT = ingredient("titanium_ingot");
	public static final DeferredItem<Item> TITANIUM_NUGGET = ingredient("titanium_nugget");
	public static final DeferredItem<Item> TRANSPARISTEEL_INGOT = ingredient("transparisteel_ingot");
	public static final DeferredItem<Item> ZERSIUM_CRYSTAL = ingredient("zersium_crystal");
	public static final DeferredItem<Item> ZERSIUM_DUST = ingredient("zersium_dust");
	public static final DeferredItem<Item> STRIPPED_JAPOR_BRANCH = ingredient("stripped_japor_branch");
	public static final DeferredItem<Item> MOLO_FLOWER = ingredient("molo_flower");
	public static final DeferredItem<Item> SALT_PILE = ingredient("salt_pile");
	public static final DeferredItem<Item> JOGAN_FRUIT = food("jogan_fruit", 4, 0.3F);
	public static final DeferredItem<Item> CHASUKA_LEAF = food("chasuka_leaf", 4, 0.3F);
	public static final DeferredItem<ItemNameBlockItem> CHASUKA_SEEDS = ITEMS.register("chasuka_seeds",
			() -> new ItemNameBlockItem(GalaxiesBlocks.CHASUKA.get(), new Item.Properties()));
	public static final DeferredItem<Item> MEILOORUN = food("meiloorun", 4, 0.3F);
	public static final DeferredItem<Item> MYNOCK_WING = food("mynock_wing", 3, 0.3F);
	public static final DeferredItem<Item> FRIED_MYNOCK_WING = food("cooked_mynock_wing", 8, 0.8F);
	public static final DeferredItem<Item> BANTHA_CHOP = food("bantha_chop", 3, 0.3F);
	public static final DeferredItem<Item> BANTHA_STEAK = food("cooked_bantha_chop", 8, 0.8F);
	public static final DeferredItem<Item> NERF_CHOP = food("nerf_chop", 3, 0.3F);
	public static final DeferredItem<Item> NERF_STEAK = food("cooked_nerf_chop", 8, 0.8F);
	public static final DeferredItem<Item> GIZKA_CHOP = food("gizka_chop", 3, 0.3F);
	public static final DeferredItem<Item> GIZKA_STEAK = food("cooked_gizka_chop", 8, 0.8F);
	public static final DeferredItem<Item> FLANGTH_TAKEOUT = food("flangth_takeout", 3, 0.3F);
	public static final DeferredItem<Item> FLANGTH_PLATE = food("flangth_plate", 8, 0.8F);
	public static final DeferredItem<Item> BLUE_YOGURT = food("blue_yogurt", 4, 0.3F);
	public static final DeferredItem<Item> BANTHA_COOKIE = food("bantha_cookie", 5, 0.6F);
	public static final DeferredItem<Item> QRIKKI_BREAD = food("qrikki_bread", 5, 0.6F);
	public static final DeferredItem<Item> QRIKKI_WAFFLE = food("qrikki_waffle", 8, 0.7F);
	public static final DeferredItem<Item> AHRISA_BOWL = food("ahrisa_bowl", 4, 0.3F);
	public static final DeferredItem<Item> BLACK_MELON = food("black_melon", 2, 0.3F);
	public static final DeferredItem<Item> DESERT_PLUMS = food("desert_plums", 2, 0.3F);
	public static final DeferredItem<Item> DRIED_POONTEN_GRASS = food("dried_poonten_grass_bushel", 2, 0.3F);
	public static final DeferredItem<Item> HAROUN_BREAD = food("haroun_bread", 5, 0.3F);
	public static final DeferredItem<Item> PALLIE_FRUIT = food("pallie_fruit", 2, 0.3F);
	public static final DeferredItem<Item> PIKA_FRUIT = food("pika_fruit", 2, 0.3F);
	public static final DeferredItem<Item> TUBER = food("tuber", 2, 0.3F);
	public static final DeferredItem<Item> COOKED_EOPIE_LOIN = food("cooked_eopie_loin", 8, 0.3F);
	public static final DeferredItem<Item> CRISPY_GORG = food("crispy_gorg", 6, 0.3F);
	public static final DeferredItem<Item> DEWBACK_EGG = food("dewback_egg", 4, 0.3F);
	public static final DeferredItem<Item> DEWBACK_OMELETTE = food("dewback_omelette", 6, 0.3F);
	public static final DeferredItem<Item> JERBA_RACK = food("jerba_rack", 4, 0.3F);
	public static final DeferredItem<Item> JERBA_RIB = food("jerba_rib", 8, 0.3F);
	public static final DeferredItem<Item> KRAYT_MEAT = food("krayt_meat", 6, 0.3F);
	public static final DeferredItem<Item> RAW_SKETTO_NUGGET = food("raw_sketto_nugget", 2, 0.3F);
	public static final DeferredItem<Item> ROAST_KRAYT = food("roast_krayt", 8, 0.3F);
	public static final DeferredItem<Item> RONTO_CHUCK = food("ronto_chuck", 3, 0.3F);
	public static final DeferredItem<Item> TUBER_MASH = food("tuber_mash", 4, 0.3F);
	public static final DeferredItem<Item> VAPORATOR_MUSHROOM = food("vaporator_mushroom", 2, 0.3F);
	public static final DeferredItem<Item> WORRT_EGG = food("worrt_egg", 2, 0.3F);
	public static final DeferredItem<Item> DEB_DEB = food("deb_deb", 2, 0.3F);
	public static final DeferredItem<Item> EOPIE_LOIN = food("eopie_loin", 3, 0.3F);
	public static final DeferredItem<Item> HUBBA_GOURD = food("hubba_gourd", 2, 0.3F);
	public static final DeferredItem<Item> CORPSE_OF_GORG = ingredient("corpse_of_gorg");
	public static final DeferredItem<Item> BANTHA_HORN = ingredient("bantha_horn");
	public static final DeferredItem<Item> DEWBACK_BONE = ingredient("dewback_bone");
	public static final DeferredItem<Item> DEWBACK_BONE_SHARD = ingredient("dewback_bone_shard");
	public static final DeferredItem<Item> EYE_OF_SKETTO = ingredient("eye_of_sketto");
	public static final DeferredItem<Item> HIDE = ingredient("hide");
	public static final DeferredItem<Item> KRAYT_PEARL = ingredient("krayt_pearl");
	public static final DeferredItem<Item> KRAYT_TOOTH = ingredient("krayt_tooth");
	public static final DeferredItem<Item> KREETLE_HUSK = ingredient("kreetle_husk");
	public static final DeferredItem<Item> LIZARD_GIZZARD = ingredient("lizard_gizzard");
	public static final DeferredItem<Item> SQUILL_LIVER = ingredient("squill_liver");
	public static final DeferredItem<Item> TONGUE_OF_WORRT = ingredient("tongue_of_worrt");
	public static final DeferredItem<Item> TOUGH_HIDE = ingredient("tough_hide");
	public static final DeferredItem<PickaxeItem> BESKAR_PICKAXE = tool("beskar_pickaxe", BESKAR, 1, -2.8F, PickaxeItem::new);
	public static final DeferredItem<AxeItem> BESKAR_AXE = tool("beskar_axe", BESKAR, 5, -3.0F, AxeItem::new);
	public static final DeferredItem<ShovelItem> BESKAR_SHOVEL = tool("beskar_shovel", BESKAR, 1.5F, -3.0F, ShovelItem::new);
	public static final DeferredItem<HoeItem> BESKAR_HOE = tool("beskar_hoe", BESKAR, 0, 0.0F, HoeItem::new);
	public static final DeferredItem<PickaxeItem> DURASTEEL_PICKAXE = tool("durasteel_pickaxe", DURASTEEL, 1, -2.8F, PickaxeItem::new);
	public static final DeferredItem<AxeItem> DURASTEEL_AXE = tool("durasteel_axe", DURASTEEL, 5, -3.0F, AxeItem::new);
	public static final DeferredItem<ShovelItem> DURASTEEL_SHOVEL = tool("durasteel_shovel", DURASTEEL, 1.5F, -3.0F, ShovelItem::new);
	public static final DeferredItem<HoeItem> DURASTEEL_HOE = tool("durasteel_hoe", DURASTEEL, 0, 0.0F, HoeItem::new);
	public static final DeferredItem<PickaxeItem> TITANIUM_PICKAXE = tool("titanium_pickaxe", TITANIUM, 1, -2.8F, PickaxeItem::new);
	public static final DeferredItem<AxeItem> TITANIUM_AXE = tool("titanium_axe", TITANIUM, 5, -3.0F, AxeItem::new);
	public static final DeferredItem<ShovelItem> TITANIUM_SHOVEL = tool("titanium_shovel", TITANIUM, 1.5F, -3.0F, ShovelItem::new);
	public static final DeferredItem<HoeItem> TITANIUM_HOE = tool("titanium_hoe", TITANIUM, 0, 0.0F, HoeItem::new);
	public static final DeferredItem<MetalPipeItem> DURASTEEL_ROD = ITEMS.register("durasteel_rod", () ->
			new MetalPipeItem(new Item.Properties().component(CoreComponents.METAL_COMPONENT, 1)));

	public static final DeferredItem<Item> DEATH_STICK_RED = drink("death_stick_red", 3, true, false, new ServingEffect(MobEffects.BLINDNESS, .99F), new ServingEffect(MobEffects.HEAL, 1));
	public static final DeferredItem<Item> DEATH_STICK_YELLOW = drink("death_stick_yellow", 3, true, false, new ServingEffect(MobEffects.CONFUSION, .99F));
	public static final DeferredItem<Item> MYSTERIOUS_SMOOTHIE = drink("mysterious_smoothie", 3, true, false, new ServingEffect(MobEffects.CONFUSION, .5F), new ServingEffect(MobEffects.JUMP, 1));
	public static final DeferredItem<Item> KREETLE_JUICE = drink("kreetlejuice", 3, true, false, new ServingEffect(MobEffects.INVISIBILITY, 1));
	public static final DeferredItem<Item> ABSYNTHESIZED_MALT = drink("absynthesized_malt", 3, true, false, new ServingEffect(MobEffects.CONFUSION, .5F), new ServingEffect(MobEffects.LUCK, 1));
	public static final DeferredItem<Item> CORONET_COCKTAIL = drink("coronet_cocktail", 3, true, false, new ServingEffect(MobEffects.FIRE_RESISTANCE, 1));
	public static final DeferredItem<Item> CLASSIC_SODA = drink("classic_soda", 3, true, false, new ServingEffect(MobEffects.MOVEMENT_SPEED, 1));
	public static final DeferredItem<Item> DIET_SODA = drink("diet_soda", 3, true, false, new ServingEffect(MobEffects.MOVEMENT_SPEED, 1));
	public static final DeferredItem<Item> CITRUS_SODA = drink("citrus_soda", 3, true, false, new ServingEffect(MobEffects.MOVEMENT_SPEED, 1));
	public static final DeferredItem<Item> BOTTLED_WATER = drink("bottled_water", 3, true, false, new ServingEffect(MobEffects.SATURATION, 1));
	public static final DeferredItem<Item> BLUE_MILK = drink("blue_milk", 4, false, true);
	public static final DeferredItem<Item> BLUE_MILK_GLASS = drink("blue_milk_glass", 4, false, true);

	static
	{
		for (DyeColor color : DyeColor.values())
			ITEMS.register(color.getName() + "_door_insert", () -> new DoorInsertItem(color, new Item.Properties()));

		TABS.register("items", () -> CreativeModeTab.builder()
				.title(net.minecraft.network.chat.Component.translatable("pswg.item_group"))
				.icon(DURASTEEL_ROD::toStack)
				.displayItems((parameters, output) -> {
					for (var item : ITEMS.getEntries())
						if (!FOODS.contains(item))
							output.accept(item.get());
				})
				.build());
	}

	public static void register(IEventBus bus)
	{
		ITEMS.register(bus);
		TABS.register(bus);
		bus.addListener(GalaxiesItems::populateFoodTab);
	}

	private static void populateFoodTab(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey().equals(FOOD_TAB))
			for (var food : FOODS)
				event.accept(food.get());
	}

	private static DeferredItem<Item> ingredient(String id)
	{
		return ITEMS.registerSimpleItem(id);
	}

	private static DeferredItem<Item> ingredient(String id, Supplier<Item.Properties> properties)
	{
		return ITEMS.register(id, () -> new Item(properties.get()));
	}

	private static DeferredItem<Item> food(String id, int nutrition, float saturation)
	{
		var item = ingredient(id, () -> new Item.Properties().food(new FoodProperties.Builder()
				.nutrition(nutrition).saturationModifier(saturation).build()));
		FOODS.add(item);
		return item;
	}

	private static DeferredItem<Item> drink(String id, int nutrition, boolean alwaysEdible, boolean drinkAnimation, ServingEffect... effects)
	{
		DeferredItem<Item> item = ITEMS.register(id, () -> {
			var food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(.3F);
			if (alwaysEdible)
				food.alwaysEdible();
			for (var effect : effects)
				food.effect(() -> new MobEffectInstance(effect.type(), 200, 1), effect.probability());
			var properties = new Item.Properties().food(food.build());
			return drinkAnimation ? new BeverageItem(properties) : new Item(properties);
		});
		FOODS.add(item);
		return item;
	}

	private record ServingEffect(Holder<MobEffect> type, float probability)
	{
	}

	private static <T extends DiggerItem> DeferredItem<T> tool(String id, Tier tier, float attack, float speed,
	                                                           BiFunction<Tier, Item.Properties, T> factory)
	{
		return ITEMS.register(id, () -> factory.apply(tier, new Item.Properties()
				.attributes(DiggerItem.createAttributes(tier, attack, speed))));
	}

	private record ToolTier(TagKey<Block> getIncorrectBlocksForDrops, int getUses, float getSpeed,
	                        float getAttackDamageBonus, int getEnchantmentValue, String repairMaterial) implements Tier
	{
		@Override
		public Ingredient getRepairIngredient()
		{
			return Ingredient.of(TagKey.create(Registries.ITEM, Galaxies.id(repairMaterial + "_tool_materials")));
		}
	}

	private GalaxiesItems()
	{
	}
}
