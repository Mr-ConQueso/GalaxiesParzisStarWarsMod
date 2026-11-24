package net.mrconqueso.pswg.data;

import com.google.common.collect.Maps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.util.perf.Profiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class KeyedReloadableLoader<T> extends SimplePreparableReloadListener
{
	private static final Logger LOGGER = LogManager.getLogger();
	private final String fileSuffix;
	private final int fileSuffixLength;
	private final String startingPath;

	public KeyedReloadableLoader(String startingPath, String fileExtension)
	{
		this.startingPath = startingPath;
		fileSuffix = "." + fileExtension;
		fileSuffixLength = fileSuffix.length();
	}

	public abstract T readResource(ResourceManager resourceManager, Profiler profiler, InputStream stream) throws IOException;

	@Override
	public final CompletableFuture<Void> reload(ResourceReloader.Synchronizer synchronizer, ResourceManager manager, Profiler prepareProfiler, Profiler applyProfiler, Executor prepareExecutor, Executor applyExecutor)
	{
		return CompletableFuture.supplyAsync(() -> this.prepare(manager, prepareProfiler), prepareExecutor)
		                        .thenCompose(synchronizer::whenPrepared)
		                        .thenAcceptAsync(object -> this.apply(object, manager, applyProfiler), applyExecutor);
	}

	protected Map<ResourceLocation, T> prepare(ResourceManager resourceManager, Profiler profiler)
	{
		Map<ResourceLocation, T> map = Maps.newHashMap();
		var i = this.startingPath.length() + 1;
		var resources = resourceManager.findResources(this.startingPath, (s) -> s.getPath().endsWith(fileSuffix));

		for (var resourceEntry : resources.entrySet())
		{
			var resourceId = resourceEntry.getKey();
			var resourcePath = resourceId.getPath();
			var resourceIdWithoutExt = new ResourceLocation(resourceId.getNamespace(), resourcePath.substring(i, resourcePath.length() - fileSuffixLength));

			var resource = resourceEntry.getValue();

			try (var inputStream = resource.getInputStream())
			{
				var element = readResource(resourceManager, profiler, inputStream);
				if (element != null)
				{
					var popped = map.put(resourceIdWithoutExt, element);
					if (popped != null)
						throw new IllegalStateException("Duplicate data file ignored with ID " + resourceIdWithoutExt);
				}
				else
					LOGGER.error("Couldn't load data file {} from {} as it's null or empty", resourceIdWithoutExt, resourceId);
			}
			catch (Exception ex)
			{
				LOGGER.error("Couldn't parse data file {} from {}", resourceIdWithoutExt, resourceId, ex);
			}
		}

		return map;
	}

	protected abstract void apply(Map<ResourceLocation, T> prepared, ResourceManager manager, Profiler profiler);
}

