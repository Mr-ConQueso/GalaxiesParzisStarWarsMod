package dev.pswg.mixin.client;

import dev.pswg.vehicles.client.VehicleClient;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class VehicleMouseMixin
{
	@Shadow private double accumulatedDX;
	@Shadow private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
	private void pswg$pilotMouse(double frameTime, CallbackInfo callback)
	{
		if (VehicleClient.collectMouse(accumulatedDX, accumulatedDY))
			callback.cancel();
	}
}
