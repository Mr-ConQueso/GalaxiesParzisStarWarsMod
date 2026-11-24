package dev.pswg.blasters;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Map;
import java.util.Objects;

public final class BlasterNetwork
{
	static void register(RegisterPayloadHandlersEvent event)
	{
		var registrar = event.registrar("1");
		registrar.playToServer(Action.TYPE, Action.STREAM, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player)
				handleAction(player, payload);
		});
		registrar.playToClient(Definitions.TYPE, Definitions.STREAM, (payload, context) ->
				BlasterDefinitions.updateClient(payload.definitions()));
		registrar.playToClient(Recoil.TYPE, Recoil.STREAM, (payload, context) -> {
			var player = context.player();
			player.setXRot(Mth.clamp(player.getXRot() + payload.vertical(), -90F, 90F));
			player.setYRot(player.getYRot() + payload.horizontal());
		});
	}

	public static void handleAction(ServerPlayer player, Action payload)
	{
		if (!player.isAlive() || player.isSpectator() || player.isSleeping() || player.containerMenu != player.inventoryMenu
				|| BlasterItem.firingHand(player) != payload.hand())
			return;

		var stack = player.getItemInHand(payload.hand());
		if (!stack.is(Blasters.ITEM.get()) || !Objects.equals(stack.get(Blasters.SERIAL.get()), payload.serial()))
			return;

		switch (payload.kind())
		{
			case PRESS -> BlasterItem.fire(player.serverLevel(), player, stack, false);
			case REPEAT -> BlasterItem.fire(player.serverLevel(), player, stack, true);
			case VENT -> BlasterItem.vent(player.serverLevel(), player, stack);
		}
	}

	static void syncDefinitions(OnDatapackSyncEvent event)
	{
		var payload = new Definitions(BlasterDefinitions.SERVER.definitions());
		event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
	}

	static void sendRecoil(ServerPlayer player, float vertical, float horizontal)
	{
		if (player.connection != null)
			PacketDistributor.sendToPlayer(player, new Recoil(vertical, horizontal));
	}

	public enum Input
	{
		PRESS, REPEAT, VENT
	}

	public record Action(InteractionHand hand, Input kind, long serial) implements CustomPacketPayload
	{
		private static final Type<Action> TYPE = new Type<>(Blasters.id("action"));
		private static final StreamCodec<FriendlyByteBuf, Action> STREAM = StreamCodec.of(
				(buffer, action) -> {
					buffer.writeEnum(action.hand());
					buffer.writeEnum(action.kind());
					buffer.writeLong(action.serial());
				},
				buffer -> new Action(buffer.readEnum(InteractionHand.class), buffer.readEnum(Input.class), buffer.readLong()));

		@Override
		public Type<Action> type()
		{
			return TYPE;
		}
	}

	private record Definitions(Map<ResourceLocation, BlasterDefinition> definitions) implements CustomPacketPayload
	{
		private static final Type<Definitions> TYPE = new Type<>(Blasters.id("definitions"));
		private static final StreamCodec<ByteBuf, Definitions> STREAM = ByteBufCodecs.fromCodec(
				Codec.unboundedMap(ResourceLocation.CODEC, BlasterDefinition.CODEC)).map(Definitions::new, Definitions::definitions);

		@Override
		public Type<Definitions> type()
		{
			return TYPE;
		}
	}

	private record Recoil(float vertical, float horizontal) implements CustomPacketPayload
	{
		private static final Type<Recoil> TYPE = new Type<>(Blasters.id("recoil"));
		private static final StreamCodec<ByteBuf, Recoil> STREAM = StreamCodec.composite(
				ByteBufCodecs.FLOAT, Recoil::vertical, ByteBufCodecs.FLOAT, Recoil::horizontal, Recoil::new);

		@Override
		public Type<Recoil> type()
		{
			return TYPE;
		}
	}

	private BlasterNetwork()
	{
	}
}
