package dev.pswg.mobs.client;

import dev.pswg.mobs.Worrt;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

final class WorrtPose
{
	private WorrtPose() {}

	static void apply(ModelPart body, Worrt worrt, float partial, float age, float yaw, float pitch)
	{
		float airborne = worrt.isNoAi() ? 0 : worrt.airborneProgress(partial);
		float landing = 1 - airborne;
		boolean rising = worrt.getDeltaMovement().y > 0;
		float curl = (1 - Mth.cos(airborne * Mth.PI));
		body.xRot = rising ? curl / 4 : 0.5F - (1 - Mth.cos(landing * landing * landing * Mth.PI)) / 4;
		var head = body.getChild("head");
		head.xRot = -curl / 7;
		body.getChild("jaw").xRot = head.xRot;
		float yawRadians = yaw * Mth.DEG_TO_RAD;
		float pitchRadians = pitch * Mth.DEG_TO_RAD;
		poseSide(body, head, "Left", airborne, rising, yawRadians, pitchRadians, Mth.sin(age * 0.02F), -1);
		poseSide(body, head, "Right", airborne, rising, yawRadians, pitchRadians, Mth.cos(age * 0.02F), 1);
	}

	private static void poseSide(ModelPart body, ModelPart head, String side, float airborne, boolean rising,
	                             float yaw, float pitch, float antennaMotion, int eyeDirection)
	{
		var antenna = head.getChild("antenna" + side + "1");
		antenna.xRot = 0.4363323F + antennaMotion * 0.1F;
		antenna.yRot = eyeDirection * 0.6981317F + yaw / 6;
		antenna.getChild("antenna" + side + "2").xRot = 0.89919364F + antennaMotion * 0.05F + pitch;
		var eye = head.getChild("eye" + side);
		eye.yRot = eyeDirection * 0.34906584F + yaw / 4;
		eye.xRot = pitch;

		var leg = body.getChild("leg" + side + "1");
		var knee = leg.getChild("leg" + side + "2");
		leg.xRot = 0.34906584F + airborne * 2.6F;
		knee.xRot = -0.78539816F - airborne * 2;
		knee.getChild("foot" + side).xRot = -1.0821041F + airborne * airborne * airborne * 1.1F;
		var arm = body.getChild("arm" + side + "1");
		var elbow = arm.getChild("arm" + side + "2");
		float landing = 1 - airborne;
		arm.xRot = -0.17453292F - (rising ? airborne * airborne * airborne : airborne) * 1.2F;
		elbow.xRot = -0.10899687F - (float) Math.pow(landing, rising ? 3 : 4) * 1.2F;
		elbow.getChild("hand" + side).xRot = rising ? 0.08726646F - airborne * 0.8F
				: -0.71273357F + (float) Math.pow(landing, 6) * 0.8F;
	}
}
