package dev.pswg.blasters;

import dev.pswg.Galaxies;
import dev.pswg.container.GalaxiesBlocks;
import dev.pswg.block.WideBlock;
import dev.pswg.gadgets.WorkstationInventory;
import dev.pswg.gadgets.WorkstationMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class BlasterWorkbench extends WorkstationInventory
{
	private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Galaxies.MODID);
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Galaxies.MODID);
	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Galaxies.MODID);

	public static final Supplier<BlockEntityType<BlasterWorkbench>> ENTITY = ENTITIES.register("blaster_workbench",
			() -> BlockEntityType.Builder.of(BlasterWorkbench::new, BlasterWorkbench.BLOCK.get()).build(null));
	public static final Supplier<WideBlock<BlasterWorkbench>> BLOCK = BLOCKS.register("blaster_workbench",
			() -> new WideBlock<>(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(3.5F).requiresCorrectToolForDrops().noOcclusion(), ENTITY));
	public static final Supplier<MenuType<Menu>> MENU = MENUS.register("blaster_workbench",
			() -> new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS));
	private static final Supplier<net.minecraft.world.item.BlockItem> ITEM = ITEMS.registerSimpleBlockItem("blaster_workbench", BLOCK);

	public BlasterWorkbench(BlockPos pos, BlockState state)
	{
		super(ENTITY.get(), pos, state, 6);
	}

	public static void register(IEventBus bus)
	{
		BLOCKS.register(bus);
		ITEMS.register(bus);
		ENTITIES.register(bus);
		MENUS.register(bus);
		bus.addListener((net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey().equals(GalaxiesBlocks.CONSTRUCTION_TAB))
				event.accept(ITEM.get());
		});
	}

	@Override
	protected Component getDefaultName()
	{
		return Component.translatable("container.pswg.blaster_workbench");
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory)
	{
		return new Menu(id, inventory, this);
	}

	public static boolean toggleAttachment(ItemStack stack, BlasterDefinition definition, ResourceLocation id)
	{
		var option = definition.attachments().options().get(id);
		if (!BlasterItem.isBlaster(stack) || option == null || option.slots().isEmpty())
			return false;
		var equipment = BlasterItem.attachments(stack, definition);
		var installed = new HashMap<>(equipment.applied());
		var required = definition.legacy().map(LegacyBlaster::requiredSlots).orElse(List.of());
		if (installed.containsValue(id))
			installed.values().removeIf(id::equals);
		else
		{
			var conflicts = option.slots().stream().map(installed::get).filter(java.util.Objects::nonNull).toList();
			installed.values().removeIf(conflicts::contains);
			option.slots().forEach(slot -> installed.put(slot, id));
		}
		for (var slot : required)
		{
			if (installed.containsKey(slot))
				continue;
			var fallback = definition.attachments().defaults().get(slot);
			var defaultOption = fallback == null ? null : definition.attachments().options().get(fallback);
			if (defaultOption == null || defaultOption.slots().stream().anyMatch(installed::containsKey))
				return false;
			defaultOption.slots().forEach(empty -> installed.put(empty, fallback));
		}
		var staged = new Blasters.EquippedAttachments(equipment.hud(), installed);
		if (staged.equals(equipment) || !isValid(definition, staged))
			return false;
		stack.set(Blasters.ATTACHMENTS.get(), staged);
		return true;
	}

	private static boolean isValid(BlasterDefinition definition, Blasters.EquippedAttachments attachments)
	{
		var installed = attachments.applied();
		var required = definition.legacy().map(LegacyBlaster::requiredSlots).orElse(List.of());
		if (!installed.keySet().containsAll(required))
			return false;
		for (var entry : installed.entrySet())
		{
			var option = definition.attachments().options().get(entry.getValue());
			if (option == null || !option.slots().contains(entry.getKey())
					|| option.slots().stream().anyMatch(slot -> !entry.getValue().equals(installed.get(slot))))
				return false;
		}
		return true;
	}

	public static final class Menu extends WorkstationMenu
	{
		private final Level level;

		public Menu(int id, Inventory player)
		{
			this(id, player, new SimpleContainer(6));
		}

		private Menu(int id, Inventory player, Container inventory)
		{
			super(MENU.get(), id, inventory);
			level = player.player.level();
			addMachineSlot(0, 19, 19, 1, BlasterItem::isBlaster);
			for (int slot = 1; slot < 6; slot++)
				addMachineSlot(slot, 19, 45 + (slot - 1) * 18, 1, stack -> false);
			addPlayerSlots(player, 174);
		}

		public ItemStack weapon()
		{
			return inventory.getItem(0);
		}

		public BlasterDefinition definition()
		{
			return BlasterItem.definition(level, weapon());
		}

		public List<Map.Entry<ResourceLocation, BlasterDefinition.Attachment>> options()
		{
			var definition = definition();
			return definition == null ? List.of() : definition.attachments().options().entrySet().stream()
					.sorted(Map.Entry.comparingByKey()).toList();
		}

		public boolean build(Player player, ItemStack original, Blasters.EquippedAttachments desired)
		{
			if (player.containerMenu != this || player.isSpectator() || !stillValid(player) || level.isClientSide)
				return false;
			var weapon = weapon();
			var definition = definition();
			if (definition == null || !ItemStack.matches(weapon, original)
					|| !BlasterItem.attachments(weapon, definition).hud().equals(desired.hud()) || !isValid(definition, desired))
				return false;
			weapon.set(Blasters.ATTACHMENTS.get(), desired);
			inventory.setChanged();
			broadcastChanges();
			return true;
		}
	}
}
