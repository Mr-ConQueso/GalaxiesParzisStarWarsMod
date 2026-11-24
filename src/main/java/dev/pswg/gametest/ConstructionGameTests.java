package dev.pswg.gametest;

import dev.pswg.Galaxies;
import dev.pswg.block.ImperialLighting;
import dev.pswg.block.MultipartBlock;
import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import dev.pswg.blasters.BlasterWorkbench;
import dev.pswg.container.Crates;
import dev.pswg.container.LegacyConstruction;
import dev.pswg.sabers.Sabers;
import dev.pswg.screen.CorrugatedCrateMenu;
import dev.pswg.utilities.LegacyUtilities;
import dev.pswg.utilities.MoistureVaporator;
import dev.pswg.utilities.SlidingDoor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;

@GameTestHolder(Galaxies.MODID)
@PrefixGameTestTemplate(false)
public final class ConstructionGameTests
{
	private static InteractionResult place(GameTestHelper test, Block block, BlockPos pos, Player player, Direction look)
	{
		return place(test, new ItemStack(block), pos, player, look);
	}

	private static InteractionResult place(GameTestHelper test, ItemStack stack, BlockPos pos, Player player, Direction look)
	{
		var support = test.absolutePos(pos.below());
		player.setYRot(look.toYRot());
		player.setPos(Vec3.atBottomCenterOf(test.absolutePos(pos.relative(look.getOpposite(), 3))));
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		var hit = new BlockHitResult(Vec3.atCenterOf(support).add(0, 0.5, 0), Direction.UP, support, false);
		return player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
	}

