package dev.pswg.blasters.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterDefinitions;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.BlasterNetwork;
import dev.pswg.blasters.BlasterState;
import dev.pswg.blasters.Blasters;
import dev.pswg.blasters.LegacyBlaster;
import dev.pswg.blasters.BlasterWorkbench;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.minecraft.world.item.ItemStack;
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
	private static final KeyMapping MODE = new KeyMapping("key.pswg.blaster_mode",
			InputConstants.Type.KEYSYM, InputConstants.KEY_Z, "category.pswg.keybinds");
	private static final KeyMapping PATROL = new KeyMapping("key.pswg.toggle_patrol_posture",
			InputConstants.Type.KEYSYM, InputConstants.KEY_G, "category.pswg.keybinds");
	private static final ResourceLocation LEGACY_HUD = Galaxies.id("textures/gui/blasters.png");
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
			event.register(MODE);
			event.register(PATROL);
		}

		@SubscribeEvent
		public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
		{
			event.registerEntityRenderer(Blasters.BOLT.get(), BlasterBoltRenderer::new);
		}

		@SubscribeEvent
		public static void itemRendering(RegisterClientExtensionsEvent event)
		{
			event.registerItem(new IClientItemExtensions()
			{
				private LegacyBlasterRenderer renderer;

				@Override
				public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack)
				{
					var definition = BlasterItem.definition(entity.level(), stack);
					if (definition == null || definition.legacy().isEmpty()) return null;
					if (entity instanceof Player player && player.getData(Blasters.PATROL))
						return HumanoidModel.ArmPose.ITEM;
					if (definition.legacy().get().oneHanded())
					{
						long lastFired = BlasterItem.state(stack).lastFired();
						return lastFired > 0 && entity.level().getGameTime() - lastFired < 10 || BlasterItem.state(stack).isAiming()
								? HumanoidModel.ArmPose.TOOT_HORN : HumanoidModel.ArmPose.ITEM;
					}
					return BlasterItem.state(stack).isAiming() ? HumanoidModel.ArmPose.BOW_AND_ARROW : HumanoidModel.ArmPose.CROSSBOW_HOLD;
				}

				@Override
				public BlockEntityWithoutLevelRenderer getCustomRenderer()
				{
					if (renderer == null)
						renderer = new LegacyBlasterRenderer(Minecraft.getInstance());
					return renderer;
				}
			}, Blasters.PRESETS.values().stream().map(item -> item.get()).toArray(net.minecraft.world.item.Item[]::new));
		}

		@SubscribeEvent
		public static void screens(RegisterMenuScreensEvent event)
		{
			event.register(BlasterWorkbench.MENU.get(), BlasterWorkbenchScreen::new);
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
		if (!event.isAttack() || event.isCanceled() || player == null || player.isSpectator() || player.getVehicle() instanceof dev.pswg.vehicles.Ship
				|| !BlasterItem.isBlaster(player.getItemInHand(BlasterItem.firingHand(player))))
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

		if (client.screen != null || client.isPaused() || player.isSpectator() || player.getVehicle() instanceof dev.pswg.vehicles.Ship)
		{
			attackDown = false;
			while (VENT.consumeClick()) {}
			while (MODE.consumeClick()) {}
			while (PATROL.consumeClick()) {}
			return;
		}

		if (VENT.consumeClick())
			send(player, BlasterNetwork.Input.VENT);
		if (PATROL.consumeClick())
			send(player, BlasterNetwork.Input.PATROL);
		if (MODE.consumeClick() && !dev.pswg.sabers.client.SaberClient.send(player, false))
			send(player, BlasterNetwork.Input.MODE);

		if (!client.options.keyAttack.isDown())
		{
			attackDown = false;
			return;
		}

		var stack = player.getItemInHand(BlasterItem.firingHand(player));
		if (!BlasterItem.isBlaster(stack))
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
				if (BlasterItem.firingMode(stack, definition) == LegacyBlaster.FiringMode.AUTOMATIC
						&& (state.mode() == BlasterState.Mode.PASSIVE || BlasterItem.heat(stack, definition, now, 0F) == 0F))
					send(player, BlasterNetwork.Input.REPEAT);
				nextRepeat = now + Math.max(1, Math.round(definition.stats().automaticRepeatDelay()
						/ BlasterItem.multiplier(stack, definition, "fire_rate_multiplier")));
			}
		}
	}

	public static void ventLegacy(Player player)
	{
		var held = player.getItemInHand(BlasterItem.firingHand(player));
		var definition = BlasterItem.definition(player.level(), held);
		if (definition != null && definition.legacy().isPresent())
			send(player, BlasterNetwork.Input.VENT);
	}

	private static void send(Player player, BlasterNetwork.Input input)
	{
		var hand = BlasterItem.firingHand(player);
		var stack = player.getItemInHand(hand);
		if (BlasterItem.isBlaster(stack))
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
	public static void hud(RenderGuiLayerEvent.Pre event)
	{
		var client = Minecraft.getInstance();
		var player = BlasterSight.firstPersonPlayer();
		if (!event.getName().equals(VanillaGuiLayers.CROSSHAIR) || player == null || client.level == null || client.options.hideGui)
			return;
		var stack = player.getItemInHand(BlasterItem.firingHand(player));
		if (!BlasterItem.isBlaster(stack))
			return;
		var definition = BlasterItem.definition(client.level, stack);
		if (definition == null || definition.legacy().isEmpty() && !BlasterItem.attachments(stack, definition).hud().equals(Blasters.DEFAULT_HUD))
			return;

		int width = event.getGuiGraphics().guiWidth();
		int height = event.getGuiGraphics().guiHeight();
		long time = client.level.getGameTime();
		float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
		var graphics = event.getGuiGraphics();
		boolean legacy = definition.legacy().isPresent();
		var texture = legacy ? LEGACY_HUD : HUD;
		int reticle = BlasterSight.reticle(stack, definition, legacy && player.getData(Blasters.PATROL));
		if (reticle >= 0)
		{
			event.setCanceled(true);
			graphics.flush();
			RenderSystem.enableBlend();
			RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
					GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
			graphics.blit(LEGACY_HUD, (width - 15) / 2, (height - 15) / 2, 62 + 16 * reticle, 0, 15, 15);
			graphics.flush();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableBlend();
		}

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		if (BlasterItem.dualWield(player))
		{
			for (InteractionHand hand : InteractionHand.values())
			{
				var held = client.player.getItemInHand(hand);
				var heldDefinition = BlasterItem.definition(client.level, held);
				if (heldDefinition == null)
					continue;
				graphics.pose().pushPose();
				graphics.pose().translate(hand == InteractionHand.MAIN_HAND ? 12 : width - 12, (height + HUD_WIDTH) / 2F, 0);
				graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-90));
				drawHeat(graphics, LEGACY_HUD, 0, 0, held, heldDefinition, time, partial);
				graphics.pose().popPose();
			}
		}
		else
			drawHeat(graphics, texture, (width - HUD_WIDTH) / 2, height / 2 + 30, stack, definition, time, partial);
		RenderSystem.disableBlend();
	}

	private static void drawHeat(GuiGraphics graphics, ResourceLocation texture, int x, int y, ItemStack stack,
	                             BlasterDefinition definition, long time, float partial)
	{
		var state = BlasterItem.state(stack);
		float heat = BlasterItem.heat(stack, definition, time, partial);
		float overcharge = BlasterItem.overcharge(state, definition, time, partial);
		float ready = definition.legacy().isEmpty() ? 0 : Math.max(0, stack.getOrDefault(Blasters.READY.get(), 0L) - time - partial);
		if (heat == 0 && overcharge == 0 && ready == 0 && definition.legacy().isPresent())
			return;
		graphics.blit(texture, x, y, 0, 0, HUD_WIDTH, 3);
		if (ready > 0)
		{
			graphics.blit(texture, x, y, 0, 32, HUD_WIDTH, 3);
			drawCursor(graphics, texture, x, y, ready / Math.max(1, definition.legacy().get().mechanics().readyTime()));
		}
		else if (overcharge > 0)
			graphics.blit(texture, x, y, 0, 12, (int) (HUD_WIDTH * overcharge), 3);
		else if (state.mode() == BlasterState.Mode.PASSIVE || state.mode() == BlasterState.Mode.REQUESTED_BYPASS || heat == 0)
		{
			int heatWidth = (int) (HUD_WIDTH * Mth.clamp(heat / Math.max(1, definition.stats().heat().capacity()), 0F, 1F));
			graphics.blit(texture, x, y, 0, 4, heatWidth, 3);
		}
		else
		{
			graphics.blit(texture, x, y, 0, 16, HUD_WIDTH, 3);
			if (state.mode() == BlasterState.Mode.OVERHEAT)
			{
				var cooling = definition.stats().cooling();
				drawWindow(graphics, texture, x, y, cooling.primaryBypassTime(), cooling.primaryBypassTolerance(), 8);
				drawWindow(graphics, texture, x, y, cooling.secondaryBypassTime(), cooling.secondaryBypassTolerance(), 12);
			}
			float basis = definition.legacy().isPresent()
					? definition.stats().heat().capacity() + definition.stats().heat().overheatPenalty() : state.lastVentingHeat();
			drawCursor(graphics, texture, x, y, heat / Math.max(1, basis));
		}
		graphics.blit(texture, x, y, 0, 20, HUD_WIDTH, 3);
	}

	private static void drawCursor(GuiGraphics graphics, ResourceLocation texture, int x, int y, float progress)
	{
		graphics.blit(texture, x + (int) (Mth.clamp(progress, 0F, 1F) * (HUD_WIDTH - 3)), y - 2, 0, 24, 3, 7);
	}

	private static void drawWindow(GuiGraphics graphics, ResourceLocation texture, int x, int y, float center, float tolerance, int textureY)
	{
		int start = (int) (HUD_WIDTH * Mth.clamp(center - tolerance, 0F, 1F));
		int end = (int) (HUD_WIDTH * Mth.clamp(center + tolerance, 0F, 1F));
		if (end > start)
			graphics.blit(texture, x + start, y, start, textureY, end - start, 3);
	}

	private BlasterClient()
	{
	}
}
