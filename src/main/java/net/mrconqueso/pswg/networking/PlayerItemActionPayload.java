package net.mrconqueso.pswg.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.util.item.ItemAction;

public record PlayerItemActionPayload(ItemAction action) implements CustomPacketPayload {

    // 1. Define the unique ID for this packet (Replaces your SwgPackets string)
    public static final CustomPacketPayload.Type<PlayerItemActionPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("pswg", "player_item_action"));

    // 2. Define how to read/write the data (Replaces your buf.writeInt / buf.readInt)
    public static final StreamCodec<FriendlyByteBuf, PlayerItemActionPayload> STREAM_CODEC = StreamCodec.composite(
            // Map the integer ordinal back and forth to the Enum
            ByteBufCodecs.INT.map(
                    val -> ItemAction.values()[val], // Decoder
                    ItemAction::ordinal              // Encoder
            ),
            PlayerItemActionPayload::action,
            PlayerItemActionPayload::new
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}