package net.mrconqueso.pswg.attributes;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.mrconqueso.pswg.PSWG;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A collection of common entity attributes used in PSWG modules and addons
 */
public final class ModEntityAttributes
{
    // NeoForge: Use DeferredRegister for attributes
    public static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, PSWG.MOD_ID);

    public static final String I18N_ATTR_MULTIPLIER = "pswg.attribute.modifier.multiplier";

    /**
     * The ID of the {@link ModEntityAttributes#FIELD_OF_VIEW_ZOOM} attribute modifier
     */
    public static final ResourceLocation FIELD_OF_VIEW_ZOOM_ID = PSWG.id("field_of_view_zoom");

    /**
     * An entity attribute that can modify the entity's field of view. Units
     * are zoom multipliers (e.g. 2x, 5x, 10x zoom) and not field of view angle
     * multipliers.
     */
    public static final DeferredHolder<Attribute, Attribute> FIELD_OF_VIEW_ZOOM = ATTRIBUTES.register(
            "field_of_view_zoom",
            () -> new UnclampedEntityAttribute(
                    "%s.attribute.name.%s".formatted(FIELD_OF_VIEW_ZOOM_ID.getNamespace(), FIELD_OF_VIEW_ZOOM_ID.getPath()),
                    1
            ).setSyncable(true)
    );
}