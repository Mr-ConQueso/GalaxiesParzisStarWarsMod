package dev.pswg.rendering.g3d;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.model.g3d.G3dFiles;
import dev.pswg.model.g3d.G3dModel;
import dev.pswg.model.g3d.G3dPose;
import dev.pswg.model.g3d.G3dResources;
import dev.pswg.model.g3d.G3dSource;
import dev.pswg.model.g3d.G3dTransform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Draws G3D entities in block units around an authored socket using the animated block atlas. */
public final class G3dEntityModel
{
	private final G3dModel model;
	private final G3dPose geometryPose;
	private final Matrix4f origin;
	private final List<ResourceLocation> sprites;

	private G3dEntityModel(G3dModel model, ResourceManager resources, String socket)
	{
		this.model = model;
		this.geometryPose = new G3dPose(model.rig());
		this.origin = geometryPose.socketMatrix(socket, new Matrix4f()).invert();
		this.sprites = model.materials().stream().map(material -> {
			if (material.texture().isSlot())
				throw new IllegalArgumentException("Unbound material in entity model " + model.rig().id() + ": " + material.id());
			return G3dBlockGeometry.resolveSprite(resources, material.texture().resource());
		}).toList();
	}

	public static G3dEntityModel load(ResourceManager resources, ResourceLocation id, String socket) throws IOException
	{
		try (var input = resources.open(G3dResources.MODEL_FILES.idToFile(id)))
		{
			var model = G3dFiles.readModel(input);
			if (!model.rig().id().equals(id))
				throw new IOException("G3D model ID differs from its resource path " + id);
			return new G3dEntityModel(model, resources, socket);
		}
	}

	public void render(PoseStack pose, MultiBufferSource buffers, int light, Map<String, G3dTransform> overrides)
	{
		var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
		geometryPose.evaluate(overrides);
		pose.pushPose();
		pose.mulPose(origin);
		for (var mesh : model.meshes())
		{
			var transform = geometryPose.nodeMatrix(mesh.node());
			if (G3dPose.isCollapsed(transform))
				continue;
			var material = model.materials().get(mesh.material());
			var sprite = atlas.apply(sprites.get(mesh.material()));
			var output = buffers.getBuffer(layer(material));
			int emission = material.lightEmission();
			int packed = LightTexture.pack(Math.max(emission, LightTexture.block(light)), Math.max(emission, LightTexture.sky(light)));
			pose.pushPose();
			pose.mulPose(new Matrix4f(transform));
			for (int triangle = 0; triangle < mesh.indexCount(); triangle += 3)
				for (int corner = 0; corner < 4; corner++)
				{
					int vertex = mesh.index(triangle + Math.min(corner, 2));
					output.addVertex(pose.last(), mesh.component(vertex, 0), mesh.component(vertex, 1), mesh.component(vertex, 2))
							.setColor(-1).setUv(sprite.getU(mesh.component(vertex, 6)), sprite.getV(mesh.component(vertex, 7)))
							.setOverlay(OverlayTexture.NO_OVERLAY).setLight(packed)
							.setNormal(pose.last(), mesh.component(vertex, 3), mesh.component(vertex, 4), mesh.component(vertex, 5));
				}
			pose.popPose();
		}
		pose.popPose();
	}

	private static RenderType layer(G3dSource.Material material)
	{
		var atlas = TextureAtlas.LOCATION_BLOCKS;
		return switch (material.layers().entity().getPath())
		{
			case "entity/translucent" -> RenderType.entityTranslucent(atlas);
			case "entity/translucent_emissive" -> RenderType.entityTranslucentEmissive(atlas);
			default -> material.doubleSided() ? RenderType.entityCutoutNoCull(atlas) : RenderType.entityCutout(atlas);
		};
	}
}
