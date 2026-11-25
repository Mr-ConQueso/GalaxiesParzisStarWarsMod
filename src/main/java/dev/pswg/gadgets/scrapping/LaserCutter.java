package dev.pswg.gadgets.scrapping;

import dev.pswg.gadgets.WorkstationRecipes;
import dev.pswg.gadgets.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;

public final class LaserCutter extends Item
{
	private static final int USE_TIME = 3600;
	private static final double CUT_AREA = 100;
	private static final double PIXELS_PER_BLOCK = 16;

	public LaserCutter(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		var level = context.getLevel();
		var pos = context.getClickedPos();
		var state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos);
		var player = context.getPlayer();
		if (player == null || player.isSpectator() || player.isUsingItem() || state.isAir() || hardness < 0)
			return InteractionResult.PASS;

		if (level instanceof ServerLevel server && player instanceof ServerPlayer operator)
		{
			if (!operator.canInteractWithBlock(pos, 0.5) || !server.mayInteract(operator, pos)
					|| operator.blockActionRestricted(server, pos, operator.gameMode.getGameModeForPlayer()))
				return InteractionResult.FAIL;
			ItemStack stack = context.getItemInHand();
			reset(stack);
			if (hardness <= 0.1F)
			{
				if (!completeCut(server, operator, stack, pos, state))
					return InteractionResult.FAIL;
			}
			else
				player.startUsingItem(context.getHand());
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.EAT;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user)
	{
		return USE_TIME;
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining)
	{
		if (!(level instanceof ServerLevel server) || !(user instanceof ServerPlayer player))
			return;
		var hit = ProjectileUtil.getHitResultOnViewVector(player, Entity::isPickable, player.blockInteractionRange());
		if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != BlockHitResult.Type.BLOCK)
			return;
		BlockPos target = blockHit.getBlockPos();
		BlockState state = level.getBlockState(target);
		if (state.isAir() || state.getDestroySpeed(level, target) < 0 || !server.mayInteract(player, target)
				|| player.blockActionRestricted(server, target, player.gameMode.getGameModeForPlayer()))
		{
			reset(stack);
			return;
		}
		Direction face = blockHit.getDirection();
		Vec3 point = blockHit.getLocation();

		var dimension = level.dimension().location();
		if (!target.equals(stack.get(Workstations.CUT_TARGET.get())) || face != stack.get(Workstations.CUT_FACE.get())
				|| !dimension.equals(stack.get(Workstations.CUT_DIMENSION.get())))
		{
			reset(stack);
			stack.set(Workstations.CUT_TARGET.get(), target);
			stack.set(Workstations.CUT_FACE.get(), face);
			stack.set(Workstations.CUT_DIMENSION.get(), dimension);
		}

		var previousMin = stack.getOrDefault(Workstations.CUT_MIN.get(), point);
		var previousMax = stack.getOrDefault(Workstations.CUT_MAX.get(), point);
		var min = new Vec3(Math.min(point.x, previousMin.x), Math.min(point.y, previousMin.y), Math.min(point.z, previousMin.z));
		var max = new Vec3(Math.max(point.x, previousMax.x), Math.max(point.y, previousMax.y), Math.max(point.z, previousMax.z));
		stack.set(Workstations.CUT_MIN.get(), min);
		stack.set(Workstations.CUT_MAX.get(), max);

		Vec3 span = max.subtract(min).scale(PIXELS_PER_BLOCK);
		double area = switch (face.getAxis())
		{
			case X -> Math.max(1, span.y) * Math.max(1, span.z);
			case Y -> Math.max(1, span.x) * Math.max(1, span.z);
			case Z -> Math.max(1, span.x) * Math.max(1, span.y);
		};
		stack.set(Workstations.CUT_PROGRESS.get(), (float) Math.min(area / CUT_AREA, 1));
		if (area >= CUT_AREA)
		{
			completeCut(server, player, stack, target, state);
			reset(stack);
		}
		else if (level.getGameTime() % 4 == 0)
			markSurface(server, point, face);
	}

	private static void markSurface(ServerLevel level, Vec3 point, Direction face)
	{
		Vec3 normal = Vec3.atLowerCornerOf(face.getNormal());
		var mark = new Vec3(markCoordinate(point.x, normal.x), markCoordinate(point.y, normal.y), markCoordinate(point.z, normal.z));
		// A zero-count particle packet forwards the offsets as a single face normal.
		level.sendParticles(Workstations.LASER_CUT.get(), mark.x, mark.y, mark.z, 0, normal.x, normal.y, normal.z, 1);
	}

	private static double markCoordinate(double point, double normal)
	{
		return normal == 0 ? (Math.floor(point * PIXELS_PER_BLOCK) + 0.5) / PIXELS_PER_BLOCK : point + normal * 0.001;
	}

	private static boolean completeCut(ServerLevel level, ServerPlayer player, ItemStack cutter, BlockPos pos, BlockState state)
	{
		if (CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled())
			return false;

		var input = new SingleRecipeInput(new ItemStack(state.getBlock()));
		var recipe = level.getRecipeManager().getRecipeFor(WorkstationRecipes.CUTTING.get(), input, level).orElse(null);
		if (!level.destroyBlock(pos, recipe == null, player, Block.UPDATE_LIMIT))
			return false;
		if (recipe != null)
		{
			var result = recipe.value();
			Block.popResource(level, pos, result.primary().copy());
			if (level.random.nextFloat() < result.secondaryChance())
				Block.popResource(level, pos, result.secondary().copy());
		}
		if (!player.hasInfiniteMaterials())
			cutter.setDamageValue(Math.min(cutter.getMaxDamage() - 1, cutter.getDamageValue() + 1));
		return true;
	}

	private static void reset(ItemStack stack)
	{
		stack.remove(Workstations.CUT_TARGET.get());
		stack.remove(Workstations.CUT_FACE.get());
		stack.remove(Workstations.CUT_DIMENSION.get());
		stack.remove(Workstations.CUT_MIN.get());
		stack.remove(Workstations.CUT_MAX.get());
		stack.remove(Workstations.CUT_PROGRESS.get());
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user)
	{
		reset(stack);
		return stack;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining)
	{
		reset(stack);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if (!level.isClientSide && stack.has(Workstations.CUT_TARGET.get())
				&& (!(entity instanceof LivingEntity user) || !user.isUsingItem() || user.getUseItem() != stack))
			reset(stack);
	}
}
