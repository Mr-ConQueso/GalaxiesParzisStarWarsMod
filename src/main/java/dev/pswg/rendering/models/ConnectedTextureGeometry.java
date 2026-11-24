package dev.pswg.rendering.models;

import dev.pswg.block.ConnectedBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeGroup;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record ConnectedTextureGeometry(boolean pillar, boolean insideFaces) implements IUnbakedGeometry<ConnectedTextureGeometry>
{
	public static final IGeometryLoader<ConnectedTextureGeometry> LOADER = (json, context) -> new ConnectedTextureGeometry(
			GsonHelper.getAsBoolean(json, "pillar", false), GsonHelper.getAsBoolean(json, "inside_faces", false));

	private static final ModelProperty<Integer> DIAGONALS = new ModelProperty<>();

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
	                       ModelState state, ItemOverrides overrides)
	{
		var border = sprites.apply(context.getMaterial("border"));
		var center = sprites.apply(context.getMaterial("center"));
		var fallback = new SimpleBakedModel.Builder(context.useAmbientOcclusion(), context.useBlockLight(),
				true, context.getTransforms(), overrides).particle(sprites.apply(context.getMaterial("particle")));
		var faces = new EnumMap<Direction, List<Quadrant>>(Direction.class);

		for (var rotation : List.of(BlockModelRotation.X0_Y0, BlockModelRotation.X0_Y90, BlockModelRotation.X0_Y180,
				BlockModelRotation.X0_Y270, BlockModelRotation.X90_Y0, BlockModelRotation.X270_Y0))
		{
			var matrix = rotation.getRotation().getMatrix();
			var face = Direction.rotate(matrix, Direction.NORTH);
			var up = Direction.rotate(matrix, Direction.UP);
			var east = Direction.rotate(matrix, Direction.EAST);
			var quadrants = new ArrayList<Quadrant>(4);
			for (int index = 0; index < 4; index++)
			{
				boolean left = (index & 1) == 0;
				boolean top = index < 2;
				var tiles = new ArrayList<List<BakedQuad>>(5);
				for (int tile = 0; tile < 5; tile++)
				{
					var quad = bakeTile(rotation, left, top, tile, tile == 4 ? center : border);
					tiles.add(insideFaces ? List.of(quad, inward(quad)) : List.of(quad));
				}
				var quadrant = new Quadrant(top ? up : up.getOpposite(), left ? east.getOpposite() : east, List.copyOf(tiles));
				quadrants.add(quadrant);
				for (var quad : quadrant.tiles().get(pillar && face.getAxis().isVertical() ? 4 : 0))
					fallback.addCulledFace(face, quad);
			}
			faces.put(face, List.copyOf(quadrants));
		}

		var hint = context.getRenderTypeHint();
		return new ConnectedModel(fallback.build(hint == null ? RenderTypeGroup.EMPTY : context.getRenderType(hint)), Map.copyOf(faces), pillar);
	}

	private static BakedQuad bakeTile(ModelState rotation, boolean left, boolean top, int tile, TextureAtlasSprite sprite)
	{
		int x = left ? 0 : 8;
		int y = top ? 8 : 0;
		float u = tile == 4 ? x : (left ? 4 : 0) + ((tile & 2) != 0 ? 8 : 0) + 0.025F;
		float v = tile == 4 ? 8 - y : (top ? 0 : 4) + ((tile & 1) != 0 ? 8 : 0) + 0.025F;
		float size = tile == 4 ? 8 : 3.95F;
		var element = new BlockElementFace(Direction.NORTH, -1, "#tile", new BlockFaceUV(new float[]{u, v, u + size, v + size}, 0));
		return new FaceBakery().bakeQuad(new Vector3f(x, y, 0), new Vector3f(x + 8, y + 8, 0),
				element, sprite, Direction.NORTH, rotation, null, true);
	}

	private static BakedQuad inward(BakedQuad quad)
	{
		int[] source = quad.getVertices();
		int[] vertices = new int[source.length];
		int stride = IQuadTransformer.STRIDE;
		var direction = quad.getDirection();
		int coordinate = IQuadTransformer.POSITION + direction.getAxis().ordinal();
		for (int vertex = 0; vertex < 4; vertex++)
		{
			System.arraycopy(source, (3 - vertex) * stride, vertices, vertex * stride, stride);
			int offset = vertex * stride + coordinate;
			vertices[offset] = Float.floatToRawIntBits(Float.intBitsToFloat(vertices[offset]) - 0.001F * direction.getAxisDirection().getStep());
		}
		net.neoforged.neoforge.client.ClientHooks.fillNormal(vertices, direction.getOpposite());
		return new BakedQuad(vertices, quad.getTintIndex(), direction.getOpposite(), quad.getSprite(), quad.isShade(), quad.hasAmbientOcclusion());
	}

	private record Quadrant(Direction vertical, Direction horizontal, List<List<BakedQuad>> tiles)
	{
	}

	private static final class ConnectedModel extends BakedModelWrapper<BakedModel>
	{
		private final Map<Direction, List<Quadrant>> faces;
		private final boolean pillar;

		private ConnectedModel(BakedModel fallback, Map<Direction, List<Quadrant>> faces, boolean pillar)
		{
			super(fallback);
			this.faces = faces;
			this.pillar = pillar;
		}

		@Override
		public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data)
		{
			if (pillar)
				return data;

			int mask = 0;
			for (var face : Direction.values())
			{
				var quadrants = faces.get(face);
				for (int index = 0; index < quadrants.size(); index++)
				{
					var quadrant = quadrants.get(index);
					if (ConnectedBlock.isConnected(state, quadrant.vertical()) && ConnectedBlock.isConnected(state, quadrant.horizontal())
							&& level.getBlockState(pos.relative(quadrant.vertical()).relative(quadrant.horizontal())).is(state.getBlock()))
						mask |= 1 << (face.ordinal() * 4 + index);
				}
			}
			return data.derive().with(DIAGONALS, mask).build();
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random, ModelData data,
		                               @Nullable RenderType layer)
		{
			if (state == null)
				return originalModel.getQuads(null, face, random, data, layer);
			if (face == null || ConnectedBlock.isConnected(state, face))
				return List.of();

			var result = new ArrayList<BakedQuad>(8);
			var quadrants = faces.get(face);
			var diagonals = data.get(DIAGONALS);
			for (int index = 0; index < quadrants.size(); index++)
			{
				var q = quadrants.get(index);
				int tile = ConnectedBlock.isConnected(state, q.vertical()) ? 1 : 0;
				if (pillar && face.getAxis().isVertical())
					tile = 4;
				else if (!pillar && ConnectedBlock.isConnected(state, q.horizontal()))
					tile |= 2;
				if (tile == 3 && diagonals != null && (diagonals & 1 << (face.ordinal() * 4 + index)) != 0)
					tile = 4;
				result.addAll(q.tiles().get(tile));
			}
			return result;
		}
	}
}
