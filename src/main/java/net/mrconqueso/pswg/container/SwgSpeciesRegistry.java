package net.mrconqueso.pswg.container;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.character.SpeciesFactory;
import net.mrconqueso.pswg.character.SpeciesGender;
import net.mrconqueso.pswg.character.SwgSpecies;
import net.mrconqueso.pswg.component.PlayerData;

import java.util.*;
import java.util.function.Function;

public class SwgSpeciesRegistry
{
	protected static final HashMap<ResourceLocation, Function<String, SwgSpecies>> SPECIES = new LinkedHashMap<>();

	// Species with special meaning internally
	public static final ResourceLocation METASPECIES_GLOBAL = Resources.id("global"); // "global" species contains shared textures
	public static final ResourceLocation METASPECIES_HUMANOID = Resources.id("humanoid"); // "humanoid" species contains shared textures for the "human" model
	public static final ResourceLocation METASPECIES_NONE = ResourceLocation.fromNamespaceAndPath(PSWG.MOD_ID, "none"); // "none" species delegates player models back to Minecraft

	// Normal species and variants
	public static final ResourceLocation SPECIES_AQUALISH = Resources.id("aqualish");
	public static final ResourceLocation SPECIES_BITH = Resources.id("bith");
	public static final ResourceLocation SPECIES_BOTHAN = Resources.id("bothan");
	public static final ResourceLocation SPECIES_CHAGRIAN = Resources.id("chagrian");
	public static final ResourceLocation SPECIES_CHISS = Resources.id("chiss");
	public static final ResourceLocation SPECIES_DEVARONIAN = Resources.id("devaronian");
	public static final ResourceLocation SPECIES_DUROS = Resources.id("duros");
	public static final ResourceLocation SPECIES_HUMAN = Resources.id("human");
	public static final ResourceLocation SPECIES_IKTOTCHI = Resources.id("iktotchi");
	public static final ResourceLocation SPECIES_JAWA = Resources.id("jawa");
	public static final ResourceLocation SPECIES_KAMINOAN = Resources.id("kaminoan");
	public static final ResourceLocation SPECIES_MON_CALAMARI = Resources.id("mon_calamari");
	public static final ResourceLocation SPECIES_PANTORAN = Resources.id("pantoran");
	public static final ResourceLocation SPECIES_RODIAN = Resources.id("rodian");
	public static final ResourceLocation SPECIES_TOGRUTA = Resources.id("togruta");
	public static final ResourceLocation SPECIES_TRANDOSHAN = Resources.id("trandoshan");
	public static final ResourceLocation SPECIES_TWILEK = Resources.id("twilek");
	public static final ResourceLocation SPECIES_WOOKIEE = Resources.id("wookiee");
	public static final ResourceLocation SPECIES_GOTAL = Resources.id("gotal");
	public static final ResourceLocation SPECIES_ONGREE = Resources.id("ongree");
	public static final ResourceLocation SPECIES_GRAN = Resources.id("gran");

	public static void registerAll(Map<ResourceLocation, SpeciesFactory> speciesPresets)
	{
		for (var entry : speciesPresets.entrySet())
			SPECIES.put(entry.getKey(), (serialized) -> entry.getValue().create(serialized));
	}

	public static Set<ResourceLocation> getAllSlugs()
	{
		return SPECIES.keySet();
	}

	public static Collection<Function<String, SwgSpecies>> getFactories()
	{
		return SPECIES.values();
	}

	public static SwgSpecies deserialize(String serialized)
	{
		var id = SwgSpecies.getSpeciesSlug(serialized);
		if (!SPECIES.containsKey(id))
			return null;

		return SPECIES.get(id).apply(serialized);
	}

	public static SwgSpecies create(ResourceLocation species, SpeciesGender gender)
	{
		return SPECIES.get(species).apply(SwgSpecies.toModel(species, gender).toString());
	}

	public static String getTranslationKey(SwgSpecies species)
	{
		if (species == null)
			return "species.pswg.none";
		return getTranslationKey(species.getSlug());
	}

	public static String getTranslationKey(ResourceLocation species)
	{
		return "species." + species.getNamespace() + "." + species.getPath();
	}

	public static void handleSetOwnSpecies(MinecraftServer minecraftServer, ServerPlayerEntity serverPlayerEntity, ServerPlayNetworkHandler serverPlayNetworkHandler, PacketByteBuf packetByteBuf, PacketSender packetSender)
	{
		var speciesString = packetByteBuf.readString();

		var c = PlayerData.getPersistentPublic(serverPlayerEntity);
		c.setCharacter(deserialize(speciesString));
	}
}
