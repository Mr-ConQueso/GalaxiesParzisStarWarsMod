package dev.pswg.blasters;

import dev.pswg.blasters.BlasterState.Mode;
import dev.pswg.blasters.LegacyBlaster.FiringMode;
import dev.pswg.Galaxies;
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
import java.util.stream.Stream;

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
		var preset = Blasters.PRESETS.get(id);
		var stack = new ItemStack(preset == null ? Blasters.ITEM.get() : preset.get());
		stack.set(Blasters.ID.get(), id);
		stack.set(Blasters.ATTACHMENTS.get(), new Blasters.EquippedAttachments(definition.attachments().hud(), definition.attachments().defaults()));
		if (preset == null)
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

	public static boolean isBlaster(ItemStack stack)
	{
		return stack.getItem() instanceof BlasterItem;
	}

	public static Stream<BlasterDefinition.Attachment> equippedOptions(ItemStack stack, BlasterDefinition definition)
	{
		return attachments(stack, definition).applied().entrySet().stream().flatMap(entry -> {
			var option = definition.attachments().options().get(entry.getValue());
			return option != null && option.slots().contains(entry.getKey()) ? Stream.of(option) : Stream.empty();
		});
	}

	public static boolean hasFunction(ItemStack stack, BlasterDefinition definition, String function)
	{
		var functionId = Blasters.id(function);
		return equippedOptions(stack, definition).anyMatch(option -> option.function().equals(functionId));
	}

	public static FiringMode firingMode(ItemStack stack, BlasterDefinition definition)
	{
		var legacy = definition.legacy().orElse(null);
		if (legacy == null)
			return FiringMode.AUTOMATIC;
		var modes = legacy.availableModes(stack, definition);
		var mode = stack.get(Blasters.FIRING_MODE.get());
		return modes.contains(mode) ? mode : modes.getFirst();
	}

	public static boolean dualWield(Player player)
	{
		var main = definition(player.level(), player.getMainHandItem());
		var offhand = definition(player.level(), player.getOffhandItem());
		return isBlaster(player.getMainHandItem()) && isBlaster(player.getOffhandItem()) && main != null && offhand != null
				&& main.legacy().map(LegacyBlaster::oneHanded).orElse(false)
				&& offhand.legacy().map(LegacyBlaster::oneHanded).orElse(false);
	}

	static void cycleMode(ServerLevel level, Player player, ItemStack stack)
	{
		var definition = definition(level, stack);
		if (definition == null || definition.legacy().isEmpty())
			return;
		var modes = definition.legacy().get().availableModes(stack, definition);
		var mode = modes.get((modes.indexOf(firingMode(stack, definition)) + 1) % modes.size());
		stack.set(Blasters.FIRING_MODE.get(), mode);
		stack.set(Blasters.STATE.get(), state(stack).withoutBurst());
		player.displayClientMessage(Component.translatable("msg.pswg.blaster_mode_changed", Component.translatable(mode.translation())), true);
	}

	static void togglePatrol(Player player, ItemStack stack)
	{
		var definition = definition(player.level(), stack);
		if (definition == null || definition.legacy().isEmpty())
			return;
		boolean enabled = !player.getData(Blasters.PATROL);
		BlasterNetwork.patrol(player, enabled);
		if (enabled)
			for (var hand : InteractionHand.values())
			{
				var held = player.getItemInHand(hand);
				if (isBlaster(held) && state(held).isAiming())
					setAiming(held, false, definition(player.level(), held));
			}
	}

	public static float zoom(ItemStack stack, BlasterDefinition definition)
	{
		float base = definition.legacy().map(legacy -> legacy.mechanics().zoom()).orElse(3F);
		float scope = 1F;
		for (int magnification : new int[] { 2, 3, 5, 8 })
			if (hasFunction(stack, definition, "increase_zoom_" + magnification + "x"))
				scope = magnification;
		return base * scope * multiplier(stack, definition, "zoom_multiplier");
	}

	public static float multiplier(ItemStack stack, BlasterDefinition definition, String function)
	{
		var functionId = Blasters.id(function);
		float combined = equippedOptions(stack, definition)
				.filter(option -> option.function().equals(functionId))
				.map(BlasterDefinition.Attachment::value)
				.filter(value -> Float.isFinite(value) && value > 0)
				.reduce(1F, (left, right) -> left * right);
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
		return fire(level, player, stack, repeated, stack == player.getOffhandItem() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
	}

	private static boolean fire(ServerLevel level, Player player, ItemStack stack, boolean repeated, InteractionHand hand)
	{
		var definition = definition(level, stack);
		if (definition == null || player.getVehicle() instanceof dev.pswg.vehicles.Ship)
			return false;
		var legacy = definition.legacy().orElse(null);
		var state = state(stack);
		long now = level.getGameTime();
		if (state.fireCooldown() > now || stack.getOrDefault(Blasters.READY.get(), 0L) > now)
			return false;

		float heat = heat(stack, definition, now, 0);
		var stats = definition.stats();
		if (state.mode() != Mode.PASSIVE && heat > 0)
		{
			if (state.mode() != Mode.OVERHEAT || repeated)
				return false;
			tryBypass(level, player, stack, definition, state, heat, now);
			return false;
		}

		var mode = firingMode(stack, definition);
		if (repeated && mode != FiringMode.AUTOMATIC && (mode != FiringMode.BURST || state.burstBoltsRemaining() == 0))
			return false;

		float fireRate = multiplier(stack, definition, "fire_rate_multiplier");
		float baseDelay = stats.automaticRepeatDelay() / fireRate;
		int interval = Math.max(1, legacy == null ? Math.round(baseDelay) : Mth.ceil(baseDelay));
		int burstRemaining = 0;
		int ammo = stack.getOrDefault(Blasters.AMMO.get(), 0);
		if (legacy != null)
		{
			var mechanics = legacy.mechanics();
			if (mode == FiringMode.BURST)
			{
				burstRemaining = state.burstBoltsRemaining() > 0 ? state.burstBoltsRemaining() - 1 : mechanics.burstSize() - 1;
				int delay = mechanics.burstInterval() * (burstRemaining == 0 ? mechanics.burstGap() : 1);
				interval = Math.max(1, (int) Math.ceil(delay / fireRate));
			}
			boolean canFireUnderwater = mechanics.water().equals("can_fire_underwater") || hasFunction(stack, definition, "waterproof_firing");
			if (player.isUnderWater() && !canFireUnderwater)
				return dryFire(level, player, stack, state, now + interval);

			if (!player.hasInfiniteMaterials() && ammo == 0)
			{
				ammo = reload(level, player, stack, definition, legacy);
				if (ammo == 0)
					return dryFire(level, player, stack, state, now + interval);
			}
			if (mode == FiringMode.ION && hasFunction(stack, definition, "ion_to_gas_conversion"))
				mode = FiringMode.SEMI_AUTOMATIC;
			else if (mode == FiringMode.ION && hasFunction(stack, definition, "ion_to_repulsor_conversion"))
				mode = FiringMode.STUN;
		}

		var bolt = new BlasterBolt(Blasters.BOLT.get(), level);
		float damage = stats.damage() * multiplier(stack, definition, "damage_multiplier");
		int range = Math.max(1, Math.round(stats.range() * multiplier(stack, definition, "range_multiplier")));
		if (legacy == null)
			bolt.launch(player, hand, damage, range);
		else
			bolt.launchLegacy(player, hand, stack, definition, legacy, mode, damage, range);
		if (!level.addFreshEntity(bolt))
			return false;
		if (mode == FiringMode.STUN)
			bolt.stunCone(player);

		if (legacy != null && !player.hasInfiniteMaterials())
			stack.set(Blasters.AMMO.get(), ammo - 1);
		if (overcharge(state, definition, now, 0) == 0F)
		{
			float gain = stats.heat().perRound() * multiplier(stack, definition, "heat_multiplier");
			heat += legacy == null ? gain : (int) gain;
		}
		if (legacy != null)
			BlasterNetwork.patrol(player, false);
		state = new BlasterState(state.isAiming(), now, now + interval, now + stats.heat().passiveCooldownDelay(),
				heat, 0, Mode.PASSIVE, burstRemaining, state.overchargeStart());

		var sound = mode == FiringMode.STUN ? Galaxies.id("blaster.stun") : stats.fireSound();
		level.playSound(null, player.blockPosition(), Blasters.sound(sound), SoundSource.PLAYERS, 1F, pitch(level));
		if (heat > stats.heat().capacity())
		{
			float startHeat = (legacy == null ? heat : stats.heat().capacity()) + stats.heat().overheatPenalty();
			state = state.withCooling(Mode.OVERHEAT, now, startHeat);
			play(level, player, "overheat", legacy != null);
		}
		stack.set(Blasters.STATE.get(), state);

		if (player instanceof ServerPlayer serverPlayer && mode != FiringMode.STUN)
		{
			float recoil = multiplier(stack, definition, "recoil_multiplier");
			float vertical = -(3.6F + (float) level.random.nextGaussian() * 0.2F);
			float horizontal = 0.2F + (float) level.random.nextGaussian() * 0.2F;
			if (legacy != null)
			{
				double noise = level.random.nextGaussian();
				horizontal = legacy.mechanics().recoilX() * (float) (0.3 * noise + 0.7 * Math.signum(noise));
				vertical = -legacy.mechanics().recoilY() * (float) (0.7 + 0.15 * (level.random.nextGaussian() + 1));
			}
			BlasterNetwork.sendRecoil(serverPlayer, vertical * recoil, horizontal * recoil);
		}
		return true;
	}

	private static void tryBypass(ServerLevel level, Player player, ItemStack stack, BlasterDefinition definition,
	                              BlasterState state, float heat, long now)
	{
		var cooling = definition.stats().cooling();
		float basis = definition.legacy().isPresent() ? definition.stats().heat().capacity() : state.lastVentingHeat();
		float cursor = heat / Math.max(1F, basis);
		if (cooling.primaryBypassTolerance() > 0 && Math.abs(cursor - cooling.primaryBypassTime()) <= cooling.primaryBypassTolerance())
		{
			stack.set(Blasters.STATE.get(), state.bypass(now, false));
			play(level, player, "bypass.primary", definition.legacy().isPresent());
		}
		else if (cooling.secondaryBypassTolerance() > 0 && Math.abs(cursor - cooling.secondaryBypassTime()) <= cooling.secondaryBypassTolerance())
		{
			stack.set(Blasters.STATE.get(), state.bypass(now, true));
			play(level, player, "bypass.secondary", definition.legacy().isPresent());
		}
		else
		{
			stack.set(Blasters.STATE.get(), state.withCooling(Mode.FAILED_OVERCHARGE, now, heat));
			play(level, player, "bypass.failed", definition.legacy().isPresent());
		}
	}

	private static boolean dryFire(ServerLevel level, Player player, ItemStack stack, BlasterState state, long ready)
	{
		stack.set(Blasters.STATE.get(), state.withFireCooldown(ready).withoutBurst());
		play(level, player, "dryfire", true);
		return false;
	}

	private static int reload(ServerLevel level, Player player, ItemStack stack, BlasterDefinition definition, LegacyBlaster legacy)
	{
		var inventory = player.getInventory();
		for (int slot = 0; slot < inventory.getContainerSize(); slot++)
			if (inventory.getItem(slot).is(Blasters.SMALL_POWER_PACK.get()))
			{
				inventory.removeItem(slot, 1);
				play(level, player, "reload", true);
				return Math.min(75, Math.max(1, Math.round(legacy.mechanics().magazine()
						* multiplier(stack, definition, "magazine_multiplier"))));
			}
		return 0;
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
			if (definition.legacy().isEmpty())
				play(level, player, "vent", false);
		}
	}

	private static void play(ServerLevel level, Player player, String sound, boolean legacy)
	{
		var id = legacy ? Galaxies.id("blaster." + sound) : Blasters.id("blaster." + sound);
		level.playSound(null, player.blockPosition(), Blasters.sound(id), SoundSource.PLAYERS, 1F, legacy ? 1F : pitch(level));
	}

	private static float pitch(Level level)
	{
		return 0.9F + level.random.nextFloat() * 0.2F;
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity owner, int slot, boolean selected)
	{
		if (!(level instanceof ServerLevel server))
			return;
		if (!stack.has(Blasters.SERIAL.get()))
			stack.set(Blasters.SERIAL.get(), level.random.nextLong());
		if (!(owner instanceof LivingEntity living))
			return;
		boolean inHand = selected || stack == living.getOffhandItem();
		var definition = definition(level, stack);
		var state = state(stack);
		if (state.isAiming() && (!inHand || living.isSprinting()))
			setAiming(stack, false, definition);

		if (definition == null || definition.legacy().isEmpty())
			return;
		if (!inHand)
		{
			stack.remove(Blasters.READY.get());
			if (state.burstBoltsRemaining() > 0)
				stack.set(Blasters.STATE.get(), state(stack).withoutBurst());
			return;
		}
		long now = level.getGameTime();
		if (!stack.has(Blasters.READY.get()))
			stack.set(Blasters.READY.get(), now + definition.legacy().get().mechanics().readyTime());
		if (living instanceof Player && state.overchargeStart() >= 0 && overcharge(state, definition, now, 0F) == 0F)
		{
			stack.set(Blasters.STATE.get(), new BlasterState(state.isAiming(), state.lastFired(), state.fireCooldown(),
					state.cooldownStart(), state.lastTotalHeat(), state.lastVentingHeat(), state.mode(), state.burstBoltsRemaining(), -1));
		}
		if (state.burstBoltsRemaining() > 0 && living instanceof Player player && player.isAlive() && !player.isSpectator()
				&& player.containerMenu == player.inventoryMenu)
			fire(server, player, stack, true);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		var stack = player.getItemInHand(hand);
		var definition = definition(level, stack);
		if (definition == null || player.getVehicle() instanceof dev.pswg.vehicles.Ship)
			return InteractionResultHolder.fail(stack);
		if (dualWield(player))
		{
			if (level instanceof ServerLevel server)
				fire(server, player, player.getOffhandItem(), false);
			player.startUsingItem(InteractionHand.OFF_HAND);
			return InteractionResultHolder.consume(player.getItemInHand(hand));
		}

		boolean aiming = !player.isSprinting() && !state(stack).isAiming();
		if (aiming && definition.legacy().isPresent())
			BlasterNetwork.patrol(player, false);
		setAiming(stack, aiming, definition);
		if (!player.isSprinting())
			player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remaining)
	{
		if (level instanceof ServerLevel server && user instanceof Player player && dualWield(player)
				&& player.getUsedItemHand() == InteractionHand.OFF_HAND)
			fire(server, player, stack, true);
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
			setAiming(stack, false, definition(level, stack));
	}

	private static void setAiming(ItemStack stack, boolean aiming, @Nullable BlasterDefinition definition)
	{
		stack.set(Blasters.STATE.get(), state(stack).withAim(aiming));
		var attributes = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		var modifier = definition != null && definition.legacy().isPresent()
				? new AttributeModifier(AIMING_SPEED.id(), definition.legacy().get().mechanics().aimSpeed(), AIMING_SPEED.operation())
				: AIMING_SPEED;
		var updated = aiming
				? attributes.withModifierAdded(Attributes.MOVEMENT_SPEED, modifier, EquipmentSlotGroup.HAND)
				: new ItemAttributeModifiers(attributes.modifiers().stream()
						.filter(entry -> !entry.modifier().is(AIMING_SPEED.id())).toList(), attributes.showInTooltip());
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, updated);
	}

	@Override
	public Component getName(ItemStack stack)
	{
		var id = stack.get(Blasters.ID.get());
		return id != null && Blasters.PRESETS.containsKey(id)
				? Component.translatable("blaster." + id.toLanguageKey()) : super.getName(stack);
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
		if (Blasters.PRESETS.containsKey(stack.get(Blasters.ID.get())))
			tooltip.add(Component.translatable("tooltip.pswg.blaster.controls", Component.keybind("key.pswg.blaster_mode"),
					Component.keybind("key.pswg.primary_action")));
		else
			tooltip.add(Component.translatable("text.pswg_blasters.vent_blaster")
				.append(" · ").append(Component.keybind("key.pswg.primary_action")));
	}

	public static InteractionHand firingHand(Player player)
	{
		if (isBlaster(player.getOffhandItem()) && !dualWield(player)
				&& (player.getUsedItemHand() == InteractionHand.OFF_HAND && player.isUsingItem()
						|| state(player.getOffhandItem()).isAiming() || player.getMainHandItem().isEmpty()))
			return InteractionHand.OFF_HAND;
		return InteractionHand.MAIN_HAND;
	}
}
