package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.block.TallLampBlock;
import dev.pswg.block.VerticalSlabBlock;
import dev.pswg.block.plant.AridPlant;
import dev.pswg.block.plant.HkakBushBlock;
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
import net.minecraft.world.level.block.ColoredFallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
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
	public static final DeferredBlock<AridPlant> TUBER_STALK = aridPlant("tuber_stalk");
	public static final DeferredBlock<HkakBushBlock> HKAK_BUSH = registerWithItem("hkak_bush",
			() -> new HkakBushBlock(BlockBehaviour.Properties.of().noCollission().randomTicks().instabreak().sound(SoundType.CROP)));
	public static final DeferredItem<Item> HKAK_BEAN = ITEMS.registerSimpleItem("hkak_bean",
			new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3F).build()));

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
	static
	{
		for (DyeColor color : DyeColor.values())
			stoneProducts(color.getName() + "_pourstone", () -> minedStone(1.25F));

		CONSTRUCTION.add(TALL_LAMP);
		TABS.register("worldgen_blocks", () -> CreativeModeTab.builder()
				.title(Component.translatable("pswg.worldgen_block_group"))
				.icon(() -> CANYON.block().toStack())
				.displayItems((parameters, output) -> {
					for (var block : BLOCKS.getEntries())
						if (!CONSTRUCTION.contains(block))
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
	}

	public static Collection<DeferredHolder<Block, ? extends Block>> entries()
	{
		return BLOCKS.getEntries();
	}

	private static DeferredBlock<Block> simpleStone(String name)
	{
		return registerWithItem(name, () -> new Block(BlockBehaviour.Properties.of().strength(0.5F)));
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

	private GalaxiesBlocks()
	{
	}
}
