package dev.pswg.rendering.p3d;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.function.Function;

public final class P3dRenderer
{
	private P3dRenderer() {}

	public static void render(P3dModel model, PoseStack pose, MultiBufferSource buffers, ResourceLocation texture, int light, int overlay)
	{
		render(model, pose, buffers, part -> texture, part -> new Matrix4f(), light, overlay);
	}

	public static void render(P3dModel model, PoseStack pose, MultiBufferSource buffers, Function<String, ResourceLocation> textures,
	                          Function<String, Matrix4f> transforms, int light, int overlay)
	{
		for (var root : model.roots())
			renderPart(root, pose, buffers, textures, transforms, light, overlay);
	}

	private static void renderPart(P3dModel.Part part, PoseStack pose, MultiBufferSource buffers, Function<String, ResourceLocation> textures,
	                               Function<String, Matrix4f> transforms, int light, int overlay)
	{
		var transform = transforms.apply(part.name());
		if (transform == null)
			return;

		pose.pushPose();
		pose.mulPose(part.transform());
		pose.mulPose(transform);
		var entry = pose.last();
		if (!part.faces().isEmpty())
		{
			var texture = textures.apply(part.name());
			var type = switch (part.material())
			{
				case 2 -> RenderType.entityTranslucent(texture);
				case 3 -> RenderType.entityTranslucentEmissive(texture);
				default -> RenderType.entityCutoutNoCull(texture);
			};
			var vertices = buffers.getBuffer(type);
			int brightness = part.material() == 3 ? LightTexture.FULL_BRIGHT : light;
			var normal = new Vector3f();
			for (var face : part.faces())
			{
				entry.transformNormal(face.normal().x, face.normal().y, face.normal().z, normal);
				for (var vertex : face.vertices())
					vertices.addVertex(entry.pose(), vertex.x(), vertex.y(), vertex.z()).setColor(-1).setUv(vertex.u(), vertex.v())
							.setOverlay(overlay).setLight(brightness).setNormal(normal.x, normal.y, normal.z);
			}
		}
		for (var child : part.children())
			renderPart(child, pose, buffers, textures, transforms, light, overlay);
		pose.popPose();
	}
}
