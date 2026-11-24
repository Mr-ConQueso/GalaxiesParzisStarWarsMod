package dev.pswg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.function.Supplier;

public final class WideBlock<T extends BlockEntity> extends MultipartBlock<T>
{
	private static final IntegerProperty PART = IntegerProperty.create("part", 0, 1);
	private static final VoxelShape BENCH = Shapes.or(box(0, 10, 0, 16, 13, 16),
			box(2, 5, 0, 14, 7, 16), box(1, 0, 10, 15, 10, 15));

	public WideBlock(Properties properties, Supplier<BlockEntityType<T>> entity, VoxelShape anchor, VoxelShape extension)
	{
		super(properties, List.of(new Part(BlockPos.ZERO, anchor), new Part(BlockPos.ZERO.north(), extension)), entity, null);
	}

	public WideBlock(Properties properties, Supplier<BlockEntityType<T>> entity)
	{
		this(properties, entity, Shapes.or(BENCH, box(0, 13, 0, 9, 16, 16)),
				Shapes.or(BENCH, box(0, 13, 0, 11, 16, 16)));
	}

	@Override protected IntegerProperty partProperty() { return PART; }
}
