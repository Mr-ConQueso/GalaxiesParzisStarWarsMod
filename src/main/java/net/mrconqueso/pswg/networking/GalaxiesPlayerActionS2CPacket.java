package net.mrconqueso.pswg.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.interaction.GalaxiesPlayerActionManager;
import net.mrconqueso.pswg.interaction.ServerPlayerAction;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Encapsulates a fire-and-forget S2C player action notification
 *
 * @param action The action to invoke
 */
public record GalaxiesPlayerActionS2CPacket(ServerPlayerAction action) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<GalaxiesPlayerActionS2CPacket> ID = new CustomPacketPayload.Type<>(PSWG.id("server_player_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GalaxiesPlayerActionS2CPacket> CODEC = StreamCodec.composite(
            GalaxiesPacketCodecs.SERVER_PLAYER_ACTION,
            GalaxiesPlayerActionS2CPacket::action,
            GalaxiesPlayerActionS2CPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }

    public void handle(IPayloadContext context)
    {
        // Enqueue work to the main client thread
        context.enqueueWork(() -> {
            // In an S2C packet, context.player() automatically returns the LocalPlayer
            var player = context.player();
            if (player == null) return;

            // Handle the specific action sent by the server
            switch (this.action()) {
                case CONSUME_LEFT_ITEM -> {
                    // TODO: Call your client-side consume item logic here!
                    // Example: GalaxiesClientLeftClickManager.consumeItemCore(player);
                }
                // Add any other ServerPlayerAction cases you have here...
                default -> {
                    PSWG.LOGGER.warn("Received unknown or unhandled server player action: {}", this.action());
                }
            }
        });
    }
}