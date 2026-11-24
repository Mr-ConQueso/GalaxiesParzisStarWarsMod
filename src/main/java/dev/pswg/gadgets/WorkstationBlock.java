package dev.pswg.gadgets;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

public final class WorkstationBlock<T extends BlockEntity> extends BaseEntityBlock
{
	private final Supplier<BlockEntityType<T>> entityType;
	private final BlockEntityTicker<T> ticker;
	private final VoxelShape shape;

	public WorkstationBlock(Properties properties, Supplier<BlockEntityType<T>> entityType, BlockEntityTicker<T> ticker, VoxelShape shape)
	{
		super(properties);
		this.entityType = entityType;
		this.ticker = ticker;
		this.shape = shape;
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec()
	{
		return MapCodec.unit(this);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return entityType.get().create(pos, state);
	}

	@Override
	public <E extends BlockEntity> BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type)
	{
		return level.isClientSide ? null : createTickerHelper(type, entityType.get(), ticker);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		return shape;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		var provider = state.getMenuProvider(level, pos);
		if (!level.isClientSide && provider != null)
			player.openMenu(provider);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving)
	{
		if (!state.is(replacement.getBlock()) && !level.isClientSide)
			Containers.dropContentsOnDestroy(state, replacement, level, pos);
		super.onRemove(state, level, pos, replacement, moving);
	}
}
