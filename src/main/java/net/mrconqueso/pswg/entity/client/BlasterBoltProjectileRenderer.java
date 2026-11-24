package net.mrconqueso.pswg.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.entity.custom.BlasterBoltEntity;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;

public class BlasterBoltProjectileRenderer extends EntityRenderer<BlasterBoltEntity> {

    private final BlasterBoltProjectileModel model;
    private static final ResourceLocation SOURCE_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png");

    public BlasterBoltProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new BlasterBoltProjectileModel(context.bakeLayer(BlasterBoltProjectileModel.LAYER_LOCATION));
    }

    @Override
    public void render(BlasterBoltEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, 0.2f, 0);

        // Interpolate yaw and pitch
        float yaw = Math.lerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Math.lerp(partialTick, entity.xRotO, entity.getXRot());

        poseStack.mulPose(Axis.YP.rotationDegrees(-(yaw + 90)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-pitch));
        poseStack.translate(0.2f, 0, 0);

        VertexConsumer vertexconsumer = buffer.getBuffer(this.model.renderType(SOURCE_TEXTURE));
        this.model.renderToBuffer(poseStack, vertexconsumer, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF); // 1.21.1 color tint signature

        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull BlasterBoltEntity entity) {
        return SOURCE_TEXTURE;
    }
}