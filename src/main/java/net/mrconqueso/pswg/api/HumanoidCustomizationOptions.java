package net.mrconqueso.pswg.api;

import java.util.HashSet;

public class HumanoidCustomizationOptions
{
	public String defaultValue;
	public HashSet<String> possibleValues = new HashSet<>();

	public HumanoidCustomizationOptions(String defaultValue)
	{
		this.defaultValue = defaultValue;
		this.possibleValues.add(defaultValue);
	}
}
