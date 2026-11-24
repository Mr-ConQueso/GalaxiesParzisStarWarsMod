package dev.pswg.rendering.p3d;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;
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
			renderPart(root, pose, buffers, textures, transforms, light, overlay, null);
	}

	/** Draw a model part up to a plane in its local coordinates. */
	public static void renderClipped(P3dModel.Part part, PoseStack pose, MultiBufferSource buffers, ResourceLocation texture,
	                                 float maxX, int light, int overlay)
	{
		renderPart(part, pose, buffers, name -> texture, name -> new Matrix4f(), light, overlay, maxX);
	}

	private static void renderPart(P3dModel.Part part, PoseStack pose, MultiBufferSource buffers, Function<String, ResourceLocation> textures,
	                               Function<String, Matrix4f> transforms, int light, int overlay, Float maxX)
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
				if (maxX == null)
				{
					for (var vertex : face.vertices())
						emitVertex(vertices, entry, vertex, normal, brightness, overlay);
				}
				else
				{
					var polygon = P3dClip.clip(List.of(face.vertices()), Direction.Axis.X, maxX, false);
					P3dClip.emitQuads(polygon, quad -> {
						for (var vertex : quad)
							emitVertex(vertices, entry, vertex, normal, brightness, overlay);
					});
				}
			}
		}
		for (var child : part.children())
			renderPart(child, pose, buffers, textures, transforms, light, overlay, maxX);
		pose.popPose();
	}
	private static void emitVertex(VertexConsumer vertices, PoseStack.Pose entry, P3dModel.Vertex vertex,
	                               Vector3f normal, int light, int overlay)
	{
		vertices.addVertex(entry.pose(), vertex.x(), vertex.y(), vertex.z()).setColor(-1).setUv(vertex.u(), vertex.v())
				.setOverlay(overlay).setLight(light).setNormal(normal.x, normal.y, normal.z);
	}

}
