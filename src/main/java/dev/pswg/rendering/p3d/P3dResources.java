package dev.pswg.rendering.p3d;

import dev.pswg.Galaxies;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class P3dResources implements ResourceManagerReloadListener
{
	public static final P3dResources MODELS = new P3dResources(new FileToIdConverter("models", ".p3d"), true);
	public static final P3dResources RIGS = new P3dResources(new FileToIdConverter("rigs", ".p3dr"), false);

	private final FileToIdConverter files;
	private final boolean geometry;
	private volatile Map<ResourceLocation, P3dModel> entries = Map.of();

	private P3dResources(FileToIdConverter files, boolean geometry)
	{
		this.files = files;
		this.geometry = geometry;
	}

	public P3dModel get(ResourceLocation id)
	{
		return entries.get(id);
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager)
	{
		var loaded = new HashMap<ResourceLocation, P3dModel>();
		for (var entry : files.listMatchingResources(manager).entrySet())
		{
			try (var input = entry.getValue().open())
			{
				loaded.put(files.fileToId(entry.getKey()), P3dModel.read(input, geometry));
			}
			catch (IOException exception)
			{
				Galaxies.LOGGER.error("Could not read P3D resource {}", entry.getKey(), exception);
			}
		}
		entries = Map.copyOf(loaded);
	}
}
