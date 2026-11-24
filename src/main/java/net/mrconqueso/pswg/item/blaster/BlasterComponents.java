package net.mrconqueso.pswg.item.blaster;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.item.custom.BlasterItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlasterComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, PSWG.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> ID = COMPONENTS.registerComponentType(
            "id", builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC)
    );

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> SERIAL = COMPONENTS.registerComponentType(
            "serial", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG)
    );

    // The current heat/cooling state of the blaster
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlasterItem.StateComponent>> STATE = COMPONENTS.registerComponentType(
            "state", builder -> builder.persistent(BlasterItem.StateComponent.CODEC).networkSynchronized(BlasterItem.StateComponent.PACKET_CODEC)
    );

    // The CURRENTLY applied attachments (the ones actually on the gun right now)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlasterItem.AttachmentsComponent>> ATTACHMENTS = COMPONENTS.registerComponentType(
            "applied_attachments", builder -> builder.persistent(BlasterItem.AttachmentsComponent.CODEC).networkSynchronized(BlasterItem.AttachmentsComponent.PACKET_CODEC)
    );

    // --- ADD THESE TWO BELOW ---

    // The base performance stats (damage, range, heat capacity) defined in the datapack
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlasterItem.StatsComponent>> STATS = COMPONENTS.registerComponentType(
            "stats", builder -> builder.persistent(BlasterItem.StatsComponent.CODEC).networkSynchronized(BlasterItem.StatsComponent.PACKET_CODEC)
    );

    // The attachment options (which scopes/barrels can be put on this specific blaster)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BlasterItem.AvailableAttachmentsComponent>> AVAILABLE_ATTACHMENTS = COMPONENTS.registerComponentType(
            "available_attachments", builder -> builder.persistent(BlasterItem.AvailableAttachmentsComponent.CODEC).networkSynchronized(BlasterItem.AvailableAttachmentsComponent.PACKET_CODEC)
    );
}