package dev.pswg.blasters;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.pswg.Galaxies;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.HashMap;
import java.util.Map;

public final class BlasterDefinitions extends SimpleJsonResourceReloadListener
{
	public static final BlasterDefinitions SERVER = new BlasterDefinitions();
	private static volatile Map<ResourceLocation, BlasterDefinition> client = Map.of();

	private volatile Map<ResourceLocation, BlasterDefinition> definitions = Map.of();

	public static Map<ResourceLocation, BlasterDefinition> client()
	{
		return client;
	}

	public static void updateClient(Map<ResourceLocation, BlasterDefinition> definitions)
	{
		client = Map.copyOf(definitions);
	}

	private BlasterDefinitions()
	{
		super(new Gson(), "blasters");
	}

	public Map<ResourceLocation, BlasterDefinition> definitions()
	{
		return definitions;
	}

	@Override
	protected void apply(Map<ResourceLocation, JsonElement> entries, ResourceManager resources, ProfilerFiller profiler)
	{
		var loaded = new HashMap<ResourceLocation, BlasterDefinition>();
		entries.forEach((id, json) -> {
			var result = BlasterDefinition.CODEC.parse(JsonOps.INSTANCE, json);
			result.error().ifPresent(error -> Galaxies.LOGGER.error("Could not decode blaster definition {}: {}", id, error.message()));
			result.result().ifPresent(definition -> loaded.put(id, definition));
		});
		definitions = Map.copyOf(loaded);
	}
}
