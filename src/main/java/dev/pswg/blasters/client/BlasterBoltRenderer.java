package dev.pswg.blasters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterBolt;
import dev.pswg.blasters.LegacyBlaster.FiringMode;
import dev.pswg.legacy.client.EnergyRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

final class BlasterBoltRenderer extends EntityRenderer<BlasterBolt>
{
	private final Map<BlasterBolt, Vec3> sourceOffsets = new WeakHashMap<>();
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
		applyMuzzleOffset(bolt, partial, pose);
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
			var player = BlasterSight.firstPersonPlayer();
			if (player != null && bolt.getOwner() == player)
			{
				float distance = (float) bolt.getPosition(partial).distanceTo(player.getEyePosition(partial));
				float scale = Mth.clamp(distance / 1.5F, 0.01F, 1F);
				pose.scale(scale, scale, scale);
			}
			pose.mulPose(Axis.YP.rotation(angle - Mth.HALF_PI));
			pose.mulPose(Axis.ZP.rotation(pitch - Mth.HALF_PI));
			EnergyRenderer.beam(pose, buffers, 1.5F * bolt.length(), bolt.radius(), bolt.color(), false, false);
		}
		pose.popPose();
	}

	private void applyMuzzleOffset(BlasterBolt bolt, float partial, PoseStack pose)
	{
		if (bolt.handModel() == null || bolt.tickCount >= 15)
			return;

		var center = bolt.getPosition(partial).add(0, bolt.getBbHeight() * 0.5F, 0);
		double travel = center.distanceTo(bolt.launchPoint());
		if (travel >= 15)
			return;

		// Bend the visual path back to the sight line without changing server collision.
		var muzzle = sourceOffsets.computeIfAbsent(bolt, BlasterMuzzle::boltOffset);
		if (muzzle != null)
		{
			double fade = 1 - travel / 15;
			var offset = muzzle.scale(fade * fade * fade);
			pose.translate(offset.x, offset.y, offset.z);
		}
	}

	@Override
	public ResourceLocation getTextureLocation(BlasterBolt bolt)
	{
		return Galaxies.id("textures/effect/white.png");
	}
}
