package net.mrconqueso.pswg.character;

import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.util.math.ColorUtil;

import java.util.ArrayList;
import java.util.List;

public class SpeciesColorVariable extends SpeciesVariable
{
	private final int defaultColor;
	private final ArrayList<String> allowedValues = new ArrayList<>();

	public SpeciesColorVariable(ResourceLocation parent, String name, int defaultColor)
	{
		this(parent, name, defaultColor, new int[0]);
	}

	public SpeciesColorVariable(ResourceLocation parent, String name, int defaultColor, int[] allowedValues)
	{
		super(parent, name);
		this.defaultColor = defaultColor;

		for (var value : allowedValues)
			this.allowedValues.add(Integer.toHexString(value));
	}

	@Override
	public List<String> getPossibleValues()
	{
		return allowedValues;
	}

	@Override
	public String getDefaultValue()
	{
		return ColorUtil.toResourceId(defaultColor);
	}

	@Override
	public String getTranslationFor(String value)
	{
		if (getPossibleValues().isEmpty())
			return getTranslationKey();
		else
			return getTranslationKey() + ".preset." + value;
	}
}
