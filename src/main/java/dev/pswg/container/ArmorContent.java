package dev.pswg.container;

import dev.pswg.Galaxies;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ArmorContent
{
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final ResourceKey<CreativeModeTab> CORE_ITEMS = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Galaxies.id("items"));
	private static final Map<ResourceLocation, Appearance> APPEARANCES = new LinkedHashMap<>();

	public static final ArmorSet STORM_TROOPER = set("stormtrooper", ArmorMaterials.DIAMOND, "armor/stormtrooper", null, true);
	public static final ArmorSet SHOCK_TROOPER = set("shocktrooper", ArmorMaterials.DIAMOND, "armor/stormtrooper", "textures/armor/shocktrooper.png", true);
	public static final ArmorSet ARTILLERY_TROOPER = set("artillerytrooper", ArmorMaterials.DIAMOND, "armor/artillerytrooper", null, true);
	public static final ArmorSet DEATH_TROOPER = set("deathtrooper", ArmorMaterials.DIAMOND, "armor/deathtrooper", null, true);
	public static final ArmorSet PURGE_TROOPER = set("purgetrooper", ArmorMaterials.DIAMOND, "armor/purgetrooper", null, true);
	public static final ArmorSet INCINERATOR_TROOPER = set("incineratortrooper", ArmorMaterials.DIAMOND, "armor/incineratortrooper", null, true);
	public static final ArmorSet SANDTROOPER = set("sandtrooper", ArmorMaterials.DIAMOND, "armor/sandtrooper", null, true);
	public static final ArmorSet SHORE_TROOPER = set("shoretrooper", ArmorMaterials.DIAMOND, "armor/shoretrooper", null, true);
	public static final ArmorSet SCOUT_TROOPER = set("scouttrooper", ArmorMaterials.DIAMOND, "armor/scouttrooper", null, true);
	public static final ArmorSet JUMP_TROOPER = set("jumptrooper", ArmorMaterials.DIAMOND, "armor/jumptrooper", null, true);
	public static final ArmorSet ELITE_SQUAD_TROOPER = set("elite_squad", ArmorMaterials.DIAMOND, "armor/elite_squad_trooper", null, true);
	public static final ArmorSet HOVERTANK_PILOT = set("hovertankpilot", ArmorMaterials.DIAMOND, "armor/hovertank_pilot", null, true);
	public static final DeferredItem<ArmorItem> IMPERIAL_PILOT_HELMET = piece("imperial_pilot_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/imperial_pilot", null, true);
	public static final DeferredItem<ArmorItem> IMPERIAL_PILOT_KIT = piece("imperial_pilot_kit", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/imperial_pilot", null, true);
	public static final DeferredItem<ArmorItem> IMPERIAL_PILOT_TECHNICAL_HELMET = piece("imperial_pilot_technical_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/technical_helmet", null, true);
	public static final DeferredItem<ArmorItem> BEACH_INSURGENCE_HAT = piece("beach_insurgence_hat", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/beach_insurgence_hat", null, true);
	public static final DeferredItem<ArmorItem> DESERT_INSURGENCE_HAT = piece("desert_insurgence_hat", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/desert_insurgence_hat", null, true);
	public static final DeferredItem<ArmorItem> BLACK_IMPERIAL_OFFICER = piece("black_imperial_officer_hat", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/imperial_officer_cap", "textures/armor/imperial_officer_hat_black.png", true);
	public static final DeferredItem<ArmorItem> GRAY_IMPERIAL_OFFICER = piece("gray_imperial_officer_hat", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/imperial_officer_cap", "textures/armor/imperial_officer_hat_gray.png", false);
	public static final DeferredItem<ArmorItem> LIGHT_GRAY_IMPERIAL_OFFICER = piece("light_gray_imperial_officer_hat", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/imperial_officer_cap", "textures/armor/imperial_officer_hat_light_gray.png", false);
	public static final DeferredItem<ArmorItem> KHAKI_IMPERIAL_OFFICER = piece("khaki_imperial_officer_hat", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/imperial_officer_cap", "textures/armor/imperial_officer_hat_khaki.png", false);
	public static final DeferredItem<ArmorItem> GRAY_GOGGLES_CAP = piece("gray_goggles_cap", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/goggles_cap", "textures/armor/gray_goggles_cap.png", true);
	public static final DeferredItem<ArmorItem> BROWN_GOGGLES_CAP = piece("brown_goggles_cap", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/goggles_cap", "textures/armor/brown_goggles_cap.png", true);
	public static final DeferredItem<ArmorItem> TAN_GOGGLES_CAP = piece("tan_goggles_cap", ArmorMaterials.LEATHER, ArmorItem.Type.HELMET, "armor/goggles_cap", "textures/armor/tan_goggles_cap.png", true);
	public static final DeferredItem<ArmorItem> REBEL_TROPICAL = piece("rebel_tropical_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/rebel_helmet", "textures/armor/rebel_tropical_helmet.png", true);
	public static final DeferredItem<ArmorItem> REBEL_FOREST = piece("rebel_forest_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/rebel_helmet", "textures/armor/rebel_forest_helmet.png", true);
	public static final DeferredItem<ArmorItem> IMPERIAL_PILOT_CADET_HELMET = piece("imperial_pilot_cadet_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/tie_cadet", null, true);
	public static final DeferredItem<ArmorItem> IMPERIAL_PILOT_COLD_HELMET = piece("imperial_pilot_cold_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/tie_cold_mask", null, true);
	public static final DeferredItem<ArmorItem> REBEL_PILOT_HELMET = piece("rebel_pilot_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/rebel_pilot", null, true);
	public static final DeferredItem<ArmorItem> REBEL_PILOT_KIT = piece("rebel_pilot_kit", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/rebel_pilot", null, true);
	public static final DeferredItem<ArmorItem> AWING_PILOT_HELMET = piece("awing_pilot_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/awing_pilot", null, true);
	public static final DeferredItem<ArmorItem> AWING_PILOT_KIT = piece("awing_pilot_kit", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/awing_pilot", null, true);
	public static final DeferredItem<ArmorItem> BWING_PILOT_HELMET = piece("bwing_pilot_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/bwing_pilot", null, true);
	public static final DeferredItem<ArmorItem> BWING_PILOT_KIT = piece("bwing_pilot_kit", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/bwing_pilot", null, true);
	public static final DeferredItem<ArmorItem> UWING_PILOT_HELMET = piece("uwing_pilot_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/uwing_pilot", null, true);
	public static final DeferredItem<ArmorItem> UWING_PILOT_KIT = piece("uwing_pilot_kit", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/uwing_pilot", null, true);
	public static final DeferredItem<ArmorItem> YWING_PILOT_HELMET = piece("ywing_pilot_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/ywing_pilot", null, true);
	public static final DeferredItem<ArmorItem> YWING_PILOT_KIT = piece("ywing_pilot_kit", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/ywing_pilot", null, true);
	public static final DeferredItem<ArmorItem> XWING_PILOT_HELMET = piece("xwing_pilot_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/xwing_pilot", null, true);
	public static final DeferredItem<ArmorItem> XWING_PILOT_KIT = piece("xwing_pilot_kit", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/xwing_pilot", null, true);
	public static final DeferredItem<ArmorItem> REBEL_FLEET_TROOPER = piece("rebel_fleet_trooper_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/rebel_fleet_trooper", null, true);
	public static final DeferredItem<ArmorItem> REBEL_FLEET_TROOPER_JACKET = piece("rebel_fleet_trooper_jacket", ArmorMaterials.DIAMOND, ArmorItem.Type.CHESTPLATE, "armor/rebel_fleet_trooper", null, true);
	public static final DeferredItem<ArmorItem> REBEL_HONOR_GUARD = piece("rebel_honor_guard_helmet", ArmorMaterials.DIAMOND, ArmorItem.Type.HELMET, "armor/rebel_honor_guard", null, true);

	public static void register(IEventBus bus)
	{
		ITEMS.register(bus);
		bus.addListener(ArmorContent::populateTab);
	}

	public static @Nullable Appearance appearance(ResourceLocation id)
	{
		return APPEARANCES.get(id);
	}

	private static void populateTab(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey().equals(CORE_ITEMS))
			for (var armor : ITEMS.getEntries())
				event.accept(armor.get());
	}

	private static ArmorSet set(String base, Holder<ArmorMaterial> material, String model, @Nullable String override, boolean hideOverlay)
	{
		return new ArmorSet(
				piece(base + "_helmet", material, ArmorItem.Type.HELMET, model, override, hideOverlay),
				piece(base + "_chestplate", material, ArmorItem.Type.CHESTPLATE, model, override, hideOverlay),
				piece(base + "_leggings", material, ArmorItem.Type.LEGGINGS, model, override, hideOverlay),
				piece(base + "_boots", material, ArmorItem.Type.BOOTS, model, override, hideOverlay));
	}

	private static DeferredItem<ArmorItem> piece(String id, Holder<ArmorMaterial> material, ArmorItem.Type type,
	                                            String model, @Nullable String override, boolean hideOverlay)
	{
		APPEARANCES.put(Galaxies.id(id), new Appearance(Galaxies.id(model),
				override == null ? null : Galaxies.id(override), hideOverlay));
		int durabilityFactor = material == ArmorMaterials.DIAMOND ? 33 : 5;
		return ITEMS.register(id, () -> new ArmorItem(material, type, new Item.Properties().durability(type.getDurability(durabilityFactor))));
	}

	public record Appearance(ResourceLocation model, @Nullable ResourceLocation baseTexture, boolean hideOverlay)
	{
	}

	public record ArmorSet(DeferredItem<ArmorItem> helmet, DeferredItem<ArmorItem> chestplate,
	                       DeferredItem<ArmorItem> leggings, DeferredItem<ArmorItem> boots)
	{
	}

	private ArmorContent()
	{
	}
}
