package dev.pswg.sabers;

import dev.pswg.Galaxies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class SaberItem extends Item
{
	private static final ItemAttributeModifiers OFF = ItemAttributeModifiers.builder()
			.add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -2.4, AttributeModifier.Operation.ADD_VALUE),
					EquipmentSlotGroup.MAINHAND).build();
	private static final ItemAttributeModifiers ON = OFF.withModifierAdded(Attributes.ATTACK_DAMAGE,
			new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 22, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.MAINHAND);

	private final SaberPreset preset;

	public SaberItem(SaberPreset preset, Properties properties)
	{
		super(properties.attributes(OFF));
		this.preset = preset;
	}

	public SaberPreset preset()
	{
		return preset;
	}

	public static Sabers.BladeState state(ItemStack stack)
	{
		return stack.getOrDefault(Sabers.BLADE.get(), new Sabers.BladeState(0x6CFF00, false, false, -8));
	}

	public static boolean blocks(LivingEntity entity)
	{
		var stack = entity.getUseItem();
		return entity.isUsingItem() && stack.getItem() instanceof SaberItem && state(stack).active();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
	{
		var stack = player.getItemInHand(hand);
		if (!state(stack).active())
			return InteractionResultHolder.pass(stack);
		player.startUsingItem(hand);
		return InteractionResultHolder.consume(stack);
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack)
	{
		return UseAnim.BLOCK;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity user)
	{
		return 72000;
	}

	@Override
	public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player)
	{
		return false;
	}

	@Override
	public Component getName(ItemStack stack)
	{
		return preset.owner().isEmpty() ? Component.translatable("item.pswg.lightsaber." + preset.id())
				: Component.translatable("item.pswg.lightsaber", preset.owner());
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag)
	{
		tooltip.add(Component.translatable("tooltip.pswg.lightsaber.controls", Component.keybind("key.pswg.blaster_mode")));
	}

	@Override
	public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected)
	{
		if (!level.isClientSide && entity instanceof LivingEntity living && !selected && stack != living.getOffhandItem() && state(stack).active())
			setActive(level, living, stack, false, false);
	}

	public static void toggle(ServerLevel level, Player player, ItemStack stack)
	{
		var state = state(stack);
		if (state.changed() >= 0 && level.getGameTime() - state.changed() < 8 && (state.active() || state.changed() != 0))
			return;
		setActive(level, player, stack, !state.active(), true);
	}

	private static void setActive(Level level, LivingEntity user, ItemStack stack, boolean active, boolean animate)
	{
		var state = state(stack);
		stack.set(Sabers.BLADE.get(), new Sabers.BladeState(state.color(), state.unstable(), active, level.getGameTime() - (animate ? 0 : 8)));
		stack.set(DataComponents.ATTRIBUTE_MODIFIERS, active ? ON : OFF);
		if (!active && user.isUsingItem() && user.getUseItem() == stack)
			user.stopUsingItem();
		level.playSound(null, user.blockPosition(), (active ? Sabers.START : Sabers.STOP).get(), SoundSource.PLAYERS, 1, 1);
	}

	@Override
	public boolean shouldCauseReequipAnimation(ItemStack before, ItemStack after, boolean slotChanged)
	{
		return slotChanged || !after.is(this);
	}
}
