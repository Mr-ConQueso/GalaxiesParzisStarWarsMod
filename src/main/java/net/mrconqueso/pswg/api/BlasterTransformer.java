package net.mrconqueso.pswg.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.mrconqueso.pswg.client.blaster.BlasterItemRenderer;
import net.mrconqueso.pswg.data.blaster.BlasterData;
import net.mrconqueso.pswg.data.blaster.BlasterDescriptor;
import net.mrconqueso.pswg.p3d.P3dModel;

import java.util.ArrayList;

public interface BlasterTransformer
{
	/**
	 * Do not use this directly.
	 */
	ArrayList<BlasterTransformer> REGISTRY = new ArrayList<>();

	static void register(BlasterTransformer transformer)
	{
		REGISTRY.add(transformer);
	}

	void transformHand(PoseStack poseStack, P3dModel model, BlasterData bt, BlasterDescriptor bd, BlasterItemRenderer.AttachmentSuperset attachments, ItemDisplayContext renderMode, int light, float tickDelta, float opacity);

	void preTransform(PoseStack poseStack, P3dModel model, BlasterData bt, BlasterDescriptor bd, BlasterItemRenderer.AttachmentSuperset attachmentSet, ItemDisplayContext renderMode, int light, float tickDelta, float opacity);

	void postTransform(PoseStack poseStack, P3dModel model, BlasterData bt, BlasterDescriptor bd, BlasterItemRenderer.AttachmentSuperset attachments, ItemDisplayContext renderMode, int light, float tickDelta, float opacity);
}