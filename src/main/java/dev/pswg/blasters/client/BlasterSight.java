package dev.pswg.blasters.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.Blasters;
import dev.pswg.vehicles.Ship;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class BlasterSight
{
	private static final ResourceLocation STANDARD = Galaxies.id("textures/gui/overlay/default.png");
	private static final ResourceLocation SNIPER = Galaxies.id("textures/gui/overlay/sniper.png");
	private static final int SCOPE_TEXTURE_SIZE = 256;
	private static ItemStack aimedStack = ItemStack.EMPTY;
	private static float previousProgress;
	private static float progress;

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void registerOverlay(RegisterGuiLayersEvent event)
		{
			event.registerBelow(VanillaGuiLayers.CROSSHAIR, Blasters.id("scope"), BlasterSight::renderOverlay);
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event)
	{
		var player = firstPersonPlayer();
		var stack = player == null ? ItemStack.EMPTY : player.getItemInHand(BlasterItem.firingHand(player));
		if (!sameBlaster(stack, aimedStack))
			progress = 0;
		aimedStack = stack;
		previousProgress = progress;
		boolean aiming = player != null && !player.isSprinting() && BlasterItem.isBlaster(stack) && BlasterItem.state(stack).isAiming();
		progress = Mth.clamp(progress + (aiming ? 0.2F : -0.2F), 0F, 1F);
	}

	@SubscribeEvent
	public static void logout(ClientPlayerNetworkEvent.LoggingOut event)
	{
		aimedStack = ItemStack.EMPTY;
		previousProgress = progress = 0;
	}

	@SubscribeEvent
	public static void fov(ViewportEvent.ComputeFov event)
	{
		var player = firstPersonPlayer();
		// Minecraft computes another FOV for the hands. Zoom the world projection only.
		if (event.usedConfiguredFov() && player != null && event.getCamera().getEntity() == player)
			event.setFOV(event.getFOV() / zoom((float) event.getPartialTick()));
	}

	public static float zoom(float partial)
	{
		var player = firstPersonPlayer();
		if (player != null)
		{
			var stack = player.getItemInHand(BlasterItem.firingHand(player));
			if (!BlasterItem.isBlaster(stack) || !sameBlaster(stack, aimedStack))
				return 1F;
			var definition = BlasterItem.definition(player.level(), stack);
			if (definition != null)
			{
				float target = Math.max(1F, BlasterItem.zoom(stack, definition));
				return 1F / Mth.lerp(amount(stack, partial), 1F, 1F / target);
			}
		}
		return 1F;
	}

	private static boolean sameBlaster(ItemStack first, ItemStack second)
	{
		return first == second || BlasterItem.isBlaster(first) && first.is(second.getItem())
				&& first.has(Blasters.SERIAL.get()) && Objects.equals(first.get(Blasters.SERIAL.get()), second.get(Blasters.SERIAL.get()));
	}

	static float amount(ItemStack stack, float partial)
	{
		if (!sameBlaster(stack, aimedStack))
			return 0;
		float t = Mth.lerp(partial, previousProgress, progress);
		return t * t * (3 - 2 * t);
	}

	@Nullable
	static BlasterDefinition.Scope scope(ItemStack stack, BlasterDefinition definition)
	{
		if (BlasterItem.hasFunction(stack, definition, "sniper_scope"))
			return BlasterDefinition.Scope.SNIPER;
		if (BlasterItem.hasFunction(stack, definition, "default_scope"))
			return BlasterDefinition.Scope.STANDARD;
		return BlasterItem.equippedOptions(stack, definition).flatMap(option -> option.scope().stream()).findFirst().orElse(null);
	}

	static int reticle(ItemStack stack, BlasterDefinition definition, boolean patrol)
	{
		if (!BlasterItem.state(stack).isAiming())
			return patrol ? 11 : -1;
		var legacy = definition.legacy().orElse(null);
		if (legacy == null)
		{
			var optic = scope(stack, definition);
			return optic == BlasterDefinition.Scope.SNIPER ? 2 : optic == BlasterDefinition.Scope.STANDARD ? 0 : -1;
		}

		// The legacy presets used attachment layer order to select the first reticle override.
		return BlasterItem.equippedOptions(stack, definition)
				.sorted(Comparator.comparing(option -> option.slots().getFirst()))
				.mapToInt(BlasterSight::attachmentReticle)
				.filter(index -> index >= 0)
				.findFirst().orElse(legacy.crosshair());
	}

	private static int attachmentReticle(BlasterDefinition.Attachment attachment)
	{
		if (attachment.scope().orElse(null) == BlasterDefinition.Scope.SNIPER)
			return 2;
		if (!attachment.function().getNamespace().equals(Blasters.NAMESPACE))
			return -1;
		return switch (attachment.function().getPath())
		{
			case "sniper_scope" -> 2;
			case "waterproof_firing" -> 4;
			case "reduce_recoil", "recoil_multiplier" -> 5;
			case "reduce_spread", "spread_multiplier" -> 9;
			default -> -1;
		};
	}

	@Nullable
	static LocalPlayer firstPersonPlayer()
	{
		var client = Minecraft.getInstance();
		var player = client.player;
		return player != null && client.getCameraEntity() == player && client.options.getCameraType().isFirstPerson()
				&& !player.isSpectator() && !player.isSleeping() && !(player.getVehicle() instanceof Ship) ? player : null;
	}

	private static void renderOverlay(GuiGraphics graphics, DeltaTracker delta)
	{
		var player = firstPersonPlayer();
		if (player == null || Minecraft.getInstance().options.hideGui || !BlasterItem.isBlaster(aimedStack))
			return;
		var definition = BlasterItem.definition(player.level(), aimedStack);
		if (definition == null)
			return;
		var scope = scope(aimedStack, definition);
		float opacity = amount(aimedStack, delta.getGameTimeDeltaPartialTick(false));
		if (scope == null || opacity <= 0)
			return;

		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int size = Math.min(width, height);
		int x = (width - size) / 2;
		int y = (height - size) / 2;
		int shade = (int) (255 * opacity) << 24;
		graphics.fill(0, 0, width, y, shade);
		graphics.fill(0, y + size, width, height, shade);
		graphics.fill(0, y, x, y + size, shade);
		graphics.fill(x + size, y, width, y + size, shade);
		graphics.flush();

		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		graphics.setColor(1, 1, 1, opacity);
		graphics.blit(scope == BlasterDefinition.Scope.SNIPER ? SNIPER : STANDARD, x, y, size, size,
				0F, 0F, SCOPE_TEXTURE_SIZE, SCOPE_TEXTURE_SIZE, SCOPE_TEXTURE_SIZE, SCOPE_TEXTURE_SIZE);
		graphics.setColor(1, 1, 1, 1);
		RenderSystem.disableBlend();
	}

	@SubscribeEvent
	public static void renderHand(RenderHandEvent event)
	{
		var player = firstPersonPlayer();
		if (player == null)
			return;
		float partial = event.getPartialTick();
		var active = player.getItemInHand(BlasterItem.firingHand(player));
		var activeDefinition = BlasterItem.isBlaster(active) ? BlasterItem.definition(player.level(), active) : null;
		if (activeDefinition != null && scope(active, activeDefinition) != null
				&& (amount(active, partial) > 0 || BlasterItem.state(active).isAiming()))
		{
			event.setCanceled(true);
			return;
		}

		var stack = event.getItemStack();
		if (stack.isEmpty() && event.getHand() == InteractionHand.MAIN_HAND && activeDefinition != null
				&& !activeDefinition.legacy().map(legacy -> legacy.oneHanded()).orElse(false)
				&& BlasterItem.firingHand(player) == InteractionHand.OFF_HAND)
		{
			event.setCanceled(true);
			return;
		}
		if (!stack.is(Blasters.ITEM.get()))
			return;

		event.setCanceled(true);
		var arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		int side = arm == HumanoidArm.RIGHT ? 1 : -1;
		float ads = amount(stack, partial);
		float kick = shotKick(shotAge(stack, partial));
		var pose = event.getPoseStack();
		pose.pushPose();
		// The G3D barrel is centered around x = 0.5. Align it in the fixed hand projection.
		pose.translate(side * Mth.lerp(ads, 0.38F, 0F),
				Mth.lerp(ads, -0.21F, -0.035F) - 0.6F * event.getEquipProgress(), -0.73F + 0.12F * kick);
		pose.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(ads, 8F, 0F)));
		pose.mulPose(Axis.XP.rotationDegrees(-4F * kick));
		pose.scale(0.24F, 0.24F, 0.24F);
		var renderer = Minecraft.getInstance().getItemRenderer();
		var model = renderer.getModel(stack, player.level(), player, player.getId());
		renderer.render(stack, ItemDisplayContext.NONE, false, pose, event.getMultiBufferSource(),
				event.getPackedLight(), OverlayTexture.NO_OVERLAY, model);
		pose.popPose();
	}

	static float shotAge(ItemStack stack, float partial)
	{
		var level = Minecraft.getInstance().level;
		long fired = BlasterItem.state(stack).lastFired();
		return level == null || fired <= 0 ? 100F : level.getGameTime() - fired + partial;
	}

	static float shotKick(float age)
	{
		float cycle = age / 4.5F;
		return cycle < 0 || cycle > 1 ? 0
				: (float) (1 / ((Math.exp(-150 * cycle) + 0.00373) * (Math.exp(9.46 * cycle) + 266)));
	}

	private BlasterSight() {}
}
