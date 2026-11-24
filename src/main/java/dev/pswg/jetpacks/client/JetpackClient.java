package dev.pswg.jetpacks.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.Galaxies;
import dev.pswg.jetpacks.JetpackNetwork;
import dev.pswg.jetpacks.Jetpacks;
import dev.pswg.rendering.nem.NemModels;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class JetpackClient
{
	private static final KeyMapping SLOT = new KeyMapping("key.pswg.jetpack_slot", InputConstants.Type.KEYSYM,
			InputConstants.KEY_J, "category.pswg.keybinds");
	private static boolean sentThrust;

	private JetpackClient() {}

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(SLOT); }
		@SubscribeEvent public static void menu(RegisterMenuScreensEvent event) { event.register(Jetpacks.MENU.get(), JetpackScreen::new); }

		@SubscribeEvent
		public static void addLayer(EntityRenderersEvent.AddLayers event)
		{
			for (var skin : event.getSkins())
			{
				PlayerRenderer renderer = event.getSkin(skin);
				if (renderer != null)
					renderer.addLayer(new PackLayer(renderer));
			}
		}
	}

	@SubscribeEvent
	public static void inventoryButton(ScreenEvent.Init.Post event)
	{
		if (event.getScreen() instanceof InventoryScreen screen)
		{
			var button = Button.builder(Component.translatable("screen.pswg.jetpack_slot.short"), ignored -> open())
					.bounds(screen.getGuiLeft() + screen.getXSize() + 3, screen.getGuiTop() + 7, 21, 20).build();
			button.setTooltip(Tooltip.create(Component.translatable("container.pswg.jetpack_slot")));
			event.addListener(button);
		}
	}

	private static void open()
	{
		PacketDistributor.sendToServer(new JetpackNetwork.Input(JetpackNetwork.OPEN));
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event)
	{
		var client = Minecraft.getInstance();
		var player = client.player;
		if (player == null || client.level == null)
		{
			sentThrust = false;
			return;
		}
		while (SLOT.consumeClick())
			if (client.screen == null || client.screen instanceof InventoryScreen)
				open();

		var keys = client.options;
		boolean pressed = client.screen == null && !player.isPassenger() && !Jetpacks.equipped(player).isEmpty()
				&& keys.keyJump.isDown();
		if (pressed || sentThrust)
		{
			int bits = 0;
			if (pressed)
			{
				bits |= JetpackNetwork.ASCEND;
				if (keys.keyShift.isDown()) bits |= JetpackNetwork.DESCEND;
				if (keys.keySprint.isDown()) bits |= JetpackNetwork.CRUISE;
				if (keys.keyUp.isDown()) bits |= JetpackNetwork.FORWARD;
				if (keys.keyDown.isDown()) bits |= JetpackNetwork.BACK;
				if (keys.keyLeft.isDown()) bits |= JetpackNetwork.LEFT;
				if (keys.keyRight.isDown()) bits |= JetpackNetwork.RIGHT;
			}
			PacketDistributor.sendToServer(new JetpackNetwork.Input(bits));
		}
		sentThrust = pressed;

		if (player.tickCount % 2 == 0)
			for (var entity : client.level.players())
				if (entity.distanceToSqr(player) <= 1024 && Jetpacks.active(entity))
				{
					var forward = Vec3.directionFromRotation(0, entity.yBodyRot);
					var side = new Vec3(-forward.z, 0, forward.x).scale(0.27);
					var point = entity.position().add(0, 0.85, 0).subtract(forward.scale(0.26));
					for (int i : new int[] { -1, 1 })
					{
						var plume = point.add(side.scale(i));
						client.level.addParticle(ParticleTypes.FLAME, plume.x, plume.y, plume.z, 0, -0.16, 0);
						client.level.addParticle(ParticleTypes.SMOKE, plume.x, plume.y, plume.z, 0, -0.1, 0);
					}
				}
	}

	private static final class PackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
	{
		private final Map<String, ModelPart> models = new HashMap<>();
		private int reload = -1;

		private PackLayer(PlayerRenderer renderer) { super(renderer); }

		@Override
		public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, float step, float walk,
		                   float partial, float age, float yaw, float pitch)
		{
			if (!(Jetpacks.equipped(player).getItem() instanceof Jetpacks.Jetpack pack) || player.isInvisible())
				return;
			var id = pack.appearance();
			String armor = id.equals("jumptrooper_jetpack") ? "jumptrooper" : id;
			int version = NemModels.INSTANCE.version();
			if (reload != version)
			{
				models.clear();
				reload = version;
			}
			var model = models.computeIfAbsent(armor, name -> NemModels.INSTANCE.bake(Galaxies.id("armor/" + name)));
			if (model == null || !model.hasChild("body") || !model.getChild("body").hasChild("jetpack"))
				return;
			pose.pushPose();
			getParentModel().body.translateAndRotate(pose);
			var texture = Galaxies.id("textures/armor/" + armor + ".png");
			model.getChild("body").getChild("jetpack").render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
					light, OverlayTexture.NO_OVERLAY);
			pose.popPose();
		}
	}

	private static final class JetpackScreen extends AbstractContainerScreen<Jetpacks.Menu>
	{
		private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/dispenser.png");

		private JetpackScreen(Jetpacks.Menu menu, Inventory player, Component title)
		{
			super(menu, player, title);
			imageHeight = 156;
			inventoryLabelY = 62;
		}

		@Override
		public void render(GuiGraphics graphics, int x, int y, float partial)
		{
			super.render(graphics, x, y, partial);
			renderTooltip(graphics, x, y);
		}

		@Override
		protected void renderBg(GuiGraphics graphics, float partial, int x, int y)
		{
			graphics.blit(TEXTURE, leftPos, topPos, 0, 0, 176, 17);
			graphics.fill(leftPos, topPos + 17, leftPos + 176, topPos + 65, 0xFFC6C6C6);
			graphics.blit(TEXTURE, leftPos, topPos + 65, 0, 74, 176, 91);
			graphics.blit(TEXTURE, leftPos + 79, topPos + 29, 79, 34, 18, 18);
		}
	}
}