	@GameTest(template = "port_test_empty")
	public static void wideWorkstationsPlaceAndDropAsOne(GameTestHelper test)
	{
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var anchor = new BlockPos(2, 1, 2);
		var level = test.getLevel();
		test.setBlock(anchor.below(), Blocks.STONE);
		for (var block : List.<MultipartBlock<?>>of(BlasterWorkbench.BLOCK.get(), Sabers.FORGE.get()))
		{
			var stack = new ItemStack(block);
			var facing = Direction.EAST;
			if (block == Sabers.FORGE.get())
			{
				facing = Direction.WEST;
				stack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(Map.of("facing", "west", "part", "1")));
			}
			var side = anchor.relative(facing.getCounterClockWise());
			test.setBlock(side, Blocks.STONE);
			test.assertTrue(!place(test, stack.copy(), anchor, player, Direction.WEST).consumesAction(), "An obstruction in the second cell refuses the whole workstation");
			test.assertBlockPresent(Blocks.AIR, anchor);
			test.setBlock(side, Blocks.WATER);

			test.assertTrue(place(test, stack.copy(), anchor, player, Direction.WEST).consumesAction(), "Both workstation halves place through the block item");
			test.assertBlockPresent(block, anchor);
			test.assertBlockPresent(block, side);
			test.assertValueEqual(block.partIndex(test.getBlockState(anchor)), 0, "An item state still places an anchor and one extension");
			var sideState = test.getBlockState(side);
			var sidePos = test.absolutePos(side);
			test.assertValueEqual(block.partIndex(sideState), 1, "The extension is a distinct block state");
			test.assertTrue(sideState.getValue(BlockStateProperties.WATERLOGGED), "The extension preserves its own water");
			test.assertTrue(level.getBlockEntity(sidePos) == null && sideState.getMenuProvider(level, sidePos) == level.getBlockEntity(test.absolutePos(anchor)),
					"Both halves open the anchor's only menu provider");

			level.destroyBlock(sidePos, true);
			test.assertBlockPresent(Blocks.AIR, anchor);
			test.assertBlockPresent(Blocks.WATER, side);
			test.assertItemEntityCountIs(block.asItem(), anchor, 3, 1);
		}
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void vaporatorReservesItsOriginalHeight(GameTestHelper test)
	{
		var level = test.getLevel();
		var block = LegacyUtilities.VAPORATOR.get();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var anchor = new BlockPos(2, 1, 2);
		var tip = anchor.above(8);
		// The shared GameTest template has a ceiling below the GX-8's antenna.
		for (int part = 0; part < block.partCount(); part++)
			test.setBlock(anchor.offset(block.eastFacingOffset(part)), Blocks.AIR);
		test.setBlock(anchor.below(), Blocks.STONE);
		test.setBlock(tip, Blocks.STONE);
		test.assertTrue(!place(test, block, anchor, player, Direction.WEST).consumesAction(), "The GX-8 checks clearance all the way to the antenna");
		test.assertBlockPresent(Blocks.AIR, anchor);
		test.setBlock(tip, Blocks.AIR);
		test.assertTrue(place(test, block, anchor, player, Direction.WEST).consumesAction(), "The original-size GX-8 places in clear space");

		var center = test.absolutePos(anchor);
		var machine = (MoistureVaporator.Inventory) level.getBlockEntity(center);
		for (int part = 0; part < block.partCount(); part++)
		{
			var pos = center.offset(block.eastFacingOffset(part));
			var state = level.getBlockState(pos);
			test.assertTrue(state.is(block) && block.partIndex(state) == part, "A GX-8 mesh section has its physical block");
			test.assertTrue(state.getMenuProvider(level, pos) == machine, "The GX-8 parts share one inventory");
		}
		machine.setItem(0, new ItemStack(Items.GLASS_BOTTLE));
		level.destroyBlock(test.absolutePos(tip), true);
		for (int part = 0; part < block.partCount(); part++)
			test.assertBlockPresent(Blocks.AIR, anchor.offset(block.eastFacingOffset(part)));
		test.assertItemEntityCountIs(block.asItem(), tip, 2, 1);
		test.assertItemEntityCountIs(Items.GLASS_BOTTLE, anchor, 2, 1);
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void kyberCrateRetainsItsInventoryAndSingleDrop(GameTestHelper test)
	{
		var level = test.getLevel();
		var block = LegacyConstruction.KYBER_CRATES.getFirst().get();
		var anchor = new BlockPos(2, 1, 2);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		test.setBlock(anchor.below(), Blocks.STONE);
		test.assertTrue(place(test, block, anchor, player, Direction.WEST).consumesAction(), "Kyber crate places with both ends");
		var crate = (CorrugatedCrateBlockEntity) level.getBlockEntity(test.absolutePos(anchor));
		var end = test.absolutePos(anchor.south());
		var menu = (CorrugatedCrateMenu) level.getBlockState(end).getMenuProvider(level, end).createMenu(3, player.getInventory(), player);
		test.assertTrue(menu.getType() == Crates.KYBER_MENU.get() && menu.columns() == 13 && crate.getContainerSize() == 39,
				"Either end opens the 39-slot Kyber menu");
		crate.setItem(38, new ItemStack(Items.DIAMOND, 2));
		crate.loadWithComponents(crate.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
		test.assertValueEqual(crate.getItem(38).getCount(), 2, "The final Kyber slot survives reload");
		level.destroyBlock(end, true);
		for (var part : List.of(anchor, anchor.north(), anchor.south()))
			test.assertBlockPresent(Blocks.AIR, part);
		test.assertItemEntityCountIs(block.asItem(), anchor, 3, 1);
		test.assertItemEntityCountIs(Items.DIAMOND, anchor, 3, 2);
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void restoredBlocksHaveRecipesAndInteract(GameTestHelper test)
	{
		var level = test.getLevel();
		for (String name : List.of("orange_kyber_crate", "gray_kyber_crate", "black_kyber_crate", "toolbox", "desh_barrel", "large_pipe",
				"air_vent", "red_hangar_light", "blue_hangar_light", "light_fixture"))
			test.assertTrue(level.getRecipeManager().byKey(Galaxies.id("tarkin/" + name)).isPresent(), "Original crafting recipe for " + name);

		var pos = new BlockPos(2, 1, 2);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var scaffold = LegacyConstruction.SCAFFOLD.get();
		test.setBlock(pos.below(), Blocks.STONE);
		test.assertTrue(place(test, scaffold, pos, player, Direction.WEST).consumesAction(), "A scaffold places on a solid base");
		player.setShiftKeyDown(true);
		test.assertTrue(place(test, scaffold, pos.above(), player, Direction.WEST).consumesAction(), "A scaffold can be extended vertically");
		player.setShiftKeyDown(false);
		test.assertValueEqual(test.getBlockState(pos.above()).getValue(ScaffoldingBlock.DISTANCE), 0, "Metal scaffold carries support upward");

		var fixture = LegacyConstruction.LIGHT_FIXTURE.get();
		test.setBlock(pos.east(), fixture);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		test.useBlock(pos.east(), player);
		test.assertValueEqual(test.getBlockState(pos.east()).getValue(ImperialLighting.Fixture.BRIGHTNESS), 1, "A fixture cycles from 15 to 5");
		test.setBlock(pos.east(2), Blocks.REDSTONE_BLOCK);
		test.assertTrue(!test.getBlockState(pos.east()).getValue(BlockStateProperties.LIT), "Power extinguishes the inverted fixture");

		var grate = BuiltInRegistries.BLOCK.get(Galaxies.id("imperial_grate_1"));
		test.setBlock(pos.west(), grate);
		test.useBlock(pos.west(), player);
		test.assertTrue(test.getBlockState(pos.west()).getValue(BlockStateProperties.OPEN), "Imperial metal grates can open by hand");

		var pipe = LegacyConstruction.LARGE_PIPE.get();
		var first = pos.south().west();
		var second = pos.south();
		test.setBlock(first, pipe);
		test.setBlock(second.below(), Blocks.STONE);
		test.assertTrue(place(test, pipe, second, player, Direction.WEST).consumesAction(), "A neighboring pipe places against the first pipe");
		test.assertTrue(test.getBlockState(first).getValue(PipeBlock.EAST) && test.getBlockState(second).getValue(PipeBlock.WEST), "Adjacent pipes link their facing ports");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		test.useBlock(second, player);
		test.assertTrue(test.getBlockState(second).getValue(PipeBlock.NORTH), "An empty-hand click opens the chosen pipe port");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void slidingDoorReversesInFlight(GameTestHelper test)
	{
		var block = LegacyUtilities.SLIDING_DOOR.get();
		var anchor = new BlockPos(2, 1, 2);
		var pos = test.absolutePos(anchor);
		var level = test.getLevel();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		test.setBlock(anchor.below(), Blocks.STONE);
		test.assertTrue(place(test, block, anchor, player, Direction.EAST).consumesAction(), "Sliding door frame places both halves");
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(BuiltInRegistries.ITEM.get(Galaxies.id("blue_door_insert"))));
		test.useBlock(anchor.above(), player);
		var door = (SlidingDoor.Animation) level.getBlockEntity(pos);
		test.assertValueEqual(door.openness(0), 0F, "The insert starts closed");
		test.useBlock(anchor, player);
		test.runAfterDelay(3, () -> {
			float progress = door.openness(0);
			test.assertTrue(progress > 0 && progress < 1, "The panel passes through intermediate positions");
			test.useBlock(anchor.above(), player);
			test.assertValueEqual(door.openness(0), progress, "Reversing from the upper half keeps the current panel position");
		});
		test.runAfterDelay(14, () -> {
			test.assertValueEqual(door.openness(0), 0F, "The reversed panel closes fully");
			test.succeed();
		});
	}

	private ConstructionGameTests() {}
}
