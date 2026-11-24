package dev.pswg.vehicles.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.Galaxies;
import dev.pswg.rendering.p3d.P3dRenderer;
import dev.pswg.rendering.p3d.P3dResources;
import dev.pswg.vehicles.Ship;
import dev.pswg.vehicles.VehicleNetwork;
import dev.pswg.vehicles.Vehicles;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class VehicleClient
{
	private static final KeyMapping ROLL = new KeyMapping("key.pswg.ship_roll", InputConstants.Type.KEYSYM,
			InputConstants.KEY_C, "category.pswg.keybinds");
	private static float yaw;
	private static float pitch;
	private static final Map<Ship, EngineSound> ENGINES = new HashMap<>();

	private VehicleClient() {}

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void keys(RegisterKeyMappingsEvent event) { event.register(ROLL); }

		@SubscribeEvent
		public static void renderer(EntityRenderersEvent.RegisterRenderers event)
		{
			for (var kind : Vehicles.Kind.values())
				event.registerEntityRenderer(Vehicles.type(kind), context -> new ShipRenderer(context, kind));
		}
	}

	public static boolean collectMouse(double horizontal, double vertical)
	{
		var player = Minecraft.getInstance().player;
		if (player == null || !(player.getVehicle() instanceof Ship ship) || !ship.kind().flying() || ship.pilot() != player)
			return false;
		yaw += (float) (-horizontal * 0.1);
		pitch += (float) (-vertical * 0.1);
		return true;
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void attack(InputEvent.InteractionKeyMappingTriggered event)
	{
		var player = Minecraft.getInstance().player;
		if (event.isAttack() && player != null && player.getVehicle() instanceof Ship ship && ship.kind().flying() && ship.pilot() == player)
		{
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event)
	{
		var client = Minecraft.getInstance();
		var player = client.player;
		if (player != null && player.getVehicle() instanceof Ship ship && ship.pilot() == player)
		{
			int bits = 0;
			if (client.screen == null)
			{
				var options = client.options;
				if (options.keyUp.isDown()) bits |= VehicleNetwork.FORWARD;
				if (options.keyDown.isDown()) bits |= VehicleNetwork.BACK;
				if (options.keyRight.isDown()) bits |= VehicleNetwork.RIGHT;
				if (options.keyLeft.isDown()) bits |= VehicleNetwork.LEFT;
				if (options.keyAttack.isDown()) bits |= VehicleNetwork.FIRE;
				if (ROLL.isDown()) bits |= VehicleNetwork.ROLL;
			}
			PacketDistributor.sendToServer(new VehicleNetwork.Control(ship.getId(), bits, yaw, pitch));
		}
		yaw = pitch = 0;
		ENGINES.values().removeIf(EngineSound::isStopped);
		if (client.level != null && player != null)
			for (var entity : client.level.entitiesForRendering())
				if (entity instanceof Ship ship && ship.kind().flying() && ship.throttle() > 0.05F && !ENGINES.containsKey(ship)
						&& ship.distanceToSqr(player) < 4096)
				{
					var sound = new EngineSound(ship);
					ENGINES.put(ship, sound);
					client.getSoundManager().play(sound);
				}
	}

	public static View view(Ship ship, float partial)
	{
		var rotation = ship.orientation(partial);
		var forward = rotation.transform(new org.joml.Vector3f(0, 0, -1));
		var up = rotation.transform(new org.joml.Vector3f(0, 1, 0));
		float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
		float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(forward.y, -1, 1)));
		var levelUp = Vec3.directionFromRotation(pitch - 90, yaw);
		var direction = new Vec3(forward);
		var shipUp = new Vec3(up);
		float roll = (float) Math.toDegrees(Math.atan2(direction.dot(levelUp.cross(shipUp)), levelUp.dot(shipUp)));
		return new View(yaw, pitch, roll);
	}

	public record View(float yaw, float pitch, float roll) {}

	private static final class ShipRenderer extends EntityRenderer<Ship>
	{
		private final Vehicles.Kind kind;
		private final ResourceLocation model;
		private final ResourceLocation texture;

		private ShipRenderer(EntityRendererProvider.Context context, Vehicles.Kind kind)
		{
			super(context);
			this.kind = kind;
			model = Galaxies.id("ship/" + kind.id);
			texture = Galaxies.id("textures/ship/" + kind.id + ".png");
		}

		@Override
		public boolean shouldRender(Ship ship, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z)
		{
			return ship.shouldRender(x, y, z) && frustum.isVisible(ship.getBoundingBox().inflate(kind.flying() ? 6 : 2));
		}

		@Override
		public void render(Ship ship, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
		{
			var asset = P3dResources.MODELS.get(model);
			if (asset != null)
			{
				pose.pushPose();
				pose.mulPose(ship.orientation(partial));
				P3dRenderer.render(asset, pose, buffers, part -> texture, part -> ship.partTransform(part, partial), light, OverlayTexture.NO_OVERLAY);
				pose.popPose();
			}
		}

		@Override
		public ResourceLocation getTextureLocation(Ship ship) { return texture; }
	}

	private static final class EngineSound extends AbstractTickableSoundInstance
	{
		private final Ship ship;

		private EngineSound(Ship ship)
		{
			super(Vehicles.ENGINE.get(), SoundSource.NEUTRAL, ship.getRandom());
			this.ship = ship;
			this.looping = true;
			tick();
		}

		@Override
		public void tick()
		{
			if (ship.isRemoved() || ship.level() != Minecraft.getInstance().level || ship.throttle() <= 0.05F)
			{
				stop();
				return;
			}
			x = ship.getX();
			y = ship.getY();
			z = ship.getZ();
			volume = 0.2F + 0.3F * ship.throttle();
			pitch = 0.65F + ship.throttle() * 0.2F;
		}
	}
}
