package net.mrconqueso.pswg.client.blaster;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.client.PSWGClient;
import net.mrconqueso.pswg.component.ModDataComponents;
import net.mrconqueso.pswg.component.PlayerData;
import net.mrconqueso.pswg.data.blaster.BlasterArchetype;
import net.mrconqueso.pswg.data.blaster.BlasterAttachmentFunction;
import net.mrconqueso.pswg.data.blaster.BlasterDescriptor;
import net.mrconqueso.pswg.data.blaster.BlasterData;
import net.mrconqueso.pswg.item.custom.BlasterItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.mrconqueso.pswg.util.BlasterWield;
import net.mrconqueso.pswg.util.client.render.ICustomHudRenderer;
import org.joml.Quaternionf;

import java.util.HashMap;

public class BlasterHudRenderer implements ICustomHudRenderer
{
	public static final BlasterHudRenderer INSTANCE = new BlasterHudRenderer();

	private static final ResourceLocation OVERLAY_BASIC_SCOPE = Resources.id("textures/misc/blaster_basic_scope.png");
	private static final ResourceLocation HUD_ELEMENTS_TEXTURE = Resources.id("textures/gui/blasters.png");
	public static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/icons.png");

	private static final HashMap<BlasterAttachmentFunction, Integer> CROSSHAIR_ATTACHMENT_MAP = net.minecraft.Util.make(new HashMap<>(), (h) -> {
		h.put(BlasterAttachmentFunction.SNIPER_SCOPE, 2);
		h.put(BlasterAttachmentFunction.REDUCE_RECOIL, 5);
		h.put(BlasterAttachmentFunction.REDUCE_SPREAD, 9);
		h.put(BlasterAttachmentFunction.WATERPROOF_FIRING, 4);
	});

