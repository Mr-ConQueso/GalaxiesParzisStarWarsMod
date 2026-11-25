package dev.pswg.blasters;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pswg.Galaxies;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Supplier;

public final class Blasters
{
	public static final String NAMESPACE = "pswg_blasters";
	public static final ResourceLocation DEFAULT_HUD = id("default");
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NAMESPACE);
	private static final DeferredRegister.Items LEGACY_ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Galaxies.MODID);
	private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NAMESPACE);
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NAMESPACE);
	private static final DeferredRegister<net.neoforged.neoforge.attachment.AttachmentType<?>> PLAYER_STATES =
			DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NAMESPACE);
	private static final DeferredRegister<SoundEvent> LEGACY_SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Galaxies.MODID);
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NAMESPACE);

	public static final Supplier<net.neoforged.neoforge.attachment.AttachmentType<Boolean>> PATROL = PLAYER_STATES.register("patrol_posture",
			() -> net.neoforged.neoforge.attachment.AttachmentType.builder(() -> false).build());
	public static final ResourceLocation MISSING = id("missingno");
	public static final Supplier<DataComponentType<ResourceLocation>> ID =
			COMPONENTS.registerComponentType("id", builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
	public static final Supplier<DataComponentType<Long>> SERIAL =
			COMPONENTS.registerComponentType("serial", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
	public static final Supplier<DataComponentType<BlasterState>> STATE =
			COMPONENTS.registerComponentType("state", builder -> builder.persistent(BlasterState.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(BlasterState.CODEC)));
	public static final Supplier<DataComponentType<EquippedAttachments>> ATTACHMENTS =
			COMPONENTS.registerComponentType("attachments", builder -> builder.persistent(EquippedAttachments.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(EquippedAttachments.CODEC)));

	public static final Supplier<DataComponentType<LegacyBlaster.FiringMode>> FIRING_MODE = COMPONENTS.registerComponentType("firing_mode",
			builder -> builder.persistent(StringRepresentable.fromEnum(LegacyBlaster.FiringMode::values)));
	public static final Supplier<DataComponentType<Integer>> AMMO = COMPONENTS.registerComponentType("rounds_remaining",
			builder -> builder.persistent(Codec.intRange(0, 4096)).networkSynchronized(ByteBufCodecs.VAR_INT));
	public static final Supplier<DataComponentType<Long>> READY = COMPONENTS.registerComponentType("ready_time",
			builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
	public static final Map<ResourceLocation, DeferredItem<BlasterItem>> PRESETS = legacyPresets();
	public static final DeferredItem<Item> SMALL_POWER_PACK = LEGACY_ITEMS.registerSimpleItem("small_power_pack");

	public static final DeferredItem<BlasterItem> ITEM = ITEMS.register("blaster",
			() -> new BlasterItem(new Item.Properties().stacksTo(1).component(ID.get(), MISSING).component(STATE.get(), BlasterState.DEFAULT)));
	// Complex spawn data keeps the exact speed. Vanilla velocity updates clamp it to 3.9 blocks per tick.
	public static final Supplier<EntityType<BlasterBolt>> BOLT = ENTITIES.register("blaster_bolt",
			() -> EntityType.Builder.<BlasterBolt>of(BlasterBolt::new, MobCategory.MISC)
					.sized(0.4F, 0.4F).eyeHeight(0.2F).clientTrackingRange(4).updateInterval(20)
					.setShouldReceiveVelocityUpdates(false).build(id("blaster_bolt").toString()));

	private static final Map<ResourceLocation, Supplier<SoundEvent>> REGISTERED_SOUNDS = new HashMap<>();

	static
	{
		TABS.register("blasters", () -> CreativeModeTab.builder().title(Component.translatable("itemGroup.pswg.blasters"))
				.icon(() -> PRESETS.get(Galaxies.id("e11")).toStack())
				.displayItems((parameters, output) -> {
					PRESETS.forEach((id, item) -> {
						var definition = BlasterDefinitions.client().get(id);
						output.accept(definition != null ? BlasterItem.createStack(id, definition) : item.toStack());
					});
					output.accept(SMALL_POWER_PACK.get());
				}).build());
		for (String name : new String[]{"fire.e11", "reload", "dryfire", "bypass.primary", "bypass.secondary",
				"bypass.secondary_end", "bypass.failed", "vent", "overheat"})
		{
			var sound = id("blaster." + name);
			REGISTERED_SOUNDS.put(sound, SOUNDS.register(sound.getPath(), () -> SoundEvent.createVariableRangeEvent(sound)));
		}
		for (String name : List.of("reload", "dryfire", "overheat", "bypass.primary", "bypass.secondary", "bypass.failed", "hiss"))
		{
			var sound = Galaxies.id("blaster." + name);
			REGISTERED_SOUNDS.put(sound, LEGACY_SOUNDS.register(sound.getPath(), () -> SoundEvent.createVariableRangeEvent(sound)));
		}
	}

	public static SoundEvent sound(ResourceLocation id)
	{
		var registered = REGISTERED_SOUNDS.get(id);
		if (registered != null)
			return registered.get();
		return BuiltInRegistries.SOUND_EVENT.getOptional(id)
				.orElseGet(() -> SoundEvent.createVariableRangeEvent(id));
	}

	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}

	public static void register(IEventBus bus)
	{
		COMPONENTS.register(bus);
		PLAYER_STATES.register(bus);
		ITEMS.register(bus);
		LEGACY_ITEMS.register(bus);
		TABS.register(bus);
		ENTITIES.register(bus);
		SOUNDS.register(bus);
		LEGACY_SOUNDS.register(bus);
		bus.addListener(BlasterNetwork::register);
		bus.addListener(Blasters::populateCombatTab);
		NeoForge.EVENT_BUS.addListener(BlasterNetwork::syncDefinitions);
		NeoForge.EVENT_BUS.addListener(BlasterNetwork::syncPatrol);
	}

	private static void populateCombatTab(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey().equals(CreativeModeTabs.COMBAT))
			BlasterDefinitions.client().entrySet().stream().filter(entry -> !PRESETS.containsKey(entry.getKey())).sorted(Map.Entry.comparingByKey())
					.forEach(entry -> event.accept(BlasterItem.createStack(entry.getKey(), entry.getValue())));
	}

	private static Map<ResourceLocation, DeferredItem<BlasterItem>> legacyPresets()
	{
		var items = new LinkedHashMap<ResourceLocation, DeferredItem<BlasterItem>>();
		for (String id : new String[] { "a280", "bowcaster", "ca87", "cycler", "dc15", "dc15a", "dh17", "dl18", "dl44",
				"dlt19", "e11", "ee3", "jawa_ion", "rk3", "rt97c", "se14c", "t21" })
		{
			var model = Galaxies.id(id);
			items.put(model, LEGACY_ITEMS.register("blaster_" + id,
					() -> new BlasterItem(new Item.Properties().stacksTo(1).component(ID.get(), model).component(STATE.get(), BlasterState.DEFAULT))));
		}
		return java.util.Collections.unmodifiableMap(items);
	}

	public record EquippedAttachments(ResourceLocation hud, Map<ResourceLocation, ResourceLocation> applied)
	{
		public static final Codec<EquippedAttachments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("hud").forGetter(EquippedAttachments::hud),
				Codec.unboundedMap(ResourceLocation.CODEC, ResourceLocation.CODEC).fieldOf("applied").forGetter(EquippedAttachments::applied)
		).apply(instance, EquippedAttachments::new));

		public EquippedAttachments
		{
			applied = Map.copyOf(applied);
		}
	}

	private Blasters()
	{
	}
}
