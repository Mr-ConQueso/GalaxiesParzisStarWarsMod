package dev.pswg.gadgets.drill;

import dev.pswg.container.GalaxiesItems;
import dev.pswg.gadgets.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class ExtractionDrill extends Item
{
	private static final int DEFAULT_TIME = 100;

	public ExtractionDrill(Properties properties)
	{
		super(properties);
	}

	public static Tool toolProperties(DrillParts parts)
	{
		var incorrect = switch (parts.drill().miningLevel())
		{
			case 0 -> BlockTags.INCORRECT_FOR_WOODEN_TOOL;
			case 1 -> BlockTags.INCORRECT_FOR_STONE_TOOL;
			case 2 -> BlockTags.INCORRECT_FOR_IRON_TOOL;
			case 3 -> BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
			default -> BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
		};
		return new Tool(List.of(Tool.Rule.deniesDrops(incorrect),
				Tool.Rule.minesAndDrops(BlockTags.MINEABLE_WITH_PICKAXE, parts.drill().miningSpeed())), 1.5F, 1);
	}

	public static ItemStack configuredStack(DrillParts parts)
	{
		ItemStack stack = Workstations.DRILL.toStack();
		stack.set(Workstations.DRILL_PARTS.get(), parts);
		applyParts(stack, parts);
		return stack;
	}

	private static void applyParts(ItemStack stack, DrillParts parts)
	{
		Tool tool = toolProperties(parts);
		if (!tool.equals(stack.get(DataComponents.TOOL)))
			stack.set(DataComponents.TOOL, tool);
		if (parts.durability() != stack.getMaxDamage())
		{
			stack.set(DataComponents.MAX_DAMAGE, parts.durability());
			stack.setDamageValue(Math.min(stack.getDamageValue(), parts.durability() - 1));
		}
	}

	@Override
	public InteractionResult useOn(UseOnContext context)
	{
		Level level = context.getLevel();
		BlockPos origin = context.getClickedPos();
		Player player = context.getPlayer();
		if (player == null || player.isSpectator() || player.isUsingItem()
				|| !level.getBlockState(origin).is(Tags.Blocks.ORES))
			return InteractionResult.PASS;

		if (level instanceof ServerLevel server && player instanceof ServerPlayer operator)
		{
			if (!operator.canInteractWithBlock(origin, 0.5) || !server.mayInteract(operator, origin))
				return InteractionResult.FAIL;
			ItemStack stack = context.getItemInHand();
			DrillParts parts = stack.getOrDefault(Workstations.DRILL_PARTS.get(), DrillParts.DEFAULT);
			applyParts(stack, parts);
			var targets = markOres(server, origin, parts.capsule().maxBlockCount());
			float wear = (float) stack.getDamageValue() / stack.getMaxDamage();
			float wearMultiplier = 1F + Math.max(0, wear - 0.8F) * 5F;
			int duration = Math.max(1, (int) (DEFAULT_TIME / parts.extractor().extractionSpeed()
					* targets.size() / parts.capsule().maxBlockCount() * wearMultiplier));
			stack.set(Workstations.DRILL_EXTRACTION.get(),
					new DrillExtraction(0, targets.size(), targets, origin, level.dimension().location(), duration));
			player.startUsingItem(context.getHand());
		}
		return InteractionResult.sidedSuccess(level.isClientSide);
	}

	private static List<BlockPos> markOres(ServerLevel level, BlockPos origin, int capacity)
	{
		var marked = new ArrayList<BlockPos>();
		var seen = new HashSet<BlockPos>();
		var pending = new ArrayDeque<BlockPos>();
		marked.add(origin);
		seen.add(origin);
		pending.push(origin);
		while (!pending.isEmpty() && marked.size() < capacity)
		{
			BlockPos pos = pending.pop();
			for (Direction side : Direction.values())
			{
				BlockPos neighbor = pos.relative(side);
				if (seen.add(neighbor) && level.hasChunkAt(neighbor) && level.getBlockState(neighbor).is(Tags.Blocks.ORES))
				{
					marked.add(neighbor);
					pending.push(neighbor);
					if (marked.size() == capacity)
						break;
				}
			}
		}
		return marked;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user)
	{
		var extraction = stack.get(Workstations.DRILL_EXTRACTION.get());
		return extraction == null ? DEFAULT_TIME : extraction.duration();
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining)
	{
		if (!(level instanceof ServerLevel server) || !(user instanceof ServerPlayer player))
			return;
		var extraction = stack.get(Workstations.DRILL_EXTRACTION.get());
		if (extraction == null || !extraction.dimension().equals(level.dimension().location())
				|| !player.canInteractWithBlock(extraction.origin(), 0.5))
		{
			user.releaseUsingItem();
			return;
		}

		int elapsed = Mth.clamp(extraction.duration() - remaining + 1, 0, extraction.duration());
		int due = Math.min(extraction.blocksToExtract(), elapsed * (extraction.blocksToExtract() + 1) / extraction.duration());
		while (extraction.blocksExtracted() < due && !extraction.markedBlocks().isEmpty() && !stack.isEmpty())
		{
			BlockPos target = extraction.markedBlocks().getLast();
			extraction = extraction.advance();
			stack.set(Workstations.DRILL_EXTRACTION.get(), extraction);
			extract(server, player, stack, target);
		}
	}

	private static void extract(ServerLevel level, ServerPlayer player, ItemStack stack, BlockPos pos)
	{
		if (!level.hasChunkAt(pos) || !level.mayInteract(player, pos))
			return;
		var state = level.getBlockState(pos);
		if (!state.is(Tags.Blocks.ORES) || state.getDestroySpeed(level, pos) < 0
				|| player.blockActionRestricted(level, pos, player.gameMode.getGameModeForPlayer())
				|| CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state).isCanceled())
			return;

		var drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), player, stack);
		var replacement = state.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE) ? Blocks.DEEPSLATE
				: state.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK) ? Blocks.NETHERRACK : Blocks.STONE;
		if (!level.setBlockAndUpdate(pos, replacement.defaultBlockState()))
			return;

		float multiplier = stack.getOrDefault(Workstations.DRILL_PARTS.get(), DrillParts.DEFAULT).extractor().extractionMultiplier();
		for (var drop : drops)
		{
			int count = drop.getCount() * level.random.nextIntBetweenInclusive((int) Math.floor(multiplier), (int) Math.ceil(2 * multiplier));
			while (count > 0)
			{
				var portion = drop.copyWithCount(Math.min(count, drop.getMaxStackSize()));
				count -= portion.getCount();
				if (!player.getInventory().add(portion))
					player.drop(portion, false);
			}
		}
		stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if (level.isClientSide)
			return;
		var parts = stack.getOrDefault(Workstations.DRILL_PARTS.get(), DrillParts.DEFAULT);
		if (parts != DrillParts.DEFAULT || stack.has(Workstations.DRILL_EXTRACTION.get()))
			applyParts(stack, parts);
		if (stack.has(Workstations.DRILL_EXTRACTION.get())
				&& (!(entity instanceof LivingEntity user) || !user.isUsingItem() || user.getUseItem() != stack))
			stack.remove(Workstations.DRILL_EXTRACTION.get());
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user)
	{
		stack.remove(Workstations.DRILL_EXTRACTION.get());
		return stack;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining)
	{
		stack.remove(Workstations.DRILL_EXTRACTION.get());
	}

	@Override
	public boolean isValidRepairItem(ItemStack stack, ItemStack material)
	{
		return material.is(GalaxiesItems.TITANIUM_INGOT.get());
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
	{
		return true;
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
	{
		stack.hurtAndBreak(2, attacker, EquipmentSlot.MAINHAND);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		var parts = stack.getOrDefault(Workstations.DRILL_PARTS.get(), DrillParts.DEFAULT);
		for (var id : parts.partIds())
			tooltip.add(Component.translatable("part." + id.toLanguageKey()));
	}
}
