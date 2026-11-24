package net.mrconqueso.pswg.util.client.render;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;

public interface ICustomPoseItem
{
	HashMap<Class<? extends Item>, ICustomPoseItem> REGISTRY = new HashMap<>();

	static void register(Class<? extends Item> item, ICustomPoseItem pose)
	{
		REGISTRY.put(item, pose);
	}

	void modifyPose(LivingEntity entity, InteractionHand hand, ItemStack stack, HumanoidModel<? extends LivingEntity> model, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float tickDelta);
}
