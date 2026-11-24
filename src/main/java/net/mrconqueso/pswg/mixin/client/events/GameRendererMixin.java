//package net.mrconqueso.pswg.mixin.client.events;
//
//import com.mojang.blaze3d.vertex.PoseStack;
//import net.minecraft.client.Minecraft;
//import net.minecraft.client.renderer.GameRenderer;
//import net.minecraft.world.entity.LivingEntity;
//import net.mrconqueso.pswg.util.ModTags;
//import org.spongepowered.asm.mixin.Mixin;
//import org.spongepowered.asm.mixin.Shadow;
//import org.spongepowered.asm.mixin.injection.At;
//import org.spongepowered.asm.mixin.injection.Inject;
//import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
//@Mixin(GameRenderer.class)
//public abstract class GameRendererMixin
//{
//	@Shadow
//	public abstract Minecraft getMinecraft();
//
//	@Inject(at = @At("HEAD"), method = "bobHurt", cancellable = true)
//	void tiltViewWhenHurt(CameraRenderState cameraState, PoseStack poseStack, CallbackInfo ci)
//	{
//		if (getMinecraft().getCameraEntity() instanceof LivingEntity livingEntity)
//			if (livingEntity.getLastDamageSource() != null && livingEntity.getLastDamageSource().is(ModTags.Damage.IGNORES_DAMAGE_TILT))
//				ci.cancel();
//	}
//}
