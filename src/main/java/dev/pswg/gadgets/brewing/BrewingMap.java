package dev.pswg.gadgets.brewing;

import dev.pswg.Galaxies;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

public final class BrewingMap implements ResourceManagerReloadListener
{
	private static final int CELLS = 32;
	public static final int SIZE = CELLS * 16;
	private static final String DIRECTORY = "brewing_map";
	private static final Cell EMPTY = new Cell(Kind.EMPTY, null);
	private static volatile Cell[][] cells = new Cell[CELLS][CELLS];

	@Override
	public void onResourceManagerReload(ResourceManager manager)
	{
		Cell[][] loaded = null;
		var resources = manager.listResources(DIRECTORY, id -> true);
		for (var entry : resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList())
		{
			try
			{
				loaded = read(entry.getValue());
			}
			catch (IOException | UncheckedIOException | IllegalArgumentException e)
			{
				Galaxies.LOGGER.error("Could not load brewing map {}", entry.getKey(), e);
			}
		}
		if (loaded != null)
			cells = loaded;
	}

	private static Cell[][] read(Resource resource) throws IOException
	{
		var next = new Cell[CELLS][CELLS];
		try (var reader = resource.openAsReader())
		{
			var lines = reader.lines().toList();
			if (lines.size() != CELLS)
				throw new IllegalArgumentException("Expected " + CELLS + " rows");
			for (int y = 0; y < CELLS; y++)
			{
				var row = lines.get(y).replace(" ", "").split(",", -1);
				// The source map has two rows with an extra boundary cell.
				if (row.length < CELLS || row.length > CELLS + 1)
					throw new IllegalArgumentException("Invalid number of columns on row " + (y + 1));
				for (int x = 0; x < CELLS; x++)
					next[y][x] = parse(row[x]);
			}
		}
		return next;
	}

	private static Cell parse(String token)
	{
		if (token.equals("b"))
			return EMPTY;
		if (token.equals("c"))
			return new Cell(Kind.CORNER, null);
		int durationSeparator = token.lastIndexOf('.');
		int amplifierSeparator = token.lastIndexOf('.', durationSeparator - 1);
		if (amplifierSeparator <= 2 || token.charAt(1) != '.' || (token.charAt(0) != 'd' && token.charAt(0) != 'e'))
			throw new IllegalArgumentException("Invalid brewing cell " + token);
		var id = ResourceLocation.parse(token.substring(2, amplifierSeparator));
		var effect = BuiltInRegistries.MOB_EFFECT.getHolder(id)
				.orElseThrow(() -> new IllegalArgumentException("Unknown brewing effect " + id));
		int amplifier = Integer.parseInt(token.substring(amplifierSeparator + 1, durationSeparator)) - 1;
		int duration = Integer.parseInt(token.substring(durationSeparator + 1));
		if (amplifier < 0 || amplifier > 255 || duration <= 0)
			throw new IllegalArgumentException("Invalid brewing effect " + token);
		return new Cell(token.charAt(0) == 'd' ? Kind.DANGER : Kind.EFFECT,
				new MobEffectInstance(effect, duration, amplifier, false, false, true));
	}

	public static Cell at(float x, float y)
	{
		var cell = cells[Mth.clamp((int) y / 16, 0, CELLS - 1)][Mth.clamp((int) x / 16, 0, CELLS - 1)];
		return cell == null ? EMPTY : cell;
	}

	public static void registerReload(AddReloadListenerEvent event)
	{
		event.addListener(new BrewingMap());
	}

	public enum Kind { EMPTY, CORNER, EFFECT, DANGER }

	public record Cell(Kind kind, MobEffectInstance effect) {}
}
