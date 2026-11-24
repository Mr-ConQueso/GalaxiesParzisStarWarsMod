package net.mrconqueso.pswg.character;

import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.PSWG;

public enum SpeciesGender
{
	MALE("m"),
	FEMALE("f");

	public static final String GENDER_SEPARATOR = "/";

	private final String slug;

	SpeciesGender(String slug)
	{
		this.slug = slug;
	}

	public String getSlug()
	{
		return slug;
	}

	public static ResourceLocation stripGender(String genderedSlug)
	{
		var parts = genderedSlug.split(GENDER_SEPARATOR, 2);
		return ResourceLocation.fromNamespaceAndPath(PSWG.MOD_ID, parts[0]);
	}
}
