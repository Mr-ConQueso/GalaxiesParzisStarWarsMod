package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TallLampBlock extends WaterloggableRotatingBlock
{
	public static final MapCodec<TallLampBlock> CODEC = simpleCodec(TallLampBlock::new);
	private static final VoxelShape UPRIGHT = Block.box(5, 0, 5, 11, 24, 11);
	private static final VoxelShape UP = Block.box(0, 5, 5, 24, 11, 11);
	private static final VoxelShape DOWN = Block.box(-8, 5, 5, 16, 11, 11);

	public TallLampBlock(Properties properties)
	{
		super(properties.dynamicShape());
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return switch (state.getValue(FACING))
		{
			case UP -> UP;
			case DOWN -> DOWN;
			default -> UPRIGHT;
		};
	}
}
