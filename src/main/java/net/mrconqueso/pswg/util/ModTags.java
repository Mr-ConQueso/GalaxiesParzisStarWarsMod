package net.mrconqueso.pswg.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.mrconqueso.pswg.PSWG;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> NEEDS_BISMUTH_TOOL = createTag("needs_bismuth_tool");
        public static final TagKey<Block> INCORRECT_FOR_BISMUTH_TOOL = createTag("incorrect_for_bismuth_tool");

        private static TagKey<Block> createTag(String name) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath(PSWG.MOD_ID, name));
        }
    }

    public static class Items {
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items");

        public static final TagKey<Item> BLASTERS = createTag("blasters");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(PSWG.MOD_ID, name));
        }
    }

    public static class Damage {
        public static final TagKey<DamageType> IGNORES_INVULNERABLE_FRAMES = createTag("ignores_invulnerable_frames");
        public static final TagKey<DamageType> IGNORES_DAMAGE_TILT = createTag("ignores_damage_tilt");

        // 1. Changed TagKey<Item> to TagKey<DamageType>
        private static TagKey<DamageType> createTag(String name) {
            // 2. Used TagKey.create() with the DAMAGE_TYPE registry
            return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(PSWG.MOD_ID, name));
        }
    }
}
