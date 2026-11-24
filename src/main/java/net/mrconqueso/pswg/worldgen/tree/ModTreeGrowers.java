package net.mrconqueso.pswg.worldgen.tree;

import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.worldgen.ModConfiguredFeatures;
import net.minecraft.world.level.block.grower.TreeGrower;

import java.util.Optional;

public class ModTreeGrowers {
    public static final TreeGrower BLOODWOOD = new TreeGrower(PSWG.MOD_ID + ":bloodwood",
            Optional.empty(), Optional.of(ModConfiguredFeatures.BLOODWOOD_KEY), Optional.empty());

}
