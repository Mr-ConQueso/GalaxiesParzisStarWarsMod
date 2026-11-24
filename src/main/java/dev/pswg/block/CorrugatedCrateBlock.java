package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CorrugatedCrateBlock extends WaterloggableRotatingBlock implements EntityBlock
{
	public static final MapCodec<CorrugatedCrateBlock> CODEC = simpleCodec(CorrugatedCrateBlock::new);
	private static final VoxelShape SHAPE = box(1, 0, 1, 15, 16, 15);

	public CorrugatedCrateBlock(Properties properties)
	{
		super(properties);
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return new CorrugatedCrateBlockEntity(pos, state);
	}

	@Override
	protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos)
	{
		return level.getBlockEntity(pos) instanceof CorrugatedCrateBlockEntity crate ? crate : null;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		var crate = getMenuProvider(state, level, pos);
		if (crate == null)
			return InteractionResult.PASS;
		if (!level.isClientSide)
			player.openMenu(crate);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean movedByPiston)
	{
		if (!state.is(replacement.getBlock()) && !level.isClientSide
				&& level.getBlockEntity(pos) instanceof CorrugatedCrateBlockEntity crate)
		{
			Containers.dropContents(level, pos, crate);
			level.updateNeighbourForOutputSignal(pos, this);
		}
		super.onRemove(state, level, pos, replacement, movedByPiston);
	}
}
