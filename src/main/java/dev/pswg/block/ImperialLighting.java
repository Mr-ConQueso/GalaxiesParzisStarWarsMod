package dev.pswg.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public class ImperialLighting extends Block
{
	public static final MapCodec<ImperialLighting> CODEC = simpleCodec(ImperialLighting::new);

	public ImperialLighting(Properties properties)
	{
		super(properties);
		registerDefaultState(initialState(defaultBlockState()));
	}

	@Override
	protected MapCodec<? extends Block> codec()
	{
		return CODEC;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		addSwitchProperties(builder);
	}

	private static void addSwitchProperties(StateDefinition.Builder<Block, BlockState> builder)
	{
		builder.add(BlockStateProperties.INVERTED, BlockStateProperties.POWERED, BlockStateProperties.LIT);
	}

	private static BlockState initialState(BlockState state)
	{
		return withSignal(state.setValue(BlockStateProperties.INVERTED, true), false);
	}

	private static BlockState withSignal(BlockState state, boolean hasSignal)
	{
		return state.setValue(BlockStateProperties.POWERED, hasSignal)
				.setValue(BlockStateProperties.LIT, hasSignal ^ state.getValue(BlockStateProperties.INVERTED));
	}

	private static void refresh(BlockState state, Level level, BlockPos pos)
	{
		var updated = withSignal(state, level.hasNeighborSignal(pos));
		if (!level.isClientSide && level.getBlockState(pos) != updated)
			level.setBlockAndUpdate(pos, updated);
	}

	private static InteractionResult toggle(BlockState state, Level level, BlockPos pos, Player player, boolean requiresEmptyHand)
	{
		if (!player.getAbilities().mayBuild || requiresEmptyHand && !player.getMainHandItem().isEmpty())
			return InteractionResult.PASS;
		refresh(state.cycle(BlockStateProperties.INVERTED), level, pos);
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context)
	{
		return withSignal(defaultBlockState(), context.getLevel().hasNeighborSignal(context.getClickedPos()));
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving)
	{
		refresh(state, level, pos);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		return toggle(state, level, pos, player, true);
	}

	public static final class Slab extends VerticalSlabBlock
	{
		public static final MapCodec<Slab> CODEC = simpleCodec(Slab::new);

		public Slab(Properties properties)
		{
			super(properties);
			registerDefaultState(initialState(defaultBlockState()));
		}

		@Override
		protected MapCodec<? extends Block> codec()
		{
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
		{
			super.createBlockStateDefinition(builder);
			addSwitchProperties(builder);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context)
		{
			return withSignal(super.getStateForPlacement(context), context.getLevel().hasNeighborSignal(context.getClickedPos()));
		}

		@Override
		protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving)
		{
			refresh(state, level, pos);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
		{
			return toggle(state, level, pos, player, true);
		}
	}

	public static final class Pillar extends ConnectedBlock
	{
		public static final MapCodec<Pillar> CODEC = simpleCodec(Pillar::new);

		public Pillar(Properties properties)
		{
			super(properties);
			registerDefaultState(initialState(defaultBlockState()));
		}

		@Override
		protected MapCodec<? extends Block> codec()
		{
			return CODEC;
		}

		@Override
		protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
		{
			super.createBlockStateDefinition(builder);
			addSwitchProperties(builder);
		}

		@Override
		public BlockState getStateForPlacement(BlockPlaceContext context)
		{
			return withSignal(super.getStateForPlacement(context), context.getLevel().hasNeighborSignal(context.getClickedPos()));
		}

		@Override
		protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block source, BlockPos sourcePos, boolean moving)
		{
			refresh(state, level, pos);
		}

		@Override
		protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
		{
			return toggle(state, level, pos, player, false);
		}
	}
}
