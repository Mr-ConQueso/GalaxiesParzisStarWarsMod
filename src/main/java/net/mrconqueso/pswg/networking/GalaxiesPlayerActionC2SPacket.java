package net.mrconqueso.pswg.networking;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.interaction.ClientPlayerAction;
import net.mrconqueso.pswg.interaction.GalaxiesPlayerActionManager;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Encapsulates a fire-and-forget C2S player action notification
 *
 * @param action The action to invoke
 */
public record GalaxiesPlayerActionC2SPacket(ClientPlayerAction action) implements CustomPacketPayload
{
    public static final CustomPacketPayload.Type<GalaxiesPlayerActionC2SPacket> ID = new CustomPacketPayload.Type<>(PSWG.id("client_player_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GalaxiesPlayerActionC2SPacket> CODEC = StreamCodec.composite(
            GalaxiesPacketCodecs.CLIENT_PLAYER_ACTION,
            GalaxiesPlayerActionC2SPacket::action,
            GalaxiesPlayerActionC2SPacket::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type()
    {
        return ID;
    }

    public void handle(IPayloadContext context)
    {
        context.enqueueWork(() -> {
            // Get the server player, then pass it to the Manager!
            if (context.player() instanceof ServerPlayer serverPlayer) {
                GalaxiesPlayerActionManager.handlePlayerAction(this, serverPlayer);
            }
        });
    }
}