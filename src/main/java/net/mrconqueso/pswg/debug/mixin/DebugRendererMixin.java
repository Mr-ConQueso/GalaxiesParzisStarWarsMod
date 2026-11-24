package net.mrconqueso.pswg.debug.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.mrconqueso.pswg.debug.DebugUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DebugRenderer.class)
public class DebugRendererMixin {

	@Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;DDD)V", at = @At("TAIL"))
	public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, double cameraX, double cameraY, double cameraZ, CallbackInfo ci) {
		DebugUtil.render(poseStack, bufferSource, cameraX, cameraY, cameraZ);
	}
}