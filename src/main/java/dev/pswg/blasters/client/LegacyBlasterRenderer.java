package dev.pswg.blasters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterDefinitions;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.Blasters;
import dev.pswg.blasters.LegacyBlaster;
import dev.pswg.rendering.p3d.P3dModel;
import dev.pswg.rendering.p3d.P3dRenderer;
import dev.pswg.rendering.p3d.P3dResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;

final class LegacyBlasterRenderer extends BlockEntityWithoutLevelRenderer
{
	private static final Matrix4f IDENTITY = new Matrix4f();
	private static final float MODEL_SCALE = 0.2F;
	static LivingEntity holder;

	LegacyBlasterRenderer(Minecraft client)
	{
		super(client.getBlockEntityRenderDispatcher(), client.getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext display, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
	{
		var id = stack.get(Blasters.ID.get());
		if (id == null)
			return;
		var model = P3dResources.MODELS.get(id.withPrefix("item/blaster/"));
		if (model == null)
			return;
		var definition = BlasterDefinitions.client().get(id);
		var texture = id.withPath(path -> "textures/item/model/blaster/" + path + ".png");
		var client = Minecraft.getInstance();
		var legacy = definition == null ? null : definition.legacy().orElse(null);
		float partial = client.getTimer().getGameTimeDeltaPartialTick(false);
		boolean firstPerson = display.firstPerson();
		boolean held = firstPerson || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || display == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
		var arm = display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || display == ItemDisplayContext.THIRD_PERSON_LEFT_HAND
				? HumanoidArm.LEFT : HumanoidArm.RIGHT;
		float shotTime = held && holder != null ? BlasterMuzzle.shotAge(holder, arm, stack, partial) : 100;

		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		if (display.firstPerson())
		{
			boolean leftHanded = display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
			float ads = legacy == null ? 0F : BlasterSight.legacyAmount(stack, legacy, partial);
			applyFirstPersonPose(pose, leftHanded, legacy, ads, BlasterSight.shotKick(shotTime));
			renderForegrip(client, model, leftHanded, pose, buffers, light);
		}
		else if (display == ItemDisplayContext.GUI || display == ItemDisplayContext.FIXED)
			applyDisplayPose(pose, model, display == ItemDisplayContext.GUI);
		else
		{
			float size = display == ItemDisplayContext.GROUND ? 0.12F : 0.2F;
			pose.scale(size, size, size);
		}

		var optionalParts = new HashMap<String, BlasterDefinition.Attachment>();
		if (definition != null)
		{
			definition.attachments().options().values().forEach(option -> option.part().ifPresent(part -> optionalParts.put(part, null)));
			for (var equipped : BlasterItem.attachments(stack, definition).applied().values())
			{
				var option = definition.attachments().options().get(equipped);
				if (option != null)
					option.part().ifPresent(part -> optionalParts.put(part, option));
			}
		}
		P3dRenderer.render(model, pose, buffers, name -> {
			var option = optionalParts.get(name);
			return option == null ? texture : option.texture().orElse(texture);
		}, name -> optionalParts.containsKey(name) && optionalParts.get(name) == null ? null : IDENTITY, light, overlay);

		if (held && holder != null)
		{
			var muzzle = BlasterMuzzle.legacySocket(model, stack, definition);
			if (muzzle != null)
			{
				pose.mulPose(muzzle);
				int color = legacy == null ? 0xFF2244 : legacy.beam().color();
				BlasterMuzzle.render(holder, arm, stack, pose, buffers, overlay, color, firstPerson, partial);
			}
		}
		pose.popPose();
	}

	private static void applyDisplayPose(PoseStack pose, P3dModel model, boolean inventory)
	{
		var bounds = model.bounds();
		float angle = Mth.PI / 4;
		// Fabric used this number in degrees for the tilt, and in radians for fitting the icon.
		pose.mulPose(Axis.YP.rotationDegrees(inventory ? -90 : 90));
		pose.mulPose(Axis.XP.rotationDegrees(angle));
		double height = bounds.getYsize();
		double length = bounds.getZsize();
		double sine = Math.sin(angle);
		double cosine = Math.cos(angle);
		double fit = Math.max(height * sine + length * cosine, height * cosine + length * sine);
		float scale = inventory && fit > 0 ? (float) (MODEL_SCALE * 5 / fit) : MODEL_SCALE * 2;
		pose.scale(scale, scale, scale);
		var center = bounds.getCenter();
		pose.translate(0, -center.y, -center.z);
	}

	private static void applyFirstPersonPose(PoseStack pose, boolean leftHanded, LegacyBlaster legacy, float ads, float kick)
	{
		int side = leftHanded ? -1 : 1;
		var archetype = legacy == null ? "" : legacy.archetype();
		boolean sniper = archetype.equals("sniper");
		float aimX = sniper ? -2.8F : archetype.equals("pistol") ? -2.2F : -2.1F;
		float aimY = sniper ? 2.65F : 1.5F;
		float aimZ = sniper ? -5F : -3F;
		float recoilX = legacy == null ? 1F : legacy.mechanics().recoilX();
		float recoilY = legacy == null ? 3F : legacy.mechanics().recoilY();

		pose.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
		pose.translate(side * Mth.lerp(ads, -0.75F, aimX), Mth.lerp(ads, 1.2F, aimY), ads * aimZ);
		float pitch = 3 * ads + kick * recoilY * (0.1F + 0.05F * ads);
		float yaw = Mth.lerp(ads, 172F, 182F) - side * kick * recoilX;
		pose.mulPose(new Quaternionf().rotationXYZ(pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0));
		pose.translate(side * Mth.lerp(ads, 0.2F, 0F), Mth.lerp(ads, -0.2F, 0F),
				Mth.lerp(ads, -1.2F, 0F) - kick * (0.4F + 0.55F * ads));
		pose.mulPose(Axis.YP.rotationDegrees(180));
	}

	private static void renderForegrip(Minecraft client, P3dModel model, boolean leftHanded,
			PoseStack pose, MultiBufferSource buffers, int light)
	{
		LocalPlayer player = client.player;
		var socket = model.transform("off_hand");
		if (player == null || socket == null)
			return;
		var supportArm = leftHanded ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
		var supportHand = player.getMainArm() == supportArm ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
		if (!player.getItemInHand(supportHand).isEmpty())
			return;

		var renderer = (PlayerRenderer) client.getEntityRenderDispatcher().getRenderer(player);
		var skin = renderer.getTextureLocation(player);
		var body = renderer.getModel();
		pose.pushPose();
		// P3D sockets use a different basis from Minecraft's arm geometry. Keep the skin normals in the hand pose.
		int mirror = leftHanded ? -1 : 1;
		pose.last().pose().scale(mirror, 1, 1).mul(socket)
				.scale(4, 4, -4).translate(0, -0.625F, 0).scale(mirror, 1, 1);
		renderArmPart(leftHanded ? body.rightArm : body.leftArm, pose, buffers, RenderType.entitySolid(skin), light);
		if (player.isModelPartShown(leftHanded ? PlayerModelPart.RIGHT_SLEEVE : PlayerModelPart.LEFT_SLEEVE))
			renderArmPart(leftHanded ? body.rightSleeve : body.leftSleeve, pose, buffers, RenderType.entityTranslucent(skin), light);
		pose.popPose();
	}

	private static void renderArmPart(ModelPart part, PoseStack pose, MultiBufferSource buffers, RenderType layer, int light)
	{
		var original = part.storePose();
		boolean visible = part.visible;
		part.setPos(0, 0, 0);
		part.setRotation(0, 0, 0);
		part.visible = true;
		try
		{
			part.render(pose, buffers.getBuffer(layer), light, OverlayTexture.NO_OVERLAY);
		}
		finally
		{
			part.setPos(original.x, original.y, original.z);
			part.setRotation(original.xRot, original.yRot, original.zRot);
			part.visible = visible;
		}
	}

}
