package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.block.CorrugatedCrateBlock;
import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import dev.pswg.screen.CorrugatedCrateMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.Supplier;

public final class Crates
{
	private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Galaxies.MODID);

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorrugatedCrateBlockEntity>> ENTITY = ENTITIES.register("corrugated_crate",
			() -> BlockEntityType.Builder.of(CorrugatedCrateBlockEntity::new, GalaxiesBlocks.entries().stream()
					.map(DeferredHolder::get)
					.filter(CorrugatedCrateBlock.class::isInstance)
					.toArray(Block[]::new)).build(null));
	public static final DeferredHolder<MenuType<?>, MenuType<CorrugatedCrateMenu>> MENU = MENUS.register("corrugated_crate",
			() -> new MenuType<>(CorrugatedCrateMenu::new, FeatureFlags.DEFAULT_FLAGS));

	public static final DeferredHolder<MenuType<?>, MenuType<CorrugatedCrateMenu>> KYBER_MENU = legacyMenu("kyber_crate", 13, () -> Crates.KYBER_MENU.get());
	public static final DeferredHolder<MenuType<?>, MenuType<CorrugatedCrateMenu>> TOOLBOX_MENU = legacyMenu("toolbox", 5, () -> Crates.TOOLBOX_MENU.get());
	public static final DeferredHolder<MenuType<?>, MenuType<CorrugatedCrateMenu>> SEGMENTED_MENU = legacyMenu("segmented_crate", 5, () -> Crates.SEGMENTED_MENU.get());

	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorrugatedCrateBlockEntity>> KYBER = legacyEntity("kyber_crate", 13,
			KYBER_MENU, () -> Crates.KYBER.get(), () -> LegacyConstruction.KYBER_CRATES.stream().map(DeferredHolder::get).toArray(Block[]::new));
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorrugatedCrateBlockEntity>> TOOLBOX = legacyEntity("toolbox", 5,
			TOOLBOX_MENU, () -> Crates.TOOLBOX.get(), () -> new Block[] { LegacyConstruction.TOOLBOX.get() });
	public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CorrugatedCrateBlockEntity>> SEGMENTED = legacyEntity("segmented_crate", 5,
			SEGMENTED_MENU, () -> Crates.SEGMENTED.get(), () -> LegacyConstruction.SEGMENTED_CRATES.stream().map(DeferredHolder::get).toArray(Block[]::new));

	private static DeferredHolder<MenuType<?>, MenuType<CorrugatedCrateMenu>> legacyMenu(String name, int columns,
	                                                                                  Supplier<MenuType<CorrugatedCrateMenu>> type)
	{
		return MENUS.register(name, () -> new MenuType<>((id, inventory) ->
				new CorrugatedCrateMenu(type.get(), id, inventory, new SimpleContainer(columns * 3), columns), FeatureFlags.DEFAULT_FLAGS));
	}

	private static DeferredHolder<BlockEntityType<?>, BlockEntityType<CorrugatedCrateBlockEntity>> legacyEntity(String name, int columns,
	                                      Supplier<MenuType<CorrugatedCrateMenu>> menu, Supplier<BlockEntityType<CorrugatedCrateBlockEntity>> type,
	                                      Supplier<Block[]> blocks)
	{
		return ENTITIES.register(name, () -> BlockEntityType.Builder.of((pos, state) ->
				new CorrugatedCrateBlockEntity(type.get(), menu, pos, state, name, columns), blocks.get()).build(null));
	}

	public static void register(IEventBus bus)
	{
		ENTITIES.register(bus);
		MENUS.register(bus);
		bus.addListener(Crates::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event)
	{
		for (var type : List.of(ENTITY, KYBER, TOOLBOX, SEGMENTED))
			event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type.get(), (crate, side) -> new InvWrapper(crate));
	}

	private Crates()
	{
	}
}
