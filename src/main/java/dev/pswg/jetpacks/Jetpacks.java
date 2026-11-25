package dev.pswg.jetpacks;

import dev.pswg.Galaxies;
import dev.pswg.gadgets.WorkstationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.function.Supplier;

public final class Jetpacks
{
	private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Galaxies.MODID);
	private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Galaxies.MODID);
	private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Galaxies.MODID);

	public static final List<DeferredItem<Jetpack>> PACKS = List.of(pack("jumptrooper_jetpack"), pack("cj33_jetpack"),
			pack("jt12_jetpack"), pack("z6_jetpack"), pack("rebel_jump_pack"));
	public static final List<DeferredItem<BackEquipment>> ACCESSORIES = List.of(
			accessory("artillerytrooper_backpack", "artillerytrooper", "legacy/artillerytrooper"),
			accessory("sandtrooper_backpack", "sandtrooper", "legacy/sandtrooper"),
			accessory("incineratortrooper_tank", "incineratortrooper", "legacy/incineratortrooper"),
			accessory("brown_specialist_backpack", "specialist_backpack"), accessory("green_specialist_backpack", "specialist_backpack"),
			accessory("white_light_backpack", "light_backpack"), accessory("green_light_backpack", "light_backpack"),
			accessory("tan_light_backpack", "light_backpack"), accessory("white_heavy_backpack", "heavy_backpack"),
			accessory("green_heavy_backpack", "heavy_backpack"), accessory("tan_heavy_backpack", "heavy_backpack"));
	public static final Supplier<MenuType<Menu>> MENU = MENUS.register("jetpack_slot", () -> new MenuType<>(Menu::new, FeatureFlags.DEFAULT_FLAGS));
	public static final Supplier<AttachmentType<ItemStack>> SLOT = ATTACHMENTS.register("jetpack_slot",
			() -> AttachmentType.builder(() -> ItemStack.EMPTY).serialize(ItemStack.OPTIONAL_CODEC).build());
	static final Supplier<AttachmentType<Flight>> FLIGHT = ATTACHMENTS.register("jetpack_flight",
			() -> AttachmentType.builder(Flight::new).build());

	private Jetpacks() {}

	private static DeferredItem<Jetpack> pack(String id)
	{
		var model = id.equals("jumptrooper_jetpack") ? "jumptrooper" : id;
		return ITEMS.register(id, () -> new Jetpack(model));
	}

	private static DeferredItem<BackEquipment> accessory(String id, String model)
	{
		return accessory(id, model, id);
	}

	private static DeferredItem<BackEquipment> accessory(String id, String model, String texture)
	{
		return ITEMS.register(id, () -> new BackEquipment(model, texture, "backpack"));
	}

	public static void register(IEventBus bus)
	{
		ITEMS.register(bus);
		MENUS.register(bus);
		ATTACHMENTS.register(bus);
		bus.addListener(JetpackNetwork::register);
		bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
			if (event.getTabKey().equals(CreativeModeTabs.COMBAT))
			{
				PACKS.forEach(item -> event.accept(item.get()));
				ACCESSORIES.forEach(item -> event.accept(item.get()));
			}
		});
		var events = NeoForge.EVENT_BUS;
		events.addListener(Jetpacks::tick);
		events.addListener(Jetpacks::drop);
		events.addListener(Jetpacks::clonePlayer);
		events.addListener(Jetpacks::playerJoin);
		events.addListener(Jetpacks::startTracking);
		events.addListener(Jetpacks::changedDimension);
		events.addListener(Jetpacks::respawn);
	}

	private static void playerJoin(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			JetpackNetwork.broadcast(player);
	}

	private static void respawn(PlayerEvent.PlayerRespawnEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
			JetpackNetwork.broadcast(player);
	}

	private static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
	{
		if (event.getEntity() instanceof ServerPlayer player)
		{
			player.removeData(FLIGHT);
			JetpackNetwork.broadcast(player);
		}
	}

	private static void startTracking(PlayerEvent.StartTracking event)
	{
		if (event.getEntity() instanceof ServerPlayer viewer && event.getTarget() instanceof ServerPlayer player)
			JetpackNetwork.send(viewer, player);
	}

	private static void clonePlayer(PlayerEvent.Clone event)
	{
		if (!event.isWasDeath() || event.getOriginal().level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))
			event.getEntity().setData(SLOT, equipped(event.getOriginal()).copy());
	}

	private static void drop(LivingDropsEvent event)
	{
		if (!(event.getEntity() instanceof ServerPlayer player) || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))
			return;
		var stack = player.removeData(SLOT);
		if (stack != null && !stack.isEmpty())
			event.getDrops().add(new ItemEntity(player.level(), player.getX(), player.getY(), player.getZ(), stack.copy()));
	}

	public static ItemStack equipped(Player player)
	{
		return player.getData(SLOT);
	}

	public static boolean active(Player player)
	{
		return player.getData(FLIGHT).active;
	}

	static boolean canThrust(Player player)
	{
		return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.isSleeping()
				&& !player.isUnderWater() && !player.getAbilities().flying && player.containerMenu == player.inventoryMenu
				&& equipped(player).getItem() instanceof Jetpack;
	}

	public static boolean authorizedFlight(ServerPlayer player)
	{
		return canThrust(player) && active(player) && player.level().getGameTime() - player.getData(FLIGHT).lastInput < 6;
	}

	public static void equip(ServerPlayer player, ItemStack stack)
	{
		if (!stack.isEmpty() && !(stack.getItem() instanceof BackEquipment))
			return;
		player.setData(SLOT, stack.copyWithCount(1));
		player.removeData(FLIGHT);
		JetpackNetwork.broadcast(player);
	}

	private static void tick(PlayerTickEvent.Post event)
	{
		if (!(event.getEntity() instanceof ServerPlayer player))
			return;
		var flight = player.getData(FLIGHT);
		boolean wasActive = flight.active;
		int bits = player.level().getGameTime() - flight.lastInput < 6 ? flight.keys : 0;
		boolean up = (bits & JetpackNetwork.ASCEND) != 0 && (bits & JetpackNetwork.DESCEND) == 0 && canThrust(player);
		flight.active = up;
		if (wasActive != up)
			JetpackNetwork.broadcast(player);
		if (!up)
		{
			flight.force = 0;
			return;
		}

		boolean cruise = (bits & JetpackNetwork.CRUISE) != 0 && !player.onGround();
		flight.force = Math.min(cruise ? 0.075 : 0.15, Math.max(0.08, flight.force) + 0.004);
		var motion = player.getDeltaMovement();
		var look = Vec3.directionFromRotation(player.getXRot(), player.getYRot());
		var horizontal = new Vec3(look.x, 0, look.z).normalize();
		var right = new Vec3(-horizontal.z, 0, horizontal.x);
		int forward = ((bits & JetpackNetwork.FORWARD) != 0 ? 1 : 0) - ((bits & JetpackNetwork.BACK) != 0 ? 1 : 0);
		int strafe = ((bits & JetpackNetwork.RIGHT) != 0 ? 1 : 0) - ((bits & JetpackNetwork.LEFT) != 0 ? 1 : 0);
		var acceleration = cruise ? look.scale(flight.force) : new Vec3(0, flight.force, 0)
				.add(horizontal.scale(forward * 0.018)).add(right.scale(strafe * 0.018));
		player.setDeltaMovement(motion.scale(cruise ? 0.95 : 0.98).add(acceleration));
		player.fallDistance = 0;
		player.hurtMarked = true;
	}

	static void open(ServerPlayer player)
	{
		if (!player.isAlive() || player.isSpectator() || player.isPassenger() || player.containerMenu != player.inventoryMenu)
			return;
		var inventory = new SimpleContainer(equipped(player).copy())
		{
			@Override
			public void setChanged()
			{
				super.setChanged();
				var stack = getItem(0);
				if (!ItemStack.matches(equipped(player), stack))
					equip(player, stack);
			}
		};
		player.openMenu(new SimpleMenuProvider((id, slots, viewer) -> new Menu(id, slots, inventory),
				Component.translatable("container.pswg.jetpack_slot")));
	}

	public static final class Menu extends WorkstationMenu
	{
		public Menu(int id, Inventory inventory)
		{
			this(id, inventory, new SimpleContainer(1));
		}

		private Menu(int id, Inventory inventory, Container equipped)
		{
			super(MENU.get(), id, equipped);
			addMachineSlot(0, 80, 30, 1, stack -> stack.getItem() instanceof BackEquipment);
			addPlayerSlots(inventory, 74);
		}
	}

	public static class BackEquipment extends Item
	{
		private final String model;
		private final String texture;
		private final String part;

		private BackEquipment(String model, String texture, String part)
		{
			super(new Properties().stacksTo(1));
			this.model = model;
			this.texture = texture;
			this.part = part;
		}

		public String model() { return model; }
		public String texture() { return texture; }
		public String part() { return part; }

		@Override
		public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand)
		{
			var held = user.getItemInHand(hand);
			if (!equipped(user).isEmpty())
				return InteractionResultHolder.pass(held);
			if (user instanceof ServerPlayer player)
			{
				equip(player, held.copyWithCount(1));
				held.shrink(1);
			}
			return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
		}
	}

	public static final class Jetpack extends BackEquipment
	{
		private Jetpack(String model) { super(model, model, "jetpack"); }
	}

	static final class Flight
	{
		long lastInput = -100;
		int keys;
		double force;
		boolean active;
	}
}
