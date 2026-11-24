package dev.pswg.blasters.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterDefinitions;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.Blasters;
import dev.pswg.blasters.LegacyBlaster.FiringMode;
import dev.pswg.rendering.p3d.P3dRenderer;
import dev.pswg.rendering.p3d.P3dResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;

final class LegacyBlasterRenderer extends BlockEntityWithoutLevelRenderer
{
	private static final Matrix4f IDENTITY = new Matrix4f();
	private static final int[] FLASH_FRAMES = { 9, 0, 5, 7, 9 };

	LegacyBlasterRenderer(Minecraft client)
	{
		super(client.getBlockEntityRenderDispatcher(), client.getEntityModels());
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext display, PoseStack pose, MultiBufferSource buffers, int light, int overlay)
	{
		var id = stack.get(Blasters.ID.get());
		if (id == null)
			return;
		var model = P3dResources.MODELS.get(id.withPrefix("item/blaster/"));
		if (model == null)
			return;
		var definition = BlasterDefinitions.client().get(id);
		var texture = id.withPath(path -> "textures/item/model/blaster/" + path + ".png");
		var client = Minecraft.getInstance();
		var legacy = definition == null ? null : definition.legacy().orElse(null);
		float partial = client.getTimer().getGameTimeDeltaPartialTick(false);
		float ads = display.firstPerson() ? BlasterSight.amount(stack, partial) : 0;
		float shotTime = BlasterSight.shotAge(stack, partial);
		float kick = BlasterSight.shotKick(shotTime);

		pose.pushPose();
		pose.translate(0.5, 0.5, 0.5);
		if (display.firstPerson())
		{
			int side = display == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? -1 : 1;
			var muzzle = model.transform("muzzle_flash");
			float sightHeight = muzzle == null ? 0.1F : 0.2F * muzzle.m31();
			// This renderer receives Minecraft's base hand pose. Raise and center the sight around
			// that pose without translating the weapon downrange as the world FOV narrows.
			pose.translate(side * Mth.lerp(ads, -0.11F, -0.56F),
					Mth.lerp(ads, 0.2F, 0.5F - sightHeight), Mth.lerp(ads, 0.23F, 0.10F) + kick * 0.11F);
			float pitch = -kick * (legacy != null ? legacy.mechanics().recoilY() : 3) * Mth.lerp(ads, 0.1F, 0.15F);
			float yaw = side * (Mth.lerp(ads, -8F, 0F) - kick * (legacy != null ? legacy.mechanics().recoilX() : 1));
			if (client.player != null && client.player.getData(Blasters.PATROL))
			{
				pose.translate(0, -0.22, 0.07);
				pitch += 24F;
			}
			pose.mulPose(new Quaternionf().rotationXYZ(pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0));
			pose.scale(0.2F, 0.2F, 0.2F);
		}
		else if (display == ItemDisplayContext.GUI || display == ItemDisplayContext.FIXED)
		{
			pose.mulPose(Axis.YP.rotationDegrees(display == ItemDisplayContext.GUI ? -90 : 90));
			pose.mulPose(Axis.XP.rotationDegrees(20));
			float size = legacy != null && legacy.oneHanded() ? 0.23F : 0.14F;
			pose.scale(size, size, size);
			pose.translate(0, -1.15, 1.15);
		}
		else
		{
			float size = display == ItemDisplayContext.GROUND ? 0.12F : 0.2F;
			pose.scale(size, size, size);
			var grip = model.transform("main_hand");
			if (grip != null)
				pose.translate(-grip.m30(), -grip.m31(), -grip.m32());
		}

		var optionalParts = new HashMap<String, BlasterDefinition.Attachment>();
		if (definition != null)
		{
			definition.attachments().options().values().forEach(option -> option.part().ifPresent(part -> optionalParts.put(part, null)));
			for (var equipped : BlasterItem.attachments(stack, definition).applied().values())
			{
				var option = definition.attachments().options().get(equipped);
				if (option != null)
					option.part().ifPresent(part -> optionalParts.put(part, option));
			}
		}
		P3dRenderer.render(model, pose, buffers, name -> {
			var option = optionalParts.get(name);
			return option == null ? texture : option.texture().orElse(texture);
		}, name -> optionalParts.containsKey(name) && optionalParts.get(name) == null ? null : IDENTITY, light, overlay);

		if (shotTime >= 0 && shotTime < 4 && display != ItemDisplayContext.GUI && display != ItemDisplayContext.FIXED)
		{
			var muzzle = model.transform("muzzle_flash");
			if (definition != null)
				for (var selected : BlasterItem.attachments(stack, definition).applied().values())
				{
					String path = selected.getPath();
					var socket = model.transform("muzzle_flash." + path.substring(path.lastIndexOf('/') + 1));
					if (socket != null) muzzle = socket;
				}
			if (muzzle != null)
			{
				pose.mulPose(muzzle);
				int color = legacy == null ? 0xFF2244 : legacy.beam().color();
				var mode = definition == null ? FiringMode.SEMI_AUTOMATIC : BlasterItem.firingMode(stack, definition);
				if (mode == FiringMode.STUN || mode == FiringMode.ION)
					color = 0x3377FF;
				muzzleFlash(pose, buffers, overlay, shotTime, color, display.firstPerson());
			}
		}
		pose.popPose();
	}

	private static void muzzleFlash(PoseStack pose, MultiBufferSource buffers, int overlay, float age, int color, boolean firstPerson)
	{
		int frame = Math.min(4, (int) (age * 1.2F));
		int alpha = (int) (255 * Mth.clamp(1 - age * age / 16, 0, 1));
		int tint = alpha << 24 | color;
		var flash = Galaxies.id("textures/item/model/blaster/effect/muzzleflash_" + FLASH_FRAMES[frame] + ".png");
		drawFlash(pose, buffers, flash, overlay, tint, 0.45F, 0.45F, 15 / 16F, 15 / 16F);
		if (!firstPerson)
		{
			var side = Galaxies.id("textures/item/model/blaster/effect/muzzleflash_forward_" + (frame + 4) % 5 + ".png");
			pose.pushPose();
			pose.mulPose(Axis.YP.rotationDegrees(90));
			drawFlash(pose, buffers, side, overlay, tint, 1.05F, 0.45F, 30 / 32F, 15 / 32F);
			pose.mulPose(Axis.XP.rotationDegrees(90));
			drawFlash(pose, buffers, side, overlay, tint, 1.05F, 0.45F, 30 / 32F, 15 / 32F);
			pose.popPose();
		}
	}

	private static void drawFlash(PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int overlay,
	                              int color, float x, float y, float u, float v)
	{
		var out = buffers.getBuffer(RenderType.entityTranslucentEmissive(texture));
		for (int i = 0; i < 4; i++)
			out.addVertex(pose.last().pose(), (i == 0 || i == 3) ? -x : x, i < 2 ? -y : y, 0)
					.setColor(color).setUv((i == 0 || i == 3) ? 0 : u, i < 2 ? 0 : v)
					.setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 0, 1);
	}
}
