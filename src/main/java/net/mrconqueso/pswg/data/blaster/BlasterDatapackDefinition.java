package net.mrconqueso.pswg.data.blaster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.mrconqueso.pswg.item.custom.BlasterItem;

/**
 * Defines the format for a blaster datapack JSON entry
 *
 * @param stats       The stats the blaster preset will use
 * @param attachments The attachments the blaster preset will use
 */
public record BlasterDatapackDefinition(
        BlasterItem.StatsComponent stats,
        BlasterItem.AvailableAttachmentsComponent attachments
) {
    public static final Codec<BlasterDatapackDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlasterItem.StatsComponent.CODEC.fieldOf("stats").forGetter(BlasterDatapackDefinition::stats),
            BlasterItem.AvailableAttachmentsComponent.CODEC.fieldOf("attachments").forGetter(BlasterDatapackDefinition::attachments)
    ).apply(instance, BlasterDatapackDefinition::new));
}