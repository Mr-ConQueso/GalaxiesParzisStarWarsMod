package dev.pswg.gadgets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.GadgetCharge;
import dev.pswg.gadgets.GadgetInput;
import dev.pswg.gadgets.GrenadeItem;
import dev.pswg.model.g3d.G3dTransform;
import dev.pswg.rendering.g3d.G3dEntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class DemolitionsClient
{
	private static final float BEAM_OFFSET = 1 / 16F;
	private static Map<ResourceLocation, G3dEntityModel> models = Map.of();
	private static boolean sentPrime;

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void setup(FMLClientSetupEvent event)
		{
			event.enqueueWork(() -> {
				for (var item : List.of(Demolitions.THERMAL, Demolitions.FRAGMENTATION, Demolitions.SMOKE, Demolitions.INFERNO))
					ItemProperties.register(item.get(), Demolitions.id("primed"), (stack, level, entity, seed) ->
							stack.has(Demolitions.PRIMING_TIME.get()) ? 1F : 0F);
			});
		}

		@SubscribeEvent
		public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event)
		{
			for (var variant : Demolitions.Variant.values())
				event.registerEntityRenderer(variant.entityType(), ChargeRenderer::new);
			event.registerEntityRenderer(Demolitions.SMOKE_CLOUD.get(), NoopRenderer::new);
			event.registerEntityRenderer(Demolitions.NERVE_CLOUD.get(), NoopRenderer::new);
		}

		@SubscribeEvent
		public static void registerModels(RegisterClientReloadListenersEvent event)
		{
			event.registerReloadListener((ResourceManagerReloadListener) DemolitionsClient::reload);
		}
	}

	@SubscribeEvent
	public static void onAttack(InputEvent.InteractionKeyMappingTriggered event)
	{
		var player = Minecraft.getInstance().player;
		if (!event.isAttack() || player == null || player.isSpectator())
			return;
		var hand = GadgetInput.primingHand(player);
		if (player.getItemInHand(hand).getItem() instanceof GrenadeItem grenade)
		{
			if (!sentPrime)
			{
				PacketDistributor.sendToServer(new GadgetInput(hand, BuiltInRegistries.ITEM.getKey(grenade)));
				sentPrime = true;
			}
			event.setSwingHand(false);
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public static void releaseAttack(ClientTickEvent.Post event)
	{
		if (!Minecraft.getInstance().options.keyAttack.isDown())
			sentPrime = false;
	}

	private static void reload(ResourceManager resources)
	{
		var loaded = new HashMap<ResourceLocation, G3dEntityModel>();
		for (var variant : Demolitions.Variant.values())
		{
			for (boolean primed : new boolean[] { false, true })
			{
				var id = model(variant, primed);
				if (loaded.containsKey(id))
					continue;
				try
				{
					loaded.put(id, G3dEntityModel.load(resources, id, "entity_origin"));
				}
				catch (IOException | RuntimeException exception)
				{
					Galaxies.LOGGER.error("Could not load charge model {}", id, exception);
				}
			}
		}
		models = Map.copyOf(loaded);
	}

	private static ResourceLocation model(Demolitions.Variant variant, boolean primed)
	{
		String item = BuiltInRegistries.ITEM.getKey(variant.item()).getPath();
		String suffix = switch (variant)
		{
			case THERMAL, FRAGMENTATION, SMOKE, INFERNO -> primed ? "_primed_in_hand" : "_in_hand";
			case IMPACT, NERVE -> "_in_hand";
			case PRESSURE, TRIPWIRE -> "";
		};
		return Demolitions.id("item/" + item + suffix);
	}

	private static final class ChargeRenderer extends EntityRenderer<GadgetCharge>
	{
		private ChargeRenderer(EntityRendererProvider.Context context)
		{
			super(context);
		}

		@Override
		public boolean shouldRender(GadgetCharge charge, Frustum frustum, double x, double y, double z)
		{
			var face = charge.mountedFace();
			if (face == null || charge.beamLength() <= BEAM_OFFSET)
				return super.shouldRender(charge, frustum, x, y, z);
			var beam = Vec3.atLowerCornerOf(face.getNormal()).scale(charge.beamLength());
			return charge.shouldRender(x, y, z) && frustum.isVisible(charge.getBoundingBox().expandTowards(beam).inflate(0.5));
		}

		@Override
		public void render(GadgetCharge charge, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
		{
			if (charge.isDetonating() && charge.variant() != Demolitions.Variant.SMOKE && charge.variant() != Demolitions.Variant.NERVE)
				return;
			var model = models.get(model(charge.variant(), charge.isPrimed()));
			if (model == null)
				return;

			pose.pushPose();
			var face = charge.mountedFace();
			Map<String, G3dTransform> overrides = Map.of();
			if (face != null)
			{
				pose.mulPose(face.getRotation());
				float length = charge.beamLength() - BEAM_OFFSET;
				if (length > 0 && charge.isPrimed())
					overrides = Map.of("beam", new G3dTransform(new Vector3f(0.5F, BEAM_OFFSET, 0.5F),
							new Quaternionf(), new Vector3f(1, length, 1)));
			}
			else
				pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, charge.yRotO, charge.getYRot())));
			model.render(pose, buffers, light, overrides);
			pose.popPose();
		}

		@Override
		public ResourceLocation getTextureLocation(GadgetCharge charge)
		{
			return TextureAtlas.LOCATION_BLOCKS;
		}
	}

	private DemolitionsClient()
	{
	}
}
