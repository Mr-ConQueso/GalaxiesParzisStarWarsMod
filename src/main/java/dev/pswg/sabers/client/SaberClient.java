package dev.pswg.sabers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.sabers.SaberItem;
import dev.pswg.sabers.SaberNetwork;
import dev.pswg.sabers.Sabers;
import dev.pswg.sabers.ThrownSaber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class SaberClient
{
	private static final Map<Entity, Hum> HUMS = new HashMap<>();

	private SaberClient() {}

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void itemRendering(RegisterClientExtensionsEvent event)
		{
			event.registerItem(new IClientItemExtensions()
			{
				private SaberRenderer renderer;

				@Override
				public BlockEntityWithoutLevelRenderer getCustomRenderer()
				{
					if (renderer == null)
						renderer = new SaberRenderer(Minecraft.getInstance());
					return renderer;
				}
			}, Sabers.ITEMS_BY_HILT.values().stream().map(item -> item.get()).toArray(Item[]::new));
		}

		@SubscribeEvent
		public static void menus(RegisterMenuScreensEvent event) { event.register(Sabers.FORGE_MENU.get(), SaberForgeScreen::new); }

		@SubscribeEvent
		public static void renderers(EntityRenderersEvent.RegisterRenderers event) { event.registerEntityRenderer(Sabers.THROWN.get(), ThrownRenderer::new); }
	}

	public static boolean send(Player player)
	{
		for (InteractionHand hand : InteractionHand.values())
			if (player.getItemInHand(hand).getItem() instanceof SaberItem)
			{
				PacketDistributor.sendToServer(new SaberNetwork.Action(hand, false));
				return true;
			}
		return false;
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event)
	{
		var client = Minecraft.getInstance();
		HUMS.values().removeIf(Hum::isStopped);
		if (client.level == null || client.player == null)
		{
			HUMS.clear();
			return;
		}
		for (var entity : client.level.entitiesForRendering())
			if (entity.distanceToSqr(client.player) < 256 && active(entity) && !HUMS.containsKey(entity))
			{
				var sound = new Hum(entity);
				HUMS.put(entity, sound);
				client.getSoundManager().play(sound);
			}
	}

	private static boolean active(Entity entity)
	{
		if (entity instanceof ThrownSaber)
			return entity.isAlive();
		if (!(entity instanceof net.minecraft.world.entity.LivingEntity living))
			return false;
		for (InteractionHand hand : InteractionHand.values())
		{
			var stack = living.getItemInHand(hand);
			if (stack.getItem() instanceof SaberItem && SaberItem.state(stack).active())
				return true;
		}
		return false;
	}

	private static final class Hum extends AbstractTickableSoundInstance
	{
		private final Entity entity;

		private Hum(Entity entity)
		{
			super(Sabers.IDLE.get(), SoundSource.PLAYERS, entity.getRandom());
			this.entity = entity;
			looping = true;
			volume = 0.5F;
			tick();
		}

		@Override
		public void tick()
		{
			var client = Minecraft.getInstance();
			if (entity.isRemoved() || client.level != entity.level() || !active(entity))
				stop();
			else
			{
				x = entity.getX();
				y = entity.getY();
				z = entity.getZ();
			}
		}
	}

	private static final class ThrownRenderer extends EntityRenderer<ThrownSaber>
	{
		private ThrownRenderer(EntityRendererProvider.Context context) { super(context); }

		@Override
		public void render(ThrownSaber saber, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
		{
			pose.pushPose();
			pose.mulPose(Axis.YP.rotationDegrees(-saber.getYRot()));
			pose.mulPose(Axis.XP.rotationDegrees(90));
			pose.mulPose(Axis.ZP.rotationDegrees((saber.tickCount + partial) * 45F));
			SaberRenderer.draw(saber.getItem(), pose, buffers, light, OverlayTexture.NO_OVERLAY, true, true);
			pose.popPose();
		}

		@Override
		public ResourceLocation getTextureLocation(ThrownSaber saber) { return Galaxies.id("textures/item/model/lightsaber/luke_rotj.png"); }
	}
}
