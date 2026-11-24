package dev.pswg.gadgets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record GadgetInput(InteractionHand hand, ResourceLocation item) implements CustomPacketPayload
{
	private static final Type<GadgetInput> TYPE = new Type<>(Demolitions.id("prime"));
	private static final StreamCodec<FriendlyByteBuf, GadgetInput> STREAM = StreamCodec.of(
			(buffer, action) -> {
				buffer.writeEnum(action.hand());
				buffer.writeResourceLocation(action.item());
			},
			buffer -> new GadgetInput(buffer.readEnum(InteractionHand.class), buffer.readResourceLocation()));

	static void register(RegisterPayloadHandlersEvent event)
	{
		event.registrar("1").playToServer(TYPE, STREAM, (payload, context) -> {
			if (!(context.player() instanceof ServerPlayer player) || player.isSpectator() || !player.isAlive()
					|| player.isSleeping() || player.containerMenu != player.inventoryMenu || payload.hand() != primingHand(player))
				return;
			var stack = player.getItemInHand(payload.hand());
			if (stack.getItem() instanceof GrenadeItem grenade
					&& net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(grenade).equals(payload.item()))
				grenade.togglePrimed(player.serverLevel(), player, stack);
		});
	}

	public static InteractionHand primingHand(Player player)
	{
		return player.getMainHandItem().isEmpty() && player.getOffhandItem().getItem() instanceof GrenadeItem
				? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
	}

	@Override
	public Type<GadgetInput> type()
	{
		return TYPE;
	}
}
