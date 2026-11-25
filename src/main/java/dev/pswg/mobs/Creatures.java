package dev.pswg.mobs;

import dev.pswg.Galaxies;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.MobBucketItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public final class Creatures
{
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Galaxies.MODID);
	public static final TagKey<Item> BANTHA_FOOD = TagKey.create(Registries.ITEM, Galaxies.id("bantha_tempt_items"));
	public static final TagKey<Block> SKITTER_DIGGABLE = TagKey.create(Registries.BLOCK, Galaxies.id("sand_skitter_can_dig"));
	public static final Supplier<EntityType<Worrt>> WORRT = entity("worrt", Worrt::new, MobCategory.CREATURE, 0.6F, 0.6F, 8);
	public static final Supplier<EntityType<Bantha>> BANTHA = entity("bantha", Bantha::new, MobCategory.CREATURE, 2, 2.9F, 10);
	public static final Supplier<EntityType<SandSkitter>> SAND_SKITTER = entity("sand_skitter", SandSkitter::new, MobCategory.MISC, 0.4F, 0.3F, 8);
	public static final Supplier<EntityType<GalaxiesFish>> FAA = entity("faa", GalaxiesFish::new, MobCategory.WATER_AMBIENT, 0.5F, 0.3F, 8);
	public static final Supplier<EntityType<GalaxiesFish>> LAA = entity("laa", GalaxiesFish::new, MobCategory.WATER_AMBIENT, 0.5F, 0.3F, 8);
	public static final List<DroidType> DROIDS = List.of(droid("artoo_detoo", "r2d2"), droid("artoo_cuefive", "r2q5"),
			droid("artoo_kaype", "r2kp"), droid("artoo_arseven", "r2r7"), droid("artoo_wyten", "r2y10"), droid("cutee_katee", "qtkt"));
	public static final Supplier<MenuType<Astromech.Menu>> DROID_MENU = MENUS.register("astromech",
			() -> new MenuType<>(Astromech.Menu::new, FeatureFlags.DEFAULT_FLAGS));
	private static final List<Supplier<Item>> EGGS = List.of(egg("worrt", WORRT, 0x5b482c, 0x635735), egg("bantha", BANTHA, 0x362318, 0xd1b693),
			egg("sand_skitter", SAND_SKITTER, 0x4f3733, 0xe2946e), egg("faa", FAA, 0xe9933e, 0x9471eb), egg("laa", LAA, 0x2f5747, 0xd5bc92));
	public static final Supplier<Item> FAA_BUCKET = bucket("faa", FAA);
	public static final Supplier<Item> LAA_BUCKET = bucket("laa", LAA);

	private Creatures() {}

	private static <T extends Mob> Supplier<EntityType<T>> entity(String id, EntityType.EntityFactory<T> factory,
			MobCategory category, float width, float height, int tracking)
	{
		return ENTITIES.register(id, () -> EntityType.Builder.of(factory, category).sized(width, height)
				.clientTrackingRange(tracking).build(Galaxies.id(id).toString()));
	}

	private static DroidType droid(String id, String paint)
	{
		return new DroidType(entity(id, (type, world) -> new Astromech(type, world, paint), MobCategory.MISC, 0.75F, 1.2F, 8));
	}

	private static Supplier<Item> egg(String id, Supplier<? extends EntityType<? extends Mob>> entity, int base, int spots)
	{
		return ITEMS.register("spawn_" + id, () -> new DeferredSpawnEggItem(entity, base, spots, new Item.Properties()));
	}

	private static Supplier<Item> bucket(String id, Supplier<? extends EntityType<?>> entity)
	{
		return ITEMS.register(id + "_bucket", () -> new MobBucketItem(entity.get(), Fluids.WATER, SoundEvents.BUCKET_EMPTY_FISH, new Item.Properties().stacksTo(1)));
	}

	public static void register(IEventBus bus)
	{
		ENTITIES.register(bus);
		ITEMS.register(bus);
		MENUS.register(bus);
		bus.addListener(Creatures::attributes);
		bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS))
				EGGS.forEach(egg -> event.accept(egg.get()));
			if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))
			{
				event.accept(FAA_BUCKET.get());
				event.accept(LAA_BUCKET.get());
			}
		});
	}

	private static void attributes(EntityAttributeCreationEvent event)
	{
		event.put(WORRT.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.MOVEMENT_SPEED, 1).build());
		event.put(BANTHA.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 40).add(Attributes.MOVEMENT_SPEED, 0.1).build());
		event.put(SAND_SKITTER.get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 4).add(Attributes.MOVEMENT_SPEED, 0.2).add(Attributes.FOLLOW_RANGE, 8).build());
		event.put(FAA.get(), AbstractFish.createAttributes().build());
		event.put(LAA.get(), AbstractFish.createAttributes().build());
		DROIDS.forEach(droid -> event.put(droid.type().get(), Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8).build()));
	}

	public record DroidType(Supplier<EntityType<Astromech>> type) {}
}
