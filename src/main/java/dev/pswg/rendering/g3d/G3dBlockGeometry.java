package dev.pswg.rendering.g3d;

import com.google.gson.JsonParseException;
import dev.pswg.model.g3d.G3dFiles;
import dev.pswg.model.g3d.G3dModel;
import dev.pswg.model.g3d.G3dPose;
import dev.pswg.model.g3d.G3dResources;
import dev.pswg.rendering.ptex.PtexSpriteSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

public record G3dBlockGeometry(G3dModel model) implements IUnbakedGeometry<G3dBlockGeometry>
{
	public static final IGeometryLoader<G3dBlockGeometry> LOADER = (json, context) -> {
		var id = ResourceLocation.parse(GsonHelper.getAsString(json, "g3d"));
		var file = G3dResources.MODEL_FILES.idToFile(id);
		var resources = Minecraft.getInstance().getResourceManager();
		try (var stream = resources.getResourceOrThrow(file).open())
		{
			var model = G3dFiles.readModel(stream);
			if (!model.rig().id().equals(id))
				throw new JsonParseException("G3D ID does not match model resource " + file);
			return new G3dBlockGeometry(model);
		}
		catch (IOException exception)
		{
			throw new JsonParseException("Could not read G3D geometry " + file, exception);
		}
	};

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBaker baker,
	                       Function<Material, TextureAtlasSprite> sprites, ModelState state, ItemOverrides overrides)
	{
		var output = CompositeModel.Baked.builder(context, sprites.apply(context.getMaterial("particle")), overrides, context.getTransforms());
		var pose = new G3dPose(model.rig());
		var resources = Minecraft.getInstance().getResourceManager();
		var base = state.getRotation().compose(context.getRootTransform()).blockCenterToCorner();
		for (var mesh : model.meshes())
		{
			if (G3dPose.isCollapsed(pose.nodeMatrix(mesh.node())))
				continue;

			var material = model.materials().get(mesh.material());
			var texture = material.texture();
			var sprite = sprites.apply(texture.isSlot() ? context.getMaterial(texture.slot()) :
					new Material(TextureAtlas.LOCATION_BLOCKS, resolveSprite(resources, texture.resource())));
			var transform = base.getMatrix().mul(pose.nodeMatrix(mesh.node()), new Matrix4f());
			var normal = transform.normal(new Matrix3f());
			var quads = new ArrayList<BakedQuad>();
			var layers = material.layers().block();
			var renderType = context.getRenderType(layers.withPath(path -> path.startsWith("block/") ? path.substring(6) : path));
			int light = LightTexture.pack(material.lightEmission(), material.lightEmission());
			var vertexConsumer = new QuadBakingVertexConsumer();
			vertexConsumer.setSprite(sprite);
			vertexConsumer.setTintIndex(material.tintIndex());
			vertexConsumer.setShade(true);
			vertexConsumer.setHasAmbientOcclusion(context.useAmbientOcclusion());
			for (int triangle = 0; triangle < mesh.indexCount(); triangle += 3)
			{
				quads.add(bakeTriangle(mesh, triangle, false, transform, normal, sprite, light, vertexConsumer));
				if (material.doubleSided())
					quads.add(bakeTriangle(mesh, triangle, true, transform, normal, sprite, light, vertexConsumer));
			}
			output.addQuads(renderType, quads);
		}
		return output.build();
	}

	static ResourceLocation resolveSprite(ResourceManager resources, ResourceLocation texture)
	{
		if (resources.getResource(PtexSpriteSource.definitionFile(texture)).isPresent())
			return PtexSpriteSource.spriteId(texture);

		var path = texture.getPath();
		if (path.startsWith("textures/") && path.endsWith(".png"))
			return texture.withPath(path.substring("textures/".length(), path.length() - ".png".length()));
		return texture;
	}

	private static BakedQuad bakeTriangle(G3dModel.Mesh mesh, int offset, boolean reverse, Matrix4f transform, Matrix3f normal,
	                                      TextureAtlasSprite sprite, int light, QuadBakingVertexConsumer consumer)
	{
		var position = new Vector3f();
		var direction = new Vector3f();
		for (int corner = 0; corner < 4; corner++)
		{
			int sourceCorner = Math.min(corner, 2);
			int index = mesh.index(offset + (reverse ? 2 - sourceCorner : sourceCorner));
			position.set(mesh.component(index, 0), mesh.component(index, 1), mesh.component(index, 2));
			transform.transformPosition(position);
			direction.set(mesh.component(index, 3), mesh.component(index, 4), mesh.component(index, 5));
			normal.transform(direction);
			if (reverse)
				direction.negate();
			if (direction.lengthSquared() > 0)
				direction.normalize();
			if (corner == 0)
				consumer.setDirection(Direction.getNearest(direction.x, direction.y, direction.z));
			consumer.addVertex(position.x, position.y, position.z)
					.setColor(255, 255, 255, 255)
					.setUv(sprite.getU(mesh.component(index, 6)), sprite.getV(mesh.component(index, 7)))
					.setLight(light)
					.setNormal(direction.x, direction.y, direction.z);
		}
		return consumer.bakeQuad();
	}
}
