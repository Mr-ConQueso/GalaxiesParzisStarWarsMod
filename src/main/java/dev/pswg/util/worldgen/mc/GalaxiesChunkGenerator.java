package dev.pswg.util.worldgen.mc;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pswg.structure.DerelictContainer;
import dev.pswg.util.worldgen.TerrainGenerator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.GenerationStep;

/**
 * Generates Tatooine terrain using PSWG's custom terrain and decoration layers.
 */
public class GalaxiesChunkGenerator extends ChunkGenerator
{
	public static final MapCodec<GalaxiesChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(
			instance -> instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(GalaxiesChunkGenerator::getBiomeSource)).apply(instance, GalaxiesChunkGenerator::new));

	private final TerrainGenerator backing;

	public GalaxiesChunkGenerator(BiomeSource biomeSource)
	{
		super(biomeSource);

		if (!(biomeSource instanceof GalaxiesBiomeSource bs))
		{
			throw new IllegalStateException("Biome source must be galaxies biome source");
		}

		this.backing = new TerrainGenerator(100, bs.getBackingGen(), this, Blocks.STONE.defaultBlockState());
	}

	@Override
	public BiomeSource getBiomeSource()
	{
		return super.getBiomeSource();
	}

	@Override
	protected MapCodec<? extends ChunkGenerator> codec()
	{
		return CODEC;
	}

	@Override
	public void spawnOriginalMobs(WorldGenRegion region)
	{

	}

	@Override
	public int getGenDepth()
	{
		return 384;
	}

	@Override
	public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState,
	                                                    StructureManager structures, ChunkAccess chunk)
	{
		backing.buildNoise(new MinecraftChunkView(chunk));
		return CompletableFuture.completedFuture(chunk);
	}

	@Override
	public void buildSurface(WorldGenRegion world, StructureManager structures, RandomState randomState, ChunkAccess chunk)
	{
		backing.buildSurface(new MinecraftChunkView(chunk));
	}

	@Override
	public void applyCarvers(WorldGenRegion world, long seed, RandomState randomState, BiomeManager biomes,
	                         StructureManager structures, ChunkAccess chunk, GenerationStep.Carving step)
	{
	}

	@Override
	public void applyBiomeDecoration(WorldGenLevel world, ChunkAccess chunk, StructureManager structureAccessor)
	{
		DerelictContainer.placeInTatooine(world, chunk, structureAccessor, this);
		this.backing.generateDecorations(new MinecraftWorldView(world), new MinecraftChunkView(chunk));
	}

	@Override
	public int getSeaLevel()
	{
		return 0;
	}

	@Override
	public int getMinY()
	{
		return -64;
	}

	@Override
	public int getBaseHeight(int x, int z, Heightmap.Types heightmap, LevelHeightAccessor world, RandomState noiseConfig)
	{
		var column = getBaseColumn(x, z, world, noiseConfig);
		int minY = Math.max(world.getMinBuildHeight(), getMinY());
		int maxY = Math.min(world.getMaxBuildHeight(), getMinY() + getGenDepth());
		var opaque = heightmap.isOpaque();
		for (int y = maxY - 1; y >= minY; y--)
			if (opaque.test(column.getBlock(y)))
				return y + 1;
		return world.getMinBuildHeight();
	}

	@Override
	public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor world, RandomState noiseConfig)
	{
		return backing.sampleBaseColumn(x, z);
	}

	@Override
	public void addDebugScreenInfo(List<String> text, RandomState noiseConfig, BlockPos pos)
	{

	}
}
