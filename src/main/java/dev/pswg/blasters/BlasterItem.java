package dev.pswg.blasters;

import dev.pswg.blasters.BlasterState.Mode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class BlasterItem extends Item
{
	private static final AttributeModifier AIMING_SPEED = new AttributeModifier(Blasters.id("aiming_speed_penalty"),
			-0.5F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

	public BlasterItem(Properties properties)
	{
		super(properties);
	}

	public static ItemStack createStack(ResourceLocation id, BlasterDefinition definition)
	{
		var stack = new ItemStack(Blasters.ITEM.get());
		stack.set(Blasters.ID.get(), id);
		stack.set(Blasters.ATTACHMENTS.get(), new Blasters.EquippedAttachments(definition.attachments().hud(), definition.attachments().defaults()));
		stack.set(DataComponents.ITEM_NAME, Component.translatable(id.toLanguageKey()));
		return stack;
	}

	public static BlasterState state(ItemStack stack)
	{
		return stack.getOrDefault(Blasters.STATE.get(), BlasterState.DEFAULT);
	}

	@Nullable
	public static BlasterDefinition definition(Level level, ItemStack stack)
	{
		return (level.isClientSide ? BlasterDefinitions.client() : BlasterDefinitions.SERVER.definitions())
				.get(stack.getOrDefault(Blasters.ID.get(), Blasters.MISSING));
	}

	public static Blasters.EquippedAttachments attachments(ItemStack stack, @Nullable BlasterDefinition definition)
	{
		var applied = stack.get(Blasters.ATTACHMENTS.get());
		if (applied != null)
			return applied;
		if (definition != null)
			return new Blasters.EquippedAttachments(definition.attachments().hud(), definition.attachments().defaults());
		return new Blasters.EquippedAttachments(Blasters.DEFAULT_HUD, java.util.Map.of());
	}

	public static float multiplier(ItemStack stack, BlasterDefinition definition, String function)
	{
		float combined = 1F;
		var functionId = Blasters.id(function);
		for (var entry : attachments(stack, definition).applied().entrySet())
		{
			var option = definition.attachments().options().get(entry.getValue());
			if (option != null && option.slots().contains(entry.getKey()) && option.function().equals(functionId)
					&& Float.isFinite(option.value()) && option.value() > 0F)
				combined *= option.value();
		}
		return Mth.clamp(combined, 0.01F, 100F);
	}

	public static float heat(ItemStack stack, BlasterDefinition definition, long now, float partial)
	{
		var state = state(stack);
		var heat = definition.stats().heat();
		var speed = (state.mode() == Mode.PASSIVE ? heat.drainSpeed() : heat.overheatDrainSpeed())
				* multiplier(stack, definition, "cooling_multiplier");
		var initial = state.mode() == Mode.PASSIVE ? state.lastTotalHeat() : state.lastVentingHeat();
		return (float) Math.max(0F, initial - speed * state.timeSince(now, state.cooldownStart(), partial));
	}

	public static float overcharge(BlasterState state, BlasterDefinition definition, long now, float partial)
	{
		int duration = definition.stats().heat().overchargeBonus();
		if (state.overchargeStart() < 0 || duration <= 0)
			return 0F;
		return Mth.clamp(1F - (float) state.timeSince(now, state.overchargeStart(), partial) / duration, 0F, 1F);
	}

	static boolean fire(ServerLevel level, Player player, ItemStack stack, boolean repeated)
	{
		var definition = definition(level, stack);
		if (definition == null)
			return false;

		var state = state(stack);
		long now = level.getGameTime();
		if (state.fireCooldown() > now)
			return false;

		float heat = heat(stack, definition, now, 0);
		if (state.mode() != Mode.PASSIVE && heat > 0)
		{
			if (state.mode() != Mode.OVERHEAT || repeated)
				return false;

			var cooling = definition.stats().cooling();
			float cursor = heat / state.lastVentingHeat();
			if (Math.abs(cursor - cooling.primaryBypassTime()) <= cooling.primaryBypassTolerance())
			{
				stack.set(Blasters.STATE.get(), state.bypass(now, false));
				play(level, player, "bypass.primary");
			}
			else if (Math.abs(cursor - cooling.secondaryBypassTime()) <= cooling.secondaryBypassTolerance())
			{
				stack.set(Blasters.STATE.get(), state.bypass(now, true));
				play(level, player, "bypass.secondary");
			}
			else
			{
				stack.set(Blasters.STATE.get(), state.withCooling(Mode.FAILED_OVERCHARGE, now, heat));
				play(level, player, "bypass.failed");
			}
			return false;
		}

		var stats = definition.stats();
		var bolt = new BlasterBolt(Blasters.BOLT.get(), level);
		bolt.launch(player, stats.damage(), stats.range());
		if (!level.addFreshEntity(bolt))
			return false;

		int interval = Math.max(1, Math.round(stats.automaticRepeatDelay() / multiplier(stack, definition, "fire_rate_multiplier")));
		if (overcharge(state, definition, now, 0) == 0F)
			heat += stats.heat().perRound();
		state = new BlasterState(state.isAiming(), now, now + interval, now + stats.heat().passiveCooldownDelay(),
				heat, 0, Mode.PASSIVE, 0, state.overchargeStart());

		level.playSound(null, player.blockPosition(), Blasters.sound(stats.fireSound()), SoundSource.NEUTRAL, 1F, pitch(level));
		if (heat > stats.heat().capacity())
		{
			state = state.withCooling(Mode.OVERHEAT, now, heat + stats.heat().overheatPenalty());
			play(level, player, "overheat");
		}
		stack.set(Blasters.STATE.get(), state);

		if (player instanceof ServerPlayer serverPlayer)
		{
			float recoil = multiplier(stack, definition, "recoil_multiplier");
			float vertical = -(3.6F + (float) level.random.nextGaussian() * 0.2F) * recoil;
			float horizontal = (0.2F + (float) level.random.nextGaussian() * 0.2F) * recoil;
			BlasterNetwork.sendRecoil(serverPlayer, vertical, horizontal);
		}
		return true;
	}

	static void vent(ServerLevel level, Player player, ItemStack stack)
	{
		var definition = definition(level, stack);
		if (definition == null)
			return;
		float heat = heat(stack, definition, level.getGameTime(), 0);
		var state = state(stack);
		if (state.mode() == Mode.PASSIVE && heat > 0)
		{
			stack.set(Blasters.STATE.get(), state.withCooling(Mode.REQUESTED_BYPASS, level.getGameTime(), heat));
			play(level, player, "vent");
		}
	}

	private static void play(ServerLevel level, Player player, String sound)
	{
		level.playSound(null, player.blockPosition(), Blasters.sound(Blasters.id("blaster." + sound)), SoundSource.PLAYERS, 1F, pitch(level));
	}

	private static float pitch(Level level)
	{
		return 0.9F + level.random.nextFloat() * 0.2F;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity owner, int slot, boolean selected)
	{
		if (level.isClientSide)
			return;
		if (!stack.has(Blasters.SERIAL.get()))
			stack.set(Blasters.SERIAL.get(), level.random.nextLong());
		if (state(stack).isAiming() && owner instanceof LivingEntity living
				&& stack != living.getMainHandItem() && stack != living.getOffhandItem())
			setAiming(stack, false);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		var stack = player.getItemInHand(hand);
		if (definition(level, stack) == null)
			return InteractionResultHolder.fail(stack);
		setAiming(stack, !state(stack).isAiming());
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user)
	{
		return 72000;
	}

	@Override
	public void releaseUsing(ItemStack stack, Level level, LivingEntity user, int remaining)
	{
		if (state(stack).isAiming() && getUseDuration(stack, user) - remaining > 3)
			setAiming(stack, false);
	}

	private static void setAiming(ItemStack stack, boolean aiming)
	{
		stack.set(Blasters.STATE.get(), state(stack).withAim(aiming));
		var attributes = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		var updated = aiming
				? attributes.withModifierAdded(Attributes.MOVEMENT_SPEED, AIMING_SPEED, EquipmentSlotGroup.HAND)
				: new ItemAttributeModifiers(attributes.modifiers().stream()
						.filter(entry -> !entry.modifier().is(AIMING_SPEED.id())).toList(), attributes.showInTooltip());
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, updated);
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player)
	{
		return false;
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack before, ItemStack after, boolean movedSlot)
	{
		var serial = before.get(Blasters.SERIAL.get());
		return movedSlot || !after.is(this) || serial != null && !Objects.equals(serial, after.get(Blasters.SERIAL.get()));
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		tooltip.add(Component.translatable("text.pswg_blasters.vent_blaster")
				.append(" · ").append(Component.keybind("key.pswg.primary_action")));
	}

	public static InteractionHand firingHand(Player player)
	{
		if (player.getOffhandItem().is(Blasters.ITEM.get())
				&& (player.getUsedItemHand() == InteractionHand.OFF_HAND && player.isUsingItem()
						|| player.getMainHandItem().isEmpty()))
			return InteractionHand.OFF_HAND;
		return InteractionHand.MAIN_HAND;
	}
}
