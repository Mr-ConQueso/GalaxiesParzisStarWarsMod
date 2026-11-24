package net.mrconqueso.pswg.item.blaster;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.item.ModItems;
import net.mrconqueso.pswg.item.custom.BlasterItem;

public class HasAttachmentProperty {

    /**
     * Registers float-based properties for 1.21.1 models to evaluate (1.0F if present, 0.0F if not)
     */
    public static void registerClient() {
        ItemProperties.register(ModItems.BLASTER.get(), ResourceLocation.fromNamespaceAndPath("pswg", "has_bipod"),
                (stack, level, entity, seed) -> {
                    var attachments = BlasterItem.getAttachments(stack);
                    // Return 1.0F if condition is met, otherwise 0.0F
                    return attachments.applied().containsValue(ResourceLocation.fromNamespaceAndPath("pswg", "e11/bipod")) ? 1.0F : 0.0F;
                });
    }
}
