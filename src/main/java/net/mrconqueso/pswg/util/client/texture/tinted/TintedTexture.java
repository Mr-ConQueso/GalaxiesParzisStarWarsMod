package net.mrconqueso.pswg.util.client.texture.tinted;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.mrconqueso.pswg.data.TintedIdentifier;
import net.mrconqueso.pswg.util.client.texture.CallbackTexture;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.io.IOException;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class TintedTexture extends CallbackTexture
{
	private final TintedIdentifier texture;

	public TintedTexture(TintedIdentifier texture, ResourceLocation fallbackSkin, Consumer<Boolean> completionCallback)
	{
		super(fallbackSkin, completionCallback);
		this.texture = texture;
	}

	@Override
	protected NativeImage generateImage(ResourceManager manager) throws IOException
	{
		var textureManager = MinecraftClient.getInstance().getTextureManager();

		var tex = textureManager.getTexture(texture);

		NativeImage nativeImage;
		if (tex instanceof NativeImageBackedTexture nibt)
			nativeImage = nibt.getImage();
		else if (tex instanceof CallbackTexture t)
			nativeImage = t.getImage();
		else
		{
			var texData = TextureData.load(manager, texture);
			nativeImage = texData.getImage();
		}

		if (nativeImage == null)
			throw new IOException("NativeImage was null");

		var width = nativeImage.getWidth();
		var height = nativeImage.getHeight();
		for (var x = 0; x < width; x++)
		{
			for (var y = 0; y < height; y++)
				nativeImage.setColor(x, y, NativeImageUtil.tint(nativeImage.getColor(x, y), texture.getTint(), texture.getTintMode()));
		}

		return nativeImage;
	}
}
