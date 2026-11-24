package dev.pswg.mixin.client;

import dev.pswg.gadgets.Workstations;
import dev.pswg.gadgets.scrapping.LaserCutter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class LaserCutterMouseMixin
{
	@Shadow private double accumulatedDX;
	@Shadow private double accumulatedDY;

	@Inject(method = "turnPlayer", at = @At("HEAD"))
	private void pswg$slowCutterAim(double timeDelta, CallbackInfo callback)
	{
		var player = Minecraft.getInstance().player;
		if (player == null || !player.isUsingItem())
			return;
		var stack = player.getUseItem();
		if (!(stack.getItem() instanceof LaserCutter))
			return;
		var target = stack.get(Workstations.CUT_TARGET.get());
		if (target == null)
			return;

		double range = player.blockInteractionRange();
		double distanceFactor = Mth.clamp(1 - player.position().distanceTo(Vec3.atCenterOf(target)) / range, 0.05, 1);
		double wear = (double) stack.getDamageValue() / Math.max(1, stack.getMaxDamage());
		double limit = (1.125 - 0.125 * wear) * distanceFactor;
		double delta = Math.hypot(accumulatedDX, accumulatedDY);
		if (delta > limit)
		{
			accumulatedDX *= limit / delta;
			accumulatedDY *= limit / delta;
		}
	}
}
