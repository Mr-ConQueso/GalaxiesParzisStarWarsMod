package dev.pswg.gadgets.drill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

import java.util.List;

public record DrillParts(Extractor extractor, Bit drill, Capsule capsule)
{
	public static final Extractor BASE_EXTRACTOR = new Extractor(id("base_extractor"), 1F, 1F, 1F);
	public static final Extractor LASER_EXTRACTOR = new Extractor(id("laser_extractor"), 2.5F, 0.75F, 0.5F);
	public static final Extractor PLASTEEL_EXTRACTOR = new Extractor(id("plasteel_extractor"), 1F, 1.65F, 0.35F);
	public static final Bit BASE_DRILL = new Bit(id("base_drill"), 2F, 3, 600);
	public static final Bit TITANIUM_DRILL = new Bit(id("titanium_drill"), 2.5F, 5, 900);
	public static final Bit PLASTEEL_DRILL = new Bit(id("plasteel_drill"), 3.5F, 3, 300);
	public static final Capsule BASE_CAPSULE = new Capsule(id("base_capsule"), 4, 200);
	public static final Capsule TRANSPARISTEEL_CAPSULE = new Capsule(id("transparisteel_capsule"), 6, -100);
	public static final Capsule REINFORCED_CAPSULE = new Capsule(id("reinforced_capsule"), 5, 300);
	public static final DrillParts DEFAULT = new DrillParts(BASE_EXTRACTOR, BASE_DRILL, BASE_CAPSULE);

	public static final Codec<DrillParts> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Extractor.CODEC.fieldOf("extractor").forGetter(DrillParts::extractor),
			Bit.CODEC.fieldOf("drill").forGetter(DrillParts::drill),
			Capsule.CODEC.fieldOf("capsule").forGetter(DrillParts::capsule)
	).apply(builder, DrillParts::new));

	public int durability()
	{
		return Math.max(1, (int) (drill.durabilityBase * extractor.durabilityModifier + capsule.durabilityBoost));
	}

	public List<ResourceLocation> partIds()
	{
		return List.of(extractor.id(), drill.id(), capsule.id());
	}

	public static List<DrillParts> creativeVariants()
	{
		return List.of(DEFAULT,
				new DrillParts(LASER_EXTRACTOR, BASE_DRILL, BASE_CAPSULE),
				new DrillParts(PLASTEEL_EXTRACTOR, BASE_DRILL, BASE_CAPSULE),
				new DrillParts(BASE_EXTRACTOR, TITANIUM_DRILL, BASE_CAPSULE),
				new DrillParts(BASE_EXTRACTOR, PLASTEEL_DRILL, BASE_CAPSULE),
				new DrillParts(BASE_EXTRACTOR, BASE_DRILL, TRANSPARISTEEL_CAPSULE),
				new DrillParts(BASE_EXTRACTOR, BASE_DRILL, REINFORCED_CAPSULE));
	}

	public static ResourceLocation id(String name)
	{
		return ResourceLocation.fromNamespaceAndPath("pswg_gadgets", name);
	}

	public record Extractor(ResourceLocation id, float extractionMultiplier, float extractionSpeed, float durabilityModifier)
	{
		private static final Codec<Extractor> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				ResourceLocation.CODEC.fieldOf("identifier").forGetter(Extractor::id),
				Codec.floatRange(0F, 16F).fieldOf("extractionMultiplier").forGetter(Extractor::extractionMultiplier),
				Codec.floatRange(0.05F, 100F).fieldOf("extractionSpeed").forGetter(Extractor::extractionSpeed),
				Codec.floatRange(0.01F, 16F).fieldOf("durabilityModifier").forGetter(Extractor::durabilityModifier)
		).apply(builder, Extractor::new));
	}

	public record Bit(ResourceLocation id, float miningSpeed, int miningLevel, int durabilityBase)
	{
		private static final Codec<Bit> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				ResourceLocation.CODEC.fieldOf("identifier").forGetter(Bit::id),
				ExtraCodecs.POSITIVE_FLOAT.fieldOf("miningSpeed").forGetter(Bit::miningSpeed),
				ExtraCodecs.NON_NEGATIVE_INT.fieldOf("mininsgLevel").forGetter(Bit::miningLevel),
				ExtraCodecs.POSITIVE_INT.fieldOf("durabilityBase").forGetter(Bit::durabilityBase)
		).apply(builder, Bit::new));
	}

	public record Capsule(ResourceLocation id, int maxBlockCount, int durabilityBoost)
	{
		private static final Codec<Capsule> CODEC = RecordCodecBuilder.create(builder -> builder.group(
				ResourceLocation.CODEC.fieldOf("identifier").forGetter(Capsule::id),
				Codec.intRange(1, 64).fieldOf("maxBlockCount").forGetter(Capsule::maxBlockCount),
				Codec.INT.fieldOf("durabilityBoost").forGetter(Capsule::durabilityBoost)
		).apply(builder, Capsule::new));
	}
}
