package dev.pswg.sabers;

import dev.pswg.Galaxies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class SaberNetwork
{
	private SaberNetwork() {}

	static void register(RegisterPayloadHandlersEvent event)
	{
		event.registrar("1").playToServer(Action.TYPE, Action.STREAM, (message, context) -> {
			if (context.player() instanceof ServerPlayer player)
				handle(player, message);
		});
	}

	public static void handle(ServerPlayer player, Action action)
	{
		var stack = player.getItemInHand(action.hand());
		if (!player.isAlive() || player.isSpectator() || player.isSleeping() || player.containerMenu != player.inventoryMenu
				|| !(stack.getItem() instanceof SaberItem))
			return;
		if (action.throwBlade())
			SaberItem.throwBlade(player.serverLevel(), player, action.hand());
		else
			SaberItem.toggle(player.serverLevel(), player, stack);
	}

	public record Action(InteractionHand hand, boolean throwBlade) implements CustomPacketPayload
	{
		public static final Type<Action> TYPE = new Type<>(Galaxies.id("saber_action"));
		private static final StreamCodec<FriendlyByteBuf, Action> STREAM = StreamCodec.of(
				(buf, action) -> { buf.writeEnum(action.hand()); buf.writeBoolean(action.throwBlade()); },
				buf -> new Action(buf.readEnum(InteractionHand.class), buf.readBoolean()));

		@Override
		public Type<Action> type() { return TYPE; }
	}
}
