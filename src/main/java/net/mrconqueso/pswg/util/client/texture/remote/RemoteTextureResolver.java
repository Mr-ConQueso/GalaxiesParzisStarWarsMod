package net.mrconqueso.pswg.util.client.texture.remote;

import net.minecraft.resources.ResourceLocation;

public interface RemoteTextureResolver
{
	RemoteTextureUrl getTexture(ResourceLocation id);
}
