package net.mrconqueso.pswg.event;

import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.entity.ModEntities;
import net.mrconqueso.pswg.entity.custom.GeckoEntity;
import net.mrconqueso.pswg.entity.client.GeckoModel;
import net.mrconqueso.pswg.entity.client.TomahawkProjectileModel;
import net.mrconqueso.pswg.entity.client.BlasterBoltProjectileModel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;

public class ModEventBusEvents
{
    // -------------------------------------------------------------
    // COMMON EVENTS (Runs on both Client and Dedicated Server)
    // -------------------------------------------------------------
    @EventBusSubscriber(modid = PSWG.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
    public static class CommonSetup
    {
        @SubscribeEvent
        public static void registerAttributes(EntityAttributeCreationEvent event) {
            event.put(ModEntities.GECKO.get(), GeckoEntity.createAttributes().build());
        }

        @SubscribeEvent
        public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
            // NOTE: In 1.21.1, Spawn Placements often require a custom PlacementPredicate if the entity isn't a standard animal.
            event.register(ModEntities.GECKO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
    }

    // -------------------------------------------------------------
    // CLIENT-ONLY EVENTS (Will crash server if not separated)
    // -------------------------------------------------------------
    @EventBusSubscriber(modid = PSWG.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientSetup
    {
        @SubscribeEvent
        public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(GeckoModel.LAYER_LOCATION, GeckoModel::createBodyLayer);
            event.registerLayerDefinition(TomahawkProjectileModel.LAYER_LOCATION, TomahawkProjectileModel::createBodyLayer);
            event.registerLayerDefinition(BlasterBoltProjectileModel.LAYER_LOCATION, BlasterBoltProjectileModel::createBodyLayer);
        }
    }
}