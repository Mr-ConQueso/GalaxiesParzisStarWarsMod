package dev.pswg.block;

import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collections;

public class RepellerBlock extends WaterloggableRotatingBlock
{
	// TODO: move to a repellent manager to also manage where not to naturally deposit sand via weather
	private static final int REPELLENT_RADIUS = 8;
	private static final int REPELLENT_RADIUS_SQUARED = REPELLENT_RADIUS * REPELLENT_RADIUS;

	private static final VoxelShape SHAPE = box(0, 3, 2.5, 1, 14, 13.5);

	public RepellerBlock(Properties settings)
	{
		super(settings.randomTicks().dynamicShape());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return VoxelShapeUtil.rotateToFace(SHAPE, state.getValue(FACING));
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		var targets = new ArrayList<BlockPos>();

		// TODO: make a tag for sand repellents
		var center = Vec3.atCenterOf(pos);
		for (BlockPos blockPos : BlockPos.withinManhattan(pos, REPELLENT_RADIUS, REPELLENT_RADIUS, REPELLENT_RADIUS))
			if (Vec3.atCenterOf(blockPos).distanceToSqr(center) < REPELLENT_RADIUS_SQUARED && level.getBlockState(blockPos).is(GalaxiesBlocks.LOOSE_DESERT_SAND.get()))
				targets.add(blockPos.immutable());

		for (int i = targets.size() - 1; i > 0; i--)
			Collections.swap(targets, i, random.nextInt(i + 1));

		for (int i = 0; i < Math.min(5, targets.size()); i++)
		{
			var blockPos = targets.get(i);
			var layerState = level.getBlockState(blockPos);
			int layerCount = layerState.getValue(AccumulatingBlock.LAYERS);
			if (layerCount == 1)
				level.removeBlock(blockPos, false);
			else
				level.setBlockAndUpdate(blockPos, layerState.setValue(AccumulatingBlock.LAYERS, layerCount - 1));
		}
	}
}
