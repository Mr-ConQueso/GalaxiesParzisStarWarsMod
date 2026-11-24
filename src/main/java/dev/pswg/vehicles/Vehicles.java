package dev.pswg.vehicles;

import dev.pswg.Galaxies;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;

public final class Vehicles
{
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Galaxies.MODID);
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Galaxies.MODID);
	private static final Map<Kind, Supplier<EntityType<Ship>>> TYPES = new EnumMap<>(Kind.class);
	private static final Map<Kind, DeferredItem<Item>> SPAWNERS = new EnumMap<>(Kind.class);

	public static final Supplier<SoundEvent> ENGINE = SOUNDS.register("ship.exterior.xwingt65b",
			() -> SoundEvent.createVariableRangeEvent(Galaxies.id("ship.exterior.xwingt65b")));
	public static final Supplier<SoundEvent> CANNON = SOUNDS.register("ship.blaster.fire.xwingt65b",
			() -> SoundEvent.createVariableRangeEvent(Galaxies.id("ship.blaster.fire.xwingt65b")));

	static
	{
		for (Kind kind : Kind.values())
		{
			TYPES.put(kind, ENTITIES.register(kind.id, () -> EntityType.Builder.<Ship>of((type, level) -> new Ship(type, level, kind), MobCategory.MISC)
					.sized(1F, 1F).eyeHeight(0.8F).clientTrackingRange(8).updateInterval(1).build(Galaxies.id(kind.id).toString())));
			SPAWNERS.put(kind, ITEMS.register("spawn_" + kind.id, () -> new Spawner(kind)));
		}
		TABS.register("vehicles", () -> CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.pswg.vehicles")).icon(() -> spawner(Kind.X_WING).toStack())
				.displayItems((params, out) -> SPAWNERS.values().forEach(item -> out.accept(item.get()))).build());
	}

	private Vehicles() {}

	public static void register(IEventBus bus)
	{
		ENTITIES.register(bus);
		ITEMS.register(bus);
		TABS.register(bus);
		SOUNDS.register(bus);
		bus.addListener(VehicleNetwork::register);
	}

	public static EntityType<Ship> type(Kind kind) { return TYPES.get(kind).get(); }
	public static DeferredItem<Item> spawner(Kind kind) { return SPAWNERS.get(kind); }

	public enum Kind
	{
		X_WING("xwing_t65b", 1, 0), LANDSPEEDER("landspeeder_x34", 2, 0.4), ZEPHYR("zephyr_j", 1, 0);

		public final String id;
		public final int seats;
		public final double hover;

		Kind(String id, int seats, double hover)
		{
			this.id = id;
			this.seats = seats;
			this.hover = hover;
		}

		public boolean flying() { return this == X_WING; }

		Vec3 seat(int index)
		{
			return switch (this)
			{
				case X_WING -> new Vec3(0, 0, 3);
				case LANDSPEEDER -> new Vec3(index == 0 ? -0.5 : 0.5, 0.1, 1.25);
				case ZEPHYR -> new Vec3(0, 0.13, 0.45);
			};
		}
	}

	private static final class Spawner extends Item
	{
		private final Kind kind;

		private Spawner(Kind kind)
		{
			super(new Properties());
			this.kind = kind;
		}

		@Override
		public InteractionResult useOn(UseOnContext context)
		{
			if (context.getPlayer() == null)
				return InteractionResult.PASS;
			var level = context.getLevel();
			if (!level.isClientSide)
			{
				var ship = type(kind).create(level);
				if (ship == null)
					return InteractionResult.FAIL;
				var point = Vec3.atBottomCenterOf(context.getClickedPos().relative(context.getClickedFace())).add(0, kind.flying() ? 3 : 0, 0);
				ship.setPos(point);
				ship.setHeading(context.getRotation());
				if (!ship.hullClear(point) || !level.addFreshEntity(ship))
					return InteractionResult.FAIL;
				context.getItemInHand().consume(1, context.getPlayer());
			}
			return InteractionResult.sidedSuccess(level.isClientSide);
		}
	}
}
