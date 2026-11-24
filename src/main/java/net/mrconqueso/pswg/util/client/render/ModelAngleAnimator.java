package net.mrconqueso.pswg.util.client.render;

import net.minecraft.world.entity.Entity;

@FunctionalInterface
public interface ModelAngleAnimator<T extends Entity>
{
	void setAngles(MutableAnimatedModel<T> model, T entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch, float tickDelta);
}
