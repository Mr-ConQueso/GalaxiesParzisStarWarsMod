package net.mrconqueso.pswg.character;

import com.google.common.collect.ImmutableList;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.api.HumanoidCustomizationOptions;

import java.util.List;

public class BakedSpeciesVariable extends SpeciesVariable
{
	private String defaultValue;
	private List<String> possibleValues;

	public BakedSpeciesVariable(ResourceLocation sourceSpecies, String name)
	{
		super(sourceSpecies, name);
	}

	public void bakeWith(HumanoidCustomizationOptions options)
	{
		if (this.defaultValue != null || this.possibleValues != null)
			throw new RuntimeException("Cannot re-bake species variable");

		this.defaultValue = options.defaultValue;
		this.possibleValues = ImmutableList.copyOf(options.possibleValues);

		if (this.defaultValue == null)
			throw new RuntimeException("Species variable cannot have null default value");
	}

	@Override
	public List<String> getPossibleValues()
	{
		return possibleValues;
	}

	@Override
	public String getDefaultValue()
	{
		return defaultValue;
	}
}
