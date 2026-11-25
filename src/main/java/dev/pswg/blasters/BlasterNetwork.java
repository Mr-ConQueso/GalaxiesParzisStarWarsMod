package dev.pswg.blasters;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.HumanoidArm;
import dev.pswg.blasters.LegacyBlaster.FiringMode;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.Map;
import java.util.Objects;

public final class BlasterNetwork
{
	static void register(RegisterPayloadHandlersEvent event)
	{
		var registrar = event.registrar("2");
		registrar.playToServer(Action.TYPE, Action.STREAM, (payload, context) -> {
			if (context.player() instanceof ServerPlayer player)
				handleAction(player, payload);
		});
		registrar.playToServer(WorkbenchBuild.TYPE, WorkbenchBuild.STREAM, (payload, context) -> {
			if (context.player().containerMenu instanceof BlasterWorkbench.Menu menu && menu.containerId == payload.menuId())
				menu.build(context.player(), payload.original(), payload.attachments());
		});
		registrar.playToClient(Definitions.TYPE, Definitions.STREAM, (payload, context) ->
				BlasterDefinitions.updateClient(payload.definitions()));
		registrar.playToClient(BoltMotion.TYPE, BoltMotion.STREAM, (payload, context) -> {
			if (context.player().level().getEntity(payload.id()) instanceof BlasterBolt bolt)
			{
				bolt.setPos(payload.position());
				bolt.setDeltaMovement(payload.velocity());
			}
		});
		registrar.playToClient(BoltImpact.TYPE, BoltImpact.STREAM, (payload, context) ->
				BlasterEffects.impact(context.player().level(), payload.position(), payload.incident(), payload.block(), payload.face()));
		registrar.playToClient(MuzzleShot.TYPE, MuzzleShot.STREAM, (payload, context) ->
				dev.pswg.blasters.client.BlasterMuzzle.onShot(context.player().level(), payload));
		registrar.playToClient(Patrol.TYPE, Patrol.STREAM, (payload, context) -> {
			if (context.player().level().getEntity(payload.entity()) instanceof Player player)
				player.setData(Blasters.PATROL, payload.active());
		});
		registrar.playToClient(Recoil.TYPE, Recoil.STREAM, (payload, context) -> {
			var player = context.player();
			player.setXRot(Mth.clamp(player.getXRot() + payload.vertical(), -90F, 90F));
			player.setYRot(player.getYRot() + payload.horizontal());
		});
	}

	public record WorkbenchBuild(int menuId, ItemStack original, Blasters.EquippedAttachments attachments) implements CustomPacketPayload
	{
		private static final Type<WorkbenchBuild> TYPE = new Type<>(Blasters.id("workbench_build"));
		private static final StreamCodec<RegistryFriendlyByteBuf, WorkbenchBuild> STREAM = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, WorkbenchBuild::menuId, ItemStack.STREAM_CODEC, WorkbenchBuild::original,
				ByteBufCodecs.fromCodec(Blasters.EquippedAttachments.CODEC), WorkbenchBuild::attachments, WorkbenchBuild::new);

