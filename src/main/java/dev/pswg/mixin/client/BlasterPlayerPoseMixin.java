package dev.pswg.mixin.client;

import dev.pswg.blasters.client.LegacyBlasterPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
abstract class BlasterPlayerPoseMixin
{
	@Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/HumanoidModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", shift = At.Shift.AFTER))
	private void pswg$poseBlaster(LivingEntity entity, float limbAngle, float limbDistance, float age,
	                              float headYaw, float headPitch, CallbackInfo callback)
	{
		LegacyBlasterPose.apply((PlayerModel<?>) (Object) this, entity, limbAngle, limbDistance, age);
	}
}
