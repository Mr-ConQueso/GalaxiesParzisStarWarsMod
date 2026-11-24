package dev.pswg.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.pswg.Galaxies;
import dev.pswg.block.VerticalSlabBlock;
import dev.pswg.container.DrinkItems;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.model.g3d.G3dCompiler;
import dev.pswg.model.g3d.G3dFiles;
import dev.pswg.model.g3d.G3dPose;
import dev.pswg.model.g3d.G3dResources;
import dev.pswg.model.g3d.G3dSource;
import dev.pswg.util.worldgen.mc.GalaxiesChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.Blender;
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

		var chunk = new ProtoChunk(new ChunkPos(0, 0), UpgradeData.EMPTY,
				LevelHeightAccessor.create(type.minY(), type.height()), level.registryAccess().registryOrThrow(Registries.BIOME), null);
		var randomState = level.getChunkSource().randomState();
		var generator = stem.generator();
		generator.fillFromNoise(Blender.empty(), randomState, level.structureManager(), chunk).join();
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

	private PortGameTests()
	{
	}
}