	@Override
	public boolean renderCrosshair(GuiGraphics guiGraphics, Player player, InteractionHand hand, ItemStack stack)
	{
		var poseStack = guiGraphics.pose();
		poseStack.pushPose();
		var bd = BlasterItem.getBlasterDescriptor(stack);

		if (bd == null)
		{
			poseStack.popPose();
			return false;
		}

		var client = Minecraft.getInstance();
		var scaledWidth = guiGraphics.guiWidth();
		var scaledHeight = guiGraphics.guiHeight();

		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		var left = scaledWidth / 2;
		var top = scaledHeight / 2;
		final var cooldownWidth = 61;
		var cooldownOffset = 30;

		var isDual = BlasterItem.getWield(player) == BlasterWield.Dual;
		var isOff = hand == InteractionHand.OFF_HAND;

		if (isDual)
		{
			poseStack.translate(left, top, 0);
			poseStack.mulPose(new Quaternionf().rotationZ(-90 * Mth.DEG_TO_RAD));
			poseStack.translate(-left, -top, 0);

			cooldownOffset = 0;
			poseStack.translate(-top + 10 + cooldownWidth / 2f, (isOff ? -1 : 1) * 130, 0);
		}

		// NEW
		var bt = stack.getOrDefault(ModDataComponents.BLASTER_DATA.get(), BlasterData.EMPTY);
		var profile = bd.cooling;
		var crosshairIdx = getCrosshairIndex(bd, bt.attachmentBitmask);
		var tickDelta = PSWGClient.getTickDelta();

		if (bt.isCooling() || bt.heat > 0 || bt.overchargeTimer > 0 || bt.readyTimer > 0)
		{
			var cooldownBarX = (scaledWidth - cooldownWidth) / 2;

			guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX, top + cooldownOffset, 0, 0, cooldownWidth, 3);
			final float maxHeat = bd.heat.capacity;

			if (bt.isCooling() && (bt.coolingMode == BlasterData.COOLING_MODE_OVERHEAT || bt.coolingMode == BlasterData.COOLING_MODE_PENALTY_BYPASS))
			{
				var cooldownTimer = (bt.ventingHeat - bd.heat.overheatDrainSpeed * tickDelta) / (maxHeat + bd.heat.overheatPenalty);
				cooldownTimer = Mth.clamp(cooldownTimer, 0, 0.98f);

				guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX, top + cooldownOffset, 0, 16, cooldownWidth, 3);

				if (bt.canBypassCooling)
				{
					var pBStartX = (int)((profile.primaryBypassTime - profile.primaryBypassTolerance) * cooldownWidth);
					var pBWidth = (int)(2 * profile.primaryBypassTolerance * cooldownWidth);
					var sBStartX = (int)((profile.secondaryBypassTime - profile.secondaryBypassTolerance) * cooldownWidth);
					var sBWidth = (int)(2 * profile.secondaryBypassTolerance * cooldownWidth);

					guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX + pBStartX, top + cooldownOffset, pBStartX, 8, pBWidth, 3);
					guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX + sBStartX, top + cooldownOffset, sBStartX, 12, sBWidth, 3);
				}

				poseStack.pushPose();
				poseStack.translate(cooldownBarX + cooldownTimer * cooldownWidth - 1, 0, 0);
				guiGraphics.blit(HUD_ELEMENTS_TEXTURE, 0, top + cooldownOffset - 2, 0, 24, 3, 7);
				poseStack.popPose();
			}
			else if (bt.readyTimer > 0)
			{
				var readyTimer = Mth.clamp((bt.readyTimer - tickDelta) / bd.quickdrawDelay, 0, 0.98f);
				guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX, top + cooldownOffset, 0, 32, cooldownWidth, 3);

				poseStack.pushPose();
				poseStack.translate(cooldownBarX + readyTimer * cooldownWidth - 1, 0, 0);
				guiGraphics.blit(HUD_ELEMENTS_TEXTURE, 0, top + cooldownOffset - 2, 0, 24, 3, 7);
				poseStack.popPose();
			}
			else
			{
				var heatPercentage = 0f;
				if (bt.isCooling()) heatPercentage = (bt.ventingHeat - (bt.passiveCooldownTimer == 0 ? bd.heat.overheatDrainSpeed * tickDelta : 0)) / maxHeat;
				else if (bt.overchargeTimer > 0) heatPercentage = (bt.overchargeTimer - tickDelta) / bd.heat.overchargeBonus;
				else heatPercentage = (bt.heat - (bt.passiveCooldownTimer == 0 ? bd.heat.drainSpeed * tickDelta : 0)) / maxHeat;

				if (bt.overchargeTimer > 0) guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX, top + cooldownOffset, 0, 12, (int)(cooldownWidth * heatPercentage), 3);
				else guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX, top + cooldownOffset, 0, 4, (int)(cooldownWidth * heatPercentage), 3);
			}

			guiGraphics.blit(HUD_ELEMENTS_TEXTURE, cooldownBarX, top + cooldownOffset, 0, 20, cooldownWidth, 3);
		}

		var cancelCrosshair = false;
		if (bt.isAimingDownSights)
		{
			drawCrosshair(guiGraphics, scaledWidth, scaledHeight, crosshairIdx);
			cancelCrosshair = true;
		}
		else
		{
			var data = PlayerData.getVolatilePublic(player);
			if (data.isPatrolPosture())
			{
				drawCrosshair(guiGraphics, scaledWidth, scaledHeight, 11);
				cancelCrosshair = true;
			}
		}

		poseStack.popPose();
		return cancelCrosshair;
	}

	private static void drawCrosshair(GuiGraphics guiGraphics, int scaledWidth, int scaledHeight, int crosshairIdx)
	{
		RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		guiGraphics.blit(HUD_ELEMENTS_TEXTURE, (scaledWidth - 15) / 2, (scaledHeight - 15) / 2, 62 + 16 * crosshairIdx, 0, 15, 15);
		RenderSystem.defaultBlendFunc();
	}

	public static int getCrosshairIndex(BlasterDescriptor bd, int attachmentBitmask)
	{
		return bd.mapWithAttachment(attachmentBitmask, CROSSHAIR_ATTACHMENT_MAP).orElse(bd.defaultCrosshair);
	}

	@Override
	public void renderOverlay(GuiGraphics guiGraphics, Player player, InteractionHand hand, ItemStack stack, int scaledWidth, int scaledHeight, float tickDelta)
	{
		var bd = BlasterItem.getBlasterDescriptor(stack);
		if (bd == null || bd.type != BlasterArchetype.SNIPER) return;

		// You can adjust scale/opacity as needed based on your old logic
		float scale = 1;

		RenderSystem.disableDepthTest();
		RenderSystem.depthMask(false);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // 1.0 = full opacity

		float f = (float)Math.min(scaledWidth, scaledHeight);
		float h = Math.min((float)scaledWidth / f, (float)scaledHeight / f) * scale;
		float i = f * h;
		float j = f * h;
		float k = ((float)scaledWidth - i) / 2.0F;
		float l = ((float)scaledHeight - j) / 2.0F;

		// guiGraphics blit automatically handles the vertex positioning and shaders for you
		guiGraphics.blit(OVERLAY_BASIC_SCOPE, (int)k, (int)l, 0, 0, (int)i, (int)j, (int)i, (int)j);

		RenderSystem.depthMask(true);
		RenderSystem.enableDepthTest();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}
}