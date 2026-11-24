package dev.pswg.model.g3d;

import dev.pswg.Galaxies;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Common resource discovery and the dedicated server's rig cache.
 */
public final class G3dResources implements PreparableReloadListener
{
	/**
	 * Server datapack projection path, separate from client assets.
	 */
	public static final FileToIdConverter RIG_FILES = new FileToIdConverter("g3d/rigs", ".g3d");

	/**
	 * Client geometry path follows vanilla model identifiers.
	 */
	public static final FileToIdConverter MODEL_FILES = new FileToIdConverter("models", ".g3d");

	/**
	 * Datagen-only source path.
	 */
	public static final FileToIdConverter SOURCE_FILES = new FileToIdConverter("g3d/source", ".jg3d");

	/**
	 * Shared server loader. Integrated servers use the same datapack path.
	 */
	public static final G3dResources SERVER = new G3dResources();

	/**
	 * One complete reload snapshot, published only during apply.
	 */
	private volatile Map<ResourceLocation, G3dRig> _rigs = Map.of();

	/**
	 * Prevents additional global server caches.
	 */
	private G3dResources()
	{
	}

	/**
	 * Finds a gameplay rig loaded from the current datapacks.
	 */
	public Optional<G3dRig> get(ResourceLocation id)
	{
		return Optional.ofNullable(_rigs.get(id));
	}

	/**
	 * Loads client files for a preparable model-loading plugin.
	 */
	public static Map<ResourceLocation, G3dModel> loadModels(ResourceManager manager)
	{
		var result = new HashMap<ResourceLocation, G3dModel>();
		for (var entry : MODEL_FILES.listMatchingResources(manager).entrySet())
		{
			var id = MODEL_FILES.fileToId(entry.getKey());
			try (var input = entry.getValue().open())
			{
				var model = G3dFiles.readModel(input);
				if (!model.rig().id().equals(id))
					throw new IOException("Model id does not match its resource path");

				result.put(id, model);
			}
			catch (Exception exception)
			{
				Galaxies.LOGGER.error("Could not load G3D model {}", entry.getKey(), exception);
			}
		}

		return Map.copyOf(result);
	}

	/**
	 * Prepares away from the game thread and swaps the cache atomically on apply.
	 */
	@Override
	public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, ProfilerFiller prepareProfiler,
	                                     ProfilerFiller applyProfiler, Executor prepare, Executor apply)
	{
		return CompletableFuture.supplyAsync(() -> loadRigs(manager), prepare)
		                        .thenCompose(barrier::wait)
		                        .thenAcceptAsync(rigs -> _rigs = rigs, apply);
	}

	/**
	 * Reads only the common section, never client geometry or texture classes.
	 */
	private static Map<ResourceLocation, G3dRig> loadRigs(ResourceManager manager)
	{
		var result = new HashMap<ResourceLocation, G3dRig>();
		for (var entry : RIG_FILES.listMatchingResources(manager).entrySet())
		{
			var id = RIG_FILES.fileToId(entry.getKey());
			try (var input = entry.getValue().open())
			{
				var rig = G3dFiles.readRig(input);
				if (!rig.id().equals(id))
					throw new IOException("Rig id does not match its resource path");

				result.put(id, rig);
			}
			catch (Exception exception)
			{
				Galaxies.LOGGER.error("Could not load G3D rig {}", entry.getKey(), exception);
			}
		}

		return Map.copyOf(result);
	}
}
