package net.mrconqueso.pswg.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.entity.custom.BlasterBoltEntity;
import net.mrconqueso.pswg.entity.custom.ChairEntity;
import net.mrconqueso.pswg.entity.custom.GeckoEntity;
import net.mrconqueso.pswg.entity.custom.TomahawkProjectileEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, PSWG.MOD_ID);

    public static final Supplier<EntityType<GeckoEntity>> GECKO =
            ENTITY_TYPES.register("gecko", () -> EntityType.Builder.of(GeckoEntity::new, MobCategory.CREATURE)
                    .sized(0.75f, 0.35f).build("gecko"));

    public static final Supplier<EntityType<TomahawkProjectileEntity>> TOMAHAWK =
            ENTITY_TYPES.register("tomahawk", () -> EntityType.Builder.<TomahawkProjectileEntity>of(TomahawkProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5f, 1.15f).build("tomahawk"));

    public static final Supplier<EntityType<BlasterBoltEntity>> BLASTER_BOLT =
            ENTITY_TYPES.register("blaster_bolt", () -> EntityType.Builder.<BlasterBoltEntity>of(BlasterBoltEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).build("blaster_bolt"));

    public static final Supplier<EntityType<ChairEntity>> CHAIR_ENTITY =
            ENTITY_TYPES.register("chair_entity", () -> EntityType.Builder.of(ChairEntity::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).build("chair_entity"));


    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
