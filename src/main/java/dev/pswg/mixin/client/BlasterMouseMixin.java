package dev.pswg.mixin.client;

import dev.pswg.blasters.client.BlasterSight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class BlasterMouseMixin
{
	@Shadow private double accumulatedDX;
	@Shadow private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"))
	private void pswg$scaleBlasterAim(double timeDelta, CallbackInfo callback)
	{
		float zoom = BlasterSight.zoom(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
		accumulatedDX /= zoom;
		accumulatedDY /= zoom;
	}
}
