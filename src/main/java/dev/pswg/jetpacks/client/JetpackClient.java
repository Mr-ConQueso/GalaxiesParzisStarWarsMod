package dev.pswg.jetpacks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.Galaxies;
import dev.pswg.jetpacks.JetpackNetwork;
import dev.pswg.jetpacks.Jetpacks;
import dev.pswg.rendering.nem.NemModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.client.ICurioRenderer;

import java.util.HashMap;
import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class JetpackClient
{
	private static boolean sentThrust;

	private JetpackClient() {}

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class ModEvents
	{
		@SubscribeEvent
		public static void registerRenderers(FMLClientSetupEvent event)
		{
			event.enqueueWork(() -> {
				var renderer = new PackRenderer();
				Jetpacks.PACKS.forEach(item -> CuriosRendererRegistry.register(item.get(), () -> renderer));
				Jetpacks.ACCESSORIES.forEach(item -> CuriosRendererRegistry.register(item.get(), () -> renderer));
			});
		}
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
		var keys = client.options;
		boolean pressed = client.screen == null && !player.isPassenger() && Jetpacks.hasJetpack(player)
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

	private static final class PackRenderer implements ICurioRenderer
	{
		private final Map<String, ModelPart> models = new HashMap<>();
		private int reload = -1;

		@Override
		public <T extends LivingEntity, M extends EntityModel<T>> void render(ItemStack stack, SlotContext slot, PoseStack pose,
				RenderLayerParent<T, M> parent, MultiBufferSource buffers, int light, float step, float walk,
				float partial, float age, float yaw, float pitch)
		{
			if (!(stack.getItem() instanceof Jetpacks.BackEquipment pack) || !(parent.getModel() instanceof HumanoidModel<?> wearer)
					|| slot.entity().isInvisible())
				return;
			int version = NemModels.INSTANCE.version();
			if (reload != version)
			{
				models.clear();
				reload = version;
			}
			var model = models.computeIfAbsent(pack.model(), name -> NemModels.INSTANCE.bake(Galaxies.id("armor/" + name)));
			if (model == null || !model.hasChild("body") || !model.getChild("body").hasChild(pack.part()))
				return;
			pose.pushPose();
			wearer.body.translateAndRotate(pose);
			var texture = Galaxies.id("textures/armor/" + pack.texture() + ".png");
			model.getChild("body").getChild(pack.part()).render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),
					light, OverlayTexture.NO_OVERLAY);
			pose.popPose();
		}
	}
}
