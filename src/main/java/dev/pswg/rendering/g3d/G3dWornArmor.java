package dev.pswg.rendering.g3d;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.JsonOps;
import dev.pswg.Galaxies;
import dev.pswg.container.ArmorContent;
import dev.pswg.model.g3d.G3dFiles;
import dev.pswg.model.g3d.G3dModel;
import dev.pswg.model.g3d.G3dPose;
import dev.pswg.model.g3d.G3dResources;
import dev.pswg.model.g3d.G3dTextureBindings;
import dev.pswg.model.g3d.G3dTextureReference;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class G3dWornArmor
{
	private static Map<ResourceLocation, WornModel> models = Map.of();

	public static void reload(ResourceManager resources)
	{
		var replacement = new HashMap<ResourceLocation, WornModel>();
		for (var entry : G3dResources.MODEL_FILES.listMatchingResources(resources).entrySet())
		{
			var id = G3dResources.MODEL_FILES.fileToId(entry.getKey());
			if (!id.getPath().startsWith("armor/"))
				continue;
			try (var stream = entry.getValue().open())
			{
				var model = G3dFiles.readModel(stream);
				if (!model.rig().id().equals(id))
					throw new IOException("Model ID does not match its resource path");

				var sidecar = id.withPath(path -> "models/" + path + ".json");
				G3dTextureBindings bindings = G3dTextureBindings.EMPTY;
				var resource = resources.getResource(sidecar);
				if (resource.isPresent())
				{
					try (var reader = resource.get().openAsReader())
					{
						var metadata = JsonParser.parseReader(reader).getAsJsonObject();
						if (metadata.has("textures"))
							bindings = G3dTextureBindings.CODEC.parse(JsonOps.INSTANCE, metadata.get("textures")).getOrThrow();
					}
				}
				replacement.put(id, new WornModel(model, new G3dArmorPose(model.rig()), bindings));
			}
			catch (IOException | RuntimeException exception)
			{
				Galaxies.LOGGER.error("Could not load PSWG armor model {}", id, exception);
			}
		}
		models = Map.copyOf(replacement);
	}

	public static boolean hidesSkinOverlay(ItemStack stack)
	{
		var appearance = ArmorContent.appearance(BuiltInRegistries.ITEM.getKey(stack.getItem()));
		return appearance != null && appearance.hideOverlay() && models.containsKey(appearance.model());
	}

	public static boolean render(PoseStack stack, MultiBufferSource buffers, LivingEntity wearer, EquipmentSlot slot,
	                             int packedLight, HumanoidModel<?> humanoid)
	{
		var item = wearer.getItemBySlot(slot);
		if (!(item.getItem() instanceof ArmorItem armor) || armor.getEquipmentSlot() != slot)
			return false;
		var appearance = ArmorContent.appearance(BuiltInRegistries.ITEM.getKey(armor));
		if (appearance == null)
			return false;
		var model = models.get(appearance.model());
		if (model == null)
			return false;

		boolean slim = wearer instanceof AbstractClientPlayer player && player.getSkin().model() == PlayerSkin.Model.SLIM;
		var pose = model.pose().capture(humanoid, slot, slim);
		var textures = appearance.baseTexture() == null ? model.textures() : model.textures().with("base", appearance.baseTexture());
		for (var mesh : model.geometry().meshes())
		{
			if (G3dPose.isCollapsed(pose[mesh.node()]))
				continue;

			var material = model.geometry().materials().get(mesh.material());
			var texture = resolve(material.texture(), textures);
			if (texture == null)
				continue;

			var layer = material.layers().entity().getPath();
			var renderType = switch (layer)
			{
				case "entity/translucent" -> RenderType.entityTranslucent(texture);
				case "entity/translucent_emissive" -> RenderType.entityTranslucentEmissive(texture);
				default -> RenderType.armorCutoutNoCull(texture);
			};
			int emission = material.lightEmission();
			int light = LightTexture.pack(Math.max(LightTexture.block(packedLight), emission),
					Math.max(LightTexture.sky(packedLight), emission));
			int color = material.tintIndex() < 0 ? -1 : DyedItemColor.getOrDefault(item, -1) | 0xFF000000;
			var buffer = ItemRenderer.getArmorFoilBuffer(buffers, renderType, item.hasFoil());
			stack.pushPose();
			try
			{
				stack.mulPose(new Matrix4f(pose[mesh.node()]));
				emit(mesh, stack.last(), buffer, light, color);
			}
			finally
			{
				stack.popPose();
			}
		}
		return true;
	}

	private static ResourceLocation resolve(G3dTextureReference reference, G3dTextureBindings bindings)
	{
		for (int i = 0; reference != null && i <= bindings.values().size(); i++)
		{
			if (!reference.isSlot())
			{
				var resource = reference.resource();
				return resource.getPath().startsWith("textures/") ? resource : resource.withPath(path -> "textures/" + path + ".png");
			}
			reference = bindings.values().get(reference.slot());
		}
		return null;
	}

	private static void emit(G3dModel.Mesh mesh, PoseStack.Pose pose, VertexConsumer buffer, int light, int color)
	{
		for (int triangle = 0; triangle < mesh.indexCount(); triangle += 3)
		{
			for (int corner = 0; corner < 4; corner++)
			{
				int vertex = mesh.index(triangle + Math.min(corner, 2));
				buffer.addVertex(pose, mesh.component(vertex, 0), mesh.component(vertex, 1), mesh.component(vertex, 2))
						.setColor(color).setUv(mesh.component(vertex, 6), mesh.component(vertex, 7))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
						.setNormal(pose, mesh.component(vertex, 3), mesh.component(vertex, 4), mesh.component(vertex, 5));
			}
		}
	}

	private record WornModel(G3dModel geometry, G3dArmorPose pose, G3dTextureBindings textures)
	{
	}

	private G3dWornArmor()
	{
	}
}
