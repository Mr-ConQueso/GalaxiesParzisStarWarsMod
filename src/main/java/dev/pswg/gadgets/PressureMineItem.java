package dev.pswg.gadgets;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class PressureMineItem extends Item
{
	PressureMineItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		var stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server)
		{
			var mine = GadgetCharge.create(Demolitions.Variant.PRESSURE, server, player.position().add(0, 1.5, 0), player);
			mine.setDeltaMovement(player.getForward().multiply(0.6, 0.4, 0.6));
			if (!server.addFreshEntity(mine))
				return InteractionResultHolder.fail(stack);
			stack.consume(1, player);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}
}
