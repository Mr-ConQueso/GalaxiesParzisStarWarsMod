package dev.pswg.jetpacks;

import dev.pswg.Galaxies;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class JetpackNetwork
{
	public static final int ASCEND = 1;
	public static final int DESCEND = 2;
	public static final int CRUISE = 4;
	public static final int FORWARD = 8;
	public static final int BACK = 16;
	public static final int LEFT = 32;
	public static final int RIGHT = 64;
	public static final int OPEN = 128;

	private JetpackNetwork() {}

	static void register(RegisterPayloadHandlersEvent event)
	{
		var registrar = event.registrar("1");
		registrar.playToServer(Input.TYPE, Input.STREAM, (input, context) -> {
			if (context.player() instanceof ServerPlayer player)
				handle(player, input);
		});
		registrar.playToClient(Update.TYPE, Update.STREAM, (payload, context) -> {
			if (context.player().level().getEntity(payload.entity()) instanceof Player player)
			{
				player.setData(Jetpacks.SLOT, payload.pack());
				player.getData(Jetpacks.FLIGHT).active = payload.active();
			}
		});
	}

	public static void handle(ServerPlayer player, Input input)
	{
		if (input.keys() == OPEN)
			Jetpacks.open(player);
		else if (Jetpacks.canThrust(player))
		{
			var flight = player.getData(Jetpacks.FLIGHT);
			flight.lastInput = player.level().getGameTime();
			flight.keys = input.keys() & 127;
		}
	}

	static void broadcast(ServerPlayer player)
	{
		if (player.connection != null)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, create(player));
	}

	static void send(ServerPlayer viewer, ServerPlayer player)
	{
		PacketDistributor.sendToPlayer(viewer, create(player));
	}

	private static Update create(ServerPlayer player)
	{
		return new Update(player.getId(), Jetpacks.equipped(player).copy(), Jetpacks.active(player));
	}

	public record Input(int keys) implements CustomPacketPayload
	{
		private static final Type<Input> TYPE = new Type<>(Galaxies.id("jetpack_input"));
		private static final StreamCodec<io.netty.buffer.ByteBuf, Input> STREAM = ByteBufCodecs.VAR_INT.map(Input::new, Input::keys);
		@Override public Type<Input> type() { return TYPE; }
	}

	private record Update(int entity, ItemStack pack, boolean active) implements CustomPacketPayload
	{
		private static final Type<Update> TYPE = new Type<>(Galaxies.id("jetpack_slot"));
		private static final StreamCodec<RegistryFriendlyByteBuf, Update> STREAM = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Update::entity, ItemStack.OPTIONAL_STREAM_CODEC, Update::pack,
				ByteBufCodecs.BOOL, Update::active, Update::new);
		@Override public Type<Update> type() { return TYPE; }
	}
}
