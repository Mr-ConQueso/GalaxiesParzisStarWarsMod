package dev.pswg.blasters;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

public record BlasterState(boolean isAiming, long lastFired, long fireCooldown, long cooldownStart,
                           float lastTotalHeat, float lastVentingHeat, Mode mode, int burstBoltsRemaining,
                           long overchargeStart)
{
	public static final BlasterState DEFAULT = new BlasterState(false, 0, 0, 0, 0, 0, Mode.PASSIVE, 0, -1);

	public static final Codec<BlasterState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.fieldOf("isAiming").forGetter(BlasterState::isAiming),
			Codec.LONG.fieldOf("lastFired").forGetter(BlasterState::lastFired),
			Codec.LONG.fieldOf("fireCooldown").forGetter(BlasterState::fireCooldown),
			Codec.LONG.fieldOf("cooldownStart").forGetter(BlasterState::cooldownStart),
			Codec.FLOAT.fieldOf("lastTotalHeat").forGetter(BlasterState::lastTotalHeat),
			Codec.FLOAT.fieldOf("lastVentingHeat").forGetter(BlasterState::lastVentingHeat),
			StringRepresentable.fromEnum(Mode::values).fieldOf("coolingMode").forGetter(BlasterState::mode),
			Codec.INT.fieldOf("burstBoltsRemaining").forGetter(BlasterState::burstBoltsRemaining),
			Codec.LONG.fieldOf("overchargeStart").forGetter(BlasterState::overchargeStart)
	).apply(instance, BlasterState::new));

	public BlasterState withAim(boolean aiming)
	{
		return new BlasterState(aiming, lastFired, fireCooldown, cooldownStart, lastTotalHeat, lastVentingHeat, mode,
				burstBoltsRemaining, overchargeStart);
	}

	public BlasterState withCooling(Mode newMode, long start, float heat)
	{
		return new BlasterState(isAiming, lastFired, fireCooldown, start, 0, heat, newMode, 0, overchargeStart);
	}

	public BlasterState withFireCooldown(long end)
	{
		return new BlasterState(isAiming, lastFired, end, cooldownStart, lastTotalHeat, lastVentingHeat, mode,
				burstBoltsRemaining, overchargeStart);
	}

	public BlasterState withoutBurst()
	{
		return new BlasterState(isAiming, lastFired, fireCooldown, cooldownStart, lastTotalHeat, lastVentingHeat, mode,
				0, overchargeStart);
	}

	public BlasterState bypass(long now, boolean overcharge)
	{
		return new BlasterState(isAiming, lastFired, now, now, 0, 0, Mode.PASSIVE, 0, overcharge ? now : -1);
	}

	public double timeSince(long tick, long start, float partialTick)
	{
		return Math.max(0, (double) (tick - start) + partialTick);
	}

	public enum Mode implements StringRepresentable
	{
		PASSIVE, OVERHEAT, REQUESTED_BYPASS, FAILED_OVERCHARGE;

		@Override
		public String getSerializedName()
		{
			return name().toLowerCase(java.util.Locale.ROOT);
		}
	}
}
