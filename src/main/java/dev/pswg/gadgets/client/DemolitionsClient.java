package dev.pswg.gadgets.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.pswg.Galaxies;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.GadgetCharge;
import dev.pswg.gadgets.GadgetInput;
import dev.pswg.gadgets.GrenadeItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class DemolitionsClient
{
	private static boolean sentPrime;
	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void setup(FMLClientSetupEvent event)
		{
			event.enqueueWork(() -> {
				for (var item : List.of(Demolitions.THERMAL, Demolitions.FRAGMENTATION, Demolitions.SMOKE))
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
		}

		@SubscribeEvent
		public static void registerModels(ModelEvent.RegisterAdditional event)
		{
			for (var variant : Demolitions.Variant.values())
			{
				event.register(model(variant, false));
				if (variant == Demolitions.Variant.THERMAL || variant == Demolitions.Variant.FRAGMENTATION || variant == Demolitions.Variant.SMOKE)
					event.register(model(variant, true));
			}
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

	private static ModelResourceLocation model(Demolitions.Variant variant, boolean primed)
	{
		String item = BuiltInRegistries.ITEM.getKey(variant.item()).getPath();
		String model = switch (variant)
		{
			case THERMAL, FRAGMENTATION, SMOKE -> item + (primed ? "_primed_in_hand" : "_in_hand");
			case IMPACT -> item + "_in_hand";
			case PRESSURE -> item;
		};
		return ModelResourceLocation.standalone(Demolitions.id("item/" + model));
	}

	private static final class ChargeRenderer extends EntityRenderer<GadgetCharge>
	{
		private ChargeRenderer(EntityRendererProvider.Context context)
		{
			super(context);
		}

		@Override
		public void render(GadgetCharge charge, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light)
		{
			if (charge.isDetonating() && charge.variant() != Demolitions.Variant.SMOKE)
				return;
			var client = Minecraft.getInstance();
			var model = client.getModelManager().getModel(model(charge.variant(), charge.isPrimed()));
			pose.pushPose();
			pose.translate(0, charge.getBbHeight() / 2F, 0);
			pose.mulPose(Axis.YP.rotationDegrees(yaw));
			client.getItemRenderer().render(new ItemStack(charge.variant().item()), ItemDisplayContext.NONE, false,
					pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
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
