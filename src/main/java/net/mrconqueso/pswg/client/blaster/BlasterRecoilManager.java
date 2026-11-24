package net.mrconqueso.pswg.client.blaster;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.HumanoidArm;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.util.math.Ease;
import net.mrconqueso.pswg.util.math.MathUtil;
import org.joml.Quaternionf;

public class BlasterRecoilManager
{
	private static final float recoilCompAccel = 1.25f;

	private static float impulse = 0;
	private static float verticalVelocity = 0;
	private static float horizontalVelocity = 0;

	public static void tick(Minecraft mc)
	{
		if (verticalVelocity + recoilCompAccel < 0)
		{
			verticalVelocity += recoilCompAccel;

			if (mc.player != null)
			{
				var p = mc.player.getXRot();
				mc.player.xRotO = p;
				mc.player.setXRot(p + verticalVelocity);
			}
		}
		else
			verticalVelocity = 0;

		var horizVelSign = Math.signum(horizontalVelocity);
		var hComp = recoilCompAccel * -horizVelSign;

		if (horizVelSign == Math.signum(horizontalVelocity + hComp))
		{
			horizontalVelocity += hComp;

			if (mc.player != null)
			{
				var y = mc.player.getYRot();
				mc.player.yRotO = y;
				mc.player.setYRot(y + horizontalVelocity);
			}
		}
		else
			horizontalVelocity = 0;

		impulse--;
		impulse = Math.max(impulse, 0);
	}

	public static float getVerticalRecoilMovement(float tickDelta)
	{
		if (verticalVelocity + recoilCompAccel >= 0)
			return 0;
		return (verticalVelocity + recoilCompAccel) * tickDelta;
	}

	public static float getHorizontalRecoilMovement(float tickDelta)
	{
		var horizVelSign = Math.signum(horizontalVelocity);
		var comp = recoilCompAccel * -horizVelSign;

		if (horizVelSign != Math.signum(horizontalVelocity + comp))
			return 0;
		return (horizontalVelocity + comp) * tickDelta;
	}

	public static void setRecoil(float vertical, float horizontal)
	{
		impulse = 8;
		verticalVelocity = -vertical;
		horizontalVelocity = horizontal;
	}

	// NOTE: 1.21.1 uses CustomPacketPayload, adapt signature to your new Payload Handler
	public static void handleAccumulateRecoil(float horiz, float vert)
	{
		Minecraft.getInstance().execute(() -> {
			setRecoil(vert, horiz);
		});
	}

	public static void applyCameraShake(Minecraft mc, PoseStack poseStack, Camera camera, float tickDelta, double fov)
	{
		if (mc.player == null || !Resources.CONFIG.get().view.enableScreenShake || fov < 1)
			return;

		var smoothImpulse = Ease.inCubic(Math.max(impulse - tickDelta, 0) / 6);
		var fovCompensatedImpulse = smoothImpulse * (13 / fov);
		var dz = -0.2 * fovCompensatedImpulse;

		poseStack.translate(0, 0, dz);

		var scale = mc.options.mainHand().get() == HumanoidArm.LEFT ? 1 : -1;
		poseStack.mulPose(new Quaternionf().rotationY(MathUtil.toRadians(0.1f * scale * smoothImpulse)));
		poseStack.mulPose(new Quaternionf().rotationZ(MathUtil.toRadians(-0.4f * smoothImpulse)));
	}
}