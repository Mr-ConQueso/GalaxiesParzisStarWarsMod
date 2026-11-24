package dev.pswg.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterDefinitions;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.BlasterNetwork;
import dev.pswg.blasters.BlasterState;
import dev.pswg.blasters.Blasters;
import dev.pswg.block.AccumulatingBlock;
import dev.pswg.block.ClusterLightBlock;
import dev.pswg.block.ConnectedBlock;
import dev.pswg.block.VerticalSlabBlock;
import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import dev.pswg.container.ArmorContent;
import dev.pswg.container.CoreComponents;
import dev.pswg.container.DrinkItems;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.container.GalaxiesItems;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.GadgetCharge;
import dev.pswg.gadgets.SmokeCloud;
import dev.pswg.item.DoorInsertItem;
import dev.pswg.model.g3d.G3dCompiler;
import dev.pswg.model.g3d.G3dFiles;
import dev.pswg.model.g3d.G3dPose;
import dev.pswg.model.g3d.G3dResources;
import dev.pswg.model.g3d.G3dSource;
import dev.pswg.structure.DerelictContainer;
import dev.pswg.structure.DerelictContainerPiece;
import dev.pswg.util.worldgen.mc.GalaxiesChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.joml.Matrix4f;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@GameTestHolder(Galaxies.MODID)
@PrefixGameTestTemplate(false)
public final class PortGameTests
{
	@GameTest(template = "port_test_empty")
	public static void canyonSlabGeometryAndLoot(GameTestHelper test)
	{
		var slab = GalaxiesBlocks.CANYON.slab().get();
		var state = slab.defaultBlockState()
				.setValue(VerticalSlabBlock.AXIS, Direction.Axis.X)
				.setValue(VerticalSlabBlock.WATERLOGGED, true);
		var pos = test.absolutePos(new BlockPos(1, 1, 1));
		var shape = slab.getShape(state, test.getLevel(), pos, CollisionContext.empty());
		test.assertValueEqual(shape.bounds().maxX, 0.5D, "West slab should fill half the block");
		test.assertTrue(state.getFluidState().is(Fluids.WATER), "Single slab should preserve water");
		test.assertTrue(GalaxiesBlocks.CANYON.wall().get().defaultBlockState().is(BlockTags.WALLS), "Canyon walls must connect as walls");

		var doubleState = state.setValue(VerticalSlabBlock.TYPE, SlabType.DOUBLE).setValue(VerticalSlabBlock.WATERLOGGED, false);
		var drops = Block.getDrops(doubleState, test.getLevel(), pos, null, null, Items.IRON_PICKAXE.getDefaultInstance());
		test.assertValueEqual(drops.size(), 1, "Double slab should yield one stack");
		test.assertValueEqual(drops.getFirst().getCount(), 2, "Double slab should yield two slabs");
		test.assertTrue(drops.getFirst().is(slab.asItem()), "Drop should be the registered canyon slab");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void g3dModelAndRigRoundTrip(GameTestHelper test) throws IOException
	{
		var document = JsonParser.parseString("""
				{
				  "version": 1,
				  "materials": [{"id":"surface","texture":"pswg:test"}],
				  "nodes": [{"id":"root","meshes":["triangle"]}],
				  "meshes": [{
				    "id":"triangle","material":"surface","indices":[0,1,2],
				    "vertices":[
				      {"position":[0,0,0],"normal":[0,0,1],"uv":[0,0]},
				      {"position":[16,0,0],"normal":[0,0,1],"uv":[1,0]},
				      {"position":[0,16,0],"normal":[0,0,1],"uv":[0,1]}
				    ]
				  }],
				  "sockets": [{"id":"tip","node":"root","localTransform":{"translation":[0,0,16]}}]
				}
				""");
		var source = G3dSource.CODEC.parse(JsonOps.INSTANCE, document).getOrThrow();
		var model = G3dCompiler.compile(Galaxies.id("test/socket"), source);
		var visual = G3dFiles.readModel(new ByteArrayInputStream(G3dFiles.write(model, true)));
		var rig = G3dFiles.readRig(new ByteArrayInputStream(G3dFiles.write(model, false)));
		test.assertValueEqual(visual.rig().sourceHash(), rig.sourceHash(), "Visual and gameplay assets must share a source hash");
		test.assertValueEqual(visual.meshes().getFirst().indexCount(), 3, "Triangle indices survive the binary round trip");
		var socketMatrix = new G3dPose(rig).socketMatrix("tip", new Matrix4f());
		test.assertValueEqual(socketMatrix.m32(), 1.0F, "A 16-unit authoring translation is one block");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void filledGlassAppliesEffectsAndReturnsGlass(GameTestHelper test)
	{
		var glass = DrinkItems.GLASSES.getFirst();
		var empty = glass.empty().toStack();
		var filled = glass.filled().toStack();
		test.assertTrue(empty.is(DrinkItems.DRINK_CONTAINER), "Empty glass must be tagged for the mixer");
		test.assertTrue(!filled.is(DrinkItems.DRINK_CONTAINER), "Filled glass must stay outside the empty container tag");

		filled.set(DataComponents.POTION_CONTENTS,
				PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1)));
		var user = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(1, 1, 1));
		ItemStack remainder = filled.finishUsingItem(test.getLevel(), user);
		test.assertTrue(user.hasEffect(MobEffects.MOVEMENT_SPEED), "Drinking must apply the stored effect");
		test.assertTrue(remainder.is(glass.empty().get()), "Drinking must return the original glass");
		test.assertTrue(filled.isEmpty(), "The filled stack must be consumed");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void tallLampHasBundledRig(GameTestHelper test)
	{
		var rig = G3dResources.SERVER.get(Galaxies.id("block/tall_lamp")).orElseThrow();
		test.assertTrue(rig.nodeIndex("light") >= 0, "Tall lamp rig contains its light node");
		test.assertValueEqual(GalaxiesBlocks.TALL_LAMP.get().defaultBlockState().getLightEmission(), 15, "Tall lamp light emission");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void tatooineTerrain(GameTestHelper test) throws IOException
	{
		// Vanilla GameTestServer forces the flat preset. Decode the bundled dimension
		// ourselves using the server's datapack registries, then generate an isolated chunk.
		var level = test.getLevel();
		var dimension = level.getServer().getResourceManager().getResourceOrThrow(Galaxies.id("dimension/tatooine.json"));
		LevelStem stem;
		try (var reader = dimension.openAsReader())
		{
			var ops = RegistryOps.create(JsonOps.INSTANCE, level.registryAccess());
			stem = LevelStem.CODEC.parse(ops, JsonParser.parseReader(reader)).getOrThrow();
		}

		var type = stem.type().value();
		test.assertValueEqual(type.minY(), -64, "Tatooine minimum build height");
		test.assertValueEqual(type.height(), 384, "Tatooine total build height");
		test.assertTrue(stem.generator() instanceof GalaxiesChunkGenerator, "Tatooine must use the PSWG chunk generator");

		var heightAccessor = LevelHeightAccessor.create(type.minY(), type.height());
		var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
		var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY, heightAccessor, biomes, null);
		var randomState = level.getChunkSource().randomState();
		var generator = stem.generator();
		generator.fillFromNoise(Blender.empty(), randomState, level.structureManager(), chunk).join();
		test.assertValueEqual(generator.getBaseHeight(0, 0, Heightmap.Types.WORLD_SURFACE_WG, heightAccessor, randomState),
				chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, 0, 0) + 1, "Base height matches the generated terrain height");

