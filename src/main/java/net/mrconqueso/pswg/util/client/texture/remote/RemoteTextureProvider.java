package net.mrconqueso.pswg.util.client.texture.remote;

import com.google.common.hash.Hashing;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.util.client.texture.CallbackTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class RemoteTextureProvider extends TextureProvider<ResourceLocation>
{
	public static final ResourceLocation ROOT = Resources.id("///remote_skin");

	private final ResourceLocation transparentTexture;
	private final Path skinCacheDir;
	private final RemoteTextureResolver remoteTextureResolver;

	public RemoteTextureProvider(TextureManager textureManager, ResourceLocation transparentTexture, RemoteTextureResolver remoteTextureResolver, Path skinCacheDir)
	{
		super(ROOT, textureManager);
		this.transparentTexture = transparentTexture;
		this.skinCacheDir = skinCacheDir;
		this.remoteTextureResolver = remoteTextureResolver;
	}

	public ResourceLocation getId(String requestName, Supplier<ResourceLocation> fallback)
	{
		return getId(requestName, fallback, () -> createCacheId(requestName));
	}

	@Override
	protected CallbackTexture createTexture(ResourceLocation destId, ResourceLocation requestData, Consumer<Boolean> callback)
	{
		registerDependencyCallbacks(destId, requestData);

		final var remoteTextureUrl = remoteTextureResolver.getTexture(requestData);

		var string = Hashing.sha1().hashUnencodedChars(remoteTextureUrl.getHash()).toString();

		var skinCacheDir = this.skinCacheDir.resolve(string.length() > 2 ? string.substring(0, 2) : "xx");
		var skinCacheFile = skinCacheDir.resolve(string);
		return new RemoteTexture(requestData, skinCacheFile, remoteTextureUrl.getUrl(), transparentTexture, success -> {
			if (!success)
				markTextureFailure(requestData);
			callback.accept(success);
		});
	}
}
