package dev.pswg.util.worldgen.mc;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pswg.util.worldgen.BiomeGenerator;
import dev.pswg.util.worldgen.biome.BiomeList;
import dev.pswg.util.worldgen.biome.TerrainBiomes;
import java.util.stream.Stream;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;

/**
 * Supplies Tatooine biomes from PSWG's layered biome sampler.
 */
public class GalaxiesBiomeSource extends BiomeSource
{
	public static final MapCodec<GalaxiesBiomeSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			RegistryOps.retrieveGetter(Registries.BIOME)
	).apply(instance, GalaxiesBiomeSource::new));

	private final BiomeGenerator backingGen = new BiomeGenerator(10000);
	private final HolderGetter<Biome> biomes;

	protected GalaxiesBiomeSource(HolderGetter<Biome> biomes)
	{
		super();

		TerrainBiomes.init();
		this.biomes = biomes;
	}

	@Override
	protected MapCodec<? extends BiomeSource> codec()
	{
		return CODEC;
	}

	@Override
	protected Stream<Holder<Biome>> collectPossibleBiomes()
	{
		return BiomeList.stream().map(biome -> this.biomes.getOrThrow(biome.backing()));
	}

	/**
	 * Samples Tatooine's layered biome source in quart coordinates.
	 *
	 * @param sampler the cached climate sampler supplied by Minecraft
	 * @return the backing biome used by Minecraft
	 */
	@Override
	public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler)
	{
		return this.biomes.getOrThrow(this.backingGen.getBiome(x, z).backing());
	}

	public BiomeGenerator getBackingGen()
	{
		return backingGen;
	}
}
