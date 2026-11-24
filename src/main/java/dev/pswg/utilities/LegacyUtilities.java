package dev.pswg.utilities;

import dev.pswg.Galaxies;
import dev.pswg.gadgets.WorkstationRecipes;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class LegacyUtilities
{
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Galaxies.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Galaxies.MODID);
	private static final DeferredRegister<RecipeType<?>> RECIPES = DeferredRegister.create(Registries.RECIPE_TYPE, Galaxies.MODID);
	private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Galaxies.MODID);
	private static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Galaxies.MODID);

	public static final DeferredBlock<MoistureVaporator> VAPORATOR = block("gx8_moisture_vaporator", () -> new MoistureVaporator(machine().strength(10)));
	public static final DeferredBlock<PowerCoupling> COUPLING = block("power_coupling", () -> new PowerCoupling(machine()));
	public static final DeferredBlock<SlidingDoor> SLIDING_DOOR = block("sliding_door", () -> new SlidingDoor(machine()));
	public static final DeferredBlock<CreatureCage.Metal> CAGE = block("creature_cage", () -> new CreatureCage.Metal(cage()));
	public static final DeferredBlock<CreatureCage.Terrarium> TERRARIUM = block("creature_terrarium", () -> new CreatureCage.Terrarium(cage()));
	public static final List<DeferredBlock<CreatureCage.Terrarium>> DYED_TERRARIA = Arrays.stream(DyeColor.values())
			.map(color -> block(color.getName() + "_stained_creature_terrarium", () -> new CreatureCage.Terrarium(cage().mapColor(color))))
			.toList();
	public static final Supplier<Item> CABLE = ITEMS.register("insulated_desh_cable", PowerCoupling.Cable::new);
	public static final Supplier<net.minecraft.core.component.DataComponentType<GlobalPos>> CABLE_SOURCE = COMPONENTS.registerComponentType("cable_source",
			builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

	public static final Supplier<BlockEntityType<MoistureVaporator.Inventory>> VAPORATOR_ENTITY = ENTITIES.register("gx8_moisture_vaporator",
			() -> BlockEntityType.Builder.of(MoistureVaporator.Inventory::new, VAPORATOR.get()).build(null));
	public static final Supplier<BlockEntityType<PowerCoupling.Links>> COUPLING_ENTITY = ENTITIES.register("power_coupling",
			() -> BlockEntityType.Builder.of(PowerCoupling.Links::new, COUPLING.get()).build(null));
	public static final Supplier<BlockEntityType<SlidingDoor.Animation>> DOOR_ENTITY = ENTITIES.register("sliding_door",
			() -> BlockEntityType.Builder.of(SlidingDoor.Animation::new, SLIDING_DOOR.get()).build(null));
	public static final Supplier<BlockEntityType<CreatureCage.Contents>> CAGE_ENTITY = ENTITIES.register("creature_terrarium",
			() -> BlockEntityType.Builder.of(CreatureCage.Contents::new, Stream.concat(Stream.of(CAGE, TERRARIUM), DYED_TERRARIA.stream())
					.map(DeferredBlock::get).toArray(Block[]::new)).build(null));
	public static final Supplier<MenuType<MoistureVaporator.Menu>> VAPORATOR_MENU = MENUS.register("gx8_moisture_vaporator",
			() -> new MenuType<>(MoistureVaporator.Menu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final Supplier<RecipeType<VaporatorRecipe>> VAPORATING = RECIPES.register("vaporator", () -> RecipeType.simple(Galaxies.id("vaporator")));
	public static final Supplier<RecipeSerializer<VaporatorRecipe>> VAPORATOR_SERIALIZER = SERIALIZERS.register("vaporator",
			() -> new WorkstationRecipes.Serializer<>(VaporatorRecipe.CODEC, VaporatorRecipe.STREAM));

	private LegacyUtilities() {}

	private static BlockBehaviour.Properties machine()
	{
		return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).sound(SoundType.COPPER).noOcclusion().strength(3.5F).requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties cage()
	{
		return machine().isValidSpawn((state, level, pos, type) -> false).isRedstoneConductor((state, level, pos) -> false)
				.isViewBlocking((state, level, pos) -> false).isSuffocating((state, level, pos) -> false);
	}

	private static <T extends Block> DeferredBlock<T> block(String name, Supplier<T> block)
	{
		var registered = BLOCKS.register(name, block);
		ITEMS.register(name, () -> new BlockItem(registered.get(), new Item.Properties()));
		return registered;
	}

	public static void register(IEventBus bus)
	{
		BLOCKS.register(bus);
		ITEMS.register(bus);
		ENTITIES.register(bus);
		MENUS.register(bus);
		RECIPES.register(bus);
		SERIALIZERS.register(bus);
		COMPONENTS.register(bus);
		bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS))
				ITEMS.getEntries().forEach(item -> event.accept(item.get()));
		});
	}
}
