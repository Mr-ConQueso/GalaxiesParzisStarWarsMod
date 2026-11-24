package dev.pswg.gadgets;

import com.mojang.serialization.Codec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class Demolitions
{
	public static final String NAMESPACE = "pswg_gadgets";
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NAMESPACE);
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NAMESPACE);
	private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NAMESPACE);
	private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NAMESPACE);
	private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, NAMESPACE);
	private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, NAMESPACE);
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NAMESPACE);
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, NAMESPACE);

	public static final Supplier<DataComponentType<Long>> PRIMING_TIME =
			COMPONENTS.registerComponentType("priming_time", builder -> builder.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

	public static final TagKey<Block> FRAGMENTATION_DESTROY = TagKey.create(Registries.BLOCK, id("fragmentation_destroy"));
	public static final TagKey<Block> DETONATES_GRENADE = TagKey.create(Registries.BLOCK, id("detonates_grenade"));
	public static final TagKey<Block> GAS_PASS_THROUGH = TagKey.create(Registries.BLOCK, id("gas_pass_through"));

	public static final Supplier<SimpleParticleType> SCORCH = PARTICLES.register("inferno_scorch", () -> new SimpleParticleType(false));
	public static final Supplier<SimpleParticleType> DENSE_SCORCH = PARTICLES.register("dense_inferno_scorch", () -> new SimpleParticleType(false));
	public static final Supplier<SimpleParticleType> WIRE_LASER = PARTICLES.register("tripwire_laser", () -> new SimpleParticleType(false));
	public static final Supplier<SimpleParticleType> EXPLOSION_SMOKE = PARTICLES.register("explosion_smoke", () -> new SimpleParticleType(false));
	public static final Supplier<SimpleParticleType> FRAGMENTATION_SPARK = PARTICLES.register("fragmentation_grenade_spark", () -> new SimpleParticleType(false));
	public static final Supplier<SimpleParticleType> FRAGMENTATION_WAVE = PARTICLES.register("fragmentation_grenade_wave", () -> new SimpleParticleType(true));
	public static final Supplier<ParticleType<GasParticleOptions>> SMOKE_PARTICLES = PARTICLES.register("smoke", GasParticleOptions::createType);
	public static final Supplier<ParticleType<GasParticleOptions>> NERVE_PARTICLES = PARTICLES.register("nerve_gas", GasParticleOptions::createType);

	public static final TagKey<Block> INFERNO_CHAR = TagKey.create(Registries.BLOCK, id("inferno_char"));
	public static final TagKey<Block> INFERNO_DESTROY = TagKey.create(Registries.BLOCK, id("inferno_destroy"));
	public static final net.neoforged.neoforge.registries.DeferredHolder<MobEffect, MobEffect> INTOXICATED =
			EFFECTS.register("intoxicated", Intoxicated::new);
	public static final DeferredBlock<Block> CHARRED = BLOCKS.registerSimpleBlock("charred_block", BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS));
	public static final DeferredBlock<Block> FERTILE = BLOCKS.register("fertile_dirt",
			() -> new FertileDirt(BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).randomTicks()));

	public static final DeferredItem<GrenadeItem> THERMAL = grenade("thermal_detonator", Variant.THERMAL);
	public static final DeferredItem<GrenadeItem> FRAGMENTATION = grenade("fragmentation_grenade", Variant.FRAGMENTATION);
	public static final DeferredItem<GrenadeItem> SMOKE = grenade("smoke_signal_grenade", Variant.SMOKE);
	public static final DeferredItem<GrenadeItem> IMPACT = grenade("impact_grenade", Variant.IMPACT);
	public static final DeferredItem<PressureMineItem> MINE = ITEMS.register("pressure_mine", () -> new PressureMineItem(Variant.PRESSURE, new Item.Properties()));

	public static final DeferredItem<GrenadeItem> NERVE = grenade("nerve_gas_grenade", Variant.NERVE);
	public static final DeferredItem<GrenadeItem> INFERNO = grenade("inferno_grenade", Variant.INFERNO);
	public static final DeferredItem<PressureMineItem> TRIPWIRE = ITEMS.register("tripwire_mine",
			() -> new PressureMineItem(Variant.TRIPWIRE, new Item.Properties()));
	private static final DeferredItem<net.minecraft.world.item.BlockItem> CHARRED_ITEM = ITEMS.registerSimpleBlockItem("charred_block", CHARRED);
	private static final DeferredItem<net.minecraft.world.item.BlockItem> FERTILE_ITEM = ITEMS.registerSimpleBlockItem("fertile_dirt", FERTILE);

	public static final DeferredBlock<ChargeClusterBlock> THERMAL_CLUSTER = BLOCKS.register("thermal_detonator_block",
			() -> new ChargeClusterBlock(Variant.THERMAL, BlockBehaviour.Properties.of().instabreak().noOcclusion()));
	public static final DeferredBlock<ChargeClusterBlock> FRAG_CLUSTER = BLOCKS.register("fragmentation_grenade_block",
			() -> new ChargeClusterBlock(Variant.FRAGMENTATION, BlockBehaviour.Properties.of().instabreak().noOcclusion()));

	public static final Supplier<EntityType<GadgetCharge>> THERMAL_ENTITY = entity("thermal_detonator", Variant.THERMAL, 0.2F, 0.2F);
	public static final Supplier<EntityType<GadgetCharge>> FRAGMENTATION_ENTITY = entity("fragmentation_grenade", Variant.FRAGMENTATION, 0.2F, 0.4F);
	public static final Supplier<EntityType<GadgetCharge>> SMOKE_ENTITY = entity("smoke_grenade", Variant.SMOKE, 0.2F, 0.2F);
	public static final Supplier<EntityType<GadgetCharge>> IMPACT_ENTITY = entity("impact_grenade", Variant.IMPACT, 0.3F, 0.3F);
	public static final Supplier<EntityType<GadgetCharge>> MINE_ENTITY = entity("pressure_mine", Variant.PRESSURE, 0.2F, 0.1F);
	public static final Supplier<EntityType<SmokeCloud>> SMOKE_CLOUD = ENTITIES.register("smoke",
			() -> EntityType.Builder.<SmokeCloud>of(SmokeCloud::new, MobCategory.MISC).sized(2F, 2F).clientTrackingRange(10).updateInterval(20)
					.build(id("smoke").toString()));
	public static final Supplier<EntityType<GadgetCharge>> NERVE_ENTITY = entity("nerve_gas_grenade", Variant.NERVE, 0.2F, 0.4F);
	public static final Supplier<EntityType<GadgetCharge>> INFERNO_ENTITY = entity("inferno_grenade", Variant.INFERNO, 0.15F, 0.3F);
	public static final Supplier<EntityType<GadgetCharge>> TRIPWIRE_ENTITY = entity("tripwire_mine", Variant.TRIPWIRE, 0.2F, 0.1F);
	public static final Supplier<EntityType<SmokeCloud>> NERVE_CLOUD = ENTITIES.register("nerve_gas",
			() -> EntityType.Builder.<SmokeCloud>of((type, level) -> new SmokeCloud(type, level, true), MobCategory.MISC)
					.sized(2F, 2F).clientTrackingRange(10).updateInterval(20).build(id("nerve_gas").toString()));

	private static final Map<String, Supplier<SoundEvent>> REGISTERED_SOUNDS = new HashMap<>();

	static
	{
		for (String sound : List.of("shared.arm", "shared.disarm", "shared.throw", "thermaldetonator.beep",
				"thermaldetonator.explode", "fragmentationgrenade.beep", "fragmentationgrenade.explode1",
				"fragmentationgrenade.explode2", "fragmentationgrenade.explode3", "fragmentationgrenade.explode4"))
			REGISTERED_SOUNDS.put(sound, SOUNDS.register(sound, () -> SoundEvent.createVariableRangeEvent(id(sound))));

		TABS.register("demolitions_items", () -> CreativeModeTab.builder()
				.title(net.minecraft.network.chat.Component.translatable("pswg_gadgets.demolitions_items_group"))
				.icon(THERMAL::toStack)
				.displayItems((parameters, output) -> {
					for (var item : List.of(THERMAL, FRAGMENTATION, SMOKE, NERVE, INFERNO, IMPACT, MINE, TRIPWIRE, CHARRED_ITEM, FERTILE_ITEM))
						output.accept(item.get());
				})
				.build());
	}

	public static void register(IEventBus bus)
	{
		ITEMS.register(bus);
		BLOCKS.register(bus);
		COMPONENTS.register(bus);
		ENTITIES.register(bus);
		EFFECTS.register(bus);
		PARTICLES.register(bus);
		SOUNDS.register(bus);
		TABS.register(bus);
		bus.addListener(Demolitions::registerDispenserBehaviors);
		bus.addListener(GadgetInput::register);
	}

	private static void registerDispenserBehaviors(FMLCommonSetupEvent event)
	{
		event.enqueueWork(() -> {
			for (var item : List.of(THERMAL, FRAGMENTATION, SMOKE, INFERNO, NERVE, IMPACT))
				DispenserBlock.registerProjectileBehavior(item.get());
		});
	}

	private static DeferredItem<GrenadeItem> grenade(String name, Variant variant)
	{
		return ITEMS.register(name, () -> new GrenadeItem(variant, new Item.Properties()));
	}

	private static Supplier<EntityType<GadgetCharge>> entity(String name, Variant variant, float width, float height)
	{
		return ENTITIES.register(name, () -> EntityType.Builder.<GadgetCharge>of((type, level) -> new GadgetCharge(type, level, variant), MobCategory.MISC)
				.sized(width, height).clientTrackingRange(8).updateInterval(2).build(id(name).toString()));
	}

	public static ResourceLocation id(String path)
	{
		return ResourceLocation.fromNamespaceAndPath(NAMESPACE, path);
	}

	public static SoundEvent sound(String path)
	{
		return REGISTERED_SOUNDS.get(path).get();
	}

	public enum Variant
	{
		THERMAL(150, 5F), FRAGMENTATION(50, 4F), SMOKE(80, 0F), IMPACT(250, 1.5F), PRESSURE(50, 2F), TRIPWIRE(60, 2.5F), NERVE(60, 0F), INFERNO(80, 0F);

		public final int delay;
		public final float power;

		Variant(int delay, float power)
		{
			this.delay = delay;
			this.power = power;
		}

		public EntityType<GadgetCharge> entityType()
		{
			return switch (this)
			{
				case THERMAL -> THERMAL_ENTITY.get();
				case FRAGMENTATION -> FRAGMENTATION_ENTITY.get();
				case SMOKE -> SMOKE_ENTITY.get();
				case IMPACT -> IMPACT_ENTITY.get();
				case PRESSURE -> MINE_ENTITY.get();
				case TRIPWIRE -> TRIPWIRE_ENTITY.get();
				case NERVE -> NERVE_ENTITY.get();
				case INFERNO -> INFERNO_ENTITY.get();
			};
		}

		public Item item()
		{
			return switch (this)
			{
				case THERMAL -> Demolitions.THERMAL.get();
				case FRAGMENTATION -> Demolitions.FRAGMENTATION.get();
				case SMOKE -> Demolitions.SMOKE.get();
				case IMPACT -> Demolitions.IMPACT.get();
				case PRESSURE -> MINE.get();
				case TRIPWIRE -> Demolitions.TRIPWIRE.get();
				case NERVE -> Demolitions.NERVE.get();
				case INFERNO -> Demolitions.INFERNO.get();
			};
		}

		public ChargeClusterBlock cluster()
		{
			return switch (this)
			{
				case THERMAL -> THERMAL_CLUSTER.get();
				case FRAGMENTATION -> FRAG_CLUSTER.get();
				default -> throw new IllegalStateException("No cluster block for " + this);
			};
		}

		boolean isMine()
		{
			return this == PRESSURE || this == TRIPWIRE;
		}

		public boolean canCluster()
		{
			return this == THERMAL || this == FRAGMENTATION;
		}
	}

	private Demolitions()
	{
	}
}
