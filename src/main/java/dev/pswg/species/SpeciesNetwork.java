package dev.pswg.species;

import dev.pswg.Galaxies;
import dev.pswg.species.Species.Appearance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;

public final class SpeciesNetwork
{
	private static final StreamCodec<FriendlyByteBuf, Appearance> APPEARANCE = StreamCodec.of((buffer, appearance) -> {
		buffer.writeUtf(appearance.species(), 32);
		buffer.writeBoolean(appearance.female());
		buffer.writeMap(appearance.options(), (buf, key) -> buf.writeUtf(key, 48), (buf, value) -> buf.writeUtf(value, 48));
	}, buffer -> new Appearance(buffer.readUtf(32), buffer.readBoolean(),
			buffer.readMap(FriendlyByteBuf.limitValue(LinkedHashMap::new, 32), buf -> buf.readUtf(48), buf -> buf.readUtf(48))));
	private static BiConsumer<Integer, Appearance> openEditor = (entity, appearance) -> {};

	private SpeciesNetwork() {}

	public static void registerEditor(BiConsumer<Integer, Appearance> editor) { openEditor = editor; }

	static void register(RegisterPayloadHandlersEvent event)
	{
		var registrar = event.registrar("1");
		registrar.playToServer(Select.TYPE, Select.STREAM, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player)
				select(player, payload.entity(), payload.appearance());
		});
		registrar.playToClient(Update.TYPE, Update.STREAM, (payload, context) -> {
			if (payload.edit())
				openEditor.accept(payload.entity(), payload.appearance());
			else if (context.player().level().getEntity(payload.entity()) instanceof Player player)
				Species.set(player, payload.appearance());
		});
	}

	public static void select(ServerPlayer player, int entity, Appearance appearance)
	{
		if (!player.isAlive() || player.isSpectator())
			return;
		Species.validate(appearance).ifPresent(verified -> {
			if (entity == player.getId())
				Species.set(player, verified);
			else if (player.level().getEntity(entity) instanceof Mannequin mannequin && player.canInteractWithEntity(mannequin, 0)
					&& (player.getMainHandItem().is(SpeciesItems.WIZARD.get()) || player.getOffhandItem().is(SpeciesItems.WIZARD.get())))
				mannequin.appearance(verified);
		});
	}

	static void broadcast(ServerPlayer player)
	{
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new Update(player.getId(), Species.appearance(player), false));
	}

	static void send(ServerPlayer viewer, ServerPlayer player)
	{
		PacketDistributor.sendToPlayer(viewer, new Update(player.getId(), Species.appearance(player), false));
	}

	public static void open(ServerPlayer player, int entity, Appearance appearance)
	{
		if (!player.isSpectator())
			PacketDistributor.sendToPlayer(player, new Update(entity, appearance, true));
	}

	public record Select(int entity, Appearance appearance) implements CustomPacketPayload
	{
		private static final Type<Select> TYPE = new Type<>(Galaxies.id("select_species"));
		private static final StreamCodec<FriendlyByteBuf, Select> STREAM = StreamCodec.of((buf, value) -> {
			buf.writeVarInt(value.entity());
			APPEARANCE.encode(buf, value.appearance());
		}, buf -> new Select(buf.readVarInt(), APPEARANCE.decode(buf)));
		@Override public Type<Select> type() { return TYPE; }
	}

	private record Update(int entity, Appearance appearance, boolean edit) implements CustomPacketPayload
	{
		private static final Type<Update> TYPE = new Type<>(Galaxies.id("species_update"));
		private static final StreamCodec<FriendlyByteBuf, Update> STREAM = StreamCodec.of((buf, value) -> {
			buf.writeVarInt(value.entity());
			APPEARANCE.encode(buf, value.appearance());
			buf.writeBoolean(value.edit());
		}, buf -> new Update(buf.readVarInt(), APPEARANCE.decode(buf), buf.readBoolean()));
		@Override public Type<Update> type() { return TYPE; }
	}
}
