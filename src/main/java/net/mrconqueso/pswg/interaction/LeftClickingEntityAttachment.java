package net.mrconqueso.pswg.interaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.mrconqueso.pswg.PSWG;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * An attachment for entities that contains the data required to
 * represent their left-using state
 */
public record LeftClickingEntityAttachment(
        boolean isUsingItemLeft,
        int itemLeftUseTimeLeft,
        ItemStack leftActiveItemStack
)
{
    // 1. Manually defined Codec
    public static final Codec<LeftClickingEntityAttachment> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("isUsingItemLeft").forGetter(LeftClickingEntityAttachment::isUsingItemLeft),
            Codec.INT.fieldOf("itemLeftUseTimeLeft").forGetter(LeftClickingEntityAttachment::itemLeftUseTimeLeft),
            ItemStack.OPTIONAL_CODEC.fieldOf("leftActiveItemStack").forGetter(LeftClickingEntityAttachment::leftActiveItemStack)
    ).apply(instance, LeftClickingEntityAttachment::new));

    // 2. Manually defined Packet/Stream Codec
    public static final StreamCodec<RegistryFriendlyByteBuf, LeftClickingEntityAttachment> PACKET_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, LeftClickingEntityAttachment::isUsingItemLeft,
            ByteBufCodecs.INT, LeftClickingEntityAttachment::itemLeftUseTimeLeft,
            ItemStack.OPTIONAL_STREAM_CODEC, LeftClickingEntityAttachment::leftActiveItemStack,
            LeftClickingEntityAttachment::new
    );

    // 3. Mutable Record Helper Methods (Replacing @MutableRecord)
    public LeftClickingEntityAttachment withIsUsingItemLeft(boolean isUsingItemLeft) {
        return new LeftClickingEntityAttachment(isUsingItemLeft, this.itemLeftUseTimeLeft, this.leftActiveItemStack);
    }

    public LeftClickingEntityAttachment withItemLeftUseTimeLeft(int itemLeftUseTimeLeft) {
        return new LeftClickingEntityAttachment(this.isUsingItemLeft, itemLeftUseTimeLeft, this.leftActiveItemStack);
    }

    public LeftClickingEntityAttachment withLeftActiveItemStack(ItemStack leftActiveItemStack) {
        return new LeftClickingEntityAttachment(this.isUsingItemLeft, this.itemLeftUseTimeLeft, leftActiveItemStack);
    }

    // 4. NeoForge Attachment Registration
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PSWG.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LeftClickingEntityAttachment>> ATTACHMENT = ATTACHMENT_TYPES.register(
            "left_clicking_entity",
            () -> AttachmentType.builder(() -> new LeftClickingEntityAttachment(false, 0, ItemStack.EMPTY))
                    .serialize(CODEC)
                    .build()
    );

    /**
     * Gets the current attachment data for the entity.
     */
    public static LeftClickingEntityAttachment get(LivingEntity entity)
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