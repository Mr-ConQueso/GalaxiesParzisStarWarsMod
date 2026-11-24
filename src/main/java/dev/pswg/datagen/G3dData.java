package dev.pswg.datagen;

import com.google.common.hash.Hashing;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.pswg.model.g3d.G3dCompiler;
import dev.pswg.model.g3d.G3dFiles;
import dev.pswg.model.g3d.G3dSource;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;

public record G3dData(Path output, Collection<Path> inputs, boolean visuals, boolean rigs) implements DataProvider
{
	private static final String SOURCE_EXTENSION = ".jg3d";

	public static void gather(GatherDataEvent event)
	{
		var generator = event.getGenerator();
		generator.addProvider(event.includeClient() || event.includeServer(),
				new G3dData(generator.getPackOutput().getOutputFolder(), event.getInputs(), event.includeClient(), event.includeServer()));
	}

	@Override
	public CompletableFuture<?> run(CachedOutput cache)
	{
		try
		{
			var compiled = new HashSet<ResourceLocation>();
			for (var root : inputs)
			{
				var assets = root.resolve("assets");
				if (!Files.isDirectory(assets))
					continue;
				try (var namespaces = Files.list(assets))
				{
					for (var namespace : namespaces.filter(Files::isDirectory).sorted().toList())
					{
						var sourceDir = namespace.resolve("g3d/source");
						if (!Files.isDirectory(sourceDir))
							continue;

						try (var sources = Files.walk(sourceDir))
						{
							for (var file : sources.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(SOURCE_EXTENSION)).sorted().toList())
							{
								String relative = sourceDir.relativize(file).toString().replace('\\', '/');
								var id = ResourceLocation.fromNamespaceAndPath(namespace.getFileName().toString(),
										relative.substring(0, relative.length() - SOURCE_EXTENSION.length()));
								if (!compiled.add(id))
									throw new IOException("Duplicate G3D authoring source " + id);
								compile(cache, file, id);
							}
						}
					}
				}
			}
			return CompletableFuture.completedFuture(null);
		}
		catch (IOException | RuntimeException exception)
		{
			return CompletableFuture.failedFuture(exception);
		}
	}

	private void compile(CachedOutput cache, Path file, ResourceLocation id) throws IOException
	{
		try (var reader = Files.newBufferedReader(file))
		{
			var model = G3dCompiler.compile(id, G3dSource.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseReader(reader))
					.getOrThrow(error -> new IOException(file + ": " + error)));
			if (visuals)
				write(cache, output.resolve("assets/" + id.getNamespace() + "/models/" + id.getPath() + ".g3d"), G3dFiles.write(model, true));
			if (rigs)
				write(cache, output.resolve("data/" + id.getNamespace() + "/g3d/rigs/" + id.getPath() + ".g3d"), G3dFiles.write(model, false));
		}
	}

	private static void write(CachedOutput cache, Path destination, byte[] data) throws IOException
	{
		cache.writeIfNeeded(destination, data, Hashing.sha1().hashBytes(data));
	}

	@Override
	public String getName()
	{
		return "Galaxies G3D models and rigs";
	}
}
