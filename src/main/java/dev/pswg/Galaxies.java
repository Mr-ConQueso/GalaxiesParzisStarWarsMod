package dev.pswg;

import com.mojang.logging.LogUtils;
import dev.pswg.command.DimensionCommand;
import dev.pswg.container.DrinkItems;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.container.Tatooine;
import dev.pswg.datagen.G3dData;
import dev.pswg.datagen.TerrainData;
import dev.pswg.model.g3d.G3dResources;
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
	}
}
