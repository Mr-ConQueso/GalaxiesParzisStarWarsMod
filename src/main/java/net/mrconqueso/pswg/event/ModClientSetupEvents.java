package net.mrconqueso.pswg.event;

import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.client.blaster.BlasterItemRenderer;
import net.mrconqueso.pswg.client.PSWGClient;
import net.mrconqueso.pswg.item.custom.BlasterItem;
import net.mrconqueso.pswg.p3d.P3dManager;
import net.mrconqueso.pswg.util.client.render.ICustomItemRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;

@EventBusSubscriber(modid = PSWG.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientSetupEvents
{
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        // Block Entity Renderers
//        event.registerBlockEntityRenderer(SwgBlocks.Workbench.BlasterBlockEntityType, BlasterWorkbenchWeaponRenderer::new);
//        event.registerBlockEntityRenderer(SwgBlocks.Power.CouplingBlockEntityType, PowerCouplingCableRenderer::new);
//        event.registerBlockEntityRenderer(SwgBlocks.Cage.CreatureCageBlockEntityType, TerrariumRenderer::new);
//
//        // Entity Renderers
//        event.registerEntityRenderer(SwgEntities.Ship.T65bXwing, T65BXwingRenderer::new);
//        event.registerEntityRenderer(SwgEntities.Speeder.X34, X34LandspeederRenderer::new);
        // ... (Add all other EntityRendererRegistry calls here)
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event)
    {
        event.register(PSWGClient.KEY_PRIMARY_ITEM_ACTION);
        event.register(PSWGClient.KEY_SECONDARY_ITEM_ACTION);
        event.register(PSWGClient.KEY_SHIP_INPUT_MODE_OVERRIDE);
        event.register(PSWGClient.KEY_PATROL_POSTURE);
        event.register(PSWGClient.KEY_SPECIES_SELECT);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event)
    {
//        event.register(SwgScreenTypes.Crate.Octagon, CrateOctagonScreen::new);
//        event.register(SwgScreenTypes.Crate.MosEisley, CrateGenericSmallScreen::new);
        // ... (Add all HandledScreens.register calls here)
    }

    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Block event)
    {
//        event.register((state, world, pos, tintIndex) -> 0x8AB534, SwgBlocks.Tree.SequoiaLeaves);
//        event.register((state, world, pos, tintIndex) -> 0xFFFFFF, SwgBlocks.Tree.JaporLeaves);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event)
    {
//        event.register((stack, tintIndex) -> 0x8AB534, SwgBlocks.Tree.SequoiaLeaves);
//        event.register((stack, tintIndex) -> 0xFFFFFF, SwgBlocks.Tree.JaporLeaves);
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event)
    {
        event.registerReloadListener(PSWGClient.NEM_MANAGER);
        event.registerReloadListener(P3dManager.INSTANCE);
        event.registerReloadListener(BlasterItemRenderer.INSTANCE);
        // event.registerReloadListener(StatelessWaterRenderer.INSTANCE);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event)
    {
        event.enqueueWork(() -> {
            PSWGClient.blasterZoomOverlayDefault = new SpyglassZoomOverlay(Resources.id("textures/gui/overlay/default.png"));
            PSWGClient.blasterZoomOverlaySniper = new SpyglassZoomOverlay(Resources.id("textures/gui/overlay/sniper.png"));

            PSWGClient.blasterZoomInstance = new ZoomInstance(
                    Resources.id("blaster_zoom"),
                    10.0F,
                    new SmoothTransitionMode(),
                    new ZoomDivisorMouseModifier(),
                    null
            );

            // Custom armor registrations and custom item renderer assignments go here
            // ArmorRenderer.register(...);
            ICustomItemRenderer.register(BlasterItem.class, BlasterItemRenderer.INSTANCE);
            // ...

            // IMPORTANT 1.21.1 NOTE: BlockRenderLayerMap logic MUST be removed.
            // Transparency is now handled strictly in your block model JSON files using:
            // "render_type": "minecraft:cutout" or "minecraft:translucent"
        });
    }
}