package dev.pswg.mixin.client;

import dev.pswg.vehicles.Ship;
import dev.pswg.vehicles.client.VehicleClient;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class VehicleCameraMixin
{
	@Shadow protected abstract void setRotation(float yaw, float pitch, float roll);
	@Shadow protected abstract void setPosition(double x, double y, double z);
	@Shadow protected abstract void move(float back, float up, float side);
	@Shadow private float getMaxZoom(float distance) { throw new AssertionError(); }

	@Inject(method = "setup", at = @At("TAIL"))
	private void pswg$camera(BlockGetter level, Entity entity, boolean detached, boolean front, float partial, CallbackInfo ci)
	{
		if (!(entity.getVehicle() instanceof Ship ship) || !ship.kind().flying())
			return;
		var view = VehicleClient.view(ship, partial);
		var cockpit = entity.getEyePosition(partial);
		setPosition(cockpit.x, cockpit.y, cockpit.z);
		if (front)
			setRotation(view.yaw() + 180, -view.pitch(), -view.roll());
		else
			setRotation(view.yaw(), view.pitch(), view.roll());
		if (detached)
		{
			move(0, 1.5F, 0);
			move(-getMaxZoom(9), 0, 0);
		}
	}
}
