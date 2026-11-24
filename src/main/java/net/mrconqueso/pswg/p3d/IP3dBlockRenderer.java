package net.mrconqueso.pswg.p3d;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.function.Supplier;

public interface IP3dBlockRenderer {
	void renderBlock(PoseStack poseStack, QuadEmitter quadEmitter, P3dBlockRenderTarget target, Supplier<RandomSource> randomSupplier, RenderContext renderContext, P3dModel model, TextureAtlasSprite baseSprite, HashMap<String, TextureAtlasSprite> additionalSprites);
}