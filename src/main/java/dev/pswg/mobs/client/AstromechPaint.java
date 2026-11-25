package dev.pswg.mobs.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.pswg.Galaxies;
import dev.pswg.mobs.Astromech.Paint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.LinkedHashMap;

final class AstromechPaint
{
	private static final ResourceLocation FALLBACK = texture("preset/r2d2");
	private static final LinkedHashMap<Paint, ResourceLocation> CUSTOM = new LinkedHashMap<>(16, 0.75F, true);

	private AstromechPaint() {}

	static ResourceLocation resolve(Paint paint)
	{
		var minecraft = Minecraft.getInstance();
		if (paint.usingPaintPreset())
		{
			var preset = texture("preset/" + paint.paintPreset());
			return minecraft.getResourceManager().getResource(preset).isPresent() ? preset : FALLBACK;
		}
		var cached = CUSTOM.get(paint);
		if (cached != null)
			return cached;
		if (CUSTOM.size() >= 64)
		{
			var old = CUSTOM.pollFirstEntry().getValue();
			if (!old.equals(FALLBACK))
				minecraft.getTextureManager().release(old);
		}
		return CUSTOM.computeIfAbsent(paint, AstromechPaint::compose);
	}

	static void clear()
	{
		var textures = Minecraft.getInstance().getTextureManager();
		CUSTOM.values().stream().filter(id -> !id.equals(FALLBACK)).forEach(textures::release);
		CUSTOM.clear();
	}

	private static ResourceLocation compose(Paint paint)
	{
		var minecraft = Minecraft.getInstance();
		try (var input = minecraft.getResourceManager().open(texture("base")); var base = NativeImage.read(input))
		{
			var result = new NativeImage(base.getWidth(), base.getHeight(), true);
			try
			{
				blend(result, base, paint.baseTint());
				blend(result, "2/dome", paint.domeTint());
				blend(result, "design/1", paint.paintTint());
				blend(result, "details", 0xffffff);
				blend(result, "wires", 0xffffff);
				return minecraft.getTextureManager().register("pswg_astromech", new DynamicTexture(result));
			}
			catch (IOException | RuntimeException exception)
			{
				result.close();
				throw exception;
			}
		}
		catch (IOException | RuntimeException exception)
		{
			Galaxies.LOGGER.error("Could not load the astromech paint layers", exception);
			return FALLBACK;
		}
	}

	private static void blend(NativeImage result, String path, int tint) throws IOException
	{
		try (var input = Minecraft.getInstance().getResourceManager().open(texture(path)); var layer = NativeImage.read(input))
		{
			blend(result, layer, tint);
		}
	}

	private static void blend(NativeImage result, NativeImage layer, int tint)
	{
		for (int y = 0; y < result.getHeight(); y++)
			for (int x = 0; x < result.getWidth(); x++)
			{
				int abgr = layer.getPixelRGBA(x * layer.getWidth() / result.getWidth(), y * layer.getHeight() / result.getHeight());
				if (abgr >>> 24 == 0)
					continue;
				int red = (abgr & 255) * (tint >>> 16 & 255) / 255;
				int green = (abgr >>> 8 & 255) * (tint >>> 8 & 255) / 255;
				int blue = (abgr >>> 16 & 255) * (tint & 255) / 255;
				result.blendPixel(x, y, (abgr & 0xff000000) | blue << 16 | green << 8 | red);
			}
	}

	private static ResourceLocation texture(String path)
	{
		return Galaxies.id("textures/droid/r/" + path + ".png");
	}
}
