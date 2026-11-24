package net.mrconqueso.pswg.p3d;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.mrconqueso.pswg.util.block.DisplacingBlock;
import net.mrconqueso.pswg.util.block.IPicklingBlock;
import net.mrconqueso.pswg.util.block.WaterloggableRotating3BlockWithGuiEntity;
import net.mrconqueso.pswg.util.block.rotating.WaterloggableRotatingBlock;
import net.mrconqueso.pswg.util.client.model.DynamicBakedModel;
import net.mrconqueso.pswg.util.math.MathUtil;
import net.mrconqueso.pswg.util.math.QuatUtil;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.awt.image.renderable.RenderContext;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Supplier;

public class P3dBakedBlockModel extends DynamicBakedModel
{
	private final CacheMethod cacheMethod;
	private final TextureAtlasSprite baseSprite;
	private final HashMap<String, TextureAtlasSprite> additionalSprites;
	private final ResourceLocation[] modelIds;

	private P3dBakedBlockModel(CacheMethod cacheMethod, TextureAtlasSprite baseSprite, TextureAtlasSprite particleSprite, HashMap<String, TextureAtlasSprite> additionalSprites, ResourceLocation[] modelIds)
	{
		super(particleSprite, ModelHelper.MODEL_TRANSFORM_BLOCK);
		this.cacheMethod = cacheMethod;
		this.baseSprite = baseSprite;
		this.additionalSprites = additionalSprites;
		this.modelIds = modelIds;
	}

	@Override
	protected CacheMethod getDiscriminator()
	{
		return cacheMethod;
	}

	@Override
	protected Mesh createBlockMesh(BlockRenderView blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context, Matrix4f transformation)
	{
		if (state != null)
		{
			if (state.getBlock() instanceof DisplacingBlock db)
			{
				var shape = db.getOutlineShape(state, blockView, pos, ShapeContext.absent());
				var center = VoxelShapeUtil.getCenter(shape);
				transformation.translate((float)center.x - 0.5f, 0, (float)center.z - 0.5f);
			}
			else if (state.getBlock() instanceof WaterloggableRotating3BlockWithGuiEntity)
			{
				var side = state.get(WaterloggableRotating3BlockWithGuiEntity.SIDE);
				if (side != WaterloggableRotating3BlockWithGuiEntity.Side.MIDDLE)
					return createMeshBuilder().build();
			}
		}

		return createMesh(new P3dBlockRenderTarget.Block(blockView, state, pos), randomSupplier, context, transformation);
	}

	@Override
	protected Mesh createItemMesh(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context, Matrix4f transformation)
	{
		return createMesh(new P3dBlockRenderTarget.Item(stack), randomSupplier, context, transformation);
	}

	private Mesh createMesh(P3dBlockRenderTarget target, Supplier<Random> randomSupplier, RenderContext context, Matrix4f transformation)
	{
		var meshBuilder = createMeshBuilder();
		var quadEmitter = meshBuilder.getEmitter();

		var modelId = modelIds[0];

		if (target instanceof P3dBlockRenderTarget.Block blockRenderTarget)
			modelId = getPickleModel(modelId, blockRenderTarget.getState());

		var model = P3dManager.INSTANCE.get(modelId);

		var ms = new MatrixStack();
		var e = ms.peek();
		e.getPositionMatrix().mul(transformation);
		e.getNormalMatrix().mul(new Matrix3f(transformation));

		// blocks are centered in P3D models
		ms.translate(0.5f, 0, 0.5f);

		// one pixel in P3D models is 1.6 game pixels
		MathUtil.scalePos(ms, 0.625f, 0.625f, 0.625f);

		// P3D models are +Z forward
		ms.multiply(new Quaternionf().rotationY((float)(Math.PI / -2)));

		Block block = null;
		if (target instanceof P3dBlockRenderTarget.Block blockRenderTarget && blockRenderTarget.getState() != null)
			block = blockRenderTarget.getState().getBlock();
		else if (target instanceof P3dBlockRenderTarget.Item itemRenderTarget && itemRenderTarget.getStack().getItem() instanceof BlockItem blockItem)
			block = blockItem.getBlock();

		P3dBlockRendererRegistry.get(block).renderBlock(ms, quadEmitter, target, randomSupplier, context, model, baseSprite, additionalSprites);

		return meshBuilder.build();
	}

