package net.mrconqueso.pswg.networking;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

public class SwgPackets {
    // Ship Controls
    public record ShipControlsPayload(short controls) implements CustomPacketPayload {
        public static final Type<ShipControlsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("pswg", "ship_controls"));
        public static final StreamCodec<ByteBuf, ShipControlsPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.SHORT, ShipControlsPayload::controls,
                ShipControlsPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // Ship Rotation
    public record ShipRotationPayload(Quaternionf rotation) implements CustomPacketPayload {
        public static final Type<ShipRotationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("pswg", "ship_rotation"));
        public static final StreamCodec<ByteBuf, ShipRotationPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.QUATERNION, ShipRotationPayload::rotation,
                ShipRotationPayload::new
        );
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // Ship Fire
    public record ShipFirePayload() implements CustomPacketPayload {
        public static final Type<ShipFirePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("pswg", "ship_fire"));
        public static final StreamCodec<ByteBuf, ShipFirePayload> STREAM_CODEC = StreamCodec.unit(new ShipFirePayload());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}