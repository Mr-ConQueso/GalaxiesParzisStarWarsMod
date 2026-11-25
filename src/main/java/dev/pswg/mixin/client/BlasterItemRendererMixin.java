package dev.pswg.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.client.BlasterClient;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemRenderer.class)
abstract class BlasterItemRendererMixin
{
	@WrapOperation(method = "renderStatic(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;III)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"))
	private void pswg$renderHeldBlaster(ItemRenderer renderer, ItemStack stack, ItemDisplayContext display, boolean left,
	                                    PoseStack pose, MultiBufferSource buffers, int light, int overlay, BakedModel model,
	                                    Operation<Void> original, LivingEntity holder)
	{
		if (holder == null || !BlasterItem.isBlaster(stack))
			original.call(renderer, stack, display, left, pose, buffers, light, overlay, model);
		else
			BlasterClient.withHeldItem(holder, stack, display, left, pose, buffers, model,
					() -> original.call(renderer, stack, display, left, pose, buffers, light, overlay, model));
	}
}
