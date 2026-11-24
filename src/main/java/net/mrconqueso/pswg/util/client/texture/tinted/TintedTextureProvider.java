package net.mrconqueso.pswg.util.client.texture.tinted;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.data.TintedIdentifier;
import net.mrconqueso.pswg.util.client.texture.CallbackTexture;
import net.mrconqueso.pswg.util.client.texture.TextureProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class TintedTextureProvider extends TextureProvider<TintedIdentifier>
{
	public static final ResourceLocation ROOT = Resources.id("///tinted");

	public TintedTextureProvider(TextureManager textureManager)
	{
		super(ROOT, textureManager);
	}

	public ResourceLocation tint(String textureId, ResourceLocation texture, int color)
	{
		return getId(
				textureId,
				() -> texture,
				() -> new TintedIdentifier(texture.getNamespace(), texture.getPath(), NativeImageUtil.argbToAbgr(color))
		);
	}

	@Override
	protected CallbackTexture createTexture(ResourceLocation destId, TintedIdentifier request, Consumer<Boolean> callback)
	{
		registerDependencyCallbacks(destId, request);
		return new TintedTexture(request, DefaultSkinHelper.getTexture(), callback);
	}
}
