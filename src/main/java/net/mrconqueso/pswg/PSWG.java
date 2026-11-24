package net.mrconqueso.pswg;


import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTabs;
import net.mrconqueso.pswg.attributes.ModEntityAttributes;
import net.mrconqueso.pswg.block.ModBlocks;
import net.mrconqueso.pswg.block.entity.ModBlockEntities;
import net.mrconqueso.pswg.block.entity.renderer.PedestalBlockEntityRenderer;
import net.mrconqueso.pswg.client.blaster.BlasterHudRenderer;
import net.mrconqueso.pswg.component.ModDataComponents;
import net.mrconqueso.pswg.data.CodecDataLoader;
import net.mrconqueso.pswg.data.IdentifierUtil;
import net.mrconqueso.pswg.data.blaster.BlasterDatapackDefinition;
import net.mrconqueso.pswg.effect.ModEffects;
import net.mrconqueso.pswg.enchantment.ModEnchantmentEffects;
import net.mrconqueso.pswg.entity.ModEntities;
import net.mrconqueso.pswg.entity.ShipEntity;
import net.mrconqueso.pswg.entity.client.BlasterBoltProjectileRenderer;
import net.mrconqueso.pswg.entity.client.ChairRenderer;
import net.mrconqueso.pswg.entity.client.GeckoRenderer;
import net.mrconqueso.pswg.entity.client.TomahawkProjectileRenderer;
import net.mrconqueso.pswg.interaction.LeftClickingEntityAttachment;
import net.mrconqueso.pswg.interaction.RecoilEntityAttachment;
import net.mrconqueso.pswg.item.ModCreativeModeTabs;
import net.mrconqueso.pswg.item.ModItems;
import net.mrconqueso.pswg.item.blaster.BlasterComponents;
import net.mrconqueso.pswg.item.blaster.HasAttachmentProperty;
import net.mrconqueso.pswg.item.custom.BlasterItem;
import net.mrconqueso.pswg.loot.ModLootModifiers;
import net.mrconqueso.pswg.networking.SwgPackets;
import net.mrconqueso.pswg.particle.BismuthParticles;
import net.mrconqueso.pswg.particle.ModParticles;
import net.mrconqueso.pswg.potion.ModPotions;
import net.mrconqueso.pswg.recipe.ModRecipes;
import net.mrconqueso.pswg.screen.ModMenuTypes;
import net.mrconqueso.pswg.screen.custom.GrowthChamberScreen;
import net.mrconqueso.pswg.screen.custom.PedestalScreen;
import net.mrconqueso.pswg.sound.ModSounds;
import net.mrconqueso.pswg.util.ModItemProperties;
import net.mrconqueso.pswg.villager.ModVillagers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(PSWG.MOD_ID)
public class PSWG {

    public static final String MOD_ID = "pswg";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String I18N_VENT_BLASTER = "key.pswg.vent_blaster";
    private static final BlasterHudRenderer hudRenderer = new BlasterHudRenderer();

