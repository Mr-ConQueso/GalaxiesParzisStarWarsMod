package dev.pswg.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.rendering.g3d.G3dWornArmor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidArmorLayer.class)
abstract class HumanoidArmorLayerMixin
{
	@Inject(method = "renderArmorPiece(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;"
			+ "Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;ILnet/minecraft/client/model/HumanoidModel;FFFFFF)V",
			at = @At("HEAD"), cancellable = true)
	private void pswg$renderWornArmor(PoseStack stack, MultiBufferSource buffers, LivingEntity wearer, EquipmentSlot slot,
	                                  int light, HumanoidModel<?> vanillaArmor, float limbSwing, float limbSwingAmount,
	                                  float partialTick, float ageInTicks, float headYaw, float headPitch, CallbackInfo callback)
	{
		var humanoid = ((HumanoidArmorLayer<?, ?, ?>) (Object) this).getParentModel();
		if (G3dWornArmor.render(stack, buffers, wearer, slot, light, humanoid))
			callback.cancel();
	}
}
