package dev.pswg.blasters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterBolt;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.LegacyBlaster.FiringMode;
import dev.pswg.legacy.client.EnergyRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

final class BlasterBoltRenderer extends EntityRenderer<BlasterBolt>
{
	BlasterBoltRenderer(EntityRendererProvider.Context context)
	{
		super(context);
	}

	@Override
	public void render(BlasterBolt bolt, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
	{
		var direction = bolt.getDeltaMovement().normalize();
		if (direction.lengthSqr() == 0)
			return;
		pose.pushPose();
		pose.translate(0, bolt.getBbHeight() * 0.5F, 0);
		float angle = (float) Math.atan2(direction.x, direction.z);
		float pitch = (float) Math.asin(direction.y);
		if (bolt.mode() == FiringMode.STUN)
		{
			pose.mulPose(Axis.YP.rotation(angle));
			pose.mulPose(Axis.XP.rotation(-pitch));
			EnergyRenderer.stun(pose, buffers, (float) Math.pow(Math.max(3 / 16F, (bolt.tickCount + partial) / 10F), 0.75));
		}
		else
		{
			var client = Minecraft.getInstance();
			boolean local = bolt.getOwner() == client.player && client.options.getCameraType().isFirstPerson() && client.player != null;
			if (local)
			{
				float distance = (float) bolt.position().distanceTo(client.player.getEyePosition(partial));
				float scale = Mth.clamp(distance / 1.5F, 0.01F, 1F);
				pose.scale(scale, scale, scale);
			}
			pose.mulPose(Axis.YP.rotation(angle - Mth.HALF_PI));
			pose.mulPose(Axis.ZP.rotation(pitch - Mth.HALF_PI));
			if (local && !BlasterItem.state(client.player.getItemInHand(BlasterItem.firingHand(client.player))).isAiming())
			{
				double d = Mth.clamp(1 - bolt.position().distanceTo(client.player.getEyePosition(partial)) / 15, 0, 1);
				pose.translate(0.2 * d * d * d, 0, (bolt.arm() == HumanoidArm.LEFT ? -0.5 : 0.5) * d * d * d);
			}
			if (bolt.mode() != FiringMode.SLUGTHROWER)
				EnergyRenderer.beam(pose, buffers, 1.5F * bolt.length(), bolt.radius(), bolt.color(), false, false);
		}
		pose.popPose();
	}

	@Override
	public ResourceLocation getTextureLocation(BlasterBolt bolt)
	{
		return Galaxies.id("textures/effect/white.png");
	}
}
