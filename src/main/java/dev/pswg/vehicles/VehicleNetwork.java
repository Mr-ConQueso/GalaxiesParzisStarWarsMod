package dev.pswg.vehicles;

import dev.pswg.Galaxies;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class VehicleNetwork
{
	public static final int FORWARD = 1;
	public static final int BACK = 2;
	public static final int FIRE = 4;
	public static final int RIGHT = 8;
	public static final int LEFT = 16;
	public static final int ROLL = 32;
	static final int MASK = 63;

	private VehicleNetwork() {}

	static void register(RegisterPayloadHandlersEvent event)
	{
		event.registrar("1").playToServer(Control.TYPE, Control.STREAM, (control, context) -> {
			if (context.player() instanceof ServerPlayer player)
				handle(player, control);
		});
	}

	public static void handle(ServerPlayer player, Control control)
	{
		if (player.isAlive() && !player.isSpectator() && player.getVehicle() instanceof Ship ship
				&& ship.getId() == control.entity() && ship.pilot() == player)
			ship.input(player, control.bits(), control.yaw(), control.pitch());
	}

	public record Control(int entity, int bits, float yaw, float pitch) implements CustomPacketPayload
	{
		private static final Type<Control> TYPE = new Type<>(Galaxies.id("ship_control"));
		private static final StreamCodec<FriendlyByteBuf, Control> STREAM = StreamCodec.of(
				(buffer, input) -> {
					buffer.writeVarInt(input.entity());
					buffer.writeByte(input.bits());
					buffer.writeFloat(input.yaw());
					buffer.writeFloat(input.pitch());
				},
				buffer -> new Control(buffer.readVarInt(), buffer.readUnsignedByte(), buffer.readFloat(), buffer.readFloat()));

		@Override
		public Type<Control> type() { return TYPE; }
	}
}
