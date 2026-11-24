package dev.pswg.block;

import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class HangarLightBlock extends WaterloggableRotatingBlock
{
	private static final VoxelShape SHAPE = VoxelShapeUtil.getCentered(16, 14, 8);

	public HangarLightBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return VoxelShapeUtil.rotateToFace(SHAPE, state.getValue(FACING));
	}
}
