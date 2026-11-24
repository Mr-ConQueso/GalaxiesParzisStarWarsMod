package dev.pswg.container;

import dev.pswg.Galaxies;
import dev.pswg.block.CorrugatedCrateBlock;
import dev.pswg.blockEntity.CorrugatedCrateBlockEntity;
import dev.pswg.screen.CorrugatedCrateMenu;
import net.minecraft.core.registries.Registries;
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

	public static void register(IEventBus bus)
	{
		ENTITIES.register(bus);
		MENUS.register(bus);
		bus.addListener(Crates::registerCapabilities);
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event)
	{
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), (crate, side) -> new InvWrapper(crate));
	}

	private Crates()
	{
	}
}
