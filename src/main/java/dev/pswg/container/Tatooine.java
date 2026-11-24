package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.util.worldgen.mc.GalaxiesBiomeSource;
import dev.pswg.util.worldgen.mc.GalaxiesChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class Tatooine
{
	public static final ResourceKey<Level> LEVEL = ResourceKey.create(Registries.DIMENSION, Galaxies.id("tatooine"));

	public static void register(RegisterEvent event)
	{
		event.register(Registries.BIOME_SOURCE, Galaxies.id("galaxies"), () -> GalaxiesBiomeSource.CODEC);
		event.register(Registries.CHUNK_GENERATOR, Galaxies.id("galaxies"), () -> GalaxiesChunkGenerator.CODEC);
	}

	private Tatooine()
	{
	}
}
