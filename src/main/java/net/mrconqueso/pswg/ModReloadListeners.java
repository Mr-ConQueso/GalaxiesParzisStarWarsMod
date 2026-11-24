package net.mrconqueso.pswg;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

@EventBusSubscriber(modid = PSWG.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class ModReloadListeners {

    @SubscribeEvent
    public static void onReloadListeners(AddReloadListenerEvent event) {
        // Registers your loader to scan /data/pswg/blasters/*.json
        event.addListener(PSWG.DATAPACK_LOADER);
    }
}