package dev.pswg.rendering.ptex;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pswg.Galaxies;
import dev.pswg.utility.ColorUtil;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.FastColor;
import net.neoforged.neoforge.client.event.RegisterSpriteSourceTypesEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class PtexSpriteSource implements SpriteSource
{
	private static final int MAX_DEFINITION_BYTES = 1024 * 1024;
	private static final FileToIdConverter FILES = FileToIdConverter.json("ptex");
	private static final SpriteSourceType TYPE = new SpriteSourceType(MapCodec.unit(new PtexSpriteSource()));

	public static void register(RegisterSpriteSourceTypesEvent event)
	{
		event.register(Galaxies.id("ptex"), TYPE);
	}

	public static ResourceLocation spriteId(ResourceLocation definition)
	{
		return definition.withPrefix("ptex/");
	}

	public static ResourceLocation definitionFile(ResourceLocation definition)
	{
		return FILES.idToFile(definition);
	}

	@Override
	public SpriteSourceType type()
	{
		return TYPE;
	}

	@Override
	public void run(ResourceManager manager, Output output)
	{
		for (var entry : FILES.listMatchingResources(manager).entrySet())
		{
			var id = FILES.fileToId(entry.getKey());
			try (var input = entry.getValue().open())
			{
				var bytes = input.readNBytes(MAX_DEFINITION_BYTES + 1);
				if (bytes.length > MAX_DEFINITION_BYTES)
					throw new IOException("Ptex definition exceeds the size limit");

				var document = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8));
				var definition = Definition.CODEC.parse(JsonOps.INSTANCE, document).getOrThrow(IOException::new);
				if (!definition.atlas())
					continue;

				var sprite = spriteId(id);
				if (definition.graph() instanceof Source source)
					output.add(sprite, manager.getResourceOrThrow(source.texture()));
				else
					output.add(sprite, loader -> generateSprite(manager, sprite, definition.graph()));
			}
			catch (IOException | RuntimeException exception)
			{
				Galaxies.LOGGER.error("Could not load Ptex sprite {}", id, exception);
			}
		}
	}

	private static SpriteContents generateSprite(ResourceManager manager, ResourceLocation sprite, Texture texture)
	{
		try
		{
			var image = texture.open(manager);
			return new SpriteContents(sprite, new FrameSize(image.getWidth(), image.getHeight()), image, ResourceMetadata.EMPTY);
		}
		catch (IOException | RuntimeException exception)
		{
			Galaxies.LOGGER.error("Could not generate {}", sprite, exception);
			return null;
		}
	}

	private record Definition(Texture graph, boolean atlas)
	{
		static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Texture.CODEC.fieldOf("graph").forGetter(Definition::graph),
				Codec.BOOL.optionalFieldOf("atlas", true).forGetter(Definition::atlas)
		).apply(instance, Definition::new));
	}

	private sealed interface Texture permits Source, Tint, Composite
	{
		Codec<Texture> CODEC = Codec.recursive("Ptex graph", graph -> {
			MapCodec<Source> source = ResourceLocation.CODEC.fieldOf("texture").xmap(Source::new, Source::texture);
			MapCodec<Tint> tint = RecordCodecBuilder.mapCodec(instance -> instance.group(
					ExtraCodecs.ARGB_COLOR_CODEC.fieldOf("color").forGetter(Tint::color),
					graph.fieldOf("input").forGetter(Tint::input)
			).apply(instance, Tint::new));
			MapCodec<Composite> composite = graph.listOf(1, 256).fieldOf("layers").xmap(Composite::new, Composite::layers);
			var codecs = Map.of(Galaxies.id("source"), source, Galaxies.id("tint"), tint, Galaxies.id("composite"), composite);
			return ResourceLocation.CODEC.partialDispatch("type",
					texture -> DataResult.success(texture.typeId()),
					id -> {
						MapCodec<? extends Texture> codec = codecs.get(id);
						return codec != null ? DataResult.success(codec)
								: DataResult.error(() -> "Unknown Ptex texture type " + id);
					});
		});

		ResourceLocation typeId();

		NativeImage open(ResourceManager manager) throws IOException;
	}

	private record Source(ResourceLocation texture) implements Texture
	{
		@Override
		public ResourceLocation typeId()
		{
			return Galaxies.id("source");
		}

		@Override
		public NativeImage open(ResourceManager manager) throws IOException
		{
			try (var stream = manager.getResourceOrThrow(texture).open())
			{
				return NativeImage.read(stream);
			}
		}
	}

	private record Tint(int color, Texture input) implements Texture
	{
		@Override
		public ResourceLocation typeId()
		{
			return Galaxies.id("tint");
		}

		@Override
		public NativeImage open(ResourceManager manager) throws IOException
		{
			var image = input.open(manager);
			int tint = FastColor.ABGR32.fromArgb32(color);
			for (int y = 0; y < image.getHeight(); y++)
				for (int x = 0; x < image.getWidth(); x++)
					image.setPixelRGBA(x, y, ColorUtil.multiplyTint(image.getPixelRGBA(x, y), tint));
			return image;
		}
	}

	private record Composite(List<Texture> layers) implements Texture
	{
		@Override
		public ResourceLocation typeId()
		{
			return Galaxies.id("composite");
		}

		@Override
		public NativeImage open(ResourceManager manager) throws IOException
		{
			var composite = layers.getFirst().open(manager);
			try
			{
				for (var layer : layers.subList(1, layers.size()))
				{
					try (var top = layer.open(manager))
					{
						if (top.getWidth() != composite.getWidth() || top.getHeight() != composite.getHeight())
							throw new IOException("Ptex composite layers must have the same dimensions");
						for (int y = 0; y < composite.getHeight(); y++)
							for (int x = 0; x < composite.getWidth(); x++)
								composite.setPixelRGBA(x, y, ColorUtil.alphaComposite(composite.getPixelRGBA(x, y), top.getPixelRGBA(x, y)));
					}
				}
				return composite;
			}
			catch (IOException | RuntimeException exception)
			{
				composite.close();
				throw exception;
			}
		}
	}
}
