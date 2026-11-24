package net.mrconqueso.pswg.util.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

public interface ICustomItemRenderer
{
	HashMap<Class<? extends Item>, ICustomItemRenderer> REGISTRY = new HashMap<>();

	static void register(Class<? extends Item> item, ICustomItemRenderer renderer)
	{
		REGISTRY.put(item, renderer);
	}

	void render(LivingEntity player, ItemStack stack, ItemDisplayContext renderMode, boolean leftHanded, PoseStack poseStack, MultiBufferSource bufferSource, int light, int overlay, BakedModel model);
}