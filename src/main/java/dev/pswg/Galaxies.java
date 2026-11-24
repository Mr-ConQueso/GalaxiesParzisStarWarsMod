package dev.pswg;

import com.mojang.logging.LogUtils;
import dev.pswg.blasters.BlasterDefinitions;
import dev.pswg.blasters.Blasters;
import dev.pswg.command.DimensionCommand;
import dev.pswg.container.ArmorContent;
import dev.pswg.container.CoreComponents;
import dev.pswg.container.CoreSounds;
import dev.pswg.container.Crates;
import dev.pswg.container.DrinkItems;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.container.GalaxiesItems;
import dev.pswg.container.Tatooine;
import dev.pswg.datagen.G3dData;
import dev.pswg.datagen.TerrainData;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.model.g3d.G3dResources;
import dev.pswg.structure.DerelictContainer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import org.slf4j.Logger;

@Mod(Galaxies.MODID)
public final class Galaxies
{
	public static final String MODID = "pswg";
	public static final Logger LOGGER = LogUtils.getLogger();

	public Galaxies(IEventBus modBus)
	{
		NeoForge.EVENT_BUS.addListener(Galaxies::registerReloadListeners);
		NeoForge.EVENT_BUS.addListener(DimensionCommand::register);
		GalaxiesBlocks.register(modBus);
		CoreComponents.register(modBus);
		CoreSounds.register(modBus);
		GalaxiesItems.register(modBus);
		ArmorContent.register(modBus);
		Blasters.register(modBus);
		Demolitions.register(modBus);
		DerelictContainer.register(modBus);
		Crates.register(modBus);
		DrinkItems.register(modBus);
		modBus.addListener(TerrainData::gather);
		modBus.addListener(G3dData::gather);
		modBus.addListener(Tatooine::register);
	}

	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(MODID, path);
	}

	private static void registerReloadListeners(AddReloadListenerEvent event)
	{
		event.addListener(G3dResources.SERVER);
		event.addListener(BlasterDefinitions.SERVER);
	}
}
