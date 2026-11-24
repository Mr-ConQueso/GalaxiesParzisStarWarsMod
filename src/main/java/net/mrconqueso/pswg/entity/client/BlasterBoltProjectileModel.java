package net.mrconqueso.pswg.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.entity.custom.BlasterBoltEntity;

public class BlasterBoltProjectileModel extends EntityModel<BlasterBoltEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(PSWG.MOD_ID, "blaster_bolt"), "main");
    private final ModelPart blaster_bolt;

    public BlasterBoltProjectileModel(ModelPart root) {
        super(RenderType::entityCutout);
        this.blaster_bolt = root; //.getChild("blaster_bolt");
    }

    public static LayerDefinition createBodyLayer()
    {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("back", CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -2.5F, -2.5F, 0.0F, 5.0F, 5.0F), PartPose.offsetAndRotation(-11.0F, 0.0F, 0.0F, 0.7853982F, 0.0F, 0.0F)); // TODO: .withScale(0.8F)
        CubeListBuilder modelPartBuilder = CubeListBuilder.create().texOffs(0, 0).addBox(-12.0F, -2.0F, 0.0F, 16.0F, 4.0F, 0.0F, CubeDeformation.NONE, 1.0F, 0.8F);
        partdefinition.addOrReplaceChild("cross_1", modelPartBuilder, PartPose.rotation(0.7853982F, 0.0F, 0.0F));
        partdefinition.addOrReplaceChild("cross_2", modelPartBuilder, PartPose.rotation(2.3561945F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 32);
    }

    @Override
    public void setupAnim(BlasterBoltEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        blaster_bolt.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
