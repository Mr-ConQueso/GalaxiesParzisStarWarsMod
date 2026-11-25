package dev.pswg.blasters.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterBolt;
import dev.pswg.blasters.BlasterDefinition;
import dev.pswg.blasters.BlasterItem;
import dev.pswg.blasters.BlasterNetwork.MuzzleShot;
import dev.pswg.blasters.Blasters;
import dev.pswg.model.g3d.G3dFiles;
import dev.pswg.model.g3d.G3dPose;
import dev.pswg.model.g3d.G3dResources;
import dev.pswg.particle.GalaxiesParticles;
import dev.pswg.rendering.p3d.P3dModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class BlasterMuzzle
{
	private static final Map<LivingEntity, Map<HumanoidArm, Hand>> HANDS = new WeakHashMap<>();
	private static final ResourceLocation E11D = Blasters.id("item/e11d");
	private static Matrix4f e11dSocket;
	private static final Matrix4f WORLD_FROM_CLIP = new Matrix4f();

	private static final class Hand
	{
		private Socket socket;
		private MuzzleShot shot;
		private long firedTick;
		private float firedPartial;
		private boolean pyroPending;
	}

	private record Socket(ResourceLocation model, Vec3 offset, boolean firstPerson, long tick) {}

	public static void onShot(Level level, MuzzleShot shot)
	{
		if (!(level.getEntity(shot.entity()) instanceof LivingEntity holder))
			return;
		var hand = hand(holder, shot.arm());
		hand.shot = shot;
		hand.firedTick = level.getGameTime();
		hand.firedPartial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
		hand.pyroPending = shot.pyrotechnics();
	}

	static float shotAge(LivingEntity holder, HumanoidArm arm, ItemStack stack, float partial)
	{
		var hand = hand(holder, arm);
		return matches(hand, stack)
				? Math.max(0, holder.level().getGameTime() - hand.firedTick + partial - hand.firedPartial) : 100;
	}

	private static boolean matches(Hand hand, ItemStack stack)
	{
		return hand.shot != null && hand.shot.model().equals(stack.get(Blasters.ID.get()));
	}

	static Matrix4f legacySocket(P3dModel model, ItemStack stack, BlasterDefinition definition)
	{
		var muzzle = model.transform("muzzle_flash");
		if (definition == null)
			return muzzle;

		for (var attachment : BlasterItem.attachments(stack, definition).applied().values())
		{
			var option = definition.attachments().options().get(attachment);
			if (option == null || option.part().isEmpty())
				continue;
			var alternate = model.transform("muzzle_flash." + option.part().get());
			if (alternate != null)
				muzzle = alternate;
		}
		return muzzle;
	}

	static void reload(ResourceManager resources)
	{
		e11dSocket = null;
		var file = G3dResources.MODEL_FILES.idToFile(E11D);
		try (var input = resources.getResourceOrThrow(file).open())
		{
			var model = G3dFiles.readRig(input);
			e11dSocket = new G3dPose(model).socketMatrix("muzzle_flash", new Matrix4f());
		}
		catch (IOException | IllegalArgumentException exception)
		{
			Galaxies.LOGGER.warn("Could not load E-11D muzzle socket from {}", file, exception);
		}
	}

	static void current(LivingEntity holder, HumanoidArm arm, ItemStack stack, PoseStack pose, MultiBufferSource buffers,
	                    boolean firstPerson, float partial)
	{
		if (e11dSocket == null)
			return;
		pose.mulPose(e11dSocket);
		float flashScale = 0.2F / pose.last().pose().getScale(new Vector3f()).x;
		pose.scale(flashScale, flashScale, flashScale);
		render(holder, arm, stack, pose, buffers, OverlayTexture.NO_OVERLAY, 0xFF2244, firstPerson, partial);
	}

	static void render(LivingEntity holder, HumanoidArm arm, ItemStack stack, PoseStack pose, MultiBufferSource buffers,
	                   int overlay, int color, boolean firstPerson, float partial)
	{
		var hand = hand(holder, arm);
		var client = Minecraft.getInstance();
		var camera = client.gameRenderer.getMainCamera();
		var transform = pose.last().pose();
		var point = transform.getTranslation(new Vector3f());
		var forward = transform.transformDirection(0, 0, 1, new Vector3f());
		if (firstPerson)
		{
			// Project through the hand camera, then recover the same point in the world camera.
			var worldFromHand = new Matrix4f(WORLD_FROM_CLIP).mul(RenderSystem.getProjectionMatrix())
					.mul(RenderSystem.getModelViewMatrix()).mul(transform);
			worldFromHand.transformProject(point.set(0, 0, 0));
			worldFromHand.transformProject(forward.set(0, 0, 0.25F)).sub(point);
		}

		var worldPoint = camera.getPosition().add(point.x, point.y, point.z);
		var offset = worldPoint.subtract(holder.getEyePosition(partial));
		hand.socket = new Socket(stack.get(Blasters.ID.get()), offset, firstPerson, holder.level().getGameTime());

		float age = shotAge(holder, arm, stack, partial);
		if (age >= BlasterFlash.DURATION)
			return;
		if (hand.pyroPending)
		{
			hand.pyroPending = false;
			pyrotechnics(holder.level(), worldPoint, new Vec3(forward.normalize()));
		}
		switch (hand.shot.mode())
		{
			case STUN, ION -> color = 0x0065FF;
		}
		BlasterFlash.render(pose, buffers, overlay, age, color, firstPerson);
	}

	static Vec3 boltOffset(BlasterBolt bolt)
	{
		if (bolt.handModel() == null || !(bolt.getOwner() instanceof LivingEntity holder))
			return null;
		var socket = hand(holder, bolt.arm()).socket;
		boolean firstPerson = holder == BlasterSight.firstPersonPlayer();
		return socket != null && bolt.handModel().equals(socket.model()) && firstPerson == socket.firstPerson()
				&& holder.level().getGameTime() - socket.tick() <= 2 ? socket.offset() : null;
	}

	static void renderStage(RenderLevelStageEvent event)
	{
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL)
			WORLD_FROM_CLIP.set(event.getProjectionMatrix()).mul(event.getModelViewMatrix()).invert();
	}

	static void clear()
	{
		HANDS.clear();
	}

	private static Hand hand(LivingEntity holder, HumanoidArm arm)
	{
		return HANDS.computeIfAbsent(holder, key -> new EnumMap<>(HumanoidArm.class))
				.computeIfAbsent(arm, key -> new Hand());
	}

	private static void pyrotechnics(Level level, Vec3 point, Vec3 forward)
	{
		for (int i = 0; i < 25; i++)
		{
			var scatter = new Vec3(level.random.nextGaussian() * 0.03, level.random.nextGaussian() * 0.03,
					level.random.nextGaussian() * 0.03);
			var spark = forward.scale(0.6F * (0.5F + 0.5F * level.random.nextFloat())).add(scatter);
			level.addParticle(GalaxiesParticles.SPARK.get(), point.x, point.y, point.z, spark.x, spark.y, spark.z);
			if (i % 3 == 0)
			{
				var smoke = forward.scale(0.05F * (0.5F + 0.5F * level.random.nextFloat())).add(scatter);
				level.addParticle(ParticleTypes.SMOKE, point.x, point.y, point.z, smoke.x, smoke.y, smoke.z);
			}
		}
	}

	private BlasterMuzzle() {}
}
