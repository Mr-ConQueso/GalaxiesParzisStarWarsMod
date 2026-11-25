package dev.pswg.blasters.client;

import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.Blasters;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class LegacyBlasterPose
{
	public static void apply(PlayerModel<?> model, LivingEntity entity, float limbAngle, float limbDistance, float age)
	{
		if (!(entity instanceof Player player) || player.isSwimming())
			return;
		model.head.zRot = 0;
		if (player == BlasterSight.firstPersonPlayer())
			return;
		var main = player.getMainHandItem();
		var off = player.getOffhandItem();
		var mainDefinition = BlasterItem.definition(player.level(), main);
		var offDefinition = BlasterItem.definition(player.level(), off);
		var mainLegacy = mainDefinition == null ? null : mainDefinition.legacy().orElse(null);
		var offLegacy = offDefinition == null ? null : offDefinition.legacy().orElse(null);
		if (mainLegacy == null && offLegacy == null)
			return;

		boolean patrol = player.getData(Blasters.PATROL);
		if (mainLegacy != null && offLegacy != null)
		{
			if (mainLegacy.oneHanded() && offLegacy.oneHanded())
				dual(model, player, limbAngle, limbDistance, age, patrol);
			return;
		}
		boolean mainHand = mainLegacy != null;
		boolean left = (player.getMainArm() == HumanoidArm.LEFT) == mainHand;
		var primary = left ? model.leftArm : model.rightArm;
		var support = left ? model.rightArm : model.leftArm;
		int side = left ? -1 : 1;
		boolean ads = BlasterItem.state(mainHand ? main : off).isAiming();
		float bounce = limbDistance * Mth.sin(limbAngle / 2F) * 0.05F;
		if ((mainHand ? mainLegacy : offLegacy).oneHanded())
		{
			if (!patrol)
			{
				primary.xRot = (ads ? -1.6F : -1.35F) + model.head.xRot - side * bounce;
				primary.yRot = model.head.yRot - side * 0.1F;
				if (ads)
				{
					support.xRot = -1.5F + model.head.xRot + side * bounce;
					support.yRot = model.head.yRot + side * 0.5F;
				}
			}
			float blend = patrol ? 1F : walkBlend(limbDistance);
			lerp(primary, blend, -1.436F + bounce, -side * 0.808F, side * 0.269F);
			lerp(support, blend, -1.077F - bounce, 0, side * 0.539F);
		}
		else if (patrol)
		{
			float leftBreath = Mth.sin(age / 15F + entity.getId()) * 0.01F;
			float rightBreath = Mth.cos(age / 15F - entity.getId()) * 0.01F;
			lerp(support, 1, -0.539F - bounce + (left ? rightBreath : leftBreath), -side * 0.269F, side * 0.09F);
			lerp(primary, 1, -0.808F + bounce + (left ? leftBreath : rightBreath), -side * 1.077F, 0);
		}
		else
		{
			float aim = ads ? 0.4F : 0;
			primary.xRot = -1.1F + model.head.xRot - aim;
			primary.yRot = model.head.yRot - side * 0.1F;
			support.xRot = -1.2F + model.head.xRot - aim;
			support.yRot = model.head.yRot + side * 0.5F;
			if (ads)
				model.head.zRot = -side * 0.2F;
		}
	}

	private static void dual(PlayerModel<?> model, Player player, float limbAngle, float limbDistance, float age, boolean patrol)
	{
		float bounce = limbDistance * Mth.sin(limbAngle / 2F) * 0.1F;
		float leftBreath = Mth.sin(age / 15F + player.getId()) * 0.01F;
		float rightBreath = Mth.cos(age / 15F - player.getId()) * 0.01F;
		float blend = walkBlend(limbDistance);
		if (patrol)
		{
			lerp(model.leftArm, 1 - blend, -2.424F + bounce + leftBreath, -0.18F, 0.09F);
			lerp(model.rightArm, 1 - blend, -2.424F - bounce + rightBreath, 0.18F, -0.09F);
		}
		else
		{
			lerp(model.leftArm, 1, -1.526F + leftBreath, 0, -0.18F);
			lerp(model.rightArm, 1, -1.526F + rightBreath, 0, 0.18F);
			lerp(model.leftArm, blend, -0.718F - bounce + leftBreath, 0.09F, 0.359F);
			lerp(model.rightArm, blend, -0.628F + bounce + rightBreath, -0.539F, -0.359F);
		}
	}

	private static float walkBlend(float distance)
	{
		float remaining = 1 - Mth.clamp(Mth.square(distance / 0.8F), 0, 1);
		return 1 - remaining * remaining * remaining;
	}

	private static void lerp(ModelPart arm, float amount, float pitch, float yaw, float roll)
	{
		arm.xRot = lerpAngle(amount, arm.xRot, pitch);
		arm.yRot = lerpAngle(amount, arm.yRot, yaw);
		arm.zRot = lerpAngle(amount, arm.zRot, roll);
	}

	private static float lerpAngle(float amount, float from, float to)
	{
		return Mth.rotLerp(amount, from * Mth.RAD_TO_DEG, to * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
	}

	private LegacyBlasterPose() {}
}
