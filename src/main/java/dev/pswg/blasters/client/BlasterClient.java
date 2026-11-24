package dev.pswg.blasters.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterDefinitions;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.BlasterNetwork;
import dev.pswg.blasters.BlasterState;
import dev.pswg.blasters.Blasters;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class BlasterClient
{
	private static final KeyMapping VENT = new KeyMapping("key.pswg.primary_action",
			InputConstants.Type.KEYSYM, InputConstants.KEY_V, "category.pswg.keybinds");
	private static final ResourceLocation HUD = Blasters.id("textures/gui/hud_elements.png");
	private static final int HUD_WIDTH = 61;
	private static boolean attackDown;
	private static long nextRepeat;
	private static Map<ResourceLocation, BlasterDefinition> lastCreativeDefinitions;

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void registerKeys(RegisterKeyMappingsEvent event)
		{
			event.register(VENT);
		}

		@SubscribeEvent
		public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
		{
			event.registerEntityRenderer(Blasters.BOLT.get(), BlasterBoltRenderer::new);
		}

		@SubscribeEvent
		public static void setup(FMLClientSetupEvent event)
		{
			event.enqueueWork(() -> ItemProperties.register(Blasters.ITEM.get(), Blasters.id("bipod"),
					(stack, level, entity, seed) -> {
						var definition = BlasterDefinitions.client().get(stack.get(Blasters.ID.get()));
						var equipped = BlasterItem.attachments(stack, definition).applied();
						return Blasters.id("e11/bipod").equals(equipped.get(Blasters.id("barrel_slot"))) ? 1F : 0F;
					}));
		}
	}

	@SubscribeEvent
	public static void preventVanillaAttack(InputEvent.InteractionKeyMappingTriggered event)
	{
		var client = Minecraft.getInstance();
		var player = client.player;
		if (!event.isAttack() || player == null || player.isSpectator()
				|| !player.getItemInHand(BlasterItem.firingHand(player)).is(Blasters.ITEM.get()))
			return;

		if (!attackDown)
		{
			send(player, BlasterNetwork.Input.PRESS);
			attackDown = true;
			nextRepeat = player.level().getGameTime() + 1;
		}
		event.setSwingHand(false);
		event.setCanceled(true);
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event)
	{
		var client = Minecraft.getInstance();
		var player = client.player;
		if (player == null || client.level == null)
			return;

		var definitions = BlasterDefinitions.client();
		if (lastCreativeDefinitions != definitions)
		{
			lastCreativeDefinitions = definitions;
			var flags = client.level.enabledFeatures();
			boolean operator = player.canUseGameMasterBlocks();
			if (!CreativeModeTabs.tryRebuildTabContents(flags, operator, client.level.registryAccess()))
				BuiltInRegistries.CREATIVE_MODE_TAB.get(CreativeModeTabs.COMBAT)
						.buildContents(new CreativeModeTab.ItemDisplayParameters(flags, operator, client.level.registryAccess()));
		}

		if (client.screen != null || client.isPaused() || player.isSpectator())
		{
			attackDown = false;
			while (VENT.consumeClick()) {}
			return;
		}

		if (VENT.consumeClick())
			send(player, BlasterNetwork.Input.VENT);

		if (!client.options.keyAttack.isDown())
		{
			attackDown = false;
			return;
		}

		var stack = player.getItemInHand(BlasterItem.firingHand(player));
		if (!stack.is(Blasters.ITEM.get()))
			return;

		long now = client.level.getGameTime();
		if (!attackDown)
		{
			send(player, BlasterNetwork.Input.PRESS);
			attackDown = true;
			nextRepeat = now + 1;
		}
		else if (now >= nextRepeat && BlasterItem.state(stack).fireCooldown() <= now)
		{
			var definition = BlasterItem.definition(client.level, stack);
			if (definition != null)
			{
				var state = BlasterItem.state(stack);
				if (state.mode() == BlasterState.Mode.PASSIVE || BlasterItem.heat(stack, definition, now, 0F) == 0F)
					send(player, BlasterNetwork.Input.REPEAT);
				nextRepeat = now + Math.max(1, Math.round(definition.stats().automaticRepeatDelay()
						/ BlasterItem.multiplier(stack, definition, "fire_rate_multiplier")));
			}
		}
	}

	private static void send(Player player, BlasterNetwork.Input input)
	{
		var hand = BlasterItem.firingHand(player);
		var stack = player.getItemInHand(hand);
		if (stack.is(Blasters.ITEM.get()))
		{
			var serial = stack.get(Blasters.SERIAL.get());
			if (serial != null)
				PacketDistributor.sendToServer(new BlasterNetwork.Action(hand, input, serial));
		}
	}

	@SubscribeEvent
	public static void logout(ClientPlayerNetworkEvent.LoggingOut event)
	{
		BlasterDefinitions.updateClient(Map.of());
		lastCreativeDefinitions = null;
		attackDown = false;
	}

	@SubscribeEvent
	public static void fov(ComputeFovModifierEvent event)
	{
		var player = event.getPlayer();
		var stack = player.getItemInHand(BlasterItem.firingHand(player));
		if (stack.is(Blasters.ITEM.get()) && BlasterItem.state(stack).isAiming())
		{
			var definition = BlasterItem.definition(player.level(), stack);
			if (definition != null)
				event.setNewFovModifier(1F / (3F * BlasterItem.multiplier(stack, definition, "zoom_multiplier")));
		}
	}

	@SubscribeEvent
	public static void hud(RenderGuiLayerEvent.Post event)
	{
		var client = Minecraft.getInstance();
		if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || client.player == null || client.level == null
				|| !client.options.getCameraType().isFirstPerson())
			return;
		var stack = client.player.getItemInHand(BlasterItem.firingHand(client.player));
		if (!stack.is(Blasters.ITEM.get()))
			return;
		var definition = BlasterItem.definition(client.level, stack);
		if (definition == null || !BlasterItem.attachments(stack, definition).hud().equals(Blasters.DEFAULT_HUD))
			return;

		int x = (event.getGuiGraphics().guiWidth() - HUD_WIDTH) / 2;
		int y = event.getGuiGraphics().guiHeight() / 2 + 30;
		long time = client.level.getGameTime();
		float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		var graphics = event.getGuiGraphics();
		var state = BlasterItem.state(stack);
		float heat = BlasterItem.heat(stack, definition, time, partial);
		float overcharge = BlasterItem.overcharge(state, definition, time, partial);
		graphics.blit(HUD, x, y, 0, 0, HUD_WIDTH, 3);
		if (overcharge > 0)
		{
			graphics.blit(HUD, x, y, 0, 12, HUD_WIDTH, 3);
			drawCursor(graphics, x, y, overcharge);
		}
		else if (state.mode() == BlasterState.Mode.PASSIVE || heat == 0F)
		{
			int heatWidth = (int) (HUD_WIDTH * Mth.clamp(heat / Math.max(1, definition.stats().heat().capacity()), 0F, 1F));
			graphics.blit(HUD, x, y, 0, 4, heatWidth, 3);
		}
		else
		{
			graphics.blit(HUD, x, y, 0, 16, HUD_WIDTH, 3);
			if (state.mode() == BlasterState.Mode.OVERHEAT)
			{
				var cooling = definition.stats().cooling();
				drawWindow(graphics, x, y, cooling.primaryBypassTime(), cooling.primaryBypassTolerance(), 8);
				drawWindow(graphics, x, y, cooling.secondaryBypassTime(), cooling.secondaryBypassTolerance(), 12);
			}
			drawCursor(graphics, x, y, heat / state.lastVentingHeat());
		}
		graphics.blit(HUD, x, y, 0, 20, HUD_WIDTH, 3);
	}

	private static void drawCursor(GuiGraphics graphics, int x, int y, float progress)
	{
		graphics.blit(HUD, x + (int) (Mth.clamp(progress, 0F, 1F) * (HUD_WIDTH - 3)), y - 2, 0, 24, 3, 7);
	}

	private static void drawWindow(GuiGraphics graphics, int x, int y, float center, float tolerance, int textureY)
	{
		int start = (int) (HUD_WIDTH * Mth.clamp(center - tolerance, 0F, 1F));
		int end = (int) (HUD_WIDTH * Mth.clamp(center + tolerance, 0F, 1F));
		if (end > start)
			graphics.blit(HUD, x + start, y, start, textureY, end - start, 3);
	}

	private BlasterClient()
	{
	}
}
