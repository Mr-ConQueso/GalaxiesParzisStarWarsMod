package dev.pswg.gadgets;

import com.mojang.serialization.Codec;
import dev.pswg.gadgets.brewing.BrewingMap;
import dev.pswg.gadgets.brewing.Mixer;
import dev.pswg.gadgets.brewing.MixerMenu;
import dev.pswg.gadgets.drill.DrillExtraction;
import dev.pswg.gadgets.drill.DrillParts;
import dev.pswg.gadgets.drill.ExtractionDrill;
import dev.pswg.gadgets.scrapping.LaserCutter;
import dev.pswg.gadgets.scrapping.ScrappingMenu;
import dev.pswg.gadgets.scrapping.ScrappingTable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public final class Workstations
{
	private static final String NAMESPACE = Demolitions.NAMESPACE;
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NAMESPACE);
	private static final DeferredRegister.DataComponents COMPONENTS =
			DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NAMESPACE);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NAMESPACE);
	private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, NAMESPACE);
	private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, NAMESPACE);
	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, NAMESPACE);
	private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
			DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NAMESPACE);

	public static final Supplier<AttachmentType<List<ResourceLocation>>> BREWING_DISCOVERIES = ATTACHMENTS.register("player_brewing_data",
			() -> AttachmentType.<List<ResourceLocation>>builder(() -> List.of()).serialize(ResourceLocation.CODEC.listOf()).copyOnDeath().build());

	public static final Supplier<BlockEntityType<Mixer>> MIXER_ENTITY = ENTITIES.register("mixer",
			() -> BlockEntityType.Builder.of(Mixer::new, Workstations.MIXER.get()).build(null));
	public static final Supplier<WorkstationBlock<Mixer>> MIXER = BLOCKS.register("mixer",
			() -> new WorkstationBlock<>(BlockBehaviour.Properties.of().strength(0.5F).noOcclusion(), MIXER_ENTITY, Mixer::tick,
					Shapes.or(Block.box(1, 0, 1, 15, 2, 15), Block.box(7, 0, 7, 9, 14, 9))));
	public static final Supplier<MenuType<MixerMenu>> MIXER_MENU = MENUS.register("mixer",
			() -> new MenuType<>(MixerMenu::new, FeatureFlags.DEFAULT_FLAGS));
	private static final Supplier<BlockItem> MIXER_ITEM = ITEMS.registerSimpleBlockItem("mixer", MIXER);

	public static final Supplier<DataComponentType<DrillParts>> DRILL_PARTS = component("drill_extractor_properties", DrillParts.CODEC);
	public static final Supplier<DataComponentType<DrillExtraction>> DRILL_EXTRACTION = component("drill_extraction_instance", DrillExtraction.CODEC);
	public static final DeferredItem<ExtractionDrill> DRILL = ITEMS.register("extraction_drill", () -> new ExtractionDrill(
			new Item.Properties().durability(DrillParts.DEFAULT.durability())
					.component(DRILL_PARTS.get(), DrillParts.DEFAULT)
					.component(DataComponents.TOOL, ExtractionDrill.toolProperties(DrillParts.DEFAULT))
					.attributes(DiggerItem.createAttributes(Tiers.NETHERITE, 1, -2.8F))));

	public static final Supplier<BlockEntityType<ScrappingTable>> SCRAPPING_ENTITY = ENTITIES.register("scrapping_table",
			() -> BlockEntityType.Builder.of(ScrappingTable::new, Workstations.SCRAPPING_TABLE.get()).build(null));
	public static final Supplier<WorkstationBlock<ScrappingTable>> SCRAPPING_TABLE = BLOCKS.register("scrapping_table",
			() -> new WorkstationBlock<>(BlockBehaviour.Properties.of().strength(1.5F), SCRAPPING_ENTITY, ScrappingTable::tick, Shapes.block()));
	public static final Supplier<MenuType<ScrappingMenu>> SCRAPPING_MENU = MENUS.register("scrapping_table",
			() -> new MenuType<>(ScrappingMenu::new, FeatureFlags.DEFAULT_FLAGS));
	private static final Supplier<BlockItem> SCRAPPING_ITEM = ITEMS.registerSimpleBlockItem("scrapping_table", SCRAPPING_TABLE);
	public static final DeferredItem<LaserCutter> CUTTER = ITEMS.register("cutter", () -> new LaserCutter(new Item.Properties().durability(100)));
	public static final Supplier<SimpleParticleType> LASER_CUT = PARTICLES.register("laser_cut", () -> new SimpleParticleType(false));
	public static final Supplier<DataComponentType<Float>> CUT_PROGRESS = component("cutting_progress", Codec.floatRange(0, 1));
	public static final Supplier<DataComponentType<BlockPos>> CUT_TARGET = component("current_block", BlockPos.CODEC);
	public static final Supplier<DataComponentType<Vec3>> CUT_MIN = component("min_pos", Vec3.CODEC);
	public static final Supplier<DataComponentType<Vec3>> CUT_MAX = component("max_pos", Vec3.CODEC);
	public static final Supplier<DataComponentType<Direction>> CUT_FACE = component("cutting_face", Direction.CODEC);
	public static final Supplier<DataComponentType<ResourceLocation>> CUT_DIMENSION = component("cutting_dimension", ResourceLocation.CODEC);
	public static final DeferredItem<Item> SPANNER = ITEMS.registerSimpleItem("spanner", new Item.Properties().durability(100));
	public static final DeferredItem<Item> CALIBRATOR = ITEMS.registerSimpleItem("calibrator", new Item.Properties().durability(100));

	public static void register(IEventBus bus)
	{
		BLOCKS.register(bus);
		ITEMS.register(bus);
		COMPONENTS.register(bus);
		ENTITIES.register(bus);
		MENUS.register(bus);
		PARTICLES.register(bus);
		ATTACHMENTS.register(bus);
		WorkstationRecipes.register(bus);
		bus.addListener(Workstations::populateTabs);
		NeoForge.EVENT_BUS.addListener(BrewingMap::registerReload);
	}

	private static void populateTabs(BuildCreativeModeTabContentsEvent event)
	{
		if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))
		{
			event.accept(MIXER_ITEM.get());
			event.accept(SCRAPPING_ITEM.get());
		}
		if (event.getTabKey().equals(CreativeModeTabs.TOOLS_AND_UTILITIES))
		{
			DrillParts.creativeVariants().forEach(parts -> event.accept(ExtractionDrill.configuredStack(parts)));
			event.accept(CUTTER.get());
			event.accept(SPANNER.get());
			event.accept(CALIBRATOR.get());
		}
	}

	private static <T> Supplier<DataComponentType<T>> component(String name, Codec<T> codec)
	{
		return COMPONENTS.registerComponentType(name, builder -> builder.persistent(codec).networkSynchronized(ByteBufCodecs.fromCodec(codec)));
	}

	private Workstations()
	{
	}
}
