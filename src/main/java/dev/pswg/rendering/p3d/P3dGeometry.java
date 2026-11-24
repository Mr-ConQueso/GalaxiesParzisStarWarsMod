package dev.pswg.rendering.p3d;

import com.google.gson.JsonParseException;
import dev.pswg.block.MultipartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.CompositeModel;
import net.neoforged.neoforge.client.model.data.ModelData;
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
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public record P3dGeometry(P3dModel model, float scale, float y, float z, float yaw, Set<String> hidden, boolean fitItem,
                          ResourceLocation multipart, BlockPos slice, List<Direction.Axis> clipAxes)
		implements IUnbakedGeometry<P3dGeometry>
{
	private static final float CLIP_EPSILON = 0.00001F;

	public static final IGeometryLoader<P3dGeometry> LOADER = (json, context) -> {
		var id = ResourceLocation.parse(GsonHelper.getAsString(json, "p3d")).withPath(path -> "models/" + path + ".p3d");
		try (var input = Minecraft.getInstance().getResourceManager().getResourceOrThrow(id).open())
		{
			var excluded = new HashSet<String>();
			if (json.has("hidden_parts"))
				GsonHelper.getAsJsonArray(json, "hidden_parts").forEach(part -> excluded.add(part.getAsString()));
			var multipart = json.has("multipart_block") ? ResourceLocation.parse(GsonHelper.getAsString(json, "multipart_block")) : null;
			var clipAxes = new ArrayList<Direction.Axis>();
			String axes = GsonHelper.getAsString(json, "clip_axes", "");
			for (var axis : Direction.Axis.values())
				if (axes.contains(axis.getName()))
					clipAxes.add(axis);
			var slice = BlockPos.ZERO;
			if (json.has("slice"))
			{
				var values = GsonHelper.getAsJsonArray(json, "slice");
				if (values.size() != 3)
					throw new JsonParseException("P3D slice requires three block offsets");
				slice = new BlockPos(values.get(0).getAsInt(), values.get(1).getAsInt(), values.get(2).getAsInt());
			}
			return new P3dGeometry(P3dModel.read(input, true), GsonHelper.getAsFloat(json, "scale", 1),
					GsonHelper.getAsFloat(json, "y", 0), GsonHelper.getAsFloat(json, "z", 0), GsonHelper.getAsFloat(json, "rotation_y", 0),
					Set.copyOf(excluded), GsonHelper.getAsBoolean(json, "fit_item", false), multipart, slice, List.copyOf(clipAxes));
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
		var particle = sprites.apply(context.getMaterial("particle"));
		var texture = context.hasMaterial("texture") ? sprites.apply(context.getMaterial("texture")) : particle;
		var root = new Matrix4f().translate(0.5F, y, 0.5F + z).rotateY(yaw * Mth.DEG_TO_RAD).scale(scale);
		if (fitItem)
			root = fitItem(root);

		var rotation = state.getRotation().compose(context.getRootTransform()).blockCenterToCorner().getMatrix();
		if (multipart == null)
			return bakeSlice(context, particle, texture, overrides, root, rotation, slice);

		if (!(BuiltInRegistries.BLOCK.get(multipart) instanceof MultipartBlock<?> block))
			throw new JsonParseException("P3D model references an unknown multipart block " + multipart);
		var parts = new ArrayList<BakedModel>(block.partCount());
		for (int part = 0; part < block.partCount(); part++)
			parts.add(bakeSlice(context, particle, texture, overrides, root, rotation, block.eastFacingOffset(part)));
		return new SlicedModel(block, List.copyOf(parts));
	}

	private BakedModel bakeSlice(IGeometryBakingContext context, TextureAtlasSprite particle, TextureAtlasSprite texture,
	                             ItemOverrides overrides, Matrix4f root, Matrix4f rotation, BlockPos offset)
	{
		var builder = CompositeModel.Baked.builder(context, particle, overrides, context.getTransforms());
		var local = new Matrix4f(rotation).translate(-offset.getX(), -offset.getY(), -offset.getZ());
		for (var part : model.roots())
			bakePart(context, builder, part, root, local, offset, texture);
		return builder.build();
	}

	private void bakePart(IGeometryBakingContext context, CompositeModel.Baked.Builder builder, P3dModel.Part part,
	                      Matrix4f parent, Matrix4f local, BlockPos offset, TextureAtlasSprite sprite)
	{
		if (hidden.contains(part.name()))
			return;
		var transform = parent.mul(part.transform(), new Matrix4f());
		var normalMatrix = transform.normal(new Matrix3f());
		var localNormals = local.normal(new Matrix3f());
		var quads = new ArrayList<BakedQuad>(part.faces().size());
		var position = new Vector3f();
		var consumer = new QuadBakingVertexConsumer();
		consumer.setSprite(sprite);
		consumer.setTintIndex(-1);
		consumer.setShade(part.material() != 3);
		for (var face : part.faces())
		{
			var normal = normalMatrix.transform(face.normal(), new Vector3f()).normalize();
			List<P3dModel.Vertex> vertices = new ArrayList<>(4);
			for (var vertex : face.vertices())
			{
				transform.transformPosition(position.set(vertex.x(), vertex.y(), vertex.z()));
				vertices.add(new P3dModel.Vertex(position.x, position.y, position.z, vertex.u(), vertex.v()));
			}
			for (var axis : clipAxes)
			{
				int lower = axis.choose(offset.getX(), offset.getY(), offset.getZ());
				vertices = clipSlice(vertices, axis, lower, (float) axis.choose(normal.x, normal.y, normal.z));
			}
			localNormals.transform(normal).normalize();
			consumer.setDirection(Direction.getNearest(normal.x, normal.y, normal.z));
			P3dClip.emitQuads(vertices, quad -> {
				for (var vertex : quad)
				{
					local.transformPosition(position.set(vertex.x(), vertex.y(), vertex.z()));
					consumer.addVertex(position.x, position.y, position.z).setColor(-1)
							.setUv(sprite.getU(vertex.u()), sprite.getV(vertex.v()))
							.setLight(part.material() == 3 ? LightTexture.FULL_BRIGHT : 0)
							.setNormal(normal.x, normal.y, normal.z);
				}
				quads.add(consumer.bakeQuad());
			});
		}
		if (!quads.isEmpty())
		{
			var layer = ResourceLocation.withDefaultNamespace(part.material() == 2 ? "translucent" : part.material() == 0 ? "solid" : "cutout");
			builder.addQuads(context.getRenderType(layer), quads);
		}
		for (var child : part.children())
			bakePart(context, builder, child, transform, local, offset, sprite);
	}

	private static List<P3dModel.Vertex> clipSlice(List<P3dModel.Vertex> vertices, Direction.Axis axis, int lower, float normal)
	{
		int upper = lower + 1;
		// Assign a face on a seam to the cell whose geometry it encloses.
		int edge = normal > 0 ? lower : upper;
		if (vertices.stream().allMatch(vertex -> Math.abs(P3dClip.coordinate(vertex, axis) - edge) < CLIP_EPSILON))
			return List.of();
		return P3dClip.clip(P3dClip.clip(vertices, axis, lower - CLIP_EPSILON, true), axis, upper + CLIP_EPSILON, false);
	}

	private Matrix4f fitItem(Matrix4f root)
	{
		var min = new Vector3f(Float.POSITIVE_INFINITY);
		var max = new Vector3f(Float.NEGATIVE_INFINITY);
		for (var part : model.roots())
			includeBounds(part, root, min, max);
		var size = max.sub(min, new Vector3f());
		float longest = Math.max(size.x, Math.max(size.y, size.z));
		if (!(longest > 0))
			return root;
		var center = min.add(max).mul(0.5F);
		return new Matrix4f().translate(0.5F, 0.5F, 0.5F).scale(1 / longest).translate(center.negate()).mul(root);
	}

	private void includeBounds(P3dModel.Part part, Matrix4f parent, Vector3f min, Vector3f max)
	{
		if (hidden.contains(part.name()))
			return;
		var transform = parent.mul(part.transform(), new Matrix4f());
		var point = new Vector3f();
		for (var face : part.faces())
			for (var vertex : face.vertices())
			{
				transform.transformPosition(point.set(vertex.x(), vertex.y(), vertex.z()));
				min.min(point);
				max.max(point);
			}
		for (var child : part.children())
			includeBounds(child, transform, min, max);
	}

	private static final class SlicedModel extends BakedModelWrapper<BakedModel>
	{
		private final MultipartBlock<?> block;
		private final List<BakedModel> parts;

		SlicedModel(MultipartBlock<?> block, List<BakedModel> parts)
		{
			super(parts.getFirst());
			this.block = block;
			this.parts = parts;
		}

		private BakedModel part(BlockState state) { return state != null && state.is(block) ? parts.get(block.partIndex(state)) : originalModel; }

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random)
		{
			return part(state).getQuads(state, side, random, ModelData.EMPTY, null);
		}

		@Override
		public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random, ModelData data, RenderType type)
		{
			return part(state).getQuads(state, side, random, data, type);
		}

		@Override
		public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data)
		{
			return part(state).getRenderTypes(state, random, data);
		}
	}
}
