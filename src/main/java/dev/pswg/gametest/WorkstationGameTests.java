package dev.pswg.gametest;

import com.mojang.authlib.GameProfile;
import dev.pswg.Galaxies;
import dev.pswg.container.CoreComponents;
import dev.pswg.container.DrinkItems;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.container.GalaxiesItems;
import dev.pswg.gadgets.WorkstationRecipes;
import dev.pswg.gadgets.Workstations;
import dev.pswg.gadgets.brewing.BrewingMap;
import dev.pswg.gadgets.brewing.Mixer;
import dev.pswg.gadgets.brewing.MixerMenu;
import dev.pswg.gadgets.drill.DrillParts;
import dev.pswg.gadgets.drill.ExtractionDrill;
import dev.pswg.gadgets.scrapping.ScrappingMenu;
import dev.pswg.gadgets.scrapping.ScrappingTable;
import dev.pswg.model.g3d.G3dResources;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(Galaxies.MODID)
@PrefixGameTestTemplate(false)
public final class WorkstationGameTests
{
	@GameTest(template = "port_test_empty")
	public static void mixerMovesOnMapAndBottlesPersistedAdditions(GameTestHelper test)
	{
		var pos = new BlockPos(1, 1, 1);
		var absolute = test.absolutePos(pos);
		var level = test.getLevel();
		test.setBlock(pos, Workstations.MIXER.get());
		var mixer = (Mixer) level.getBlockEntity(absolute);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atBottomCenterOf(absolute));
		var menu = (MixerMenu) mixer.createMenu(1, player.getInventory(), player);
		var cup = DrinkItems.DURASTEEL_CUP;
		test.assertTrue(!menu.clickMenuButton(player, 1), "An inactive mixer menu rejects requests");
		player.containerMenu = menu;
		test.assertTrue(!menu.clickMenuButton(player, 1), "The mixer cannot bottle without a vessel");
		player.getInventory().setItem(9, cup.empty().toStack(2));
		menu.quickMoveStack(player, 3);
		test.assertValueEqual(mixer.getItem(2).getCount(), 1, "Shift-click accepts one drink container");
		mixer.setItem(0, new ItemStack(Items.COAL));
		mixer.setItem(1, GalaxiesItems.STRIPPED_JAPOR_BRANCH.toStack());
		test.assertTrue(BrewingMap.at(328, 24).effect().getEffect().equals(MobEffects.FIRE_RESISTANCE),
				"The packaged map loads effect cells");

