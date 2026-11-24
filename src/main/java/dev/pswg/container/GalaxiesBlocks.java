package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.block.AccumulatingBlock;
import dev.pswg.block.ConnectedBlock;
import dev.pswg.block.ClusterLightBlock;
import dev.pswg.block.CorrugatedCrateBlock;
import dev.pswg.block.HangarLightBlock;
import dev.pswg.block.ImperialLighting;
import dev.pswg.block.RepellerBlock;
import dev.pswg.block.StainedConnectedGlassBlock;
import dev.pswg.block.TallLampBlock;
import dev.pswg.block.VerticalSlabBlock;
import dev.pswg.block.WaterloggableRotatingBlock;
import dev.pswg.block.WetPourstone;
import dev.pswg.block.plant.SequoiaLeavesBlock;
import dev.pswg.block.plant.AridPlant;
import dev.pswg.block.plant.BushLeavesBlock;
import dev.pswg.block.plant.ChasukaCrop;
import dev.pswg.block.plant.HkakBushBlock;
import dev.pswg.block.plant.MoloShrubBlock;
import dev.pswg.block.plant.VaporatorMushroomBlock;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ColorRGBA;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public final class GalaxiesBlocks
{
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Galaxies.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Galaxies.MODID);

	private static final List<DeferredBlock<?>> CONSTRUCTION = new ArrayList<>();

	public static final StoneProducts CANYON = stoneProducts("canyon_stone", () -> BlockBehaviour.Properties.of().strength(0.5F));
	public static final DeferredBlock<Block> CANYON_BRICKS = simpleStone("canyon_stone_bricks");
	public static final DeferredBlock<Block> POLISHED_CANYON = simpleStone("polished_canyon_stone");
	public static final DeferredBlock<Block> CHISELED_CANYON = simpleStone("chiseled_canyon_stone");
	public static final StoneProducts CANYON_COBBLE = stoneProducts("canyon_cobblestone",
			() -> BlockBehaviour.Properties.of().strength(1.25F).requiresCorrectToolForDrops());

	public static final TagKey<Block> ARID_PLANT_PLACEABLE = TagKey.create(Registries.BLOCK, Galaxies.id("arid_plant_placeable"));
	public static final TagKey<Block> BUSH_PLACEABLE = TagKey.create(Registries.BLOCK, Galaxies.id("bush_placeable"));

	public static final DeferredBlock<Block> SMOOTH_DESERT_SANDSTONE = simpleStone("smooth_desert_sandstone");
	public static final DeferredBlock<ColoredFallingBlock> DESERT_SAND = fallingSand("desert_sand", 0xFFEDBB8A);
	public static final DeferredBlock<ColoredFallingBlock> CANYON_SAND = fallingSand("canyon_sand", 0xFFC59572);
	public static final DeferredBlock<Block> CAKED_SALT = registerWithItem("caked_salt",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.SAND).strength(0.5F)));
	public static final DeferredBlock<Block> DESERT_LOAM = registerWithItem("desert_loam",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).strength(0.5F)));
	public static final DeferredBlock<AridPlant> FUNNEL_FLOWER = aridPlant("funnel_flower");
	public static final DeferredBlock<AridPlant> BLOSSOMING_FUNNEL_FLOWER = aridPlant("blossoming_funnel_flower");
	public static final DeferredBlock<AridPlant> POONTEN_GRASS = aridPlant("poonten_grass");
	public static final DeferredBlock<AridPlant> DRIED_POONTEN_GRASS = aridPlant("dried_poonten_grass");
	public static final DeferredBlock<AridPlant> TUBER_STALK = aridPlant("tuber_stalk");
	public static final DeferredBlock<ChasukaCrop> CHASUKA = BLOCKS.register("chasuka",
			() -> new ChasukaCrop(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
	public static final DeferredBlock<MoloShrubBlock> MOLO_SHRUB = registerWithItem("molo_shrub",
			() -> new MoloShrubBlock(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.CROP)));
	public static final DeferredBlock<VaporatorMushroomBlock> VAPORATOR_MUSHROOM_COLONY = registerWithItem("vaporator_mushroom_colony",
			() -> new VaporatorMushroomBlock(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS)));
	public static final DeferredBlock<HkakBushBlock> HKAK_BUSH = registerWithItem("hkak_bush",
			() -> new HkakBushBlock(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
	public static final DeferredItem<Item> HKAK_BEAN = ITEMS.registerSimpleItem("hkak_bean",
			new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));

	public static final DeferredBlock<LeavesBlock> SEQUOIA_LEAVES = registerWithItem("sequoia_leaves",
			() -> new SequoiaLeavesBlock(BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.GRASS).noOcclusion()
					.isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false)));
	public static final DeferredBlock<BushLeavesBlock> JAPOR_LEAVES = registerWithItem("japor_leaves",
			() -> new BushLeavesBlock(BlockBehaviour.Properties.of().strength(0.2F).sound(SoundType.GRASS)
					.noCollission().pushReaction(PushReaction.DESTROY)));
	public static final DeferredBlock<RotatedPillarBlock> SEQUOIA_LOG = log("sequoia_log", MapColor.WOOD);
	public static final DeferredBlock<RotatedPillarBlock> STRIPPED_SEQUOIA_LOG = log("stripped_sequoia_log", MapColor.WOOD);
	public static final DeferredBlock<RotatedPillarBlock> MOSSY_SEQUOIA_LOG = log("mossy_sequoia_log", MapColor.WOOD);
	public static final DeferredBlock<RotatedPillarBlock> SEQUOIA_WOOD = log("sequoia_wood", MapColor.COLOR_BROWN);
	public static final DeferredBlock<RotatedPillarBlock> JAPOR_LOG = log("japor_log", MapColor.WOOD);
	public static final DeferredBlock<RotatedPillarBlock> JAPOR_WOOD = log("japor_wood", MapColor.COLOR_BROWN);
	public static final DeferredBlock<RotatedPillarBlock> TATOOINE_LOG = log("tatooine_log", MapColor.WOOD);
	public static final DeferredBlock<RotatedPillarBlock> TATOOINE_WOOD = log("tatooine_wood", MapColor.COLOR_BROWN);
	public static final WoodProducts SEQUOIA = woodProducts("sequoia");
	public static final WoodProducts JAPOR = woodProducts("japor");

	public static final DeferredBlock<Block> LAB_WALL = registerWithItem("lab_wall",
			() -> new Block(BlockBehaviour.Properties.of()));
	public static final DeferredBlock<HangarLightBlock> BLACK_HANGAR_LIGHT = hangarLight("black_hangar_light");
	public static final DeferredBlock<HangarLightBlock> YELLOW_HANGAR_LIGHT = hangarLight("yellow_hangar_light");
	public static final DeferredBlock<ClusterLightBlock> WALL_CLUSTER_LIGHT = registerWithItem("wall_cluster_light",
			() -> new ClusterLightBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL)
					.noOcclusion().lightLevel(state -> 15).strength(0.5F)), CONSTRUCTION);
	public static final DeferredBlock<WaterloggableRotatingBlock> FUSION_FUEL_TANK = registerWithItem("fusion_fuel_tank",
			() -> new WaterloggableRotatingBlock(BlockBehaviour.Properties.of().sound(SoundType.COPPER).noOcclusion()
					.strength(3.5F).requiresCorrectToolForDrops()), CONSTRUCTION);

	public static final DeferredBlock<TallLampBlock> TALL_LAMP = registerWithItem("tall_lamp",
			() -> new TallLampBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL)
					.lightLevel(state -> 15).strength(0.5F).noOcclusion()));

	public static final StoneProducts POURSTONE = stoneProducts("pourstone",
			() -> minedStone(1.25F));
	public static final StoneProducts SMOOTH_POURSTONE = stoneProducts("smooth_pourstone",
			() -> minedStone(1.25F));
	public static final StoneProducts CRACKED_POURSTONE = stoneProducts("cracked_pourstone",
			() -> minedStone(1.0F));
	public static final StoneProducts MASSASSI = stoneProducts("massassi_stone",
			() -> minedStone(1.5F), CONSTRUCTION);
	public static final ReducedStoneProducts MASSASSI_SMOOTH = reducedStoneProducts("smooth_massassi_stone",
			() -> minedStone(2F), CONSTRUCTION);
	public static final StoneProducts MASSASSI_BRICKS = stoneProducts("massassi_stone_bricks",
			() -> minedStone(1.5F), CONSTRUCTION);
	public static final ReducedStoneProducts MOSSY_MASSASSI_SMOOTH = reducedStoneProducts("mossy_smooth_massassi_stone",
			() -> minedStone(2F), CONSTRUCTION);
	public static final StoneProducts MOSSY_MASSASSI_BRICKS = stoneProducts("mossy_massassi_stone_bricks",
			() -> minedStone(1.5F), CONSTRUCTION);
	public static final StoneProducts ILUM = stoneProducts("ilum_stone",
			() -> minedStone(1.5F), CONSTRUCTION);
	public static final ReducedStoneProducts ILUM_SMOOTH = reducedStoneProducts("smooth_ilum_stone",
			() -> minedStone(2.0F), CONSTRUCTION);
	public static final StoneProducts ILUM_BRICKS = stoneProducts("ilum_stone_bricks",
			() -> minedStone(1.5F), CONSTRUCTION);
	public static final StoneProducts DESERT_SANDSTONE = stoneProducts("desert_sandstone",
			() -> minedStone(1.25F));
	public static final StoneProducts BLACK_IMPERIAL_PANEL_BLANK = stoneProducts("black_imperial_panel_blank",
			() -> BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(1.5F).requiresCorrectToolForDrops(), CONSTRUCTION);
	public static final StoneProducts GRAY_IMPERIAL_PANEL_BLANK = stoneProducts("gray_imperial_panel_blank",
			() -> BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(1.5F).requiresCorrectToolForDrops().mapColor(MapColor.COLOR_GRAY), CONSTRUCTION);
	public static final StoneProducts LIGHT_GRAY_IMPERIAL_PANEL_BLANK = stoneProducts("light_gray_imperial_panel_blank",
			() -> BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(1.5F).requiresCorrectToolForDrops().mapColor(MapColor.COLOR_LIGHT_GRAY), CONSTRUCTION);
	public static final StoneProducts WHITE_IMPERIAL_PANEL_BLANK = stoneProducts("white_imperial_panel_blank",
			() -> BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(1.5F).requiresCorrectToolForDrops(), CONSTRUCTION);
	public static final DeferredBlock<Block> ILUM_CHISELED_BRICKS = registerWithItem("chiseled_ilum_stone_bricks",
			() -> new Block(BlockBehaviour.Properties.of().strength(1.5F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> DUNESTONE = registerWithItem("dunestone",
			() -> new Block(BlockBehaviour.Properties.of().strength(1.25F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> POLISHED_DESERT_SANDSTONE = registerWithItem("polished_desert_sandstone",
			() -> new Block(BlockBehaviour.Properties.of().strength(0.5F)));
	public static final DeferredBlock<Block> CHISELED_DESERT = registerWithItem("chiseled_desert_sandstone",
			() -> new Block(BlockBehaviour.Properties.of().strength(0.5F)));
	public static final DeferredBlock<ConnectedBlock.Glass> IMPERIAL_GLASS = registerWithItem("imperial_glass",
			() -> new ConnectedBlock.Glass(glassProperties()), CONSTRUCTION);
	public static final DeferredBlock<CorrugatedCrateBlock> IMPERIAL_CORRUGATED_CRATE = crate("imperial_corrugated_crate");
	public static final DeferredBlock<CorrugatedCrateBlock> MEDICAL_CORRUGATED_CRATE = crate("medical_corrugated_crate");
	public static final DeferredBlock<CorrugatedCrateBlock> MINING_CORRUGATED_CRATE = crate("mining_corrugated_crate");
	public static final DeferredBlock<ConnectedBlock> RUSTED_METAL = registerWithItem("rusted_metal",
			() -> new ConnectedBlock(panel(MapColor.COLOR_BROWN)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> BLACK_IMPERIAL_PANEL_SECTIONAL = registerWithItem("black_imperial_panel_sectional",
			() -> new ConnectedBlock(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> BLACK_IMPERIAL_PANEL_SECTIONAL_1 = registerWithItem("black_imperial_panel_sectional_1",
			() -> new ConnectedBlock(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> BLACK_IMPERIAL_PANEL_SECTIONAL_2 = registerWithItem("black_imperial_panel_sectional_2",
			() -> new ConnectedBlock(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> GRAY_IMPERIAL_PANEL_SECTIONAL = registerWithItem("gray_imperial_panel_sectional",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> GRAY_IMPERIAL_PANEL_SECTIONAL_1 = registerWithItem("gray_imperial_panel_sectional_1",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> GRAY_IMPERIAL_PANEL_SECTIONAL_2 = registerWithItem("gray_imperial_panel_sectional_2",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> GRAY_IMPERIAL_TALL_PANEL_1 = registerWithItem("gray_imperial_tall_panel_1",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> GRAY_IMPERIAL_TALL_PANEL_2 = registerWithItem("gray_imperial_tall_panel_2",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> LIGHT_GRAY_IMPERIAL_PANEL_SECTIONAL = registerWithItem("light_gray_imperial_panel_sectional",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> LIGHT_GRAY_IMPERIAL_PANEL_SECTIONAL_1 = registerWithItem("light_gray_imperial_panel_sectional_1",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> LIGHT_GRAY_IMPERIAL_PANEL_SECTIONAL_2 = registerWithItem("light_gray_imperial_panel_sectional_2",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> WHITE_IMPERIAL_PANEL_SECTIONAL = registerWithItem("white_imperial_panel_sectional",
			() -> new ConnectedBlock(panel(MapColor.QUARTZ)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> WHITE_IMPERIAL_PANEL_SECTIONAL_1 = registerWithItem("white_imperial_panel_sectional_1",
			() -> new ConnectedBlock(panel(MapColor.QUARTZ)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> WHITE_IMPERIAL_PANEL_SECTIONAL_2 = registerWithItem("white_imperial_panel_sectional_2",
			() -> new ConnectedBlock(panel(MapColor.QUARTZ)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> BLACK_IMPERIAL_PANEL_BORDERED = registerWithItem("black_imperial_panel_bordered",
			() -> new ConnectedBlock(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> BLACK_IMPERIAL_PANEL_SPLIT = registerWithItem("black_imperial_panel_split",
			() -> new ConnectedBlock(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> BLACK_IMPERIAL_PANEL_THIN_BORDERED = registerWithItem("black_imperial_panel_thin_bordered",
			() -> new ConnectedBlock(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> EXTERNAL_IMPERIAL_PLATING = registerWithItem("external_imperial_plating",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> LARGE_IMPERIAL_PLATING = registerWithItem("large_imperial_plating",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> RUSTED_LARGE_IMPERIAL_PLATING = registerWithItem("rusted_large_imperial_plating",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> MOSSY_LARGE_IMPERIAL_PLATING = registerWithItem("mossy_large_imperial_plating",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> LARGE_LIGHT_GRAY_IMPERIAL_PLATING = registerWithItem("large_light_gray_imperial_plating",
			() -> new ConnectedBlock(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> DURASTEEL_BORDERED_POURSTONE = registerWithItem("durasteel_bordered_pourstone",
			() -> new ConnectedBlock(minedStone(1.5F).sound(SoundType.METAL)), CONSTRUCTION);
	public static final DeferredBlock<ConnectedBlock> PLASTEEL_BLOCK = registerWithItem("plasteel_block",
			() -> new ConnectedBlock(minedStone(5.0F).sound(SoundType.COPPER)), CONSTRUCTION);

	public static final DeferredBlock<ColoredFallingBlock> PIT_SAND = fallingSand("pit_sand", 0xFFEAC795);
	public static final DeferredBlock<ColoredFallingBlock> FINE_SAND = fallingSand("fine_sand", 0xFFE9C490);
	public static final DeferredBlock<ColoredFallingBlock> JUNDLAND_GRAVEL = registerWithItem("jundland_gravel",
			() -> new ColoredFallingBlock(new ColorRGBA(0xFF7A5346), BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).strength(0.5F)));
	public static final DeferredBlock<AccumulatingBlock> LOOSE_DESERT_SAND = registerWithItem("loose_desert_sand",
			() -> new AccumulatingBlock(BlockBehaviour.Properties.of().sound(SoundType.SAND).strength(0.5F),
					context -> DESERT_SAND.get().getStateForPlacement(context)), CONSTRUCTION);
	public static final DeferredBlock<RepellerBlock> ELECTROSTATIC_REPELLER = registerWithItem("electrostatic_repeller",
			() -> new RepellerBlock(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(0.5F)
					.noOcclusion().requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final ReducedStoneProducts RUINED_WET_POURSTONE = wetStone("ruined_wet_pourstone", CRACKED_POURSTONE, null, 0xFF986A39);
	public static final ReducedStoneProducts WET_POURSTONE = wetStone("wet_pourstone", POURSTONE, RUINED_WET_POURSTONE, 0xFF9E6E3B);

	public static final DeferredBlock<Block> BESKAR_ORE = registerWithItem("beskar_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(5.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> BESKAR_BLOCK = registerWithItem("beskar_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> CHROMIUM_ORE = registerWithItem("chromium_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> CHROMIUM_BLOCK = registerWithItem("chromium_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(3.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> CORTOSIS_ORE = registerWithItem("cortosis_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(5.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> CORTOSIS_BLOCK = registerWithItem("cortosis_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> DESH_ORE = registerWithItem("desh_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> DESH_BLOCK = registerWithItem("desh_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(3.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> DIATIUM_ORE = registerWithItem("diatium_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(5.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> DIATIUM_BLOCK = registerWithItem("diatium_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> IONITE_ORE = registerWithItem("ionite_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(5.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> IONITE_BLOCK = registerWithItem("ionite_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(5.0F).lightLevel(value -> 3).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> LOMMITE_ORE = registerWithItem("lommite_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> LOMMITE_BLOCK = registerWithItem("lommite_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> TITANIUM_ORE = registerWithItem("titanium_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(4.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> TITANIUM_BLOCK = registerWithItem("titanium_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> ZERSIUM_ORE = registerWithItem("zersium_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> ZERSIUM_BLOCK = registerWithItem("zersium_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> THORILIDE_ORE = registerWithItem("thorilide_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> THORILIDE_BLOCK = registerWithItem("thorilide_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> HELICITE_ORE = registerWithItem("helicite_ore",
			() -> new Block(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
	public static final DeferredBlock<Block> HELICITE_BLOCK = registerWithItem("helicite_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);
	public static final DeferredBlock<Block> DURASTEEL_BLOCK = registerWithItem("durasteel_block",
			() -> new Block(BlockBehaviour.Properties.of().sound(SoundType.COPPER).strength(5.0F).requiresCorrectToolForDrops()), CONSTRUCTION);

	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_HALF_1 = lightingPanel("gray_imperial_light_half_1", 13);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_HALF_2 = lightingPanel("gray_imperial_light_half_2", 13);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_HALF_3 = lightingPanel("gray_imperial_light_half_3", 13);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_HALF_4 = lightingPanel("gray_imperial_light_half_4", 13);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_HALF_5 = lightingPanel("gray_imperial_light_half_5", 13);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_PANEL_1 = lightingPanel("gray_imperial_light_panel_1", 11);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_PANEL_2 = lightingPanel("gray_imperial_light_panel_2", 9);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_PANEL_3 = lightingPanel("gray_imperial_light_panel_3", 14);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_1 = lightingPanel("gray_imperial_light_1", 15);
	public static final DeferredBlock<ImperialLighting> GRAY_IMPERIAL_LIGHT_2 = lightingPanel("gray_imperial_light_2", 15);
	public static final DeferredBlock<ImperialLighting.Pillar> IMPERIAL_LIGHT_TALL_1 = registerWithItem("gray_imperial_tall_light_1",
			() -> new ImperialLighting.Pillar(panel(MapColor.COLOR_GRAY).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)), CONSTRUCTION);
	public static final DeferredBlock<ImperialLighting.Pillar> IMPERIAL_LIGHT_TALL_2 = registerWithItem("gray_imperial_tall_light_2",
			() -> new ImperialLighting.Pillar(panel(MapColor.COLOR_GRAY).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 14 : 0)), CONSTRUCTION);
	public static final DeferredBlock<ImperialLighting.Slab> GRAY_IMPERIAL_LIGHTING_SLAB = registerWithItem("gray_imperial_lighting_panel_slab",
			() -> new ImperialLighting.Slab(panel(MapColor.COLOR_GRAY).lightLevel(state -> {
				if (!state.getValue(BlockStateProperties.LIT))
					return 0;
				return state.getValue(VerticalSlabBlock.TYPE) == SlabType.DOUBLE ? 12 : 15;
			})), CONSTRUCTION);

	static
	{
		for (DyeColor color : DyeColor.values())
			stoneProducts(color.getName() + "_pourstone", () -> minedStone(1.25F));

		for (DyeColor color : DyeColor.values())
		{
			registerWithItem(color.getName() + "_stained_imperial_glass", () -> new StainedConnectedGlassBlock(color, glassProperties()), CONSTRUCTION);
			crate(color.getName() + "_corrugated_crate");
		}
		registerWithItem("black_imperial_panel_tile", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_3", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("light_gray_imperial_panel_pattern_3", () -> new Block(panel(MapColor.COLOR_LIGHT_GRAY)), CONSTRUCTION);
		registerWithItem("light_gray_imperial_panel_pattern_4", () -> new Block(panel(MapColor.COLOR_LIGHT_GRAY)), CONSTRUCTION);
		registerWithItem("light_gray_imperial_panel_pattern_5", () -> new Block(panel(MapColor.COLOR_LIGHT_GRAY)), CONSTRUCTION);
		registerWithItem("rusted_gray_imperial_panel_pattern_3", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("mossy_gray_imperial_panel_pattern_3", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_4", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("rusted_gray_imperial_panel_pattern_4", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("mossy_gray_imperial_panel_pattern_4", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_5", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("rusted_gray_imperial_panel_pattern_5", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("mossy_gray_imperial_panel_pattern_5", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_6", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("rusted_gray_imperial_panel_pattern_6", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("mossy_gray_imperial_panel_pattern_6", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_7", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_8", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_9", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("rusted_gray_imperial_panel_pattern_9", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("mossy_gray_imperial_panel_pattern_9", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_10", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_11", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_12", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_panel_pattern_13", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_flooring_0", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_flooring_3", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("gray_imperial_flooring_4", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("light_gray_imperial_flooring_0", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("light_gray_imperial_flooring_3", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("light_gray_imperial_flooring_4", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("black_imperial_flooring_3", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("black_imperial_flooring_4", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("white_imperial_flooring_3", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("white_imperial_flooring_4", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("imperial_flooring_pattern_1", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("imperial_flooring_pattern_2", () -> new Block(panel(MapColor.COLOR_GRAY)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_a_1", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_a_2", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_a_3", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_a_4", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_b_1", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_b_2", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_b_3", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_b_4", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_c_1", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_c_2", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_d_1", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_d_2", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_d_3", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_d_4", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_e_1", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_e_2", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_e_3", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);
		registerWithItem("black_imperial_panel_pattern_e_4", () -> new Block(panel(MapColor.COLOR_BLACK)), CONSTRUCTION);

		CONSTRUCTION.add(TALL_LAMP);
		TABS.register("worldgen_blocks", () -> CreativeModeTab.builder()
				.title(Component.translatable("pswg.worldgen_block_group"))
				.icon(() -> CANYON.block().toStack())
				.displayItems((parameters, output) -> {
					for (var block : BLOCKS.getEntries())
						if (block != CHASUKA && block != LAB_WALL && !CONSTRUCTION.contains(block))
							output.accept(block.get());
				})
				.build());
		TABS.register("construction_blocks", () -> CreativeModeTab.builder()
				.title(Component.translatable("pswg.construction_block_group"))
				.icon(TALL_LAMP::toStack)
				.displayItems((parameters, output) -> {
					for (var block : CONSTRUCTION)
						output.accept(block.get());
				})
				.build());
		TABS.register("food_items", () -> CreativeModeTab.builder()
				.title(Component.translatable("pswg.food_item_group"))
				.icon(HKAK_BEAN::toStack)
				.displayItems((parameters, output) -> output.accept(HKAK_BEAN.get()))
				.build());
	}

	public static void register(IEventBus bus)
	{
		BLOCKS.register(bus);
		ITEMS.register(bus);
		TABS.register(bus);
		bus.addListener(GalaxiesBlocks::registerFireBehavior);
	}

	private static void registerFireBehavior(FMLCommonSetupEvent event)
	{
		event.enqueueWork(() -> {
			var fire = (FireBlock) Blocks.FIRE;
			for (var log : List.of(SEQUOIA_LOG, STRIPPED_SEQUOIA_LOG, MOSSY_SEQUOIA_LOG, SEQUOIA_WOOD,
					JAPOR_LOG, JAPOR_WOOD, TATOOINE_LOG, TATOOINE_WOOD))
				fire.setFlammable(log.get(), 5, 5);
			for (var wood : List.of(SEQUOIA, JAPOR))
				for (var block : wood.blocks())
					fire.setFlammable(block.get(), 20, 5);
			fire.setFlammable(SEQUOIA_LEAVES.get(), 60, 30);
			fire.setFlammable(JAPOR_LEAVES.get(), 60, 30);
			fire.setFlammable(CHASUKA.get(), 100, 60);
			fire.setFlammable(MOLO_SHRUB.get(), 100, 60);
		});
	}

	public static Collection<DeferredHolder<Block, ? extends Block>> entries()
	{
		return BLOCKS.getEntries();
	}

	private static DeferredBlock<Block> simpleStone(String name)
	{
		return registerWithItem(name, () -> new Block(BlockBehaviour.Properties.of().strength(0.5F)));
	}

	private static DeferredBlock<RotatedPillarBlock> log(String name, MapColor endColor)
	{
		return registerWithItem(name, () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
				.mapColor(state -> state.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.Y ? endColor : MapColor.COLOR_BROWN)
				.strength(2.0F).sound(SoundType.WOOD)));
	}

	private static BlockBehaviour.Properties woodProperties()
	{
		return BlockBehaviour.Properties.of().strength(2.0F, 3.0F).sound(SoundType.WOOD);
	}

	private static WoodProducts woodProducts(String name)
	{
		var planks = registerWithItem(name + "_planks", () -> new Block(woodProperties()), CONSTRUCTION);
		var stairs = registerWithItem(name + "_stairs", () -> new StairBlock(planks.get().defaultBlockState(), woodProperties()), CONSTRUCTION);
		var slab = registerWithItem(name + "_slab", () -> new VerticalSlabBlock(woodProperties()), CONSTRUCTION);
		var fence = registerWithItem(name + "_fence", () -> new FenceBlock(woodProperties()), CONSTRUCTION);
		var gate = registerWithItem(name + "_gate", () -> new FenceGateBlock(WoodType.OAK, woodProperties()), CONSTRUCTION);
		var trapdoor = registerWithItem(name + "_trapdoor", () -> new TrapDoorBlock(BlockSetType.OAK, woodProperties().noOcclusion()), CONSTRUCTION);
		var door = registerWithItem(name + "_door", () -> new DoorBlock(BlockSetType.OAK, woodProperties().noOcclusion()), CONSTRUCTION);
		return new WoodProducts(planks, stairs, slab, fence, gate, trapdoor, door);
	}

	private static DeferredBlock<HangarLightBlock> hangarLight(String name)
	{
		return registerWithItem(name, () -> new HangarLightBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL)
				.noCollission().noOcclusion().lightLevel(state -> 15).strength(0.5F)), CONSTRUCTION);
	}

	private static BlockBehaviour.Properties minedStone(float strength)
	{
		return BlockBehaviour.Properties.of().strength(strength).requiresCorrectToolForDrops();
	}

	private static StoneProducts stoneProducts(String name, Supplier<BlockBehaviour.Properties> properties)
	{
		return stoneProducts(name, properties, null);
	}

	private static StoneProducts stoneProducts(String name, Supplier<BlockBehaviour.Properties> properties, List<DeferredBlock<?>> tab)
	{
		var reduced = reducedStoneProducts(name, properties, tab);
		var wall = registerWithItem(name + "_wall", () -> new WallBlock(properties.get()), tab);
		return new StoneProducts(reduced.block(), reduced.slab(), reduced.stairs(), wall);
	}

	private static ReducedStoneProducts reducedStoneProducts(String name, Supplier<BlockBehaviour.Properties> properties, List<DeferredBlock<?>> tab)
	{
		var block = registerWithItem(name, () -> new Block(properties.get()), tab);
		var slab = registerWithItem(name + "_slab", () -> new VerticalSlabBlock(properties.get()), tab);
		var stairs = registerWithItem(name + "_stairs", () -> new StairBlock(block.get().defaultBlockState(), properties.get()), tab);
		return new ReducedStoneProducts(block, slab, stairs);
	}

	private static ReducedStoneProducts wetStone(String name, StoneProducts dry, ReducedStoneProducts ruined, int dust)
	{
		var block = GalaxiesBlocks.<Block>registerWithItem(name, () -> new WetPourstone.FullBlock(
				new WetPourstone(dry.block(), ruined == null ? null : ruined.block()), new ColorRGBA(dust), wetStoneProperties()));
		var slab = GalaxiesBlocks.<VerticalSlabBlock>registerWithItem(name + "_slab", () -> new WetPourstone.Slab(
				new WetPourstone(dry.slab(), ruined == null ? null : ruined.slab()), wetStoneProperties()));
		var stairs = GalaxiesBlocks.<StairBlock>registerWithItem(name + "_stairs", () -> new WetPourstone.Stairs(
				new WetPourstone(dry.stairs(), ruined == null ? null : ruined.stairs()), block.get().defaultBlockState(), wetStoneProperties()));
		return new ReducedStoneProducts(block, slab, stairs);
	}

	private static BlockBehaviour.Properties wetStoneProperties()
	{
		return BlockBehaviour.Properties.of().sound(SoundType.GRAVEL).strength(0.5F).noCollission();
	}

	private static DeferredBlock<ImperialLighting> lightingPanel(String name, int brightness)
	{
		return registerWithItem(name, () -> new ImperialLighting(panel(MapColor.COLOR_GRAY)
				.lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? brightness : 0)), CONSTRUCTION);
	}

	private static BlockBehaviour.Properties panel(MapColor color)
	{
		return minedStone(1.5F).sound(SoundType.COPPER).mapColor(color);
	}

	private static BlockBehaviour.Properties glassProperties()
	{
		return BlockBehaviour.Properties.of().strength(0.3F).sound(SoundType.GLASS).noOcclusion()
				.isValidSpawn((state, level, pos, type) -> false)
				.isRedstoneConductor((state, level, pos) -> false)
				.isSuffocating((state, level, pos) -> false)
				.isViewBlocking((state, level, pos) -> false);
	}

	private static DeferredBlock<CorrugatedCrateBlock> crate(String name)
	{
		return registerWithItem(name, () -> new CorrugatedCrateBlock(BlockBehaviour.Properties.of()
				.sound(SoundType.METAL).strength(2.5F).noOcclusion()), CONSTRUCTION);
	}

	private static DeferredBlock<ColoredFallingBlock> fallingSand(String name, int dustColor)
	{
		return registerWithItem(name, () -> new ColoredFallingBlock(new ColorRGBA(dustColor),
				BlockBehaviour.Properties.of().sound(SoundType.SAND).strength(0.5F)));
	}

	private static DeferredBlock<AridPlant> aridPlant(String name)
	{
		return registerWithItem(name, () -> new AridPlant(BlockBehaviour.Properties.of().noCollission()
				.instabreak().offsetType(BlockBehaviour.OffsetType.XZ).sound(SoundType.GRASS)));
	}

	private static <T extends Block> DeferredBlock<T> registerWithItem(String name, Supplier<T> factory)
	{
		return registerWithItem(name, factory, null);
	}

	private static <T extends Block> DeferredBlock<T> registerWithItem(String name, Supplier<T> factory, List<DeferredBlock<?>> tab)
	{
		var block = BLOCKS.register(name, factory);
		if (tab != null)
			tab.add(block);
		ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
		return block;
	}

	public record StoneProducts(DeferredBlock<Block> block, DeferredBlock<VerticalSlabBlock> slab,
	                            DeferredBlock<StairBlock> stairs, DeferredBlock<WallBlock> wall)
	{
	}

	public record ReducedStoneProducts(DeferredBlock<Block> block, DeferredBlock<VerticalSlabBlock> slab,
	                                   DeferredBlock<StairBlock> stairs)
	{
	}

	public record WoodProducts(DeferredBlock<Block> planks, DeferredBlock<StairBlock> stairs,
	                           DeferredBlock<VerticalSlabBlock> slab, DeferredBlock<FenceBlock> fence,
	                           DeferredBlock<FenceGateBlock> gate, DeferredBlock<TrapDoorBlock> trapdoor,
	                           DeferredBlock<DoorBlock> door)
	{
		List<DeferredBlock<?>> blocks()
		{
			return List.of(planks, stairs, slab, fence, gate, trapdoor, door);
		}
	}

	private GalaxiesBlocks()
	{
	}
}
