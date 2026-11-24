package dev.pswg.gadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class GrenadeItem extends Item implements ProjectileItem
{
	private final Demolitions.Variant variant;

	GrenadeItem(Demolitions.Variant variant, Properties properties)
	{
		super(properties);
		this.variant = variant;
	}

	public void togglePrimed(ServerLevel level, Player player, ItemStack stack)
	{
		boolean wasPrimed = stack.has(Demolitions.PRIMING_TIME.get());
		if (wasPrimed)
			stack.remove(Demolitions.PRIMING_TIME.get());
		else
			stack.set(Demolitions.PRIMING_TIME.get(), level.getGameTime());

		level.playSound(null, player.blockPosition(), Demolitions.sound(wasPrimed ? "shared.disarm" : "shared.arm"),
				SoundSource.PLAYERS, 1F, 1F);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		var stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel server)
		{
			var grenade = GadgetCharge.create(variant, server, player.getEyePosition().subtract(0, 0.12, 0), player);
			var primed = stack.get(Demolitions.PRIMING_TIME.get());
			if (primed != null)
				grenade.arm(remaining(level, primed));
			grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), (float) player.getLookAngle().z * 10F, 1F, 0F);
			if (!server.addFreshEntity(grenade))
				return InteractionResultHolder.fail(stack);

			server.playSound(null, player.blockPosition(), Demolitions.sound("shared.throw"), SoundSource.PLAYERS, 1F, 1F);
			stack.remove(Demolitions.PRIMING_TIME.get());
			stack.consume(1, player);
			player.awardStat(Stats.ITEM_USED.get(this));
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if (!(level instanceof ServerLevel server) || !(entity instanceof Player player))
			return;

		if (player.isOnFire())
		{
			var inventory = player.getInventory();
			int count = inventory.countItem(this);
			if (count > 0)
			{
				var grenade = GadgetCharge.create(variant, server, player.position().add(0, 1, 0), player);
				grenade.setPower(count * 2F);
				grenade.arm(0);
				if (server.addFreshEntity(grenade))
				{
					for (int i = 0; i < inventory.getContainerSize(); i++)
						if (inventory.getItem(i).is(this))
							inventory.setItem(i, ItemStack.EMPTY);
					inventory.setChanged();
				}
			}
			return;
		}

		Long primed = stack.get(Demolitions.PRIMING_TIME.get());
		if (primed != null)
		{
			int remaining = remaining(level, primed);
			if (selected || player.getOffhandItem() == stack)
				player.displayClientMessage(Component.literal(Integer.toString(remaining)), true);
			if (remaining <= 0)
			{
				var grenade = GadgetCharge.create(variant, server, player.position().add(0, 1, 0), player);
				grenade.arm(0);
				if (server.addFreshEntity(grenade))
				{
					stack.remove(Demolitions.PRIMING_TIME.get());
					stack.consume(1, player);
				}
			}
		}
	}

	private int remaining(Level level, long primed)
	{
		return (int) Mth.clamp(variant.delay - (level.getGameTime() - primed), 0, variant.delay);
	}

	@Override
	public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction side)
	{
		var grenade = GadgetCharge.create(variant, level, new net.minecraft.world.phys.Vec3(pos.x(), pos.y(), pos.z()), null);
		grenade.arm(variant.delay);
		return grenade;
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player)
	{
		return false;
	}
}