    /**
     * Create a derived logger for a subsystem within PSWG
     *
     * @param key The sub-header to log underneath the modid
     *
     * @return The nested logger
     */
    public static Logger createSubLogger(String key)
    {
        return LoggerFactory.getLogger(MOD_ID + "/" + key);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final ResourceLocation DEFAULT_HUD = id("default");

    public static final CodecDataLoader<BlasterDatapackDefinition> DATAPACK_LOADER = new CodecDataLoader<>(
            id("data"), // Uses your ResourceLocation helper method
            "blasters",
            true,
            IdentifierUtil::isJsonFile, // Assuming IdentifierUtil is updated to use ResourceLocation
            BlasterDatapackDefinition.CODEC
    );

    public PSWG(IEventBus modEventBus, ModContainer modContainer) {

        modEventBus.addListener(this::commonSetup);

        NeoForge.EVENT_BUS.register(this);

        BlasterComponents.COMPONENTS.register(modEventBus);

        ModEntityAttributes.ATTRIBUTES.register(modEventBus);
        LeftClickingEntityAttachment.ATTACHMENT_TYPES.register(modEventBus);
        RecoilEntityAttachment.ATTACHMENT_TYPES.register(modEventBus);

        ModCreativeModeTabs.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);

        ModDataComponents.register(modEventBus);
        ModSounds.register(modEventBus);

        ModEffects.register(modEventBus);
        ModPotions.register(modEventBus);

        ModEnchantmentEffects.register(modEventBus);
        ModEntities.register(modEventBus);

        ModVillagers.register(modEventBus);

        ModParticles.register(modEventBus);

        ModLootModifiers.register(modEventBus);

        ModBlockEntities.register(modEventBus);

        ModMenuTypes.register(modEventBus);

        ModRecipes.register(modEventBus);

        modEventBus.addListener(this::addCreative);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1.0");

        registrar.playToServer(
                SwgPackets.ShipControlsPayload.TYPE,
                SwgPackets.ShipControlsPayload.STREAM_CODEC,
                (payload, context) -> ShipEntity.handleControlPacket(payload.controls(), context)
        );

        registrar.playToServer(
                SwgPackets.ShipRotationPayload.TYPE,
                SwgPackets.ShipRotationPayload.STREAM_CODEC,
                (payload, context) -> ShipEntity.handleRotationPacket(payload.rotation(), context)
        );

        registrar.playToServer(
                SwgPackets.ShipFirePayload.TYPE,
                SwgPackets.ShipFirePayload.STREAM_CODEC,
                (payload, context) -> ShipEntity.handleFirePacket(context)
        );
    }

    private void commonSetup(final FMLCommonSetupEvent event) { }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {

        if(event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.BISMUTH);
            event.accept(ModItems.RAW_BISMUTH);
        }

        if(event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModBlocks.BISMUTH_BLOCK);
            event.accept(ModBlocks.BISMUTH_ORE);
        }
    }

    public static void onTooltip(ItemTooltipEvent event) {
        if (event.getItemStack().is(ModItems.BLASTER.get())) {
            event.getToolTip().add(Component.translatable(BlasterItem.MISSING_ID.toLanguageKey()));
            event.getToolTip().add(Component.translatable(I18N_VENT_BLASTER));
        }
    }

    public static void onRenderCrosshair(RenderGuiLayerEvent.Post event) {
        if (event.getName().equals(VanillaGuiLayers.CROSSHAIR)) {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null) return;

            var stack = client.player.getMainHandItem();
            if (stack.is(ModItems.BLASTER.get())) {
                hudRenderer.renderCrosshair(event.getGuiGraphics(), client.player, InteractionHand.MAIN_HAND, stack);
            }
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) { }

    @EventBusSubscriber(modid = MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

            event.enqueueWork(HasAttachmentProperty::registerClient);

            ModItemProperties.addCustomItemProperties();

            EntityRenderers.register(ModEntities.GECKO.get(), GeckoRenderer::new);
            EntityRenderers.register(ModEntities.TOMAHAWK.get(), TomahawkProjectileRenderer::new);
            EntityRenderers.register(ModEntities.CHAIR_ENTITY.get(), ChairRenderer::new);
            EntityRenderers.register(ModEntities.BLASTER_BOLT.get(), BlasterBoltProjectileRenderer::new);

            NeoForge.EVENT_BUS.addListener(PSWG::onTooltip);
            NeoForge.EVENT_BUS.addListener(PSWG::onRenderCrosshair);
        }

        @SubscribeEvent
        public static void registerParticleFactories(RegisterParticleProvidersEvent event) {

            event.registerSpriteSet(ModParticles.BISMUTH_PARTICLES.get(), BismuthParticles.Provider::new);
        }

        @SubscribeEvent
        public static void registerBER(EntityRenderersEvent.RegisterRenderers event) {

            event.registerBlockEntityRenderer(ModBlockEntities.PEDESTAL_BE.get(), PedestalBlockEntityRenderer::new);
        }

        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {

            event.register(ModMenuTypes.PEDESTAL_MENU.get(), PedestalScreen::new);
            event.register(ModMenuTypes.GROWTH_CHAMBER_MENU.get(), GrowthChamberScreen::new);
        }
    }

    /**
     * Gets the fractional number of ticks accumulated in the frames rendered
     * since the last game tick.
     *
     * @return The fractional number of ticks [0,1)
     */
    public static float getTickDelta()
    {
        // Safety check: Minecraft.getInstance() only exists on the physical client
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        }
        return 0.0F;
    }
} 