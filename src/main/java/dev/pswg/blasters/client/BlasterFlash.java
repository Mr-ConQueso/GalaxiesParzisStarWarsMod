package dev.pswg.blasters.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.pswg.Galaxies;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class BlasterFlash
{
	static final float DURATION = 4 / 1.2F;
	private static final int[] FRAMES = { 9, 0, 5, 7 };
	private static final Map<Frame, RenderType> LAYERS = new HashMap<>();

	private record Frame(boolean forward, int frame, int color) {}

	static void render(PoseStack pose, MultiBufferSource buffers, int overlay, float age, int color, boolean firstPerson)
	{
		float time = age * 1.2F;
		int frame = Math.min(FRAMES.length - 1, (int) time);
		int alpha = (int) (255 * (1 - Mth.square(time / 4)));
		int tint = alpha << 24 | 0xFFFFFF;
		float radius = 0.45F;
		float uv = 15 / 16F;
		var out = buffers.getBuffer(layer(new Frame(false, FRAMES[frame], color)));
		vertex(pose, out, overlay, tint, -radius, -radius, 0, 0, 0);
		vertex(pose, out, overlay, tint, radius, -radius, 0, uv, 0);
		vertex(pose, out, overlay, tint, radius, radius, 0, uv, uv);
		vertex(pose, out, overlay, tint, -radius, radius, 0, 0, uv);
		if (firstPerson)
			return;

		out = buffers.getBuffer(layer(new Frame(true, (frame + 4) % 5, color)));
		float near = -0.2F;
		float far = near + radius * 3;
		float v = 15 / 32F;
		vertex(pose, out, overlay, tint, 0, -radius, near, uv, 0);
		vertex(pose, out, overlay, tint, 0, -radius, far, 0, 0);
		vertex(pose, out, overlay, tint, 0, radius, far, 0, v);
		vertex(pose, out, overlay, tint, 0, radius, near, uv, v);
		vertex(pose, out, overlay, tint, -radius, 0, near, uv, 0);
		vertex(pose, out, overlay, tint, -radius, 0, far, 0, 0);
		vertex(pose, out, overlay, tint, radius, 0, far, 0, v);
		vertex(pose, out, overlay, tint, radius, 0, near, uv, v);
	}

	static void reload()
	{
		var textures = Minecraft.getInstance().getTextureManager();
		for (var frame : LAYERS.keySet())
			textures.release(tintedId(frame));
		LAYERS.clear();
	}

	private static RenderType layer(Frame frame)
	{
		return LAYERS.computeIfAbsent(frame, key -> {
			var source = Galaxies.id("textures/item/model/blaster/effect/muzzleflash_" + (key.forward() ? "forward_" : "") + key.frame() + ".png");
			var texture = tintedId(key);
			try (var input = Minecraft.getInstance().getResourceManager().getResourceOrThrow(source).open())
			{
				var image = NativeImage.read(input);
				// Overlay preserves the source texture's white highlights and alpha.
				for (int y = 0; y < image.getHeight(); y++)
					for (int x = 0; x < image.getWidth(); x++)
					{
						int pixel = image.getPixelRGBA(x, y);
						int red = overlay(pixel & 255, key.color() >> 16 & 255);
						int green = overlay(pixel >> 8 & 255, key.color() >> 8 & 255);
						int blue = overlay(pixel >> 16 & 255, key.color() & 255);
						image.setPixelRGBA(x, y, (pixel & 0xFF000000) | blue << 16 | green << 8 | red);
					}
				Minecraft.getInstance().getTextureManager().register(texture, new DynamicTexture(image));
			}
			catch (IOException exception)
			{
				Galaxies.LOGGER.warn("Could not tint muzzle flash {}", source, exception);
				texture = source;
			}
			return RenderType.create("pswg_muzzle_flash", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256,
					false, true, RenderType.CompositeState.builder()
							.setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
							.setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
							.setCullState(RenderStateShard.NO_CULL)
							.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
							.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
							.createCompositeState(true));
		});
	}

	private static ResourceLocation tintedId(Frame frame)
	{
		return Galaxies.id("dynamic/muzzle_flash/" + (frame.forward() ? "forward/" : "face/") + frame.frame()
				+ "/" + Integer.toHexString(frame.color()));
	}

	private static int overlay(int source, int tint)
	{
		float s = source / 255F;
		float t = tint / 255F;
		return (int) (255 * (s < 0.5F ? 2 * s * t : 1 - 2 * (1 - s) * (1 - t)));
	}

	private static void vertex(PoseStack pose, VertexConsumer out, int overlay, int color,
	                           float x, float y, float z, float u, float v)
	{
		out.addVertex(pose.last().pose(), x, y, z).setColor(color).setUv(u, v)
				.setOverlay(overlay).setLight(LightTexture.FULL_BRIGHT).setNormal(pose.last(), 0, 0, 1);
	}

	private BlasterFlash() {}
}
