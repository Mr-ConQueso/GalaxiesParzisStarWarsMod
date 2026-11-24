package net.mrconqueso.pswg.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.networking.GalaxiesCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * An attachment for entities that contains the data required to
 * represent recoil
 *
 * @param recoilVelocity The recoil rate in degrees per tick
 */
public record RecoilEntityAttachment(Vector3f recoilVelocity)
{
    public RecoilEntityAttachment(Vector3fc recoilVelocity)
    {
        this(new Vector3f(recoilVelocity));
    }

    // 1. Manually defined Codec
    public static final Codec<RecoilEntityAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            GalaxiesCodecs.NAMED_VECTOR_3F.fieldOf("recoilVelocity").forGetter(RecoilEntityAttachment::recoilVelocity)
    ).apply(instance, RecoilEntityAttachment::new));

    // 2. Manually defined Stream/Packet Codec
    public static final StreamCodec<ByteBuf, RecoilEntityAttachment> PACKET_CODEC = new StreamCodec<>() {
        @Override
        public RecoilEntityAttachment decode(ByteBuf buf) {
            return new RecoilEntityAttachment(new Vector3f(buf.readFloat(), buf.readFloat(), buf.readFloat()));
        }

        @Override
        public void encode(ByteBuf buf, RecoilEntityAttachment value) {
            buf.writeFloat(value.recoilVelocity().x());
            buf.writeFloat(value.recoilVelocity().y());
            buf.writeFloat(value.recoilVelocity().z());
        }
    };

    // 3. Mutable Record Helper Method (Replacing @MutableRecord)
    public RecoilEntityAttachment withRecoilVelocity(Vector3f recoilVelocity) {
        return new RecoilEntityAttachment(recoilVelocity);
    }

    // 4. NeoForge Attachment Registration
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PSWG.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<RecoilEntityAttachment>> ATTACHMENT = ATTACHMENT_TYPES.register(
            "recoil_entity",
            () -> AttachmentType.builder(() -> new RecoilEntityAttachment(new Vector3f()))
                    .serialize(CODEC)
                    .build()
    );

    /**
     * Gets the current attachment data for the entity.
     */
    public static RecoilEntityAttachment get(LivingEntity entity)
    {
        return entity.getData(ATTACHMENT);
    }

    /**
     * Sets the attachment data for the entity.
     */
    public void set(LivingEntity entity)
    {
        entity.setData(ATTACHMENT, this);
    }
}