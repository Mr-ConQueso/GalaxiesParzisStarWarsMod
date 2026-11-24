package dev.pswg.item;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Supplier;

public final class FilledDrinkItem extends Item
{
	private final Supplier<Item> empty;

	public FilledDrinkItem(Supplier<Item> empty, Properties properties)
	{
		super(properties);
		this.empty = empty;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		return ItemUtils.startUsingInstantly(level, player, hand);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.DRINK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user)
	{
		return 32;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity user)
	{
		if (user instanceof ServerPlayer player)
			CriteriaTriggers.CONSUME_ITEM.trigger(player, stack);

		if (!level.isClientSide)
		{
			var food = stack.get(DataComponents.FOOD);
			if (food != null && user instanceof Player player)
				player.getFoodData().eat(food);
			stack.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).forEachEffect(instance -> {
				var effect = instance.getEffect().value();
				if (effect.isInstantenous())
					effect.applyInstantenousEffect(user, user, user, instance.getAmplifier(), 1.0);
				else
					user.addEffect(instance);
			});
		}

		if (user instanceof Player player)
			player.awardStat(Stats.ITEM_USED.get(this));
		stack.consume(1, user);
		user.gameEvent(GameEvent.DRINK);

		if (user.hasInfiniteMaterials())
			return stack;

		var remainder = new ItemStack(empty.get());
		if (stack.isEmpty())
			return remainder;

		if (!level.isClientSide)
		{
			if (user instanceof Player player)
			{
				if (!player.getInventory().add(remainder))
					player.drop(remainder, false);
			}
			else
			{
				user.spawnAtLocation(remainder);
			}
		}
		return stack;
	}
}
