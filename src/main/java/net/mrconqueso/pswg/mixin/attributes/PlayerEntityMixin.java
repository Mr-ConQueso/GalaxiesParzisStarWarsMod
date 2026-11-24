package net.mrconqueso.pswg.mixin.attributes;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.mrconqueso.pswg.attributes.ModEntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends our custom attributes to the player's attribute builder
 */
@Mixin(Player.class)
public class PlayerEntityMixin
{
    @Inject(method = "createAttributes", at = @At("RETURN"))
    private static void modifyPlayerAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir)
    {
        AttributeSupplier.Builder builder = cir.getReturnValue();
        builder.add(ModEntityAttributes.FIELD_OF_VIEW_ZOOM);
    }
}

