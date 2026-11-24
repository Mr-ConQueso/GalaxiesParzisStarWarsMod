package net.mrconqueso.pswg.character;

import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.container.SwgSpeciesRegistry;

import java.util.List;

public abstract class SpeciesVariable
{
	public static final String NONE = "none";
	protected final ResourceLocation targetSpecies;
	protected final String name;

	public static boolean isNotNone(SwgSpecies species, SpeciesVariable variable)
	{
		return !species.getVariable(variable).equals(NONE);
	}

	public SpeciesVariable(ResourceLocation targetSpecies, String name)
	{
		this.targetSpecies = targetSpecies;
		this.name = name;
	}

	public abstract List<String> getPossibleValues();

	public abstract String getDefaultValue();

	public String getName()
	{
		return name;
	}

	public ResourceLocation getTargetSpeciesSlug()
	{
		return targetSpecies;
	}

	public ResourceLocation getDefiningSpeciesSlug()
	{
		return targetSpecies;
	}

	public String getTranslationKey()
	{
		return SwgSpeciesRegistry.getTranslationKey(getDefiningSpeciesSlug()) + "." + name;
	}

	public String getTranslationFor(String value)
	{
		return getTranslationKey() + "." + value;
	}
}
