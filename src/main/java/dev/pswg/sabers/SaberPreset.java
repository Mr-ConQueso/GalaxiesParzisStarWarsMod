package dev.pswg.sabers;

import net.minecraft.util.Mth;

public record SaberPreset(String id, String owner, int color, Blade blade, boolean unstable, float primaryLength)
{
	public static SaberPreset classic(String id, String owner, float hue)
	{
		return new SaberPreset(id, owner, Mth.hsvToRgb(hue, 1, 1), Blade.DEFAULT, false, 1);
	}

	public float length(String socket)
	{
		return socket.startsWith("blade_secondary") ? 0.15F : primaryLength;
	}

	public enum Blade { DEFAULT, DARKSABER, BRICK }
}