	private ResourceLocation getPickleModel(ResourceLocation defaultModel, BlockState state)
	{
		if (state != null && state.getBlock() instanceof IPicklingBlock pb)
		{
			var pickleProp = pb.getPickleProperty();
			var pickle = state.get(pickleProp) - 1;
			if (pickle >= 0 && pickle < modelIds.length)
				defaultModel = modelIds[pickle];
		}
		return defaultModel;
	}

	@Override
	protected Matrix4f createTransformation(BlockState state)
	{
		var modelId = getPickleModel(modelIds[0], state);

		var model = P3dManager.INSTANCE.get(modelId);

		if (state == null)
		{
			// Item transformation, scale largest dimension to 1

			return getItemTransformation(model);
		}

		var mat = new Matrix4f();

		if (state.getBlock() instanceof WaterloggableRotatingBlock)
		{
			mat.translate(0.5f, 0.5f, 0.5f);

			mat.rotate(MathUtil.getRotation(state.get(WaterloggableRotatingBlock.FACING)));

			mat.translate(-0.5f, -0.5f, -0.5f);
		}

		if (state.getBlock() instanceof PillarBlock)
		{
			var axis = state.get(RotatedPillarBlock.AXIS);

			switch (axis)
			{
				case X:
					mat.rotateZ((float)(Math.PI / -2));
					mat.translate(-1, 0, 0);
					break;
				case Z:
					mat.rotateY((float)(Math.PI / -2));
					mat.rotateZ((float)(Math.PI / -2));
					mat.translate(-1, 0, -1);
					break;
				case Y:
					break;
			}
		}

		return mat;
	}

	@NotNull
	public static Matrix4f getItemTransformation(P3dModel model)
	{
		var mat = new Matrix4f();

		var bounds = model.bounds();

		var largestDimension = 0.625f * (float)Math.max(bounds.getXLength(), Math.max(bounds.getYLength(), bounds.getZLength()));
		var scale = 1 / largestDimension;

		var minX = (float)bounds.minX * 0.625f;
		var maxX = (float)bounds.maxX * 0.625f;
		var minY = (float)bounds.minY * 0.625f;
		var minZ = (float)bounds.minZ * 0.625f;
		var maxZ = (float)bounds.maxZ * 0.625f;

		mat.translate(0.5f, 0, 0.5f);
		mat.scale(scale, scale, scale);
		mat.translate(-0.5f, 0, -0.5f);

		mat.translate((maxX - minX) / 2 - maxX, -minY, (maxZ - minZ) / 2 - maxZ);

		mat.translate(0.5f, 0, 0.5f);
		mat.rotate(QuatUtil.ROT_Y_POS90);
		mat.translate(-0.5f, 0, -0.5f);

		return mat;
	}

	public static P3dBakedBlockModel create(Function<Material, TextureAtlasSprite> spriteMap, CacheMethod cacheMethod, ResourceLocation baseTexture, ResourceLocation particleTexture, HashMap<String, ResourceLocation> additionalTextures, ResourceLocation... models)
	{
		HashMap<String, TextureAtlasSprite> additionalSprites = new HashMap<>();

		for (var entry : additionalTextures.entrySet())
			additionalSprites.put(entry.getKey(), spriteMap.apply(new Material(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, entry.getValue())));

		return new P3dBakedBlockModel(
				cacheMethod,
				spriteMap.apply(new Material(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, baseTexture)),
				spriteMap.apply(new Material(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, particleTexture)),
				additionalSprites,
				models
		);
	}

	@Override
	public boolean hasDepth()
	{
		return true;
	}

	@Override
	public boolean isSideLit()
	{
		return true;
	}
}
