package dev.pswg.gadgets.drill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record DrillExtraction(int blocksExtracted, int blocksToExtract, List<BlockPos> markedBlocks,
                              BlockPos origin, ResourceLocation dimension, int duration)
{
	public static final Codec<DrillExtraction> CODEC = RecordCodecBuilder.create(builder -> builder.group(
			Codec.intRange(0, 64).fieldOf("blocksExtracted").forGetter(DrillExtraction::blocksExtracted),
			Codec.intRange(1, 64).fieldOf("blocksToExtract").forGetter(DrillExtraction::blocksToExtract),
			BlockPos.CODEC.listOf(0, 64).fieldOf("markedBlocks").forGetter(DrillExtraction::markedBlocks),
			BlockPos.CODEC.fieldOf("origin").forGetter(DrillExtraction::origin),
			ResourceLocation.CODEC.fieldOf("dimension").forGetter(DrillExtraction::dimension),
			Codec.intRange(1, 4000).fieldOf("duration").forGetter(DrillExtraction::duration)
	).apply(builder, DrillExtraction::new));

	public DrillExtraction
	{
		markedBlocks = List.copyOf(markedBlocks);
	}

	DrillExtraction advance()
	{
		return new DrillExtraction(blocksExtracted + 1, blocksToExtract, markedBlocks.subList(0, markedBlocks.size() - 1),
				origin, dimension, duration);
	}
}