		var negativeChunk = new ProtoChunk(new ChunkPos(-1, -1), UpgradeData.EMPTY, heightAccessor, biomes, null);
		generator.fillFromNoise(Blender.empty(), randomState, level.structureManager(), negativeChunk).join();
		var column = generator.getBaseColumn(-1, -3, heightAccessor, randomState);
		var sample = new BlockPos.MutableBlockPos(-1, type.minY(), -3);
		for (int y = type.minY(); y < type.minY() + type.height(); y++)
		{
			sample.setY(y);
			test.assertValueEqual(column.getBlock(y), negativeChunk.getBlockState(sample), "Base column matches terrain at negative coordinates and Y " + y);
		}
		generator.buildSurface(null, level.structureManager(), randomState, chunk);

		int surface = chunk.getHeight(Heightmap.Types.WORLD_SURFACE_WG, 0, 0);
		test.assertTrue(surface > type.minY(), "Terrain must generate above the bottom at the origin");
		test.assertTrue(!chunk.getBlockState(new BlockPos(0, surface, 0)).isAir(), "Surface height points to terrain");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void dimensionCommandRequiresOperator(GameTestHelper test) throws Exception
	{
		var level = test.getLevel();
		int surface = level.getChunk(0, 0).getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
		var entity = test.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
		var dispatcher = level.getServer().getCommands().getDispatcher();
		var commandSource = entity.createCommandSourceStack();
		String command = "cdim " + level.dimension().location();
		test.assertTrue(dispatcher.parse(command, commandSource.withPermission(0)).getReader().canRead(),
				"Permission level zero cannot use /cdim");
		test.assertValueEqual(dispatcher.execute(command, commandSource.withPermission(2)), 1,
				"Operator dimension command should succeed");
		test.assertValueEqual(entity.blockPosition(), new BlockPos(0, surface + 1, 0), "Destination is above origin terrain");
		entity.discard();
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void connectedGlassRefreshesNeighbors(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(1, 1, 1);
		var east = pos.east();
		var glass = GalaxiesBlocks.IMPERIAL_GLASS.get();
		test.setBlock(pos, glass);
		test.setBlock(east, glass);

		var first = test.getBlockState(pos);
		test.assertTrue(ConnectedBlock.isConnected(first, Direction.EAST), "Glass connects when matching glass is placed");
		test.assertTrue(first.skipRendering(test.getBlockState(east), Direction.EAST), "Matching glass hides the shared face");
		test.assertTrue(!ConnectedBlock.isConnected(first, Direction.WEST), "Empty neighbor leaves a border");

		level.removeBlock(test.absolutePos(east), false);
		test.assertTrue(!ConnectedBlock.isConnected(test.getBlockState(pos), Direction.EAST), "Breaking the neighbor restores the border");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void corrugatedCrateMovesAndPersistsInventory(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(1, 1, 1);
		var absolute = test.absolutePos(pos);
		test.setBlock(pos, GalaxiesBlocks.IMPERIAL_CORRUGATED_CRATE.get());
		var crate = (CorrugatedCrateBlockEntity) level.getBlockEntity(absolute);
		test.assertTrue(crate != null, "Crate has a persistent block entity");

		var player = test.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5);
		var menu = crate.createMenu(1, player.getInventory(), player);
		test.assertTrue(menu != null && menu.stillValid(player), "Nearby player can use the crate menu");
		test.assertValueEqual(menu.slots.size(), 51, "Menu contains 15 crate slots and all player slots");
		player.getInventory().setItem(9, new ItemStack(Items.IRON_INGOT, 3));
		menu.quickMoveStack(player, 15);
		test.assertValueEqual(crate.getItem(0).getCount(), 3, "Shift-click moves a stack from the player to the crate");
		menu.quickMoveStack(player, 0);
		test.assertTrue(crate.isEmpty(), "Shift-click moves a stack back out of the crate");
		menu.removed(player);

		crate.setItem(14, new ItemStack(Items.GOLD_INGOT, 4));
		var serialized = crate.saveWithoutMetadata(level.registryAccess());
		var restored = new CorrugatedCrateBlockEntity(absolute, crate.getBlockState());
		restored.loadWithComponents(serialized, level.registryAccess());
		test.assertValueEqual(restored.getItem(14).getCount(), 4, "Last crate slot survives NBT reload");

		var lootTable = net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON;
		crate.setLootTable(lootTable, 47L);
		var withLoot = crate.saveWithoutMetadata(level.registryAccess());
		restored.loadWithComponents(withLoot, level.registryAccess());
		test.assertValueEqual(restored.getLootTable(), lootTable, "Unopened loot table survives NBT reload");
		test.assertValueEqual(restored.getLootTableSeed(), 47L, "Loot seed survives NBT reload");

		crate.setLootTable(null);
		level.destroyBlock(absolute, false);
		test.assertItemEntityCountIs(Items.GOLD_INGOT, pos, 2, 4);
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void wetPourstoneRetainsSlabPropertiesWhenTrampled(GameTestHelper test)
	{
		var relative = new BlockPos(2, 1, 2);
		var level = test.getLevel();
		var pos = test.absolutePos(relative);
		var wet = GalaxiesBlocks.WET_POURSTONE.slab().get().defaultBlockState()
				.setValue(VerticalSlabBlock.AXIS, Direction.Axis.X)
				.setValue(VerticalSlabBlock.TYPE, SlabType.TOP)
				.setValue(VerticalSlabBlock.WATERLOGGED, true);
		test.setBlock(relative, wet);

		var mob = test.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 1, 1));
		wet.entityInside(level, pos, mob);
		var ruined = level.getBlockState(pos);
		test.assertTrue(ruined.is(GalaxiesBlocks.RUINED_WET_POURSTONE.slab().get()), "A large entity ruins a wet slab");
		test.assertValueEqual(ruined.getValue(VerticalSlabBlock.AXIS), Direction.Axis.X, "Ruining preserves slab axis");
		test.assertValueEqual(ruined.getValue(VerticalSlabBlock.TYPE), SlabType.TOP, "Ruining preserves slab half");
		test.assertTrue(ruined.getValue(VerticalSlabBlock.WATERLOGGED), "Ruining preserves water");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void repellerRemovesOnlyOneLayerPerTick(GameTestHelper test)
	{
		var repeller = new BlockPos(1, 2, 1);
		var target = new BlockPos(2, 2, 2);
		test.setBlock(target.below(), net.minecraft.world.level.block.Blocks.STONE);
		test.setBlock(repeller, GalaxiesBlocks.ELECTROSTATIC_REPELLER.get());
		test.setBlock(target, GalaxiesBlocks.LOOSE_DESERT_SAND.get().defaultBlockState().setValue(AccumulatingBlock.LAYERS, 2));

		test.randomTick(repeller);
		test.assertValueEqual(test.getBlockState(target).getValue(AccumulatingBlock.LAYERS), 1, "Repeller removes one sand layer");
		test.randomTick(repeller);
		test.assertTrue(test.getBlockState(target).isAir(), "Repeller removes a single remaining layer");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void oresToolsAndScrapComponents(GameTestHelper test)
	{
		var level = test.getLevel();
		var beskar = GalaxiesItems.BESKAR_PICKAXE.toStack();
		test.assertValueEqual(beskar.getMaxDamage(), 2000, "Beskar tool durability");
		test.assertTrue(beskar.getItem().isValidRepairItem(beskar, GalaxiesItems.BESKAR_INGOT.toStack()), "Beskar repair tag is loaded");
		test.assertValueEqual(GalaxiesItems.DESH_WIRE.toStack().get(CoreComponents.METAL_COMPONENT), 1, "Wire metal component");
		test.assertValueEqual(GalaxiesItems.DESH_WIRE.toStack().get(CoreComponents.ENERGY_COMPONENT), 1, "Wire energy component");

		var state = GalaxiesBlocks.BESKAR_ORE.get().defaultBlockState();
		var drops = Block.getDrops(state, level, test.absolutePos(new BlockPos(1, 1, 1)), null, null, beskar);
		test.assertTrue(drops.size() == 1 && drops.getFirst().is(GalaxiesItems.BESKAR_RAW.get()), "Ore rolls the original raw material");
		test.assertTrue(level.getRecipeManager().byKey(Galaxies.id("tarkin/beskar_pickaxe")).isPresent(), "Legacy tool recipe loads");
		test.assertTrue(level.getRecipeManager().byKey(Galaxies.id("tarkin/beskar_ingot_from_raw_ore")).isPresent(), "Legacy smelting recipe loads");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void imperialLightingRespondsToPowerAndManualInversion(GameTestHelper test)
	{
		var level = test.getLevel();
		var relative = new BlockPos(1, 1, 1);
		var pos = test.absolutePos(relative);
		var light = GalaxiesBlocks.GRAY_IMPERIAL_LIGHT_HALF_1.get();
		test.setBlock(relative, light);
		test.assertValueEqual(level.getBlockState(pos).getLightEmission(), 13, "Unpowered inverted light is on");
		test.setBlock(relative.east(), net.minecraft.world.level.block.Blocks.REDSTONE_BLOCK);
		test.assertValueEqual(level.getBlockState(pos).getLightEmission(), 0, "Redstone turns an inverted light off");

		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos), Direction.NORTH, pos, false);
		level.getBlockState(pos).useWithoutItem(level, player, hit);
		test.assertTrue(!level.getBlockState(pos).getValue(BlockStateProperties.INVERTED), "Empty-hand interact changes inversion");
		test.assertValueEqual(level.getBlockState(pos).getLightEmission(), 13, "Non-inverted lamp lights while powered");
		level.removeBlock(pos.east(), false);
		test.assertValueEqual(level.getBlockState(pos).getLightEmission(), 0, "Removing power turns the non-inverted light off");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void originalCrateLootResolvesAndCupReturns(GameTestHelper test)
	{
		var level = test.getLevel();
		test.setBlock(new BlockPos(1, 1, 1), GalaxiesBlocks.MEDICAL_CORRUGATED_CRATE.get());
		var crate = (CorrugatedCrateBlockEntity) level.getBlockEntity(test.absolutePos(new BlockPos(1, 1, 1)));
		for (String name : java.util.List.of("generic_food_crate", "generic_tech_crate", "medical_crate", "mining_crate",
				"generic_weapons_crate", "imperial_crate"))
		{
			var key = net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, Galaxies.id("container/" + name));
			var table = level.getServer().reloadableRegistries().getLootTable(key);
			test.assertTrue(!table.equals(net.minecraft.world.level.storage.loot.LootTable.EMPTY), name + " is loaded");
		}

		var medical = net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, Galaxies.id("container/medical_crate"));
		crate.setLootTable(medical, 15L);
		test.assertTrue(!crate.isEmpty(), "Medical crate produces supplies from the original loot pool");
		test.assertTrue(crate.getLootTable() == null, "Opening unpacks a crate's loot once");

		var cup = DrinkItems.DURASTEEL_CUP;
		test.assertValueEqual(cup.empty().toStack().get(CoreComponents.METAL_COMPONENT), 1, "Durasteel cup can be scrapped for metal");
		var user = test.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(2, 1, 2));
		var remainder = cup.filled().toStack().finishUsingItem(level, user);
		test.assertTrue(remainder.is(cup.empty().get()), "Drinking returns a durasteel cup");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void chasukaGrowsAndDropsOriginalProduce(GameTestHelper test)
	{
		var crop = GalaxiesBlocks.CHASUKA.get();
		var pos = new BlockPos(1, 2, 1);
		test.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState());
		test.setBlock(pos, crop.defaultBlockState());
		var level = test.getLevel();
		var absolutePos = test.absolutePos(pos);
		test.assertTrue(crop.isValidBonemealTarget(level, absolutePos, test.getBlockState(pos)), "Young chasuka accepts bone meal");
		test.assertValueEqual(crop.getMaxAge(), 2, "Chasuka has three growth stages");
		var ripe = crop.getStateForAge(crop.getMaxAge());
		test.assertTrue(!crop.isValidBonemealTarget(level, absolutePos, ripe), "Ripe chasuka stops growing");
		var drops = Block.getDrops(ripe, level, absolutePos, null, null, ItemStack.EMPTY);
		test.assertTrue(drops.stream().anyMatch(item -> item.is(GalaxiesItems.CHASUKA_LEAF.get())), "Ripe chasuka drops leaves");
		test.assertTrue(drops.stream().anyMatch(item -> item.is(GalaxiesItems.CHASUKA_SEEDS.get())), "Ripe chasuka drops seeds");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void woodAndClusterBlockDrops(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = test.absolutePos(new BlockPos(1, 1, 1));
		var door = GalaxiesBlocks.JAPOR.door().get();
		var upper = door.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER);
		test.assertTrue(Block.getDrops(upper, level, pos, null, null, ItemStack.EMPTY).isEmpty(), "Door drops only from its bottom half");
		test.assertTrue(door.defaultBlockState().is(BlockTags.WOODEN_DOORS), "Japor doors are recognized as wooden doors");
		for (var color : DyeColor.values())
		{
			var insert = BuiltInRegistries.ITEM.get(Galaxies.id(color.getName() + "_door_insert"));
			test.assertTrue(insert instanceof DoorInsertItem dyed && dyed.getColor() == color, "Each insert preserves its dye color");
		}

