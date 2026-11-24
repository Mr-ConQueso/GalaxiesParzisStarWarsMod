package dev.pswg.utility;

import net.minecraft.util.FastColor.ABGR32;

/**
 * Utility methods for working with colors.
 */
public class ColorUtil
{
	/**
	 * Multiplies a source pixel by a tint color while preserving alpha.
	 *
	 * @param pixel     The source pixel.
	 * @param tintColor The tint color to multiply into the pixel.
	 *
	 * @return The tinted pixel.
	 */
	public static int multiplyTint(int pixel, int tintColor)
	{
		int alpha = ABGR32.alpha(pixel) * ABGR32.alpha(tintColor) / 255;
		int red = ABGR32.red(pixel) * ABGR32.red(tintColor) / 255;
		int green = ABGR32.green(pixel) * ABGR32.green(tintColor) / 255;
		int blue = ABGR32.blue(pixel) * ABGR32.blue(tintColor) / 255;

		return ABGR32.color(alpha, blue, green, red);
	}

	/**
	 * Alpha-composites one top pixel over one bottom pixel.
	 *
	 * @param bottomPixel The bottom pixel.
	 * @param topPixel    The top pixel.
	 *
	 * @return The composited pixel.
	 */
	public static int alphaComposite(int bottomPixel, int topPixel)
	{
		float topAlpha = ABGR32.alpha(topPixel) / 255.0f;
		float bottomAlpha = ABGR32.alpha(bottomPixel) / 255.0f;
		float outAlpha = topAlpha + bottomAlpha * (1.0f - topAlpha);

		if (outAlpha <= 0.0f)
		{
			return 0;
		}

		int red = Math.round((ABGR32.red(topPixel) * topAlpha + ABGR32.red(bottomPixel) * bottomAlpha * (1.0f - topAlpha)) / outAlpha);
		int green = Math.round((ABGR32.green(topPixel) * topAlpha + ABGR32.green(bottomPixel) * bottomAlpha * (1.0f - topAlpha)) / outAlpha);
		int blue = Math.round((ABGR32.blue(topPixel) * topAlpha + ABGR32.blue(bottomPixel) * bottomAlpha * (1.0f - topAlpha)) / outAlpha);
		int alpha = Math.round(outAlpha * 255.0f);

		return ABGR32.color(alpha, blue, green, red);
	}
}
