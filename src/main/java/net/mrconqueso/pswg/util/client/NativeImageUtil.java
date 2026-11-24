package net.mrconqueso.pswg.util.client;

import net.mrconqueso.pswg.data.TintedIdentifier;
import net.mrconqueso.pswg.util.math.MathUtil;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;

public class NativeImageUtil {
	public static int blendColorsOnSrcAlpha(int dest, int src, int tint, TintedIdentifier.Mode tintMode) {
		var destA = FastColor.ABGR32.alpha(dest) / 255f;
		var destR = FastColor.ABGR32.red(dest);
		var destG = FastColor.ABGR32.green(dest);
		var destB = FastColor.ABGR32.blue(dest);

		src = tint(src, tint, tintMode);

		var srcA = FastColor.ABGR32.alpha(src) / 255f;
		var srcR = FastColor.ABGR32.red(src);
		var srcG = FastColor.ABGR32.green(src);
		var srcB = FastColor.ABGR32.blue(src);

		var a = MathUtil.clamp((int)((destA + srcA) * 255f), 0, 255);
		var r = MathUtil.clamp((int)((1 - srcA) * destR + srcA * srcR), 0, 255);
		var g = MathUtil.clamp((int)((1 - srcA) * destG + srcA * srcG), 0, 255);
		var b = MathUtil.clamp((int)((1 - srcA) * destB + srcA * srcB), 0, 255);

		return FastColor.ABGR32.color(a, b, g, r);
	}

	public static int tint(int src, int tint, TintedIdentifier.Mode tintMode) {
		var srcA = FastColor.ABGR32.alpha(src);
		var srcR = FastColor.ABGR32.red(src);
		var srcG = FastColor.ABGR32.green(src);
		var srcB = FastColor.ABGR32.blue(src);

		var tintA = FastColor.ABGR32.alpha(tint);
		if (tintA == 0)
			tintA = 255;

		var tintR = FastColor.ABGR32.red(tint);
		var tintG = FastColor.ABGR32.green(tint);
		var tintB = FastColor.ABGR32.blue(tint);

		switch (tintMode) {
			case Multiply -> {
				srcA = (srcA * tintA) / 255;
				srcR = (srcR * tintR) / 255;
				srcG = (srcG * tintG) / 255;
				srcB = (srcB * tintB) / 255;
			}
			case Add -> {
				srcR = Mth.clamp(srcR + tintR, 0, 255);
				srcG = Mth.clamp(srcG + tintG, 0, 255);
				srcB = Mth.clamp(srcB + tintB, 0, 255);
			}
			case Overlay -> {
				var srcRf = srcR / 255f;
				var srcGf = srcG / 255f;
				var srcBf = srcB / 255f;

				var tintRf = tintR / 255f;
				var tintGf = tintG / 255f;
				var tintBf = tintB / 255f;

				srcR = (int)(255 * Mth.clamp(srcRf < 0.5 ? 2 * srcRf * tintRf : (1 - 2 * (1 - srcRf) * (1 - tintRf)), 0, 1));
				srcG = (int)(255 * Mth.clamp(srcGf < 0.5 ? 2 * srcGf * tintGf : (1 - 2 * (1 - srcGf) * (1 - tintGf)), 0, 1));
				srcB = (int)(255 * Mth.clamp(srcBf < 0.5 ? 2 * srcBf * tintBf : (1 - 2 * (1 - srcBf) * (1 - tintBf)), 0, 1));
			}
		}

		return FastColor.ABGR32.color(srcA, srcB, srcG, srcR);
	}

	public static int argbToAbgr(int value) {
		return FastColor.ABGR32.color((value >>> 24) & 0xFF, (value) & 0xFF, (value >>> 8) & 0xFF, (value >>> 16) & 0xFF);
	}
}