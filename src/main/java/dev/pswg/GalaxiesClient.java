package dev.pswg;

import dev.pswg.container.Crates;
import dev.pswg.gadgets.Workstations;
import dev.pswg.gadgets.client.MixerScreen;
import dev.pswg.gadgets.client.ScrappingScreen;
import dev.pswg.gadgets.client.DrillGeometry;
import dev.pswg.rendering.DrinkColors;
import dev.pswg.rendering.g3d.G3dBlockGeometry;
import dev.pswg.rendering.g3d.G3dWornArmor;
import dev.pswg.rendering.models.ConnectedTextureGeometry;
import dev.pswg.rendering.ptex.PtexSpriteSource;
import dev.pswg.rendering.p3d.P3dGeometry;
import dev.pswg.rendering.p3d.P3dResources;
import dev.pswg.rendering.nem.NemModels;
import dev.pswg.screen.CorrugatedCrateScreen;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

@EventBusSubscriber(modid = Galaxies.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GalaxiesClient
{
	@SubscribeEvent
	public static void registerModels(ModelEvent.RegisterGeometryLoaders event)
	{
		event.register(Galaxies.id("g3d"), G3dBlockGeometry.LOADER);
		event.register(Galaxies.id("p3d"), P3dGeometry.LOADER);
		event.register(Galaxies.id("extraction_drill"), DrillGeometry.LOADER);
		event.register(Galaxies.id("connected_texture"), ConnectedTextureGeometry.LOADER);
	}

	@SubscribeEvent
	public static void registerBlasterModels(ModelEvent.RegisterAdditional event)
	{
		for (String name : new String[] { "galaar15", "e11d", "e11d_counter", "e11d_flashlight" })
		{
			var id = ResourceLocation.fromNamespaceAndPath("pswg_blasters", "item/" + name);
			event.register(ModelResourceLocation.standalone(id));
		}
	}

	@SubscribeEvent
	public static void registerSpriteSources(RegisterSpriteSourceTypesEvent event)
	{
		PtexSpriteSource.register(event);
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event)
	{
		DrinkColors.register(event);
	}

	@SubscribeEvent
	public static void registerScreens(RegisterMenuScreensEvent event)
	{
		event.register(Crates.MENU.get(), CorrugatedCrateScreen::new);
		event.register(Crates.KYBER_MENU.get(), CorrugatedCrateScreen::new);
		event.register(Crates.TOOLBOX_MENU.get(), CorrugatedCrateScreen::new);
		event.register(Crates.SEGMENTED_MENU.get(), CorrugatedCrateScreen::new);
		event.register(Workstations.MIXER_MENU.get(), MixerScreen::new);
		event.register(Workstations.SCRAPPING_MENU.get(), ScrappingScreen::new);
	}

	@SubscribeEvent
	public static void registerReloadListeners(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener((ResourceManagerReloadListener) G3dWornArmor::reload);
		event.registerReloadListener(P3dResources.MODELS);
		event.registerReloadListener(NemModels.INSTANCE);
	}

	private GalaxiesClient()
	{
	}
}
