package net.mrconqueso.pswg.networking;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.mrconqueso.pswg.interaction.ClientPlayerAction;
import net.mrconqueso.pswg.interaction.ServerPlayerAction;
import org.joml.Vector2f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Defines packet codecs and related utilities for common data types
 */
public final class GalaxiesPacketCodecs
{
    /**
     * A packet codec that can serialize and deserialize {@link InteractionHand} enum values
     */
    public static final StreamCodec<ByteBuf, InteractionHand> HAND = forEnum(InteractionHand.class);

    /**
     * A packet codec that can serialize and deserialize {@link ResourceLocation} lists
     */
    public static final StreamCodec<ByteBuf, List<ResourceLocation>> IDENTIFIER_LIST = ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list());

    /**
     * A packet codec that can serialize and deserialize maps between {@link ResourceLocation}s
     */
    public static final StreamCodec<ByteBuf, Map<ResourceLocation, ResourceLocation>> IDENTIFIER_MAP = ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ResourceLocation.STREAM_CODEC);

    /**
     * A packet codec that can serialize and deserialize {@link ClientPlayerAction} enum values
     */
    public static final StreamCodec<ByteBuf, ClientPlayerAction> CLIENT_PLAYER_ACTION = forEnum(ClientPlayerAction.class);

    /**
     * A packet codec that can serialize and deserialize {@link ServerPlayerAction} enum values
     */
    public static final StreamCodec<ByteBuf, ServerPlayerAction> SERVER_PLAYER_ACTION = forEnum(ServerPlayerAction.class);

    /**
     * A packet codec that can serialize and deserialize {@link Vector2f}s
     */
    public static final StreamCodec<ByteBuf, Vector2f> VECTOR_2F = new StreamCodec<>()
    {
        @Override
        public Vector2f decode(ByteBuf buf)
        {
            return new Vector2f(buf.readFloat(), buf.readFloat());
        }

        @Override
        public void encode(ByteBuf buf, Vector2f vector)
        {
            buf.writeFloat(vector.x());
            buf.writeFloat(vector.y());
        }
    };

    /**
     * Creates a packet codec for the given enum type.
     * In 1.21.1, we can utilize the built-in idMapper from ByteBufCodecs which handles VarInts efficiently.
     *
     * @param <T>   The type of the enum
     * @param clazz The class of the enum
     *
     * @return A {@link StreamCodec} that can serialize and deserialize the enum type
     */
    public static <T extends Enum<T>> StreamCodec<ByteBuf, T> forEnum(Class<T> clazz)
    {
        return ByteBufCodecs.idMapper(id -> clazz.getEnumConstants()[id], Enum::ordinal);
    }

    /**
     * Creates a packet codec that compresses and decompresses the given codec using GZip
     *
     * @param codec The codec to wrap
     *
     * @return A codec that compresses and decompresses the given codec
     */
    public static <T> StreamCodec<ByteBuf, T> gzip(StreamCodec<ByteBuf, T> codec)
    {
        return new StreamCodec<>()
        {
            @Override
            public T decode(ByteBuf buf)
            {
                var payloadSize = buf.readInt();
                try (var stream = new ByteBufInputStream(buf); var gz = new GZIPInputStream(stream))
                {
                    var unzippedBuf = Unpooled.wrappedBuffer(gz.readNBytes(payloadSize));
                    return codec.decode(unzippedBuf);
                }
                catch (Exception e)
                {
                    throw new DecoderException("Failed to decompress value", e);
                }
            }

            @Override
            public void encode(ByteBuf buf, T value)
            {
                var payloadBuf = Unpooled.buffer();
                codec.encode(payloadBuf, value);

                var payloadSize = payloadBuf.writerIndex();
                buf.writeInt(payloadSize);

                try (var stream = new ByteBufOutputStream(buf); var gz = new GZIPOutputStream(stream))
                {
                    var bytes = new byte[payloadSize];
                    payloadBuf.readBytes(bytes);
                    gz.write(bytes);
                }
                catch (Exception e)
                {
                    throw new EncoderException("Failed to compress value", e);
                }
            }
        };
    }
}