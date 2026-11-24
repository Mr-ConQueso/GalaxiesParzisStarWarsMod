package dev.pswg.blasters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.blasters.BlasterBolt;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

final class BlasterBoltRenderer extends EntityRenderer<BlasterBolt>
{
	private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");
	private final ModelPart model;

	BlasterBoltRenderer(EntityRendererProvider.Context context)
	{
		super(context);
		var layers = new MeshDefinition();
		var root = layers.getRoot();
		root.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).addBox(0, -2.5F, -2.5F, 0, 5, 5),
				PartPose.offsetAndRotation(-11, 0, 0, 0.7853982F, 0, 0));
		var blade = CubeListBuilder.create().texOffs(0, 0).addBox(-12, -2, 0, 16, 4, 0);
		root.addOrReplaceChild("cross_1", blade, PartPose.rotation(0.7853982F, 0, 0));
		root.addOrReplaceChild("cross_2", blade, PartPose.rotation(2.3561945F, 0, 0));
		model = LayerDefinition.create(layers, 32, 32).bakeRoot();
	}

	@Override
	public void render(BlasterBolt bolt, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
	{
		pose.pushPose();
		pose.translate(0, 0.2F, 0);
		pose.mulPose(Axis.YP.rotationDegrees(-(Mth.rotLerp(partial, bolt.yRotO, bolt.getYRot()) + 90F)));
		pose.mulPose(Axis.ZP.rotationDegrees(-Mth.lerp(partial, bolt.xRotO, bolt.getXRot())));
		pose.translate(0.2F, 0, 0);
		pose.scale(0.9F, 0.9F, 0.9F);
		model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, OverlayTexture.NO_OVERLAY);
		pose.popPose();
		super.render(bolt, yaw, partial, pose, buffers, light);
	}

	@Override
	public ResourceLocation getTextureLocation(BlasterBolt bolt)
	{
		return TEXTURE;
	}
}
