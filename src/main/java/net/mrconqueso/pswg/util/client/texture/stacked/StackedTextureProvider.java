package net.mrconqueso.pswg.util.client.texture.stacked;

import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.util.client.texture.CallbackTexture;
import net.mrconqueso.pswg.util.client.texture.TextureProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Collection;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class StackedTextureProvider extends TextureProvider<Collection<ResourceLocation>>
{
	public static final ResourceLocation ROOT = Resources.id("///stacked");

	private final ResourceLocation transparentTexture;

	public StackedTextureProvider(TextureManager textureManager, ResourceLocation transparentTexture)
	{
		super(ROOT, textureManager);
		this.transparentTexture = transparentTexture;
	}

	@Override
	protected CallbackTexture createTexture(ResourceLocation destId, Collection<ResourceLocation> requestData, Consumer<Boolean> callback)
	{
		for (var id : requestData)
			registerDependencyCallbacks(destId, id);
		return new StackedTexture(transparentTexture, DefaultSkinHelper.getTexture(), requestData, callback);
	}
}
