package dev.pswg.datagen;

import dev.pswg.Galaxies;
import dev.pswg.block.VerticalSlabBlock;
import dev.pswg.block.plant.AridPlant;
import dev.pswg.block.plant.HkakBushBlock;
import dev.pswg.container.GalaxiesBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;

public final class TerrainData
{
	public static void gather(GatherDataEvent event)
	{
		var generator = event.getGenerator();
		var output = generator.getPackOutput();
		var lookup = event.getLookupProvider();
		var files = event.getExistingFileHelper();

		generator.addProvider(event.includeServer(), new LootTableProvider(output, Set.of(),
				List.of(new LootTableProvider.SubProviderEntry(TerrainLoot::new, LootContextParamSets.BLOCK)), lookup));
		generator.addProvider(event.includeServer(), new BlockTagsProvider(output, lookup, Galaxies.MODID, files)
		{
			@Override
			protected void addTags(HolderLookup.Provider registries)
			{
				for (var entry : GalaxiesBlocks.entries())
				{
					var block = entry.get();
					if (block instanceof VerticalSlabBlock)
						tag(BlockTags.SLABS).add(block);
					if (block instanceof StairBlock)
						tag(BlockTags.STAIRS).add(block);
					if (block instanceof WallBlock)
						tag(BlockTags.WALLS).add(block);
					if (block.defaultBlockState().requiresCorrectToolForDrops())
						tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
				}

				tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GalaxiesBlocks.CANYON_BRICKS.get(), GalaxiesBlocks.CHISELED_CANYON.get(),
						GalaxiesBlocks.POLISHED_CANYON.get(), GalaxiesBlocks.SMOOTH_DESERT_SANDSTONE.get(),
						GalaxiesBlocks.POLISHED_DESERT_SANDSTONE.get(), GalaxiesBlocks.CHISELED_DESERT.get(),
						GalaxiesBlocks.DESERT_LOAM.get(), GalaxiesBlocks.TALL_LAMP.get());

				tag(GalaxiesBlocks.ARID_PLANT_PLACEABLE).add(GalaxiesBlocks.CAKED_SALT.get(), GalaxiesBlocks.DESERT_LOAM.get(),
						Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, Blocks.COARSE_DIRT)
						.addOptionalTag(BlockTags.SAND.location()).addOptionalTag(BlockTags.TERRACOTTA.location());
				tag(GalaxiesBlocks.BUSH_PLACEABLE).add(GalaxiesBlocks.CAKED_SALT.get(), GalaxiesBlocks.DESERT_LOAM.get(),
						Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, Blocks.COARSE_DIRT)
						.addOptionalTag(BlockTags.SAND.location());
			}
		});

		generator.addProvider(event.includeClient(), new ItemModelProvider(output, Galaxies.MODID, files)
		{
			@Override
			protected void registerModels()
			{
				for (var entry : GalaxiesBlocks.entries())
				{
					if (entry.get() instanceof AridPlant || entry.get() instanceof HkakBushBlock)
						continue;
					String path = entry.getId().getPath();
					String parent = entry.get() instanceof WallBlock ? path + "_inventory" : path;
					withExistingParent(path, modLoc("block/" + parent));
				}
			}
		});
	}

	private static final class TerrainLoot extends BlockLootSubProvider
	{
		private TerrainLoot(HolderLookup.Provider registries)
		{
			super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
		}

		@Override
		protected Iterable<Block> getKnownBlocks()
		{
			return GalaxiesBlocks.entries().stream().<Block>map(entry -> entry.get()).toList();
		}

		@Override
		protected void generate()
		{
			for (Block block : getKnownBlocks())
			{
				if (block instanceof VerticalSlabBlock)
					add(block, this::createSlabItemTable);
				else
					dropSelf(block);
			}
		}
	}

	private TerrainData()
	{
	}
}
