package dev.pswg.blasters;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record LegacyBlaster(String archetype, List<FiringMode> modes, Mechanics mechanics, Beam beam,
                            List<ResourceLocation> requiredSlots, int crosshair)
{
	public static final Codec<LegacyBlaster> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.STRING.fieldOf("archetype").forGetter(LegacyBlaster::archetype),
			StringRepresentable.fromEnum(FiringMode::values).listOf(1, 6).fieldOf("modes").forGetter(LegacyBlaster::modes),
			Mechanics.CODEC.fieldOf("mechanics").forGetter(LegacyBlaster::mechanics),
			Beam.CODEC.fieldOf("beam").forGetter(LegacyBlaster::beam),
			ResourceLocation.CODEC.listOf().optionalFieldOf("requiredSlots", List.of()).forGetter(LegacyBlaster::requiredSlots),
			Codec.intRange(0, 11).optionalFieldOf("crosshair", 0).forGetter(LegacyBlaster::crosshair)
	).apply(builder, LegacyBlaster::new));

	public boolean oneHanded()
	{
		return archetype.equals("pistol") || archetype.equals("ion");
	}

	public List<FiringMode> availableModes(net.minecraft.world.item.ItemStack stack, BlasterDefinition definition)
	{
		var result = new ArrayList<>(modes);
		if (!result.contains(FiringMode.AUTOMATIC) && BlasterItem.hasFunction(stack, definition, "allow_auto"))
			result.add(FiringMode.AUTOMATIC);
		if (!result.contains(FiringMode.BURST) && BlasterItem.hasFunction(stack, definition, "allow_burst"))
			result.add(FiringMode.BURST);
		return result;
	}

	public record Mechanics(float zoom, float aimSpeed, int readyTime, int magazine, int burstInterval, int burstSize,
	                        int burstGap, float recoilX, float recoilY, float spreadX, float spreadY, String water)
	{
		public static final Codec<Mechanics> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.floatRange(1, 32).fieldOf("zoom").forGetter(Mechanics::zoom),
				Codec.floatRange(-0.95F, 0).fieldOf("aimSpeed").forGetter(Mechanics::aimSpeed),
				Codec.intRange(0, 200).fieldOf("readyTime").forGetter(Mechanics::readyTime),
				Codec.intRange(1, 4096).fieldOf("magazine").forGetter(Mechanics::magazine),
				Codec.intRange(1, 200).fieldOf("burstInterval").forGetter(Mechanics::burstInterval),
				Codec.intRange(2, 16).fieldOf("burstSize").forGetter(Mechanics::burstSize),
				Codec.intRange(1, 200).fieldOf("burstGap").forGetter(Mechanics::burstGap),
				Codec.FLOAT.fieldOf("recoilX").forGetter(Mechanics::recoilX),
				Codec.FLOAT.fieldOf("recoilY").forGetter(Mechanics::recoilY),
				Codec.FLOAT.fieldOf("spreadX").forGetter(Mechanics::spreadX),
				Codec.FLOAT.fieldOf("spreadY").forGetter(Mechanics::spreadY),
				Codec.STRING.fieldOf("water").forGetter(Mechanics::water)
		).apply(builder, Mechanics::new));
	}

	public record Beam(int color, float length, float radius, float falloff, boolean pyrotechnics)
	{
		public static final Codec<Beam> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				Codec.INT.fieldOf("color").forGetter(Beam::color),
				Codec.floatRange(0.1F, 8).fieldOf("length").forGetter(Beam::length),
				Codec.floatRange(0.1F, 8).fieldOf("radius").forGetter(Beam::radius),
				Codec.floatRange(0, 20).fieldOf("falloff").forGetter(Beam::falloff),
				Codec.BOOL.optionalFieldOf("pyrotechnics", false).forGetter(Beam::pyrotechnics)
		).apply(builder, Beam::new));
	}

	public enum FiringMode implements StringRepresentable
	{
		SEMI_AUTOMATIC("semi"), BURST("burst"), AUTOMATIC("auto"), STUN("stun"), SLUGTHROWER("slug"), ION("ion");

		private final String key;

		FiringMode(String key)
		{
			this.key = key;
		}

		public String translation()
		{
			return "info.pswg.blaster_firing_mode_" + key;
		}

		@Override
		public String getSerializedName()
		{
			return name().toLowerCase(Locale.ROOT);
		}
	}
}