		test.runAfterDelay(8, () -> {
			test.assertTrue(menu.fuel() > 0 && mixer.getItem(0).isEmpty() && mixer.getItem(1).isEmpty(), "Fuel and path ingredient are consumed");
			test.assertTrue(menu.clickMenuButton(player, 0), "The bellows engage with a heated vessel");
		});
		test.runAfterDelay(12, () -> {
			test.assertTrue(menu.mapX() > 256 && menu.mapY() < 256, "Japor branch moves the marker northeast");
			mixer.setItem(1, new ItemStack(Items.RED_DYE));
		});
		test.runAfterDelay(15, () -> mixer.setItem(1, new ItemStack(Items.APPLE)));
		test.runAfterDelay(18, () -> {
			int dye = 0xFF000000 | DyeColor.RED.getTextureDiffuseColor();
			test.assertValueEqual(menu.swatch(1, 0), dye, "Dye swatch keeps all color bits");
			test.assertTrue(menu.swatch(2, 0) != 0, "Food swatch is visible");
			var snapshot = mixer.saveWithoutMetadata(level.registryAccess());
			snapshot.putFloat("map_x", 328);
			snapshot.putFloat("map_y", 24);
			mixer.loadWithComponents(snapshot, level.registryAccess());
			test.assertValueEqual(menu.swatch(1, 0), dye, "Dye survives a block entity reload");
			test.assertTrue(menu.swatch(2, 0) != 0 && menu.fuel() > 0, "Food and fuel survive a block entity reload");
			test.assertTrue(menu.clickMenuButton(player, 2), "A beneficial cell adds its effect");
			test.assertTrue(!menu.clickMenuButton(player, 2), "An effect cannot be added twice");
			test.assertTrue(menu.clickMenuButton(player, 1), "A charged vessel can be bottled");
			var result = mixer.getItem(2);
			test.assertTrue(result.is(cup.filled().get()), "The mixer creates the matching filled cup");
			var potion = result.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
			test.assertTrue(potion.customEffects().getFirst().getEffect().equals(MobEffects.FIRE_RESISTANCE), "Bottled drink includes the map effect");
			test.assertValueEqual(potion.customColor().orElseThrow(), DyeColor.RED.getTextureDiffuseColor() & 0xFFFFFF, "Bottled drink includes the custom color");
			test.assertValueEqual(result.get(DataComponents.FOOD).nutrition(), 2, "Bottled drink includes diluted apple nutrition");
			test.assertTrue(player.getData(Workstations.BREWING_DISCOVERIES).contains(ResourceLocation.withDefaultNamespace("fire_resistance")),
					"Bottling records the effect as discovered");
			test.assertTrue(!menu.clickMenuButton(player, 1) && menu.mapX() == 256, "Bottling resets the marker and cannot be repeated");
			test.succeed();
		});
	}

	@GameTest(template = "port_test_empty")
	public static void extractionDrillUsesPartsAndRechecksMarkedOres(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(2, 1, 2);
		var absolute = test.absolutePos(pos);
		test.setBlock(pos, Blocks.IRON_ORE);
		test.setBlock(pos.north(), Blocks.DEEPSLATE_IRON_ORE);
		test.setBlock(pos.south(), Blocks.IRON_ORE);
		test.setBlock(pos.west(), Blocks.IRON_ORE);
		test.setBlock(pos.east(), Blocks.IRON_ORE);

		var player = new ServerPlayer(level.getServer(), level,
				new GameProfile(UUID.randomUUID(), "extractor-test"), ClientInformation.createDefault());
		player.setPos(Vec3.atBottomCenterOf(absolute.above()));
		var parts = new DrillParts(DrillParts.LASER_EXTRACTOR, DrillParts.TITANIUM_DRILL, DrillParts.BASE_CAPSULE);
		var stack = ExtractionDrill.configuredStack(parts);
		var restored = ItemStack.parse(level.registryAccess(), stack.save(level.registryAccess())).orElseThrow();
		test.assertValueEqual(restored.get(Workstations.DRILL_PARTS.get()), parts, "Installed drill parts survive stack serialization");
		test.assertValueEqual(restored.getMaxDamage(), 650, "Parts determine maximum durability");
		test.assertTrue(G3dResources.SERVER.get(DrillParts.id("item/extraction_drill_laser_extractor")).isPresent(),
				"A fitted extractor has its own bundled rig");
		player.setItemInHand(InteractionHand.OFF_HAND, restored);
		var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
		var drill = Workstations.DRILL.get();
		test.assertValueEqual(drill.useOn(new UseOnContext(player, InteractionHand.OFF_HAND, hit)), InteractionResult.CONSUME,
				"Right-click an ore with the offhand drill to start extracting");
		var session = restored.get(Workstations.DRILL_EXTRACTION.get());
		test.assertValueEqual(session.markedBlocks().size(), 4, "Capsule capacity limits a five-ore vein to four targets");
		test.assertTrue(session.markedBlocks().contains(test.absolutePos(pos.north())), "Deepslate ore is marked with adjacent stone ores");

		test.setBlock(pos.west(), Blocks.DIRT);
		for (int remaining = session.duration(); remaining > 0; remaining--)
			drill.onUseTick(level, player, restored, remaining);
		test.assertBlockPresent(Blocks.STONE, pos);
		test.assertBlockPresent(Blocks.DEEPSLATE, pos.north());
		test.assertBlockPresent(Blocks.DIRT, pos.west());
		test.assertBlockPresent(Blocks.IRON_ORE, pos.east());
		test.assertValueEqual(restored.getDamageValue(), 3, "Only successfully extracted blocks wear the drill");
		test.assertTrue(player.getInventory().countItem(Items.RAW_IRON) >= 6, "The laser extractor multiplies and delivers ore drops");
		player.releaseUsingItem();
		test.assertTrue(!restored.has(Workstations.DRILL_EXTRACTION.get()), "Releasing the drill clears its active extraction");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void scrappingTableConsumesMaterialAndGuardsOutputs(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(1, 1, 1);
		var absolute = test.absolutePos(pos);
		test.setBlock(pos, Workstations.SCRAPPING_TABLE.get());
		var table = (ScrappingTable) level.getBlockEntity(absolute);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		player.setPos(Vec3.atBottomCenterOf(absolute));
		var menu = (ScrappingMenu) table.createMenu(1, player.getInventory(), player);
		player.containerMenu = menu;
		test.assertValueEqual(level.getRecipeManager().getAllRecipesFor(WorkstationRecipes.SCRAPPING.get()).size(), 19,
				"All pinned scrapping recipes loaded");
		test.assertTrue(!menu.clickMenuButton(player, 0), "An empty table cannot be used");
		player.getInventory().setItem(9, Workstations.CALIBRATOR.toStack());
		menu.quickMoveStack(player, 10);
		test.assertTrue(table.getItem(2).is(Workstations.CALIBRATOR.get()), "Shift-click selects the calibrator slot");
		table.setItem(3, GalaxiesItems.BROKEN_SMALL_POWER_PACK_ITEM.toStack());
		table.setItem(8, GalaxiesItems.IONITE_INGOT.toStack(64));
		test.assertTrue(!menu.clickMenuButton(player, 2), "A full primary output blocks work");
		table.setItem(8, ItemStack.EMPTY);
		for (int click = 0; click < 5; click++)
			test.assertTrue(menu.clickMenuButton(player, 2), "The calibrator accepts the matching scrap recipe");
		test.assertValueEqual(menu.progress(2), 480, "The calibrator is ready to complete");
		var tag = table.saveWithoutMetadata(level.registryAccess());
		table.loadWithComponents(tag, level.registryAccess());
		test.assertValueEqual(menu.progress(2), 480, "Active progress survives block entity reload");
		test.runAfterDelay(2, () -> {
			test.assertTrue(table.getItem(8).is(GalaxiesItems.IONITE_INGOT.get()), "Salvage reaches the calibrator primary output");
			test.assertValueEqual(table.getItem(3).get(CoreComponents.ENERGY_COMPONENT), 1, "The calibrator drains two energy");
			test.assertValueEqual(table.getItem(3).get(CoreComponents.TECH_COMPONENT), 1, "The calibrator drains one tech");
			test.assertValueEqual(table.getItem(2).getDamageValue(), 1, "A successful job wears the tool");
			for (int click = 0; click < 5; click++)
				menu.clickMenuButton(player, 2);
		});
		test.runAfterDelay(4, () -> {
			test.assertTrue(table.getItem(3).isEmpty() && table.getItem(8).getCount() == 2, "Depleted scrap is consumed exactly once");
			test.assertValueEqual(table.getItem(2).getDamageValue(), 2, "The tool survives both jobs");
			table.setItem(3, new ItemStack(Items.STICK));
			player.getInventory().setItem(9, GalaxiesItems.IONITE_INGOT.toStack());
			test.assertTrue(menu.quickMoveStack(player, 10).isEmpty() && table.getItem(8).getCount() == 2,
					"Shift-click cannot merge a player stack into the machine output");
			test.assertTrue(!menu.clickMenuButton(player, 2), "Once depleted, the player cannot produce more salvage");
			level.destroyBlock(absolute, false);
			test.assertItemEntityCountIs(GalaxiesItems.IONITE_INGOT.get(), pos, 2, 2);
			test.succeed();
		});
	}

	@GameTest(template = "port_test_empty")
	public static void laserCutterSweepsARealFaceAndUsesSalvageRecipes(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(2, 2, 3);
		var absolute = test.absolutePos(pos);
		var panel = GalaxiesBlocks.GRAY_IMPERIAL_PANEL_BLANK.block().get();
		test.setBlock(pos, panel);
		test.assertValueEqual(level.getRecipeManager().getAllRecipesFor(WorkstationRecipes.CUTTING.get()).size(), 105,
				"All pinned cutting recipes loaded");
		var recipe = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath("pswg_gadgets", "gray_imperial_panel_blank_cutting"))
				.orElseThrow().value();
		var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess());
		try
		{
			Recipe.STREAM_CODEC.encode(buffer, recipe);
			var restored = Recipe.STREAM_CODEC.decode(buffer);
			test.assertTrue(restored.getResultItem(level.registryAccess()).is(GalaxiesItems.DURASTEEL_INGOT.get()),
					"Client recipe sync preserves the cutter output");
		}
		finally
		{
			buffer.release();
		}

		var player = new ServerPlayer(level.getServer(), level,
				new GameProfile(UUID.randomUUID(), "cutter-test"), ClientInformation.createDefault());
		player.setPos(absolute.getX() + 0.5, absolute.getY() - 1, absolute.getZ() - 2);
		var cutter = Workstations.CUTTER.get();
		var stack = Workstations.CUTTER.toStack();
		player.setItemInHand(InteractionHand.OFF_HAND, stack);
		var first = new Vec3(absolute.getX() + 0.15, absolute.getY() + 0.15, absolute.getZ());
		var second = new Vec3(absolute.getX() + 0.85, absolute.getY() + 0.85, absolute.getZ());
		test.assertValueEqual(cutter.useOn(new UseOnContext(player, InteractionHand.OFF_HAND,
				new BlockHitResult(first, Direction.NORTH, absolute, false))), InteractionResult.CONSUME,
				"The offhand Fusioncutter starts holding the use action");

		aim(player, first);
		cutter.onUseTick(level, player, stack, 3600);
		test.assertBlockPresent(panel, pos);
		test.assertValueEqual(stack.get(Workstations.CUT_FACE.get()), Direction.NORTH, "A first sample binds the targeted face");

		aim(player, second);
		cutter.onUseTick(level, player, stack, 3599);
		test.assertBlockPresent(Blocks.AIR, pos);
		test.assertItemEntityCountIs(GalaxiesItems.DURASTEEL_INGOT.get(), pos, 2, 1);
		test.assertItemEntityCountIs(panel.asItem(), pos, 2, 0);
		test.assertValueEqual(stack.getDamageValue(), 1, "One completed cut wears the cutter once");
		test.assertTrue(!stack.has(Workstations.CUT_TARGET.get()), "A completed cut clears the active target");
		player.releaseUsingItem();

		test.setBlock(pos, Blocks.BEDROCK);
		cutter.useOn(new UseOnContext(player, InteractionHand.OFF_HAND, new BlockHitResult(first, Direction.NORTH, absolute, false)));
		test.assertBlockPresent(Blocks.BEDROCK, pos);
		test.assertValueEqual(stack.getDamageValue(), 1, "Unbreakable blocks cost no durability");
		test.succeed();
	}

	private static void aim(ServerPlayer player, Vec3 point)
	{
		var delta = point.subtract(player.getEyePosition());
		float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
		float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, delta.horizontalDistance()));
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(pitch);
		player.yHeadRotO = yaw;
		player.yRotO = yaw;
		player.xRotO = pitch;
	}
}
