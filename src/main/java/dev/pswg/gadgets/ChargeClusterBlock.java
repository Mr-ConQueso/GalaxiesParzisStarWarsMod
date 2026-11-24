package dev.pswg.gadgets;

import dev.pswg.block.WaterloggableRotatingBlock;
import dev.pswg.util.VoxelShapeUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ChargeClusterBlock extends WaterloggableRotatingBlock
{
	public static final IntegerProperty SIZE = IntegerProperty.create("cluster_size", 1, 5);
	private final Demolitions.Variant variant;

	ChargeClusterBlock(Demolitions.Variant variant, Properties properties)
	{
		super(properties);
		this.variant = variant;
		registerDefaultState(defaultBlockState().setValue(SIZE, 1).setValue(FACING, Direction.UP));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(SIZE);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context)
	{
		var shapes = variant == Demolitions.Variant.THERMAL ? ChargeClusterShapes.THERMAL : ChargeClusterShapes.FRAGMENTATION;
		var shape = shapes[state.getValue(SIZE) - 1];
		return state.getValue(FACING) == Direction.UP ? shape : VoxelShapeUtil.rotateToFace(shape, state.getValue(FACING));
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
	{
		if (side == Direction.DOWN && neighbor.isAir())
			level.scheduleTick(pos, this, 1);
		return super.updateShape(state, side, neighbor, level, pos, neighborPos);
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random)
	{
		if (!level.getBlockState(pos.below()).isAir())
			return;
		level.removeBlock(pos, false);
		for (int i = 0; i < state.getValue(SIZE); i++)
		{
			var charge = GadgetCharge.create(variant, level, Vec3.atBottomCenterOf(pos), null);
			charge.setDeltaMovement((random.nextInt(15) - 7) / 100F, -0.1, (random.nextInt(15) - 7) / 100F);
			level.addFreshEntity(charge);
		}
	}

	boolean acceptCharge(ServerLevel level, BlockPos pos, GadgetCharge charge)
	{
		var current = level.getBlockState(pos);
		if (current.is(this) && current.getValue(SIZE) < 5)
			level.setBlockAndUpdate(pos, current.cycle(SIZE));
		else if (current.isAir() && !level.getBlockState(pos.below()).isAir())
			level.setBlockAndUpdate(pos, defaultBlockState());
		else
			return false;
		charge.discard();
		return true;
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
	                                          Player player, InteractionHand hand, BlockHitResult hit)
	{
		if (stack.is(variant.item()) && !player.isShiftKeyDown() && !stack.has(Demolitions.PRIMING_TIME.get()))
		{
			if (state.getValue(SIZE) >= 5)
				return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
			if (!level.isClientSide)
			{
				level.setBlockAndUpdate(pos, state.cycle(SIZE));
				stack.consume(1, player);
			}
			return ItemInteractionResult.sidedSuccess(level.isClientSide);
		}
		return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit)
	{
		if (!level.isClientSide)
		{
			if (!player.addItem(new ItemStack(variant.item())))
				return InteractionResult.PASS;
			if (state.getValue(SIZE) == 1)
				level.removeBlock(pos, false);
			else
				level.setBlockAndUpdate(pos, state.setValue(SIZE, state.getValue(SIZE) - 1));
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state)
	{
		return new ItemStack(variant.item());
	}

	@Override
	public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion)
	{
		super.onBlockExploded(state, level, pos, explosion);
		if (level instanceof ServerLevel server)
		{
			int count = state.getValue(SIZE);
			float power = variant == Demolitions.Variant.THERMAL ? count + 4 : count / 2 + 2;
			var charge = GadgetCharge.create(variant, server, Vec3.atBottomCenterOf(pos), explosion.getIndirectSourceEntity());
			charge.arm(0);
			charge.setPower(power);
			server.addFreshEntity(charge);
		}
	}
}
