package dev.pswg.rendering.p3d;

import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public record P3dGeometry(P3dModel model, float scale, float y, float yaw, Set<String> hidden) implements IUnbakedGeometry<P3dGeometry>
{
	public static final IGeometryLoader<P3dGeometry> LOADER = (json, context) -> {
		var id = ResourceLocation.parse(GsonHelper.getAsString(json, "p3d")).withPath(path -> "models/" + path + ".p3d");
		try (var input = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).open())
		{
			var excluded = new HashSet<String>();
			if (json.has("hidden_parts"))
				GsonHelper.getAsJsonArray(json, "hidden_parts").forEach(part -> excluded.add(part.getAsString()));
			return new P3dGeometry(P3dModel.read(input, true), GsonHelper.getAsFloat(json, "scale", 1),
					GsonHelper.getAsFloat(json, "y", 0), GsonHelper.getAsFloat(json, "rotation_y", 0), Set.copyOf(excluded));
		}
		catch (IOException e)
		{
			throw new JsonParseException("Could not read P3D model " + id, e);
		}
	};

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBaker bakery, Function<Material, TextureAtlasSprite> sprites,
	                       ModelState state, ItemOverrides overrides)
	{
		var sprite = sprites.apply(context.getMaterial("particle"));
		var builder = CompositeModel.Baked.builder(context, sprite, overrides, context.getTransforms());
		var texture = context.hasMaterial("texture") ? sprites.apply(context.getMaterial("texture")) : sprite;
		var root = state.getRotation().compose(context.getRootTransform()).blockCenterToCorner().getMatrix();
		root.translate(0.5F, y, 0.5F).rotateY(yaw * net.minecraft.util.Mth.DEG_TO_RAD).scale(scale);
		for (var part : model.roots())
			bakePart(context, builder, part, root, texture);
		return builder.build();
	}

	private void bakePart(IGeometryBakingContext context, CompositeModel.Baked.Builder builder, P3dModel.Part part,
	                             Matrix4f parent, TextureAtlasSprite sprite)
	{
		if (hidden.contains(part.name()))
			return;
		var transform = parent.mul(part.transform(), new Matrix4f());
		var normalMatrix = transform.normal(new Matrix3f());
		var quads = new ArrayList<BakedQuad>(part.faces().size());
		var normal = new Vector3f();
		var position = new Vector3f();
		var consumer = new QuadBakingVertexConsumer();
		consumer.setSprite(sprite);
		consumer.setTintIndex(-1);
		consumer.setShade(part.material() != 3);
		for (var face : part.faces())
		{
			normalMatrix.transform(face.normal(), normal).normalize();
			consumer.setDirection(Direction.getNearest(normal.x, normal.y, normal.z));
			for (var vertex : face.vertices())
			{
				transform.transformPosition(position.set(vertex.x(), vertex.y(), vertex.z()));
				consumer.addVertex(position.x, position.y, position.z).setColor(-1)
						.setUv(sprite.getU(vertex.u()), sprite.getV(vertex.v()))
						.setLight(part.material() == 3 ? LightTexture.FULL_BRIGHT : 0)
						.setNormal(normal.x, normal.y, normal.z);
			}
			quads.add(consumer.bakeQuad());
		}
		var layer = ResourceLocation.withDefaultNamespace(part.material() == 2 ? "translucent" : part.material() == 0 ? "solid" : "cutout");
		builder.addQuads(context.getRenderType(layer), quads);
		for (var child : part.children())
			bakePart(context, builder, child, transform, sprite);
	}
}
