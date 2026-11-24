package net.mrconqueso.pswg.debug;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;

public class DebugUtil
{
	private static final class DebugPoint
	{
		public final Vec3 position;
		public final float size;
		public final float r;
		public final float g;
		public final float b;

		public int age;

		private DebugPoint(Vec3 position, float size, float r, float g, float b)
		{
			this.position = position;
			this.size = size;
			this.r = r;
			this.g = g;
			this.b = b;
			this.age = 1;
		}
	}

	private static final HashMap<String, DebugPoint> framePoints = new HashMap<>();
	private static final HashMap<String, DebugPoint> tickPoints = new HashMap<>();

	public static void tickPoint(String id, Vec3 position, float r, float g, float b)
	{
		if (RenderSystem.isOnRenderThread())
			tickPoints.put(id, new DebugPoint(position, 0.03f, r, g, b));
		else
			RenderSystem.recordRenderCall(() -> tickPoints.put(id, new DebugPoint(position, 0.03f, r, g, b)));
	}

	public static void framePoint(String id, Vec3 position, float r, float g, float b)
	{
		if (RenderSystem.isOnRenderThread())
			framePoints.put(id, new DebugPoint(position, 0.03f, r, g, b));
		else
			RenderSystem.recordRenderCall(() -> framePoints.put(id, new DebugPoint(position, 0.03f, r, g, b)));
	}

	public static void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, double cameraX, double cameraY, double cameraZ) {
		Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

		// In MojMap, Vec3 is used and reverse() negates the X, Y, and Z coordinates.
		Vec3 invCamPos = camera.getPosition().reverse();

		if (camera.isInitialized()) {
			for (var point : framePoints.values()) {
				AABB box = new AABB(point.position, point.position).move(invCamPos).inflate(point.size);
				// LevelRenderer is standard in modern NeoForge for drawing bounding box outlines
				LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), box, point.r, point.g, point.b, 1.0F);
			}

			for (var point : tickPoints.values()) {
				AABB box = new AABB(point.position, point.position).move(invCamPos).inflate(point.size);
				LevelRenderer.renderLineBox(poseStack, bufferSource.getBuffer(RenderType.lines()), box, point.r, point.g, point.b, 1.0F);
			}
		}

		ageAndDestroy(framePoints);
	}

	public static void tick(Minecraft client)
	{
		if (RenderSystem.isOnRenderThread())
			ageAndDestroy(tickPoints);
		else
			RenderSystem.recordRenderCall(() -> ageAndDestroy(tickPoints));
	}

	private static void ageAndDestroy(HashMap<String, DebugPoint> points)
	{
		var removal = new HashSet<String>();

		// One unit-time grace period to prevent flicker
		for (var entry : points.entrySet())
		{
			entry.getValue().age--;
			if (entry.getValue().age < 0)
				removal.add(entry.getKey());
		}

		for (var key : removal)
			points.remove(key);
	}
}
