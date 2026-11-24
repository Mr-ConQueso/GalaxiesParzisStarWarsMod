package dev.pswg;

import dev.pswg.container.Crates;
import dev.pswg.rendering.DrinkColors;
import dev.pswg.rendering.g3d.G3dBlockGeometry;
import dev.pswg.rendering.g3d.G3dWornArmor;
import dev.pswg.rendering.models.ConnectedTextureGeometry;
import dev.pswg.rendering.ptex.PtexSpriteSource;
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
	}

	@SubscribeEvent
	public static void registerReloadListeners(RegisterClientReloadListenersEvent event)
	{
		event.registerReloadListener((ResourceManagerReloadListener) G3dWornArmor::reload);
	}

	private GalaxiesClient()
	{
	}
}
