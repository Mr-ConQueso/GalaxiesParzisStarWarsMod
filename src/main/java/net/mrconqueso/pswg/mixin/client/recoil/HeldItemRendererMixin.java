package net.mrconqueso.pswg.mixin.client.recoil;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.mrconqueso.pswg.interaction.IRecoilEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class HeldItemRendererMixin
{
	@Inject(method = "renderItem", at = @At("HEAD"))
	private void pswg$renderRecoil(LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, boolean leftHand, PoseStack matrices, MultiBufferSource buffer, int seed, CallbackInfo ci)
	{
		if (!(entity instanceof IRecoilEntity recoilEntity))
			return;

		// Get partial ticks for smooth rendering
		float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

		// Calculate the time passed since the last shot in ticks
		var recoilTime = entity.level().getGameTime() - recoilEntity.pswg$getRecoilTime() + partialTick;

		// Only apply effect for the first 20 ticks (1 second) after firing
		if (recoilTime <= 0 || recoilTime >= 20)
			return;

		// Recoil effect parameters
		var effectDepth = 0.4f;     // How far the gun kicks back (Z axis)
		var recoverSpeed = 0.8f;    // How fast it returns to position
		var recoilDuration = 1f;    // Peak of the kick
		var recoilAngle = 25;       // Degrees of rotation

		// Recoil math: quick kick back, exponential decay return
		var f = (float)(effectDepth * Math.min(
				Math.pow(recoilTime / recoilDuration, 2),
				Math.exp(-recoverSpeed * (recoilTime - recoilDuration))
		));

		// Apply the transformation to the PoseStack
		// Rotate the gun upward
		matrices.mulPose(new Quaternionf().rotateX((float)Math.toRadians(f * recoilAngle)));
		// Move the gun backward
		matrices.translate(0, 0, f);
	}
}