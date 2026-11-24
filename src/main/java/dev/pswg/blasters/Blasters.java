package dev.pswg.blasters;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class Blasters
{
	public static final String NAMESPACE = "pswg_blasters";
	public static final ResourceLocation DEFAULT_HUD = id("default");
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NAMESPACE);
	private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NAMESPACE);
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NAMESPACE);
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NAMESPACE);

	public static final ResourceLocation MISSING = id("missingno");
	public static final Supplier<DataComponentType<ResourceLocation>> ID =
			COMPONENTS.registerComponentType("id", builder -> builder.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));
	public static final Supplier<DataComponentType<Long>> SERIAL =
			COMPONENTS.registerComponentType("serial", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
	public static final Supplier<DataComponentType<BlasterState>> STATE =
			COMPONENTS.registerComponentType("state", builder -> builder.persistent(BlasterState.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(BlasterState.CODEC)));
	public static final Supplier<DataComponentType<EquippedAttachments>> ATTACHMENTS =
			COMPONENTS.registerComponentType("attachments", builder -> builder.persistent(EquippedAttachments.CODEC).networkSynchronized(ByteBufCodecs.fromCodec(EquippedAttachments.CODEC)));

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
		for (String name : new String[]{"fire.e11", "reload", "dryfire", "bypass.primary", "bypass.secondary",
				"bypass.secondary_end", "bypass.failed", "vent", "overheat"})
		{
			var sound = id("blaster." + name);
			REGISTERED_SOUNDS.put(sound, SOUNDS.register(sound.getPath(), () -> SoundEvent.createVariableRangeEvent(sound)));
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
		ITEMS.register(bus);
		ENTITIES.register(bus);
		SOUNDS.register(bus);
		bus.addListener(BlasterNetwork::register);
		bus.addListener(Blasters::populateCombatTab);
		NeoForge.EVENT_BUS.addListener(BlasterNetwork::syncDefinitions);
	}

	private static void populateCombatTab(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey().equals(CreativeModeTabs.COMBAT))
			BlasterDefinitions.client().entrySet().stream().sorted(Map.Entry.comparingByKey())
					.forEach(entry -> event.accept(BlasterItem.createStack(entry.getKey(), entry.getValue())));
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
