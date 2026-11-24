package dev.pswg.sabers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.legacy.client.EnergyRenderer;
import dev.pswg.rendering.p3d.P3dRenderer;
import dev.pswg.rendering.p3d.P3dResources;
import dev.pswg.sabers.SaberItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

final class SaberRenderer extends BlockEntityWithoutLevelRenderer
{
	SaberRenderer(Minecraft client)
	{
		super(client.getBlockEntityRenderDispatcher(), client.getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext display, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
	{
		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		switch (display)
		{
			case GUI -> {
				pose.mulPose(Axis.ZP.rotationDegrees(-45));
				pose.mulPose(Axis.YP.rotationDegrees(-45));
				pose.scale(2, 2, 2);
			}
			case FIXED -> {
				pose.mulPose(Axis.ZP.rotationDegrees(45));
				pose.mulPose(Axis.YP.rotationDegrees(135));
				pose.scale(2, 2, 2);
			}
			case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
				var player = Minecraft.getInstance().player;
				if (player != null && SaberItem.blocks(player) && player.getUseItem() == stack)
				{
					int side = display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
					pose.mulPose(Axis.ZP.rotationDegrees(side * -45));
				}
				pose.translate(0, 0, 0.08);
			}
			case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> pose.translate(0, 0, 0.08);
			default -> {}
		}
		draw(stack, pose, buffers, light, overlay, false, display != ItemDisplayContext.GUI);
		pose.popPose();
	}

	static void draw(ItemStack stack, PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean force, boolean showBlade)
	{
		if (!(stack.getItem() instanceof SaberItem saber))
			return;
		var preset = saber.preset();
		var model = P3dResources.MODELS.get(Galaxies.id("item/lightsaber/" + preset.id()));
		if (model == null)
			return;
		var client = Minecraft.getInstance();
		var state = SaberItem.state(stack);
		long time = client.level == null ? state.changed() + 8 : client.level.getGameTime();
		float extent = force ? 1F : state.extension(time, client.getTimer().getGameTimeDeltaPartialTick(false));
		pose.pushPose();
		pose.scale(0.2F, 0.2F, 0.2F);
		var grip = model.transform("main_hand");
		if (grip == null)
			pose.translate(0, 0.85, 0);
		else
			pose.translate(-grip.m30(), -grip.m31(), -grip.m32());
		P3dRenderer.render(model, pose, buffers, Galaxies.id("textures/item/model/lightsaber/" + preset.id() + ".png"), light, overlay);
		if (showBlade && extent > 0)
		{
			boolean socketed = false;
			for (String socket : model.sockets().keySet())
			{
				if (!socket.startsWith("blade_"))
					continue;
				socketed = true;
				pose.pushPose();
				pose.mulPose(model.transform(socket));
				pose.scale(5, 5, -5);
				drawBlade(preset, state, pose, buffers, extent, preset.length(socket));
				pose.popPose();
			}
			if (!socketed)
			{
				pose.pushPose();
				pose.scale(5, 5, 5);
				drawBlade(preset, state, pose, buffers, extent, 1);
				pose.popPose();
			}
		}
		pose.popPose();
	}

	private static void drawBlade(dev.pswg.sabers.SaberPreset preset, dev.pswg.sabers.Sabers.BladeState state, PoseStack pose,
	                              MultiBufferSource buffers, float extent, float lengthFactor)
	{
		float length = 1.6F * lengthFactor * extent;
		switch (preset.blade())
		{
			case DEFAULT -> EnergyRenderer.beam(pose, buffers, length, 1, state.color(), state.unstable(), true);
			case DARKSABER -> EnergyRenderer.darksaber(pose, buffers, 1.2F * extent);
			case BRICK -> EnergyRenderer.brick(pose, buffers, length, state.color());
		}
	}
}
