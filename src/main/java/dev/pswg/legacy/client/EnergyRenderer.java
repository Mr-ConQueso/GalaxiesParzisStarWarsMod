package dev.pswg.legacy.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.pswg.Galaxies;
import dev.pswg.util.worldgen.noise.OpenSimplexNoise;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public final class EnergyRenderer
{
	private static final RenderType BEAM = RenderType.create("pswg_energy", DefaultVertexFormat.NEW_ENTITY,
			VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
					.setShaderState(RenderStateShard.RENDERTYPE_EYES_SHADER)
					.setTextureState(new RenderStateShard.TextureStateShard(Galaxies.id("textures/effect/white.png"), false, false))
					.setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
					.setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
					.createCompositeState(true));
	private static final OpenSimplexNoise DISTORTION = new OpenSimplexNoise(0);
	private static final int[][] SIDES = { { 0, 1, 5, 4 }, { 1, 2, 6, 5 }, { 2, 3, 7, 6 }, { 3, 0, 4, 7 },
			{ 0, 3, 2, 1 }, { 4, 5, 6, 7 } };
	private static final float[][] NORMALS = { { 0, 0, -1 }, { 1, 0, 0 }, { 0, 0, 1 }, { -1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 } };

	private EnergyRenderer() {}

	public static void beam(PoseStack pose, MultiBufferSource buffers, float length, float radius, int rgb, boolean unstable, boolean taper)
	{
		if (length <= 0)
			return;
		var out = buffers.getBuffer(BEAM);
		float bottom = radius * 0.018F;
		float top = taper ? radius * 0.012F : bottom;
		float[] hsv = java.awt.Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
		double time = (System.currentTimeMillis() % Integer.MAX_VALUE) / 4.0;
		int segments = unstable ? 35 : 1;
		for (int i = 0; i < segments; i++)
		{
			float a = (float) i / segments;
			float b = (float) (i + 1) / segments;
			float lowNoise = unstable ? (float) DISTORTION.sample(time, 5 * a * length) * 0.005F : 0;
			float highNoise = unstable ? (float) DISTORTION.sample(time, 5 * b * length) * 0.005F : 0;
			float saturation = unstable ? 0.07F * (1 - (float) DISTORTION.sample(time, 3 * a * length)) * hsv[1] : 0;
			int color = 0xff000000 | Mth.hsvToRgb(0, saturation, 1);
			box(out, pose.last(), a * length, b * length, Mth.lerp(a, bottom, top) + lowNoise,
					Mth.lerp(b, bottom, top) + highNoise, 0, color, false);
		}
		for (int layer = 1; layer <= 14; layer++)
		{
			double x = layer * (60.0 / 14);
			float hue = unstable ? (float) DISTORTION.sample(0, (System.currentTimeMillis() - layer * 10) / 200.0) * 0.02F : 0;
			int color = glow(hsv, x, hue);
			if ((color >>> 24) < 16)
				continue;
			float thickness = radius * layer * 0.0028F;
			box(out, pose.last(), -thickness, length + thickness, bottom + thickness, top + thickness, 0, color, true);
		}
	}

	public static void darksaber(PoseStack pose, MultiBufferSource buffers, float length)
	{
		if (length <= 0)
			return;
		var out = buffers.getBuffer(BEAM);
		pose.pushPose();
		pose.scale(0.5F, 1, 1);
		box(out, pose.last(), 0, length * 0.6F, 0.020F, 0.018F, 0, 0xFF101010, false);
		box(out, pose.last(), length * 0.6F, length, 0.018F, 0.0045F, 0.0135F, 0xFF101010, false);
		float[] white = { 0, 0, 1 };
		for (int layer = 1; layer <= 14; layer++)
		{
			int color = glow(white, layer * 5, 0);
			if ((color >>> 24) < 16)
				continue;
			float rim = layer * 0.0028F;
			box(out, pose.last(), -rim, length * 0.6F + rim, 0.020F + rim, 0.018F + rim, 0, color, true);
			box(out, pose.last(), length * 0.6F + rim, length + rim, 0.018F + rim, 0.0045F + rim, 0.0135F, color, true);
		}
		pose.popPose();
	}

	public static void brick(PoseStack pose, MultiBufferSource buffers, float length, int rgb)
	{
		var out = buffers.getBuffer(BEAM);
		box(out, pose.last(), 0, length, 0.036F, 0.036F, 0, 0x80000000 | rgb, true);
		box(out, pose.last(), 0.35F * length, 0.65F * length, 0.022F, 0.022F, 0, 0x80000000 | rgb, true);
	}

	public static void stun(PoseStack pose, MultiBufferSource buffers, float size)
	{
		var out = buffers.getBuffer(RenderType.lightning());
		float half = size / 2;
		float inner = half * 0.8F;
		float[][] outer = { { -half, half }, { half, half }, { half, -half }, { -half, -half } };
		float[][] edges = { { -inner, inner }, { inner, inner }, { inner, -inner }, { -inner, -inner } };
		for (int i = 0; i < 4; i++)
		{
			int j = (i + 1) % 4;
			for (float[] vertex : new float[][] { outer[i], outer[j], edges[j], edges[i], edges[i], edges[j], outer[j], outer[i] })
				out.addVertex(pose.last().pose(), vertex[0], vertex[1], 0).setColor(0xFF1933FF);
		}
	}

	private static int glow(float[] hsv, double layer, float hueOffset)
	{
		float alpha = Mth.clamp((float) (1 - (layer / 100 + 0.4) / (1 + Math.exp(-0.3 * (layer - 22)))), 0, 1);
		float saturation = Mth.clamp((float) ((layer / 400 + 0.76) / (1 + Math.exp(-0.27 * (layer - 10)))), 0, 1) * hsv[1];
		float darkness = 1 - (1 - (float) layer / 75) * (1 - (float) layer / 75) * (1 - (float) layer / 75);
		float value = Mth.lerp(darkness, 1, hsv[2]);
		float hue = Mth.clamp((float) (hsv[0] + hueOffset - 0.06 * Math.exp(-0.011 * (layer - 6) * (layer - 6))), 0, 1);
		return ((int) (255 * alpha) << 24) | Mth.hsvToRgb(hue, saturation, value);
	}

	private static void box(VertexConsumer out, PoseStack.Pose pose, float low, float high, float bottom, float top,
	                        float skew, int color, boolean reverse)
	{
		float[][] vertices = {
				{ -bottom, low, -bottom }, { bottom, low, -bottom }, { bottom, low, bottom }, { -bottom, low, bottom },
				{ -top, high, skew - top }, { top, high, skew - top }, { top, high, skew + top }, { -top, high, skew + top }
		};
		boolean inverted = reverse ^ (pose.pose().determinant3x3() < 0);
		for (int side = 0; side < SIDES.length; side++)
		{
			var n = NORMALS[side];
			for (int i = 0; i < 4; i++)
			{
				var p = vertices[SIDES[side][inverted ? i : 3 - i]];
				out.addVertex(pose.pose(), p[0], p[1], p[2]).setColor(color).setUv(0, 0)
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, n[0], n[1], n[2]);
			}
		}
	}
}
