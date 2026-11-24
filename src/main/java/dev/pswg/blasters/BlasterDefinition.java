package dev.pswg.blasters;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

public record BlasterDefinition(Stats stats, Attachments attachments)
{
	public static final Codec<BlasterDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Stats.CODEC.fieldOf("stats").forGetter(BlasterDefinition::stats),
			Attachments.CODEC.fieldOf("attachments").forGetter(BlasterDefinition::attachments)
	).apply(instance, BlasterDefinition::new));

	public record Stats(float damage, int range, int automaticRepeatDelay, ResourceLocation fireSound, Heat heat, Cooling cooling)
	{
		public static final Codec<Stats> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.FLOAT.fieldOf("damage").forGetter(Stats::damage),
				Codec.INT.fieldOf("range").forGetter(Stats::range),
				Codec.INT.fieldOf("automaticRepeatDelay").forGetter(Stats::automaticRepeatDelay),
				ResourceLocation.CODEC.fieldOf("fireSound").forGetter(Stats::fireSound),
				Heat.CODEC.fieldOf("heat").forGetter(Stats::heat),
				Cooling.CODEC.fieldOf("cooling").forGetter(Stats::cooling)
		).apply(instance, Stats::new));
	}

	public record Heat(int capacity, int perRound, float drainSpeed, int overheatPenalty, int overheatDrainSpeed,
	                   int passiveCooldownDelay, int overchargeBonus)
	{
		public static final Codec<Heat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.INT.fieldOf("capacity").forGetter(Heat::capacity),
				Codec.INT.fieldOf("perRound").forGetter(Heat::perRound),
				Codec.FLOAT.fieldOf("drainSpeed").forGetter(Heat::drainSpeed),
				Codec.INT.fieldOf("overheatPenalty").forGetter(Heat::overheatPenalty),
				Codec.INT.fieldOf("overheatDrainSpeed").forGetter(Heat::overheatDrainSpeed),
				Codec.INT.fieldOf("passiveCooldownDelay").forGetter(Heat::passiveCooldownDelay),
				Codec.INT.fieldOf("overchargeBonus").forGetter(Heat::overchargeBonus)
		).apply(instance, Heat::new));
	}

	public record Cooling(float primaryBypassTime, float primaryBypassTolerance,
	                      float secondaryBypassTime, float secondaryBypassTolerance)
	{
		public static final Codec<Cooling> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.FLOAT.fieldOf("primaryBypassTime").forGetter(Cooling::primaryBypassTime),
				Codec.FLOAT.fieldOf("primaryBypassTolerance").forGetter(Cooling::primaryBypassTolerance),
				Codec.FLOAT.fieldOf("secondaryBypassTime").forGetter(Cooling::secondaryBypassTime),
				Codec.FLOAT.fieldOf("secondaryBypassTolerance").forGetter(Cooling::secondaryBypassTolerance)
		).apply(instance, Cooling::new));
	}

	public record Attachments(ResourceLocation hud, Map<ResourceLocation, ResourceLocation> defaults,
	                          Map<ResourceLocation, Attachment> options)
	{
		public static final Codec<Attachments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("hud").forGetter(Attachments::hud),
				Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).fieldOf("defaults").forGetter(Attachments::defaults),
				Codec.unboundedMap(ResourceLocation.CODEC, Attachment.CODEC).fieldOf("options").forGetter(Attachments::options)
		).apply(instance, Attachments::new));

		public Attachments
		{
			defaults = Map.copyOf(defaults);
			options = Map.copyOf(options);
		}
	}

	public record Attachment(String translationKey, List<ResourceLocation> slots, ResourceLocation function,
	                         ResourceLocation category, float value)
	{
		public static final Codec<Attachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("translationKey").forGetter(Attachment::translationKey),
				ResourceLocation.CODEC.listOf().fieldOf("slots").forGetter(Attachment::slots),
				ResourceLocation.CODEC.fieldOf("function").forGetter(Attachment::function),
				ResourceLocation.CODEC.fieldOf("category").forGetter(Attachment::category),
				Codec.FLOAT.optionalFieldOf("value", 0.0F).forGetter(Attachment::value)
		).apply(instance, Attachment::new));

		public Attachment
		{
			slots = List.copyOf(slots);
		}
	}
}
