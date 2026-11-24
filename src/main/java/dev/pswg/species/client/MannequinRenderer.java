package dev.pswg.species.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.Galaxies;
import dev.pswg.rendering.nem.NemModels;
import dev.pswg.species.Mannequin;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.core.Rotations;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

final class MannequinRenderer extends LivingEntityRenderer<Mannequin, HumanoidModel<Mannequin>>
{
	private static final ResourceLocation TEXTURE = Galaxies.id("textures/entity/mannequin.png");
	private static final ResourceLocation HUMAN = Galaxies.id("species/human");
	private final Map<ResourceLocation, HumanoidModel<Mannequin>> models = new HashMap<>();
	private final HumanoidModel<Mannequin> fallback;
	private int version = -1;

	MannequinRenderer(EntityRendererProvider.Context context)
	{
		super(context, new PosedArmor(context.bakeLayer(ModelLayers.PLAYER)), 0);
		fallback = model;
		addLayer(new HumanoidArmorLayer<>(this, new PosedArmor(context.bakeLayer(ModelLayers.ARMOR_STAND_INNER_ARMOR)),
				new PosedArmor(context.bakeLayer(ModelLayers.ARMOR_STAND_OUTER_ARMOR)), context.getModelManager()));
		addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
	}

	@Override
	public void render(Mannequin mannequin, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
	{
		if (version != NemModels.INSTANCE.version())
		{
			version = NemModels.INSTANCE.version();
			models.clear();
		}
		var appearance = mannequin.appearance();
		var id = appearance.profile() == null ? HUMAN : SpeciesClient.modelId(appearance);
		model = models.computeIfAbsent(id, resource -> {
			var root = NemModels.INSTANCE.bake(resource);
			return root == null ? fallback : new PosedPlayer(root);
		});
		super.render(mannequin, yaw, partial, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(Mannequin mannequin)
	{
		var selected = SpeciesClient.texture(mannequin.appearance());
		return selected == null ? TEXTURE : selected;
	}

	private static void rotation(ModelPart part, Rotations rotations)
	{
		part.setRotation(rotations.getX() * Mth.DEG_TO_RAD, rotations.getY() * Mth.DEG_TO_RAD, rotations.getZ() * Mth.DEG_TO_RAD);
	}

	private static void pose(HumanoidModel<Mannequin> model, Mannequin mannequin)
	{
		rotation(model.head, mannequin.getHeadPose());
		rotation(model.body, mannequin.getBodyPose());
		rotation(model.leftArm, mannequin.getLeftArmPose());
		rotation(model.rightArm, mannequin.getRightArmPose());
		rotation(model.leftLeg, mannequin.getLeftLegPose());
		rotation(model.rightLeg, mannequin.getRightLegPose());
		model.hat.copyFrom(model.head);
		SpeciesClient.animate(model, mannequin, mannequin.appearance(), mannequin.getHeadPose().getY(), mannequin.getHeadPose().getX());
	}

	private static final class PosedArmor extends HumanoidModel<Mannequin>
	{
		PosedArmor(ModelPart root) { super(root); }
		@Override public void setupAnim(Mannequin actor, float swing, float amount, float age, float yaw, float pitch) { pose(this, actor); }
	}

	private static final class PosedPlayer extends PlayerModel<Mannequin>
	{
		PosedPlayer(ModelPart root) { super(root, false); }
		@Override
		public void setupAnim(Mannequin actor, float swing, float amount, float age, float yaw, float pitch)
		{
			pose(this, actor);
			jacket.copyFrom(body);
			leftSleeve.copyFrom(leftArm);
			rightSleeve.copyFrom(rightArm);
			leftPants.copyFrom(leftLeg);
			rightPants.copyFrom(rightLeg);
		}
	}
}