		var cluster = GalaxiesBlocks.WALL_CLUSTER_LIGHT.get();
		var triple = cluster.defaultBlockState().setValue(ClusterLightBlock.CLUSTER_SIZE, 3);
		var drops = Block.getDrops(triple, level, pos, null, null, ItemStack.EMPTY);
		test.assertTrue(drops.size() == 1 && drops.getFirst().is(cluster.asItem()), "The three-lamp cluster drops its fixture");
		test.assertValueEqual(drops.getFirst().getCount(), 3, "All three lamps drop");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void stormtrooperArmorUsesOriginalStatsAndRig(GameTestHelper test)
	{
		var helmet = ArmorContent.STORM_TROOPER.helmet().get();
		test.assertValueEqual(helmet.getEquipmentSlot(), EquipmentSlot.HEAD, "Stormtrooper helmets go in the head slot");
		test.assertValueEqual(helmet.getDefense(), 3, "Stormtrooper helmets provide the source diamond defense");
		test.assertValueEqual(helmet.getDefaultInstance().getMaxDamage(), 363, "Stormtrooper helmets have diamond durability");
		var appearance = ArmorContent.appearance(ArmorContent.STORM_TROOPER.helmet().getId());
		test.assertTrue(appearance != null && appearance.hideOverlay(), "The helmet declares overlay handling");
		var rig = G3dResources.SERVER.get(appearance.model()).orElseThrow();
		test.assertTrue(rig.nodeIndex("head") >= 0, "The bundled armor rig has a head anchor");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void blasterPresetAndModelRigsLoadOnServer(GameTestHelper test)
	{
		var definitions = BlasterDefinitions.SERVER.definitions();
		var definition = definitions.get(ResourceLocation.fromNamespaceAndPath("pswg_blasters", "test_blaster"));
		test.assertTrue(definition != null, "The pinned blaster datapack definition loads");
		test.assertValueEqual(definition.stats().damage(), 8.0F, "Blaster damage matches the preset");
		test.assertValueEqual(definition.stats().heat().perRound(), 20, "Heat data matches the preset");
		test.assertValueEqual(definition.attachments().options().size(), 6, "All attachment options load");
		var barrel = ResourceLocation.fromNamespaceAndPath("pswg_blasters", "barrel_slot");
		var bipod = ResourceLocation.fromNamespaceAndPath("pswg_blasters", "e11/bipod");
		test.assertValueEqual(definition.attachments().defaults().get(barrel), bipod, "The original bipod mounts by default");
		test.assertTrue(definition.attachments().options().get(bipod).slots().contains(barrel), "The bipod fits the barrel slot");
		var json = BlasterDefinition.CODEC.encodeStart(JsonOps.INSTANCE, definition).getOrThrow();
		test.assertValueEqual(BlasterDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(), definition, "Datapack definition codec round-trips");

		var rig = G3dResources.SERVER.get(ResourceLocation.fromNamespaceAndPath("pswg_blasters", "item/e11d")).orElseThrow();
		test.assertTrue(rig.nodeIndex("barrel") >= 0 && rig.nodeIndex("stock") >= 0, "E-11D parts are available on the dedicated server");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void blasterInputUsesHeldSerialAndHeat(GameTestHelper test)
	{
		var level = test.getLevel();
		var player = new ServerPlayer(level.getServer(), level,
				new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "blaster-test"), ClientInformation.createDefault());
		var origin = test.absolutePos(new BlockPos(1, 1, 1));
		player.setPos(origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5);
		player.setYHeadRot(-90F);
		player.setXRot(0F);
		var id = Blasters.id("test_blaster");
		var definition = BlasterDefinitions.SERVER.definitions().get(id);
		var blaster = BlasterItem.createStack(id, definition);
		blaster.set(Blasters.SERIAL.get(), 11L);
		player.setItemInHand(InteractionHand.OFF_HAND, blaster);

		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.PRESS, 10L));
		test.assertValueEqual(BlasterItem.state(blaster), BlasterState.DEFAULT, "Stale selection cannot fire");
		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.MAIN_HAND, BlasterNetwork.Input.PRESS, 11L));
		test.assertValueEqual(BlasterItem.state(blaster), BlasterState.DEFAULT, "Only the active hand can fire");
		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.PRESS, 11L));
		test.assertValueEqual(BlasterItem.state(blaster).lastTotalHeat(), 20F, "A valid offhand shot adds 20 heat");
		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.REPEAT, 11L));
		test.assertValueEqual(BlasterItem.state(blaster).lastTotalHeat(), 20F, "The repeat delay is enforced on the server");

		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.VENT, 11L));
		test.assertValueEqual(BlasterItem.state(blaster).mode(), BlasterState.Mode.REQUESTED_BYPASS, "Venting enters the manual cooling state");
		long now = level.getGameTime();
		blaster.set(Blasters.STATE.get(), new BlasterState(false, now - 30, now - 26, now - 30,
				0F, 100F, BlasterState.Mode.OVERHEAT, 0, -1));
		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.REPEAT, 11L));
		test.assertValueEqual(BlasterItem.state(blaster).mode(), BlasterState.Mode.OVERHEAT, "A held trigger cannot activate the cooling bypass");
		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.PRESS, 11L));
		test.assertValueEqual(BlasterItem.state(blaster).mode(), BlasterState.Mode.PASSIVE, "A click in the first window bypasses the overheat");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void blasterBoltDamagesTarget(GameTestHelper test)
	{
		var level = test.getLevel();
		var shooter = test.spawn(EntityType.ARMOR_STAND, new BlockPos(1, 1, 1));
		var target = test.spawn(EntityType.PIG, new BlockPos(4, 1, 1));
		target.setNoAi(true);
		var bolt = new dev.pswg.blasters.BlasterBolt(Blasters.BOLT.get(), level);
		shooter.setYHeadRot(-90F);
		bolt.launch(shooter, 8F, 48);
		bolt.setPos(shooter.getX(), target.getY() + 0.45, shooter.getZ());
		level.addFreshEntity(bolt);
		test.succeedWhen(() -> test.assertTrue(target.getHealth() < target.getMaxHealth() && !bolt.isAlive(),
				"The server bolt damages the target and disappears on impact"));
	}

	@GameTest(template = "port_test_empty")
	public static void smokeGrenadeUsesPrimedStackAndPersistsFuse(GameTestHelper test)
	{
		var level = test.getLevel();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(1, 1, 1))));
		var stack = new ItemStack(Demolitions.SMOKE.get(), 2);
		player.setItemInHand(InteractionHand.OFF_HAND, stack);
		Demolitions.SMOKE.get().togglePrimed(level, player, stack);
		test.assertTrue(stack.has(Demolitions.PRIMING_TIME.get()), "Arming records a persistent stack timestamp");
		Demolitions.SMOKE.get().use(level, player, InteractionHand.OFF_HAND);
		test.assertValueEqual(stack.getCount(), 1, "Throwing from the off hand consumes one item");
		test.assertTrue(!stack.has(Demolitions.PRIMING_TIME.get()), "The remaining stack is disarmed");
		var bounds = new AABB(player.blockPosition()).inflate(3);
		var thrown = level.getEntitiesOfClass(GadgetCharge.class, bounds, charge -> charge.variant() == Demolitions.Variant.SMOKE);
		test.assertValueEqual(thrown.size(), 1, "A single server projectile was created");
		var saved = thrown.getFirst().saveWithoutId(new net.minecraft.nbt.CompoundTag());
		thrown.getFirst().discard();
		var restored = GadgetCharge.create(Demolitions.Variant.SMOKE, level, player.position(), player);
		restored.load(saved);
		test.assertTrue(restored.isPrimed(), "Primed fuse survives an entity save and load");
		restored.arm(0);
		level.addFreshEntity(restored);
		test.succeedWhen(() -> test.assertTrue(!level.getEntitiesOfClass(SmokeCloud.class, bounds).isEmpty(),
				"The smoke grenade emits a server smoke cloud"));
	}

	@GameTest(template = "port_test_empty")
	public static void pressureMineArmsBeforeTriggering(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(2, 1, 2);
		test.setBlock(pos.below(), Blocks.OBSIDIAN);
		var mine = GadgetCharge.create(Demolitions.Variant.PRESSURE, level, Vec3.atBottomCenterOf(test.absolutePos(pos)), null);
		level.addFreshEntity(mine);
		test.runAfterDelay(25, () -> test.assertTrue(!mine.isPrimed(), "Mine stays dormant before the arming period ends"));
		test.runAfterDelay(53, () -> {
			test.assertTrue(mine.isPrimed() && mine.isAlive(), "Mine arms before it can be triggered");
			var trigger = test.spawn(EntityType.ARMOR_STAND, pos);
			trigger.setPos(mine.position());
			test.succeedWhen(() -> test.assertTrue(!mine.isAlive(), "A nearby entity triggers the armed mine"));
		});
	}

	@GameTest(template = "port_test_empty")
	public static void fragmentationUsesItsBlockTag(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(2, 1, 2);
		test.setBlock(pos.below(), Blocks.OBSIDIAN);
		test.setBlock(pos.east(), Blocks.OAK_LEAVES);
		test.setBlock(pos.west(), Blocks.STONE);
		var grenade = GadgetCharge.create(Demolitions.Variant.FRAGMENTATION, level,
				Vec3.atBottomCenterOf(test.absolutePos(pos)).add(0, 0.2, 0), null);
		grenade.arm(0);
		level.addFreshEntity(grenade);
		test.succeedWhen(() -> {
			test.assertTrue(!grenade.isAlive(), "Fragmentation finishes its delayed blast");
			test.assertBlockPresent(Blocks.AIR, pos.east());
			test.assertBlockPresent(Blocks.STONE, pos.west());
		});
	}

	@GameTest(template = "port_test_empty")
	public static void impactGrenadeTriggersBeforeItsFuse(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(2, 1, 2);
		test.setBlock(pos.east(), Blocks.OBSIDIAN);
		var grenade = GadgetCharge.create(Demolitions.Variant.IMPACT, level,
				Vec3.atBottomCenterOf(test.absolutePos(pos)).add(0, 0.4, 0), null);
		grenade.arm(250);
		grenade.setDeltaMovement(1, 0, 0);
		level.addFreshEntity(grenade);
		test.runAfterDelay(4, () -> {
			test.assertTrue(!grenade.isAlive(), "An armed impact grenade detonates on contact before its fuse expires");
			test.succeed();
		});
	}

	@GameTest(template = "port_test_empty")
	public static void inventoryFireIgnitesGrenadesTogether(GameTestHelper test)
	{
		var level = test.getLevel();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(2, 1, 2))));
		player.setRemainingFireTicks(40);
		var frag = Demolitions.FRAGMENTATION.get();
		var first = new ItemStack(frag, 2);
		player.getInventory().setItem(0, first);
		player.getInventory().setItem(1, new ItemStack(frag));
		player.getInventory().setItem(2, new ItemStack(Items.FLINT));
		frag.inventoryTick(first, level, player, 0, true);
		test.assertValueEqual(player.getInventory().countItem(frag), 0, "All matching grenades are consumed together");
		test.assertValueEqual(player.getInventory().countItem(Items.FLINT), 1, "Other inventory stays intact");
		var charges = level.getEntitiesOfClass(GadgetCharge.class, new AABB(player.blockPosition()).inflate(2));
		test.assertValueEqual(charges.size(), 1, "The burning stacks merge into one charge");
		var charge = charges.getFirst();
		test.assertTrue(charge.isPrimed(), "The burning inventory charge immediately starts its fuse");
		var saved = charge.saveWithoutId(new net.minecraft.nbt.CompoundTag());
		test.assertValueEqual(saved.getFloat("Power"), 6F, "Three grenades produce the original combined power");
		charge.discard();
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void derelictMarkerSpawnsSeededCrate(GameTestHelper test)
	{
		var level = test.getLevel();
		var manager = level.getStructureManager();
		var template = manager.get(DerelictContainer.KEY.location()).orElseThrow();
		var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
		var markers = template.filterBlocks(BlockPos.ZERO, settings, Blocks.STRUCTURE_BLOCK);
		test.assertTrue(!markers.isEmpty(), "The packaged derelict template contains crate markers");
		var local = markers.stream().filter(marker -> marker.nbt() != null && "crate".equals(marker.nbt().getString("metadata")))
				.findFirst().orElseThrow().pos();
		var pos = test.absolutePos(new BlockPos(2, 2, 2));
		var origin = pos.subtract(local);
		var piece = new DerelictContainerPiece(manager, origin, Rotation.NONE);
		var clip = BoundingBox.fromCorners(pos, pos);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(),
				net.minecraft.util.RandomSource.create(123), clip, new ChunkPos(pos), origin);
		test.assertTrue(level.getBlockEntity(pos) instanceof CorrugatedCrateBlockEntity crate && crate.getLootTable() != null
				&& crate.getLootTableSeed() != 0, "A processed crate marker has seeded original loot");
		test.succeed();
	}

	private PortGameTests()
	{
	}
}
