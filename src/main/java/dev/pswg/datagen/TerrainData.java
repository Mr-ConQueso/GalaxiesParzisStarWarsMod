package dev.pswg.datagen;

import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterWorkbench;
import dev.pswg.sabers.Sabers;
import dev.pswg.block.AccumulatingBlock;
import dev.pswg.block.VerticalSlabBlock;
import dev.pswg.block.plant.AridPlant;
import dev.pswg.block.plant.ChasukaCrop;
import dev.pswg.block.plant.HkakBushBlock;
import dev.pswg.block.plant.MoloShrubBlock;
import dev.pswg.block.plant.VaporatorMushroomBlock;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.container.LegacyConstruction;
import dev.pswg.utilities.LegacyUtilities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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
					if (block instanceof FenceBlock)
						tag(BlockTags.WOODEN_FENCES).add(block);
					if (block instanceof FenceGateBlock)
						tag(BlockTags.FENCE_GATES).add(block);
					if (block instanceof TrapDoorBlock)
						tag(BlockTags.WOODEN_TRAPDOORS).add(block);
					if (block instanceof DoorBlock)
						tag(BlockTags.WOODEN_DOORS).add(block);
					if (block.defaultBlockState().requiresCorrectToolForDrops())
						tag(BlockTags.MINEABLE_WITH_PICKAXE).add(block);
				}

				tag(BlockTags.MINEABLE_WITH_PICKAXE).add(GalaxiesBlocks.CANYON_BRICKS.get(), GalaxiesBlocks.CHISELED_CANYON.get(),
						GalaxiesBlocks.POLISHED_CANYON.get(), GalaxiesBlocks.SMOOTH_DESERT_SANDSTONE.get(),
						GalaxiesBlocks.POLISHED_DESERT_SANDSTONE.get(), GalaxiesBlocks.CHISELED_DESERT.get(),
						GalaxiesBlocks.DESERT_LOAM.get(), GalaxiesBlocks.TALL_LAMP.get());

				var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
				for (var entry : LegacyConstruction.entries())
				{
					var block = entry.get();
					pickaxe.add(block);
					if (block instanceof VerticalSlabBlock)
						tag(BlockTags.SLABS).add(block);
					if (block instanceof StairBlock || block == LegacyConstruction.SCAFFOLD_STAIRS.get())
						tag(BlockTags.STAIRS).add(block);
					if (block instanceof TrapDoorBlock)
						tag(BlockTags.TRAPDOORS).add(block);
				}
				tag(BlockTags.CLIMBABLE).add(LegacyConstruction.SCAFFOLD.get());
				pickaxe.add(BlasterWorkbench.BLOCK.get(), Sabers.FORGE.get(), LegacyUtilities.VAPORATOR.get(), LegacyUtilities.COUPLING.get(), LegacyUtilities.SLIDING_DOOR.get(),
						LegacyUtilities.CAGE.get(), LegacyUtilities.TERRARIUM.get());
				LegacyUtilities.DYED_TERRARIA.forEach(terrarium -> pickaxe.add(terrarium.get()));

				tag(GalaxiesBlocks.ARID_PLANT_PLACEABLE).add(GalaxiesBlocks.CAKED_SALT.get(), GalaxiesBlocks.DESERT_LOAM.get(),
						Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, Blocks.COARSE_DIRT)
						.addOptionalTag(BlockTags.SAND.location()).addOptionalTag(BlockTags.TERRACOTTA.location());
				tag(GalaxiesBlocks.BUSH_PLACEABLE).add(GalaxiesBlocks.CAKED_SALT.get(), GalaxiesBlocks.DESERT_LOAM.get(),
						Blocks.GRASS_BLOCK, Blocks.DIRT, Blocks.PODZOL, Blocks.COARSE_DIRT)
						.addOptionalTag(BlockTags.SAND.location());
				tag(BlockTags.LOGS_THAT_BURN).add(GalaxiesBlocks.JAPOR_LOG.get(), GalaxiesBlocks.JAPOR_WOOD.get(),
						GalaxiesBlocks.SEQUOIA_LOG.get(), GalaxiesBlocks.STRIPPED_SEQUOIA_LOG.get(),
						GalaxiesBlocks.MOSSY_SEQUOIA_LOG.get(), GalaxiesBlocks.SEQUOIA_WOOD.get(),
						GalaxiesBlocks.TATOOINE_LOG.get(), GalaxiesBlocks.TATOOINE_WOOD.get());
				tag(BlockTags.LEAVES).add(GalaxiesBlocks.JAPOR_LEAVES.get(), GalaxiesBlocks.SEQUOIA_LEAVES.get());
				tag(BlockTags.CROPS).add(GalaxiesBlocks.CHASUKA.get());
				tag(BlockTags.PLANKS).add(GalaxiesBlocks.JAPOR.planks().get(), GalaxiesBlocks.SEQUOIA.planks().get());
				tag(BlockTags.WOODEN_SLABS).add(GalaxiesBlocks.JAPOR.slab().get(), GalaxiesBlocks.SEQUOIA.slab().get());
				tag(BlockTags.WOODEN_STAIRS).add(GalaxiesBlocks.JAPOR.stairs().get(), GalaxiesBlocks.SEQUOIA.stairs().get());
			}
		});

		generator.addProvider(event.includeClient(), new ItemModelProvider(output, Galaxies.MODID, files)
		{
			@Override
			protected void registerModels()
			{
				for (var entry : GalaxiesBlocks.entries())
				{
					var block = entry.get();
					if (block instanceof AridPlant || block instanceof HkakBushBlock || block instanceof ChasukaCrop
							|| block instanceof MoloShrubBlock || block instanceof VaporatorMushroomBlock || block instanceof DoorBlock
							|| block == GalaxiesBlocks.JAPOR_LEAVES.get())
						continue;
					String path = entry.getId().getPath();
					String parent = switch (block) {
						case WallBlock wall -> path + "_inventory";
						case FenceBlock fence -> path + "_inventory";
						case TrapDoorBlock door -> path + "_bottom";
						case AccumulatingBlock layers -> path + "_height2";
						default -> path;
					};
					withExistingParent(path, modLoc("block/" + parent));
				}
			}
		});
	}

	private static final class TerrainLoot extends BlockLootSubProvider
	{
		private static final Set<String> AUTHORED_LOOT = Set.of("beskar_ore", "chromium_ore", "cortosis_ore", "desh_ore",
				"diatium_ore", "helicite_ore", "ionite_ore", "lommite_ore", "thorilide_ore", "titanium_ore", "zersium_ore",
				"chasuka", "molo_shrub", "vaporator_mushroom_colony", "wall_cluster_light");

		private TerrainLoot(HolderLookup.Provider registries)
		{
			super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
		}

		@Override
		protected Iterable<Block> getKnownBlocks()
		{
			return Stream.concat(GalaxiesBlocks.entries().stream(), LegacyConstruction.entries().stream())
					.filter(entry -> !AUTHORED_LOOT.contains(entry.getId().getPath()))
					.<Block>map(entry -> entry.get()).toList();
		}

		@Override
		protected void generate()
		{
			for (Block block : getKnownBlocks())
			{
				if (block instanceof VerticalSlabBlock)
					add(block, this::createSlabItemTable);
				else if (block instanceof DoorBlock)
					add(block, this::createDoorTable);
				else
					dropSelf(block);
			}
		}
	}

	private TerrainData()
	{
	}
}
