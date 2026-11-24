package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.Block;

public final class StainedConnectedGlassBlock extends ConnectedBlock.Glass implements BeaconBeamBlock
{
	public static final MapCodec<StainedConnectedGlassBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			DyeColor.CODEC.fieldOf("color").forGetter(StainedConnectedGlassBlock::getColor),
			propertiesCodec()
	).apply(instance, StainedConnectedGlassBlock::new));

	private final DyeColor color;

	public StainedConnectedGlassBlock(DyeColor color, Properties properties)
	{
		super(properties);
		this.color = color;
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	public DyeColor getColor()
	{
		return color;
	}
}