		@Override public Type<WorkbenchBuild> type() { return TYPE; }
	}

	public static void handleAction(ServerPlayer player, Action payload)
	{
		if (!player.isAlive() || player.isSpectator() || player.isSleeping() || player.getVehicle() instanceof dev.pswg.vehicles.Ship || player.containerMenu != player.inventoryMenu
				|| BlasterItem.firingHand(player) != payload.hand())
			return;

		var stack = player.getItemInHand(payload.hand());
		if (!BlasterItem.isBlaster(stack) || !Objects.equals(stack.get(Blasters.SERIAL.get()), payload.serial()))
			return;

		switch (payload.kind())
		{
			case PRESS -> BlasterItem.fire(player.serverLevel(), player, stack, false);
			case REPEAT -> BlasterItem.fire(player.serverLevel(), player, stack, true);
			case VENT -> BlasterItem.vent(player.serverLevel(), player, stack);
			case MODE -> BlasterItem.cycleMode(player.serverLevel(), player, stack);
			case PATROL -> BlasterItem.togglePatrol(player, stack);
		}
	}

	static void syncPatrol(PlayerEvent.StartTracking event)
	{
		if (event.getEntity() instanceof ServerPlayer receiver && event.getTarget() instanceof ServerPlayer player && player.getData(Blasters.PATROL))
			PacketDistributor.sendToPlayer(receiver, new Patrol(player.getId(), true));
	}

	static void patrol(Player player, boolean enabled)
	{
		if (player.getData(Blasters.PATROL) == enabled)
			return;
		player.setData(Blasters.PATROL, enabled);
		if (player instanceof ServerPlayer server && server.connection != null)
			PacketDistributor.sendToPlayersTrackingEntityAndSelf(server, new Patrol(server.getId(), enabled));
	}

	private record Patrol(int entity, boolean active) implements CustomPacketPayload
	{
		private static final Type<Patrol> TYPE = new Type<>(Blasters.id("patrol"));
		private static final StreamCodec<ByteBuf, Patrol> STREAM = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Patrol::entity, ByteBufCodecs.BOOL, Patrol::active, Patrol::new);
		@Override public Type<Patrol> type() { return TYPE; }
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

	static void sendMuzzle(ServerPlayer player, HumanoidArm arm, ResourceLocation model, FiringMode mode, boolean pyrotechnics)
	{
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new MuzzleShot(player.getId(), arm, model, mode, pyrotechnics));
	}

	public record MuzzleShot(int entity, HumanoidArm arm, ResourceLocation model, FiringMode mode, boolean pyrotechnics)
			implements CustomPacketPayload
	{
		private static final Type<MuzzleShot> TYPE = new Type<>(Blasters.id("muzzle_shot"));
		private static final StreamCodec<FriendlyByteBuf, MuzzleShot> STREAM = StreamCodec.of(
				(buffer, shot) -> {
					buffer.writeVarInt(shot.entity());
					buffer.writeEnum(shot.arm());
					buffer.writeResourceLocation(shot.model());
					buffer.writeEnum(shot.mode());
					buffer.writeBoolean(shot.pyrotechnics());
				}, buffer -> new MuzzleShot(buffer.readVarInt(), buffer.readEnum(HumanoidArm.class), buffer.readResourceLocation(),
						buffer.readEnum(FiringMode.class), buffer.readBoolean()));

		@Override public Type<MuzzleShot> type() { return TYPE; }
	}

	static void sendBoltMotion(BlasterBolt bolt, Vec3 position, Vec3 velocity)
	{
		PacketDistributor.sendToPlayersTrackingEntity(bolt, new BoltMotion(bolt.getId(), position, velocity));
	}

	static void sendBoltImpact(ServerLevel level, BlockHitResult hit, Vec3 incident)
	{
		PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(hit.getBlockPos()),
				new BoltImpact(hit.getLocation(), incident, hit.getBlockPos(), hit.getDirection()));
	}

	private record BoltImpact(Vec3 position, Vec3 incident, BlockPos block, Direction face) implements CustomPacketPayload
	{
		private static final Type<BoltImpact> TYPE = new Type<>(Blasters.id("bolt_impact"));
		private static final StreamCodec<FriendlyByteBuf, BoltImpact> STREAM = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeVec3(payload.position());
					buffer.writeVec3(payload.incident());
					buffer.writeBlockPos(payload.block());
					buffer.writeEnum(payload.face());
				}, buffer -> new BoltImpact(buffer.readVec3(), buffer.readVec3(), buffer.readBlockPos(), buffer.readEnum(Direction.class)));

		@Override public Type<BoltImpact> type() { return TYPE; }
	}

	private record BoltMotion(int id, Vec3 position, Vec3 velocity) implements CustomPacketPayload
	{
		private static final Type<BoltMotion> TYPE = new Type<>(Blasters.id("bolt_motion"));
		private static final StreamCodec<FriendlyByteBuf, BoltMotion> STREAM = StreamCodec.of(
				(buffer, payload) -> {
					buffer.writeVarInt(payload.id());
					buffer.writeVec3(payload.position());
					buffer.writeVec3(payload.velocity());
				},
				buffer -> new BoltMotion(buffer.readVarInt(), buffer.readVec3(), buffer.readVec3()));

		@Override
		public Type<BoltMotion> type()
		{
			return TYPE;
		}
	}

	public enum Input
	{
		PRESS, REPEAT, VENT, MODE, PATROL
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
