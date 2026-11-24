package dev.pswg.gadgets.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.gadgets.Workstations;
import dev.pswg.gadgets.drill.DrillParts;
import dev.pswg.rendering.g3d.G3dBlockGeometry;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public record DrillGeometry(G3dBlockGeometry body, Map<ResourceLocation, G3dBlockGeometry> parts) implements IUnbakedGeometry<DrillGeometry>
{
	public static final IGeometryLoader<DrillGeometry> LOADER = (json, context) -> {
		var parts = new HashMap<ResourceLocation, G3dBlockGeometry>();
		for (var variant : DrillParts.creativeVariants())
			for (var id : variant.partIds())
				if (!parts.containsKey(id))
				{
					var model = new JsonObject();
					model.addProperty("g3d", id.withPrefix("item/extraction_drill_").toString());
					parts.put(id, G3dBlockGeometry.LOADER.read(model, context));
				}
		var body = new JsonObject();
		body.addProperty("g3d", "pswg_gadgets:item/extraction_drill_in_hand");
		return new DrillGeometry(G3dBlockGeometry.LOADER.read(body, context), Map.copyOf(parts));
	};

	@Override
	public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> sprites,
	                       ModelState state, ItemOverrides overrides)
	{
		var bakedBody = body.bake(context, baker, sprites, state, overrides);
		var bakedParts = new HashMap<ResourceLocation, BakedModel>();
		parts.forEach((id, part) -> bakedParts.put(id, part.bake(context, baker, sprites, state, overrides)));
		return new BakedModelWrapper<>(bakedBody)
		{
			@Override
			public BakedModel applyTransform(ItemDisplayContext display, PoseStack pose, boolean leftHand)
			{
				originalModel.applyTransform(display, pose, leftHand);
				return this;
			}

			@Override
			public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous)
			{
				var passes = new ArrayList<>(originalModel.getRenderPasses(stack, fabulous));
				var configuration = stack.getOrDefault(Workstations.DRILL_PARTS.get(), DrillParts.DEFAULT);
				var ids = configuration.partIds();
				var defaults = DrillParts.DEFAULT.partIds();
				for (int i = 0; i < ids.size(); i++)
					passes.addAll(bakedParts.getOrDefault(ids.get(i), bakedParts.get(defaults.get(i))).getRenderPasses(stack, fabulous));
				return passes;
			}
		};
	}
}
