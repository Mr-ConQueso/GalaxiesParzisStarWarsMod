package net.mrconqueso.pswg.mixin.client.models;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * GQB model injection support
 */
//@Mixin(CuboidItemModelWrapper.Unbaked.class)
//public abstract class CuboidItemModelWrapperMixin
//{
//	/**
//	 * Gets the wrapped model identifier.
//	 *
//	 * @return The wrapped model identifier.
//	 */
//	@Shadow
//	public abstract ResourceLocation model();
//
//	/**
//	 * Gets the optional additional transformation.
//	 *
//	 * @return The optional local transformation.
//	 */
//	@Shadow
//	public abstract Optional<Transformation> transformation();
//
//	/**
//	 * Gets the tint sources for this item model.
//	 *
//	 * @return The tint source list.
//	 */
//	@Shadow
//	public abstract List<ItemTintSource> tints();
//
//	@Inject(method = "bake", at = @At("HEAD"), cancellable = true)
//	private void injectGqbBake(ItemModel.BakingContext context, Matrix4fc transformation, CallbackInfoReturnable<ItemModel> cir)
//	{
//		var geometry = GalaxiesModelBakery.getGeometry(model());
//
//		if (geometry.isEmpty())
//		{
//			return;
//		}
//
//		var baker = context.blockModelBaker();
//		ResolvedModel resolvedModel = baker.getModel(model());
//		TextureSlots textureSlots = resolvedModel.getTopTextureSlots();
//		var quads = geometry.get().bake(textureSlots, baker, BlockModelRotation.X0_Y0, resolvedModel);
//		var properties = ModelRenderProperties.fromResolvedModel(baker, resolvedModel, textureSlots);
//		Matrix4fc modelTransform = Transformation.compose(transformation, transformation());
//
//		cir.setReturnValue(new GqbItemModel(tints(), quads, properties, modelTransform));
//	}
//}
