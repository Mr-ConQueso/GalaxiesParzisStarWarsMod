package net.mrconqueso.pswg.character;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SpeciesStringVariable extends SpeciesVariable
{
	private final String defaultValue;
	private final List<String> possibleValues;

	public SpeciesStringVariable(ResourceLocation parent, String name, String defaultValue, String... possibleValues)
	{
		super(parent, name);
		this.defaultValue = defaultValue;
		this.possibleValues = List.of(possibleValues);
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
