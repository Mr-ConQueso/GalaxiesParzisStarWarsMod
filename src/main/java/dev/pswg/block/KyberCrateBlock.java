package dev.pswg.block;

import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import dev.pswg.container.Crates;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import java.util.List;

public final class KyberCrateBlock extends MultipartBlock<CorrugatedCrateBlockEntity>
{
	private static final IntegerProperty PART = IntegerProperty.create("part", 0, 2);

	public KyberCrateBlock(Properties properties)
	{
		super(properties, List.of(new Part(BlockPos.ZERO, box(1, 1, 0, 15, 15, 16)),
				new Part(BlockPos.ZERO.north(), box(0, 0, 0, 16, 16, 16)),
				new Part(BlockPos.ZERO.south(), box(0, 0, 0, 16, 16, 16))), Crates.KYBER, null);
	}

	@Override protected IntegerProperty partProperty() { return PART; }
}
