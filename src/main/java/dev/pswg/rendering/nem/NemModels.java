package dev.pswg.rendering.nem;

import dev.pswg.Galaxies;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NemModels implements ResourceManagerReloadListener
{
	public static final NemModels INSTANCE = new NemModels();
	private static final FileToIdConverter FILES = new FileToIdConverter("models", ".nem");
	private static final List<String> BIPED_PARTS = List.of("head", "hat", "body", "right_arm", "left_arm", "right_leg", "left_leg",
			"jacket", "left_sleeve", "right_sleeve", "left_pants", "right_pants", "cloak", "ear");
	private int version;
	private volatile Map<ResourceLocation, LayerDefinition> layers = Map.of();

	public int version() { return version; }

	public ModelPart bake(ResourceLocation id)
	{
		var layer = layers.get(id);
		return layer == null ? null : layer.bakeRoot();
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager)
	{
		var raw = new HashMap<ResourceLocation, CompoundTag>();
		for (var entry : FILES.listMatchingResources(manager).entrySet())
		{
			try (var input = entry.getValue().open(); var data = new DataInputStream(input))
			{
				raw.put(FILES.fileToId(entry.getKey()), NbtIo.read(data, NbtAccounter.create(8 * 1024 * 1024)));
			}
			catch (IOException | RuntimeException e)
			{
				Galaxies.LOGGER.error("Could not load NEM model {}", entry.getKey(), e);
			}
		}
		var output = new HashMap<ResourceLocation, LayerDefinition>();
		for (var entry : raw.entrySet())
		{
			try
			{
				var file = entry.getValue();
				var parts = file.getCompound("parts").copy();
				var seen = new java.util.HashSet<ResourceLocation>();
				seen.add(entry.getKey());
				while (!file.getString("base").isEmpty())
				{
					var id = ResourceLocation.parse(file.getString("base"));
					file = raw.get(id);
					if (file == null || !seen.add(id))
						throw new IllegalArgumentException("Missing or circular base " + id);
					var baseParts = file.getCompound("parts");
					for (String part : baseParts.getAllKeys())
						if (!parts.contains(part))
							parts.put(part, baseParts.getCompound(part));
				}
				if (entry.getValue().getBoolean("expand_biped") || entry.getKey().getPath().startsWith("species/"))
					for (String name : BIPED_PARTS)
						if (!parts.contains(name))
							parts.put(name, new CompoundTag());
				var mesh = new MeshDefinition();
				readParts(mesh.getRoot(), parts);
				var tex = entry.getValue().getCompound("tex");
				output.put(entry.getKey(), LayerDefinition.create(mesh, Math.max(1, tex.getInt("w")), Math.max(1, tex.getInt("h"))));
			}
			catch (RuntimeException e)
			{
				Galaxies.LOGGER.error("Could not bake NEM model {}", entry.getKey(), e);
			}
		}
		layers = Map.copyOf(output);
		version++;
	}

	private static void readParts(PartDefinition parent, CompoundTag compound)
	{
		for (String key : compound.getAllKeys())
		{
			var part = compound.getCompound(key);
			var tex = part.getCompound("tex");
			var pos = part.getCompound("pos");
			var rot = part.getCompound("rot");
			var cubes = CubeListBuilder.create();
			for (var tag : part.getList("cuboids", CompoundTag.TAG_COMPOUND))
			{
				var cube = (CompoundTag) tag;
				var origin = cube.getCompound("pos");
				var size = cube.getCompound("size");
				var expand = cube.getCompound("expand");
				var uv = cube.getCompound("tex");
				cubes.texOffs(tex.getInt("u") + uv.getInt("u"), tex.getInt("v") + uv.getInt("v"))
						.mirror(tex.getBoolean("mirrored") ^ uv.getBoolean("mirrored"))
						.addBox(origin.getFloat("x"), origin.getFloat("y"), origin.getFloat("z"),
								size.getInt("x"), size.getInt("y"), size.getInt("z"),
								new CubeDeformation(expand.getFloat("x"), expand.getFloat("y"), expand.getFloat("z")));
			}
			var child = parent.addOrReplaceChild(key, cubes, PartPose.offsetAndRotation(pos.getFloat("x"), pos.getFloat("y"), pos.getFloat("z"),
					rot.getFloat("pitch"), rot.getFloat("yaw"), rot.getFloat("roll")));
			readParts(child, part.getCompound("children"));
		}
	}
}
