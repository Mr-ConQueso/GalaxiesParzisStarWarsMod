package dev.pswg;

import dev.pswg.rendering.DrinkColors;
import dev.pswg.rendering.g3d.G3dBlockGeometry;
import dev.pswg.rendering.ptex.PtexSpriteSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

@EventBusSubscriber(modid = Galaxies.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GalaxiesClient
{
	@SubscribeEvent
	public static void registerModels(ModelEvent.RegisterGeometryLoaders event)
	{
		event.register(Galaxies.id("g3d"), G3dBlockGeometry.LOADER);
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

	private GalaxiesClient()
	{
	}
}
