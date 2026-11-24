package dev.pswg.block.plant;

import dev.pswg.container.GalaxiesBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class AridPlant extends BushBlock
{
	public static final MapCodec<AridPlant> CODEC = simpleCodec(AridPlant::new);

	@Override
	protected MapCodec<? extends BushBlock> codec()
	{
		return CODEC;
	}
	protected static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

	public AridPlant(Properties settings)
	{
		super(settings);
	}
	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context)
	{
		return SHAPE;
	}

	@Override
	protected boolean mayPlaceOn(BlockState floor, BlockGetter world, BlockPos pos)
	{
		return floor.is(GalaxiesBlocks.ARID_PLANT_PLACEABLE);
	}
	@Override
	public int getFlammability(BlockState state, BlockGetter world, BlockPos pos, Direction face)
	{
		return 60;
	}

	@Override
	public int getFireSpreadSpeed(BlockState state, BlockGetter world, BlockPos pos, Direction face)
	{
		return 100;
	}

}
