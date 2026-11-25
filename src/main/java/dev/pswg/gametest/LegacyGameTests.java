package dev.pswg.gametest;

import com.mojang.authlib.GameProfile;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterBolt;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.BlasterNetwork;
import dev.pswg.blasters.BlasterState;
import dev.pswg.blasters.BlasterWorkbench;
import dev.pswg.blasters.Blasters;
import dev.pswg.blasters.LegacyBlaster.FiringMode;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.container.GalaxiesItems;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.GadgetCharge;
import dev.pswg.gadgets.SmokeCloud;
import dev.pswg.jetpacks.JetpackNetwork;
import dev.pswg.jetpacks.Jetpacks;
import dev.pswg.mobs.Creatures;
import dev.pswg.mobs.Astromech;
import dev.pswg.rendering.p3d.P3dModel;
import dev.pswg.rendering.p3d.P3dResources;
import dev.pswg.sabers.SaberForge;
import dev.pswg.sabers.SaberItem;
import dev.pswg.sabers.SaberNetwork;
import dev.pswg.sabers.Sabers;
import dev.pswg.sabers.ThrownSaber;
import dev.pswg.species.Species;
import dev.pswg.species.SpeciesItems;
import dev.pswg.species.SpeciesNetwork;
import dev.pswg.utilities.CreatureCage;
import dev.pswg.utilities.LegacyUtilities;
import dev.pswg.utilities.MoistureVaporator;
import dev.pswg.utilities.PowerCoupling;
import dev.pswg.utilities.SlidingDoor;
import dev.pswg.vehicles.VehicleNetwork;
import dev.pswg.vehicles.Vehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder(Galaxies.MODID)
@PrefixGameTestTemplate(false)
public final class LegacyGameTests
{
	private static ServerPlayer serverPlayer(GameTestHelper test)
	{
		var level = test.getLevel();
		var player = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "legacy-test"), ClientInformation.createDefault());
		player.setPos(Vec3.atBottomCenterOf(test.absolutePos(new BlockPos(2, 1, 2))));
		return player;
	}

	@GameTest(template = "port_test_empty")
	public static void legacyBlastersLoadAndUsePowerPacksAndOriginalModes(GameTestHelper test) throws IOException
	{
		var level = test.getLevel();
		test.assertValueEqual(Blasters.PRESETS.size(), 17, "Every master blaster is registered");
		for (var entry : Blasters.PRESETS.entrySet())
		{
			var stack = entry.getValue().toStack();
			var definition = BlasterItem.definition(level, stack);
			test.assertTrue(definition != null && definition.legacy().isPresent() && !definition.legacy().get().modes().isEmpty(), "Loaded " + entry.getKey());
			try (var model = LegacyGameTests.class.getResourceAsStream("/assets/pswg/models/item/blaster/" + entry.getKey().getPath() + ".p3d"))
			{
				test.assertTrue(model != null && !P3dModel.read(model, true).roots().isEmpty(), "Original mesh " + entry.getKey());
			}
		}
		var player = serverPlayer(test);
		var stack = Blasters.PRESETS.get(Galaxies.id("dl44")).toStack();
		player.setItemInHand(InteractionHand.OFF_HAND, stack);
		stack.set(Blasters.SERIAL.get(), 32L);
		var press = new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.PRESS, 32L);
		var definition = BlasterItem.definition(level, stack);
		BlasterNetwork.handleAction(player, press);
		test.assertValueEqual(BlasterItem.state(stack).lastFired(), 0L, "An empty blaster dry-fires");
		player.getInventory().setItem(15, Blasters.SMALL_POWER_PACK.toStack(2));
		stack.set(Blasters.STATE.get(), BlasterState.DEFAULT);
		BlasterNetwork.handleAction(player, press);
		test.assertValueEqual(player.getInventory().getItem(15).getCount(), 1, "Reload consumes one power pack");
		int capacity = Math.min(75, definition.legacy().get().mechanics().magazine());
		test.assertValueEqual(stack.get(Blasters.AMMO.get()), capacity - 1, "The magazine loses exactly one round");
		long now = level.getGameTime();
		var bolts = level.getEntitiesOfClass(BlasterBolt.class, new AABB(player.blockPosition()).inflate(5));
		test.assertValueEqual(bolts.size(), 1, "A semi-automatic press launches one tracked bolt");
		test.assertValueEqual(bolts.getFirst().color(), definition.legacy().get().beam().color(), "The bolt uses the original preset color");
		test.assertValueEqual(bolts.getFirst().mode(), FiringMode.SEMI_AUTOMATIC, "The bolt uses the original firing mode");
		((BlasterItem) stack.getItem()).use(level, player, InteractionHand.OFF_HAND);
		test.assertTrue(BlasterItem.state(stack).isAiming(), "Offhand aiming enters ADS");
		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.PATROL, 32L));
		test.assertTrue(player.getData(Blasters.PATROL) && !BlasterItem.state(stack).isAiming(), "Patrol lowers the gun and clears ADS");
		BlasterNetwork.handleAction(player, new BlasterNetwork.Action(InteractionHand.OFF_HAND, BlasterNetwork.Input.VENT, 32L));
		test.assertValueEqual(BlasterItem.state(stack).mode(), BlasterState.Mode.REQUESTED_BYPASS, "Manual vent keeps its original cooling mode");
		float heat = BlasterItem.heat(stack, definition, now, 0);
		test.assertTrue(heat > 0 && BlasterItem.heat(stack, definition, now + 150, 0) == 0, "Venting drains the legacy heat pool");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void scopeWorkbenchAndLightsaberForgeRoundTrip(GameTestHelper test) throws IOException
	{
		var level = test.getLevel();
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var pos = new BlockPos(2, 1, 2);
		player.setPos(Vec3.atBottomCenterOf(test.absolutePos(pos.east())));
		test.setBlock(pos, BlasterWorkbench.BLOCK.get());
		var bench = (BlasterWorkbench) level.getBlockEntity(test.absolutePos(pos));
		var gun = Blasters.PRESETS.get(Galaxies.id("dlt19")).toStack();
		bench.setItem(0, gun);
		var menu = (BlasterWorkbench.Menu) bench.createMenu(4, player.getInventory(), player);
		var scope = Galaxies.id("dlt19/scope_x");
		var original = gun.copy();
		var preview = original.copy();
		test.assertTrue(BlasterWorkbench.toggleAttachment(preview, menu.definition(), scope), "A scope can be staged");
		test.assertTrue(ItemStack.matches(original, bench.getItem(0)), "Staging leaves the live stack untouched");
		var staged = BlasterItem.attachments(preview, menu.definition());
		test.assertTrue(!menu.build(player, original, staged), "A closed menu rejects workbench edits");
		player.containerMenu = menu;
		var invalid = new Blasters.EquippedAttachments(staged.hud(), Map.of(Galaxies.id("invalid_slot"), scope));
		test.assertTrue(!menu.build(player, original, invalid), "The server rejects illegal slot mappings");
		test.assertTrue(menu.build(player, original, staged), "Build fits the staged scope");
		test.assertTrue(!menu.build(player, original, staged), "A stale preview cannot overwrite a newer build");
		var ee3 = Blasters.PRESETS.get(Galaxies.id("ee3")).toStack();
		var ee3Definition = BlasterItem.definition(level, ee3);
		var stock = Galaxies.id("ee3/stock_metal");
		test.assertTrue(BlasterWorkbench.toggleAttachment(ee3, ee3Definition, stock)
						&& BlasterWorkbench.toggleAttachment(ee3, ee3Definition, stock)
						&& BlasterItem.attachments(ee3, ee3Definition).applied().containsValue(Galaxies.id("ee3/stock_wood")),
				"Removing a staged alternate in a required slot restores the original default");
		var data = bench.saveWithoutMetadata(level.registryAccess());
		bench.loadWithComponents(data, level.registryAccess());
		test.assertTrue(BlasterItem.hasFunction(bench.getItem(0), menu.definition(), "sniper_scope"), "The fitted scope survives block-entity reload");

		test.assertValueEqual(Sabers.ITEMS_BY_HILT.size(), 23, "Every master hilt is registered");
		for (var entry : Sabers.ITEMS_BY_HILT.entrySet())
		{
			try (var model = LegacyGameTests.class.getResourceAsStream("/assets/pswg/models/item/lightsaber/" + entry.getKey() + ".p3d"))
			{
				test.assertTrue(model != null && !P3dModel.read(model, true).roots().isEmpty(), "Original hilt mesh " + entry.getKey());
			}
		}
		test.setBlock(pos, Sabers.FORGE.get());
		var forge = (SaberForge) level.getBlockEntity(test.absolutePos(pos));
		var forgeMenu = (SaberForge.Menu) forge.createMenu(5, player.getInventory(), player);
		player.containerMenu = forgeMenu;
		var blade = Sabers.ITEMS_BY_HILT.get("kylo").toStack();
		forgeMenu.getSlot(0).set(blade);
		test.assertTrue(!forgeMenu.clickMenuButton(player, -1), "The forge rejects invalid settings");
		test.assertTrue(forgeMenu.clickMenuButton(player, SaberForge.EDIT_FLAG | SaberForge.UNSTABLE_FLAG | 0x0088ff), "The forge sets color and unstable style");
		var reloaded = ItemStack.parse(level.registryAccess(), blade.save(level.registryAccess())).orElseThrow();
		test.assertTrue(SaberItem.state(reloaded).unstable() && SaberItem.state(reloaded).color() == 0x0088ff, "Blade settings survive stack save");
		var fighter = serverPlayer(test);
		fighter.setItemInHand(InteractionHand.OFF_HAND, reloaded);
		SaberNetwork.handle(fighter, new SaberNetwork.Action(InteractionHand.OFF_HAND, false));
		test.assertTrue(SaberItem.state(reloaded).active(), "An offhand blade ignites through the validated action");
		SaberNetwork.handle(fighter, new SaberNetwork.Action(InteractionHand.MAIN_HAND, false));
		test.assertTrue(SaberItem.state(reloaded).active(), "A stale hand packet leaves the active saber alone");
		((SaberItem) reloaded.getItem()).use(level, fighter, InteractionHand.OFF_HAND);
		test.assertTrue(SaberItem.blocks(fighter), "The lit offhand saber can guard");
		player.containerMenu = player.inventoryMenu;
		player.setItemInHand(InteractionHand.MAIN_HAND, blade.copy());
		SaberItem.toggle(level, player, player.getMainHandItem());
		var secondary = serverPlayer(test);
		secondary.setItemInHand(InteractionHand.MAIN_HAND, player.getMainHandItem());
		SaberNetwork.handle(secondary, new SaberNetwork.Action(InteractionHand.MAIN_HAND, true));
		test.assertTrue(!secondary.getMainHandItem().isEmpty()
						&& level.getEntitiesOfClass(ThrownSaber.class, new AABB(secondary.blockPosition()).inflate(4)).isEmpty(),
				"The released secondary saber action leaves the hilt in hand");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void shipRigsSeatsAndOrientationPersist(GameTestHelper test)
	{
		var level = test.getLevel();
		var xwing = P3dResources.RIGS.get(Galaxies.id("ship/xwing_t65b"));
		for (String emitter : List.of("Cannon", "Engine"))
			for (String wing : List.of("TopLeft", "TopRight", "BottomLeft", "BottomRight"))
				test.assertTrue(xwing.transform(emitter + wing) != null, "The X-wing rig has wake source " + emitter + wing);
		var pos = new BlockPos(2, 1, 2);
		for (var kind : Vehicles.Kind.values())
		{
			var rig = P3dResources.RIGS.get(Galaxies.id("ship/" + kind.id));
			test.assertTrue(rig != null && !rig.roots().isEmpty(), "Loaded the original rig " + kind.id);
			var ship = Vehicles.type(kind).create(level);
			ship.setPos(Vec3.atBottomCenterOf(test.absolutePos(pos)));
			ship.setHeading(73);
			var pilot = test.makeMockPlayer(GameType.SURVIVAL);
			var rider = test.makeMockPlayer(GameType.SURVIVAL);
			test.assertTrue(pilot.startRiding(ship), "Player can mount " + kind.id);
			test.assertValueEqual(rider.startRiding(ship), kind.seats == 2, "Seat count " + kind.id);
			VehicleNetwork.handle(serverPlayer(test), new VehicleNetwork.Control(ship.getId(), VehicleNetwork.FORWARD, 45, 0));
			test.assertValueEqual(ship.throttle(), 0F, "A non-pilot cannot apply throttle");
			var saved = ship.saveWithoutId(new CompoundTag());
			var restored = Vehicles.type(kind).create(level);
			restored.load(saved);
			test.assertTrue(restored.orientation(1).equals(ship.orientation(1), 0.0001F), "Quaternion persists " + kind.id);
			pilot.stopRiding();
			rider.stopRiding();
			test.assertTrue(ship.getPassengers().isEmpty(), "Dismount clears all seats");
		}
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void jetpackSlotPersistsAndRejectsInvalidFlight(GameTestHelper test)
	{
		var player = serverPlayer(test);
		var armor = new ItemStack(Items.IRON_CHESTPLATE);
		player.setItemSlot(EquipmentSlot.CHEST, armor);
		JetpackNetwork.handle(player, new JetpackNetwork.Input(JetpackNetwork.ASCEND));
		NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
		test.assertTrue(!Jetpacks.authorizedFlight(player), "A client cannot request thrust without a pack");
		var pack = Jetpacks.PACKS.getFirst().toStack();
		Jetpacks.equip(player, pack);
		var saved = player.saveWithoutId(new CompoundTag());
		var copy = serverPlayer(test);
		copy.load(saved);
		test.assertTrue(ItemStack.matches(Jetpacks.equipped(copy), pack) && copy.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
				"The attached pack and chest armor coexist after reload");
		player.fallDistance = 6;
		JetpackNetwork.handle(player, new JetpackNetwork.Input(JetpackNetwork.ASCEND));
		NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
		test.assertTrue(Jetpacks.authorizedFlight(player) && player.getDeltaMovement().y > 0 && player.fallDistance == 0,
				"Fresh input generates thrust and arrests fall damage");
		JetpackNetwork.handle(player, new JetpackNetwork.Input(JetpackNetwork.DESCEND));
		NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
		test.assertTrue(!Jetpacks.authorizedFlight(player), "Descending releases thrust");
		Jetpacks.equip(player, ItemStack.EMPTY);
		test.assertTrue(Jetpacks.equipped(player).isEmpty() && ItemStack.matches(armor, player.getItemBySlot(EquipmentSlot.CHEST)), "Removing the pack leaves chest armor equipped");
		test.assertValueEqual(Jetpacks.ACCESSORIES.size(), 11, "Every released decorative back item is registered");
		for (var item : Jetpacks.ACCESSORIES)
		{
			Jetpacks.equip(player, item.toStack());
			copy.load(player.saveWithoutId(new CompoundTag()));
			test.assertTrue(Jetpacks.equipped(copy).is(item.get()) && copy.getItemBySlot(EquipmentSlot.CHEST).is(Items.IRON_CHESTPLATE),
					"Back equipment survives reload alongside chest armor");
			JetpackNetwork.handle(player, new JetpackNetwork.Input(JetpackNetwork.ASCEND));
			NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
			test.assertTrue(!Jetpacks.authorizedFlight(player), "Decorative equipment cannot grant thrust");
		}
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void releasedCratesAndClusterLightCraft(GameTestHelper test)
	{
		var level = test.getLevel();
		var crate = new BlockPos(2, 1, 2);
		for (String name : List.of("segmented_crate", "gray_segmented_crate", "gray_panel_crate"))
		{
			var block = BuiltInRegistries.BLOCK.get(Galaxies.id(name));
			test.setBlock(crate, block);
			var storage = (Container) level.getBlockEntity(test.absolutePos(crate));
			test.assertTrue(storage != null && storage.getContainerSize() == 15, "The released ID opens 15 slots: " + name);
			test.assertValueEqual(BuiltInRegistries.ITEM.getKey(block.asItem()), Galaxies.id(name), "The crate has its released item ID");
			var state = level.getBlockState(test.absolutePos(crate));
			var east = state.setValue(BlockStateProperties.FACING, Direction.EAST).getShape(level, test.absolutePos(crate)).bounds();
			var north = state.setValue(BlockStateProperties.FACING, Direction.NORTH).getShape(level, test.absolutePos(crate)).bounds();
			test.assertTrue(east.getZsize() == 1.75 && north.getXsize() == 1.75, "The released long bounds rotate with placement");
		}
		var metal = GalaxiesItems.PLASTEEL_INGOT.toStack();
		var panel = GalaxiesItems.LIGHT_PANEL.toStack();
		var input = CraftingInput.of(3, 3, List.of(ItemStack.EMPTY, metal, ItemStack.EMPTY,
				metal, panel, metal, ItemStack.EMPTY, metal, ItemStack.EMPTY));
		var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
		test.assertValueEqual(recipe.id(), Galaxies.id("tarkin/wall_cluster_light"), "The original recipe ID is loaded");
		var result = recipe.value().assemble(input, level.registryAccess());
		test.assertTrue(result.is(GalaxiesBlocks.WALL_CLUSTER_LIGHT.get().asItem()) && result.getCount() == 4, "Original crafting output");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void releasedCreatureMovementAndPaint(GameTestHelper test)
	{
		for (int x = 0; x < 8; x++)
			for (int z = 0; z < 8; z++)
				test.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
		var worrt = test.spawn(Creatures.WORRT.get(), new BlockPos(2, 1, 2));
		var skitter = test.spawn(Creatures.SAND_SKITTER.get(), new BlockPos(2, 1, 3));
		var droid = test.spawn(Creatures.DROIDS.getFirst().type().get(), new BlockPos(2, 1, 6));
		var tag = droid.saveWithoutId(new CompoundTag());
		var paint = new Astromech.Paint("r", false, "r2d2", 0xff00ff, 0xffff00, 0x00ffff);
		var parameters = new CompoundTag();
		parameters.putString("series", paint.series());
		parameters.putBoolean("usingPaintPreset", paint.usingPaintPreset());
		parameters.putString("paintPreset", paint.paintPreset());
		parameters.putInt("baseTint", paint.baseTint());
		parameters.putInt("domeTint", paint.domeTint());
		parameters.putInt("paintTint", paint.paintTint());
		tag.put("pswg:astromech_parameters", parameters);
		droid.load(tag);
		var copy = Creatures.DROIDS.getFirst().type().get().create(test.getLevel());
		copy.load(droid.saveWithoutId(new CompoundTag()));
		test.assertValueEqual(copy.paint(), paint, "The released custom paint NBT survives reload");
		test.assertValueEqual(worrt.getAttributeValue(Attributes.MOVEMENT_SPEED), 1D, "Released Worrt movement attribute");
		var hopTarget = test.absolutePos(new BlockPos(1, 1, 2));
		worrt.getMoveControl().setWantedPosition(hopTarget.getX() + 0.5, hopTarget.getY(), hopTarget.getZ() + 0.5, 0.7);
		worrt.jumpFromGround();
		test.assertTrue(worrt.getDeltaMovement().y > 0.3, "Worrt uses the released speed-dependent hop");
		var start = droid.position();
		var target = test.absolutePos(new BlockPos(6, 1, 6));
		droid.setOnGround(true);
		test.assertTrue(droid.getNavigation().moveTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 0.2),
				"The droid finds a route");
		test.runAfterDelay(12, () -> {
			test.assertTrue(droid.extension(1) > 0 && droid.extension(1) < 1, "The center leg is extending");
			test.assertTrue(droid.position().distanceToSqr(start) < 0.01, "Leg extension precedes driving");
			test.assertTrue(skitter.isAlive() && worrt.getTarget() == null, "The Worrt leaves nearby skitters alone");
		});
		test.runAfterDelay(40, () -> {
			test.assertTrue(droid.position().distanceToSqr(start) > 0.05, "The droid travels once its leg is extended");
			test.succeed();
		});
	}

	@GameTest(template = "port_test_empty")
	public static void nerveCloudAccumulatesDoseAndPersists(GameTestHelper test)
	{
		var level = test.getLevel();
		var center = new BlockPos(2, 2, 2);
		for (var face : Direction.values()) test.setBlock(center.relative(face), Blocks.OBSIDIAN);
		var target = test.spawn(EntityType.PIG, center);
		target.setNoAi(true);
		var grenade = GadgetCharge.create(Demolitions.Variant.NERVE, level, Vec3.atBottomCenterOf(test.absolutePos(center.below())), null);
		grenade.arm(0);
		level.addFreshEntity(grenade);
		test.runAfterDelay(12, () -> {
			var gas = level.getEntitiesOfClass(SmokeCloud.class, new AABB(test.absolutePos(center)).inflate(3)).getFirst();
			test.assertTrue(target.hasEffect(Demolitions.INTOXICATED), "Nerve gas applies the registered intoxication effect after exposure");
			var tag = gas.saveWithoutId(new CompoundTag());
			test.assertTrue(!tag.getList("Cells", CompoundTag.TAG_COMPOUND).isEmpty() && !tag.getList("Exposure", CompoundTag.TAG_COMPOUND).isEmpty(),
					"Cloud cells and accumulated dose serialize");
			test.succeed();
		});
	}

	@GameTest(template = "port_test_empty")
	public static void infernoCharsExposedBlocksAndTripwireUsesMountedFace(GameTestHelper test)
	{
		var level = test.getLevel();
		var center = new BlockPos(3, 2, 3);
		var charred = center.north();
		test.setBlock(charred, Blocks.OAK_LOG);
		var grenade = GadgetCharge.create(Demolitions.Variant.INFERNO, level, Vec3.atBottomCenterOf(test.absolutePos(center)), null);
		grenade.arm(0);
		level.addFreshEntity(grenade);
		var wall = new BlockPos(0, 2, 1);
		test.setBlock(wall, Blocks.OBSIDIAN);
		var from = Vec3.atCenterOf(test.absolutePos(wall)).add(0.95, 0, 0);
		var tripwire = GadgetCharge.create(Demolitions.Variant.TRIPWIRE, level, from, null);
		tripwire.setDeltaMovement(new Vec3(-0.6, 0, 0));
		level.addFreshEntity(tripwire);
		test.runAfterDelay(12, () -> {
			test.assertBlockPresent(Demolitions.CHARRED.get(), charred);
			test.assertValueEqual(tripwire.mountedFace(), Direction.EAST, "Tripwire attaches to the impact face");
			test.setBlock(wall.east(2), Blocks.OBSIDIAN);
			tripwire.arm(0);
		});
		test.runAfterDelay(15, () -> {
			test.assertTrue(Math.abs(tripwire.beamLength() - 0.99F) < 0.02F,
					"The beam stops at the obstructing block while the tripwire stays armed");
			test.setBlock(wall.east(2), Blocks.AIR);
			var crossing = test.spawn(EntityType.ARMOR_STAND, wall.east(2));
			crossing.setPos(Vec3.atBottomCenterOf(test.absolutePos(wall.east(2))));
		});
		test.runAfterDelay(18, () -> {
			test.assertTrue(tripwire.isDetonating() || !tripwire.isAlive(), "A player-sized entity crossing the beam triggers the mine");
			test.succeed();
		});
	}

	@GameTest(template = "port_test_empty")
	public static void speciesMannequinAndCreatureStatePersists(GameTestHelper test)
	{
		var level = test.getLevel();
		var player = serverPlayer(test);
		test.assertValueEqual(Species.PROFILES.size(), 16, "All playable master species loaded");
		test.assertValueEqual(Species.appearance(player), Species.DEFAULT, "New players keep their Minecraft skin");
		var look = new Species.Appearance("togruta", true, Map.of("body", "teal", "face", "5"));
		SpeciesNetwork.select(player, player.getId(), look);
		SpeciesNetwork.select(player, player.getId(), new Species.Appearance("togruta", true, Map.of("body", "invalid")));
		var loaded = serverPlayer(test);
		loaded.load(player.saveWithoutId(new CompoundTag()));
		test.assertValueEqual(Species.appearance(loaded), look, "A validated appearance survives reload and invalid choices are ignored");
		var stand = test.spawn(SpeciesItems.TYPE.get(), new BlockPos(2, 1, 2));
		SpeciesNetwork.select(player, stand.getId(), look);
		test.assertValueEqual(stand.appearance(), Species.DEFAULT, "Only a held wizard can edit the mannequin");
		player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(SpeciesItems.WIZARD.get()));
		SpeciesNetwork.select(player, stand.getId(), look);
		var reloaded = SpeciesItems.TYPE.get().create(level);
		reloaded.load(stand.saveWithoutId(new CompoundTag()));
		test.assertValueEqual(reloaded.appearance(), look, "Mannequin appearance persists separately from its owner");
		for (var type : Creatures.DROIDS)
		{
			var droid = type.type().get().create(level);
			droid.cargo().setItem(3, new ItemStack(Items.IRON_NUGGET, 4));
			var copy = type.type().get().create(level);
			copy.load(droid.saveWithoutId(new CompoundTag()));
			test.assertTrue(copy.paint().equals(droid.paint()) && copy.cargo().getItem(3).getCount() == 4, "Droid livery and cargo reload");
		}
		var bantha = Creatures.BANTHA.get().create(level);
		test.assertTrue(bantha.getBreedOffspring(level, bantha).playerBred(), "Bred banthas accept riders");
		test.assertTrue(Creatures.FAA.get().create(level).getBucketItemStack().is(Creatures.FAA_BUCKET.get()), "Faa uses its own bucket");
		test.assertTrue(Creatures.LAA.get().create(level).getBucketItemStack().is(Creatures.LAA_BUCKET.get()), "Laa uses its own bucket");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void vaporatorRestoresInputAndProgress(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(2, 1, 2);
		test.setBlock(pos, LegacyUtilities.VAPORATOR.get());
		var machine = (MoistureVaporator.Inventory) level.getBlockEntity(test.absolutePos(pos));
		var book = level.getRecipeManager().getAllRecipesFor(LegacyUtilities.VAPORATING.get());
		test.assertValueEqual(book.size(), 2, "Both legacy vaporator recipes are discoverable");
		var farm = pos.east().below();
		test.setBlock(farm, Blocks.FARMLAND);
		var farmland = level.getBlockState(test.absolutePos(farm));
		farmland.randomTick(level, test.absolutePos(farm), level.random);
		test.assertValueEqual(level.getBlockState(test.absolutePos(farm)).getValue(net.minecraft.world.level.block.FarmBlock.MOISTURE), 7,
				"The vaporator irrigates nearby farmland");
		test.assertTrue(!machine.canPlaceItem(0, new ItemStack(Items.STICK)), "Unrelated items cannot enter input");
		machine.setItem(0, new ItemStack(Items.GLASS_BOTTLE));
		var before = level.getBlockState(test.absolutePos(pos));
		for (int tick = 0; tick < 245; tick++) MoistureVaporator.Inventory.tick(level, test.absolutePos(pos), before, machine);
		var snapshot = machine.saveWithoutMetadata(level.registryAccess());
		machine.loadWithComponents(snapshot, level.registryAccess());
		test.assertValueEqual(machine.progress(), 245, "Partly filled container progress persists");
		for (int tick = 245; tick < 400; tick++) MoistureVaporator.Inventory.tick(level, test.absolutePos(pos), before, machine);
		test.assertTrue(machine.getItem(0).isEmpty() && machine.getItem(1).get(DataComponents.POTION_CONTENTS).is(Potions.WATER),
				"A full original collection cycle makes one water bottle");
		test.succeed();
	}

	@GameTest(template = "port_test_empty")
	public static void cableTransfersPowerAndDoorAppliesInsertAndRedstone(GameTestHelper test)
	{
		var level = test.getLevel();
		var source = new BlockPos(1, 1, 1);
		var target = new BlockPos(4, 1, 1);
		var state = LegacyUtilities.COUPLING.get().defaultBlockState().setValue(PowerCoupling.FACING, Direction.UP);
		for (var pos : new BlockPos[] { source, target })
		{
			test.setBlock(pos.below(), Blocks.STONE);
			test.setBlock(pos, state);
		}
		var from = (PowerCoupling.Links) level.getBlockEntity(test.absolutePos(source));
		var to = (PowerCoupling.Links) level.getBlockEntity(test.absolutePos(target));
		test.assertTrue(from.connect(to) && !to.connect(from), "Links are directed and prevent an immediate reverse duplicate");
		from.loadWithComponents(from.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
		to.loadWithComponents(to.saveWithoutMetadata(level.registryAccess()), level.registryAccess());
		test.assertTrue(from.outgoing().contains(test.absolutePos(target)), "A cable target survives block-entity reload");
		test.setBlock(source.north(), Blocks.REDSTONE_BLOCK);
		var door = new BlockPos(2, 1, 3);
		test.setBlock(door.below(), Blocks.STONE);
		test.setBlock(door, LegacyUtilities.SLIDING_DOOR.get());
		LegacyUtilities.SLIDING_DOOR.get().setPlacedBy(level, test.absolutePos(door), level.getBlockState(test.absolutePos(door)), null, ItemStack.EMPTY);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		var insert = BuiltInRegistries.ITEM.get(Galaxies.id("red_door_insert"));
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(insert));
		test.useBlock(door.above(), player);
		var upper = level.getBlockState(test.absolutePos(door.above()));
		test.assertTrue(upper.getValue(SlidingDoor.HALF) == DoubleBlockHalf.UPPER && SlidingDoor.color(upper) == DyeColor.RED && !upper.getValue(SlidingDoor.OPEN),
				"An insert placed in the upper half updates and closes both halves");
		test.assertTrue(player.getMainHandItem().isEmpty(), "Survival consumes one door panel");
		test.setBlock(door.west(), Blocks.REDSTONE_BLOCK);
		test.assertTrue(level.getBlockState(test.absolutePos(door.above())).getValue(SlidingDoor.OPEN), "Redstone opens both door halves");
		test.runAfterDelay(8, () -> {
			test.assertValueEqual(level.getBlockState(test.absolutePos(target)).getValue(PowerCoupling.POWER), 15, "Redstone reaches a reloaded coupling");
			test.setBlock(source.north(), Blocks.AIR);
		});
		test.runAfterDelay(15, () -> {
			test.assertValueEqual(level.getBlockState(test.absolutePos(target)).getValue(PowerCoupling.POWER), 0, "A powered link clears when its input is removed");
			test.setBlock(door.west(), Blocks.AIR);
			test.assertTrue(!level.getBlockState(test.absolutePos(door)).getValue(SlidingDoor.OPEN), "Removing the signal closes the door");
			test.succeed();
		});
	}

	@GameTest(template = "port_test_empty")
	public static void occupiedTerrariumCarriesWaterAndReleasesFish(GameTestHelper test)
	{
		var level = test.getLevel();
		var pos = new BlockPos(2, 1, 2);
		var place = pos.east(2);
		var absolute = test.absolutePos(pos);
		var player = test.makeMockPlayer(GameType.SURVIVAL);
		test.setBlock(pos, LegacyUtilities.TERRARIUM.get());
		player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Creatures.FAA_BUCKET.get()));
		test.useBlock(pos, player);
		var cage = (CreatureCage.Contents) level.getBlockEntity(absolute);
		test.assertTrue(cage.occupied() && player.getMainHandItem().is(Items.BUCKET), "A fish bucket fills the terrarium, stores the fish, and returns the bucket");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.setShiftKeyDown(true);
		test.useBlock(pos, player);
		test.assertBlockNotPresent(LegacyUtilities.TERRARIUM.get(), pos);
		var packed = player.getInventory().getItem(0);
		test.assertTrue(packed.has(DataComponents.BLOCK_ENTITY_DATA) && packed.has(DataComponents.BLOCK_STATE), "The carried item stores water and fish");
		var target = test.absolutePos(place);
		test.setBlock(place.below(), Blocks.STONE);
		player.setShiftKeyDown(false);
		player.setPos(Vec3.atBottomCenterOf(target.north()));
		var hit = new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(target.below()), Direction.UP, target.below(), false);
		packed.useOn(new net.minecraft.world.item.context.UseOnContext(player, InteractionHand.MAIN_HAND, hit));
		var replacement = (CreatureCage.Contents) level.getBlockEntity(target);
		test.assertTrue(replacement != null && replacement.occupied() && level.getBlockState(target).getValue(CreatureCage.Terrarium.WATER) == 9, "Placing the item restores the contents and water");
		test.useBlock(place, player);
		test.assertTrue(!replacement.occupied(), "Releasing the fish clears the cage");
		test.assertEntityPresent(Creatures.FAA.get(), place.above());
		test.succeed();
	}
}
