package dev.pswg.species.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import dev.pswg.Galaxies;
import dev.pswg.rendering.nem.NemModels;
import dev.pswg.species.Species;
import dev.pswg.species.Species.Appearance;
import dev.pswg.species.SpeciesItems;
import dev.pswg.species.SpeciesNetwork;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT)
public final class SpeciesClient
{
	private static final KeyMapping KEY = new KeyMapping("key.pswg.species_select", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.pswg.keybinds");
	private static final LinkedHashMap<Appearance, ResourceLocation> SKINS = new LinkedHashMap<>(32, 0.75F, true);
	private static final Map<ResourceLocation, PlayerModel<AbstractClientPlayer>> MODELS = new HashMap<>();
	private static Appearance preview;

	private SpeciesClient() {}

	@EventBusSubscriber(modid = Galaxies.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
	public static final class Registration
	{
		@SubscribeEvent
		public static void keys(RegisterKeyMappingsEvent event)
		{
			event.register(KEY);
			SpeciesNetwork.registerEditor((entity, skin) -> Minecraft.getInstance().setScreen(new SpeciesScreen(entity, skin)));
		}

		@SubscribeEvent
		public static void renderers(EntityRenderersEvent.RegisterRenderers event)
		{
			event.registerEntityRenderer(SpeciesItems.TYPE.get(), MannequinRenderer::new);
		}

		@SubscribeEvent
		public static void reload(RegisterClientReloadListenersEvent event)
		{
			event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) resources -> {
				var textures = Minecraft.getInstance().getTextureManager();
				SKINS.values().forEach(textures::release);
				SKINS.clear();
				MODELS.clear();
			});
		}
	}

	@SubscribeEvent
	public static void tick(ClientTickEvent.Post event)
	{
		var minecraft = Minecraft.getInstance();
		while (KEY.consumeClick())
			if (minecraft.screen == null && minecraft.player != null && !minecraft.player.isSpectator())
				minecraft.setScreen(new SpeciesScreen(minecraft.player.getId(), Species.appearance(minecraft.player)));
	}

	static void preview(Appearance appearance, Runnable render)
	{
		preview = appearance;
		try { render.run(); }
		finally { preview = null; }
	}

	public static Appearance appearance(AbstractClientPlayer player)
	{
		return preview == null ? Species.appearance(player) : preview;
	}

	static ResourceLocation modelId(Appearance appearance)
	{
		return Galaxies.id(appearance.profile().model().replace("{gender}", appearance.gender()));
	}

	public static PlayerModel<AbstractClientPlayer> model(AbstractClientPlayer player)
	{
		var appearance = appearance(player);
		if (appearance.profile() == null)
			return null;
		var model = MODELS.computeIfAbsent(modelId(appearance), id -> {
			var mesh = NemModels.INSTANCE.bake(id);
			return mesh == null ? null : new PlayerModel<>(mesh, false)
			{
				@Override
				public void setupAnim(AbstractClientPlayer actor, float swing, float amount, float age, float yaw, float pitch)
				{
					super.setupAnim(actor, swing, amount, age, yaw, pitch);
					animate(this, actor, appearance(actor), yaw, pitch);
				}
			};
		});
		return model;
	}

	static void animate(net.minecraft.client.model.HumanoidModel<?> model, net.minecraft.world.entity.LivingEntity actor,
			Appearance appearance, float yaw, float pitch)
	{
		if (appearance.profile() == null)
			return;
		var head = model.head;
		if (model.body.hasChild("chest"))
			model.body.getChild("chest").visible = appearance.female() && actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).isEmpty();
		if (head.hasChild("hair"))
			head.getChild("hair").visible = !dev.pswg.rendering.g3d.G3dWornArmor.hidesSkinOverlay(actor.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
		if (appearance.profile().options().containsKey("horns"))
			for (int i = 1; i <= 6; i++)
				if (head.hasChild("horns" + i))
					head.getChild("horns" + i).visible = appearance.value("horns").equals(Integer.toString(i));
		if (appearance.profile().options().containsKey("beard"))
			for (int i = 1; i <= 7; i++)
				if (head.hasChild("beard" + i))
					head.getChild("beard" + i).visible = appearance.value("beard").equals(Integer.toString(i));
		if (head.hasChild("TailBaseL") && head.hasChild("TailBaseR"))
		{
			float angle = pitch * Mth.DEG_TO_RAD;
			float squared = angle * angle, cubed = squared * angle, fourth = cubed * angle;
			float y = Mth.wrapDegrees(yaw) * Mth.DEG_TO_RAD;
			for (String side : new String[] { "L", "R" })
			{
				var base = head.getChild("TailBase" + side);
				var middle = base.getChild("TailMid" + side);
				var bottom = middle.getChild("TailLower" + side);
				base.xRot = (-2.34F * cubed + 11.05F * squared + 3.4F * angle + 7.06F) * Mth.DEG_TO_RAD;
				middle.xRot = (5.11F * fourth + 2.01F * cubed - 10.2F * squared - 26.38F * angle - 1.48F) * Mth.DEG_TO_RAD;
				bottom.xRot = (-3.15F * fourth + 2.57F * squared - 23.03F * angle - 2.93F) * Mth.DEG_TO_RAD;
				base.zRot = (side.equals("L") ? Math.max(0, y) : Math.min(0, y)) * (2 / 9F) + y / 9F;
			}
		}
	}

	public static ResourceLocation texture(Appearance appearance)
	{
		if (appearance.profile() == null)
			return null;
		var minecraft = Minecraft.getInstance();
		var cached = SKINS.get(appearance);
		if (cached != null)
			return cached;
		if (SKINS.size() >= 128)
			minecraft.getTextureManager().release(SKINS.pollFirstEntry().getValue());
		return SKINS.computeIfAbsent(appearance, selected -> {
			NativeImage skin = new NativeImage(150, 150, true);
			try
			{
				for (var layer : selected.profile().layers())
				{
					String value = layer.option() == null ? "" : selected.value(layer.option());
					if (value.equals("none"))
						continue;
					if (("humanoid_clothes_top".equals(layer.option()) || "humanoid_clothes_bottom".equals(layer.option()))
							&& !selected.value("humanoid_clothes_underlayer").equals("none"))
						continue;
					String path = layer.path().replace("{gender}", selected.gender()).replace("{value}", value);
					var id = Galaxies.id("textures/species/" + path + ".png");
					int tint = layer.tint() == null ? 0xffffff : Integer.parseInt(selected.value(layer.tint()), 16);
					try (var input = minecraft.getResourceManager().open(id); var image = NativeImage.read(input))
					{
						for (int y = 0; y < skin.getHeight(); y++)
							for (int x = 0; x < skin.getWidth(); x++)
							{
								int color = image.getPixelRGBA(x * image.getWidth() / skin.getWidth(), y * image.getHeight() / skin.getHeight());
								int alpha = color >>> 24;
								if (alpha == 0)
									continue;
								int red = (color & 255) * (tint >>> 16 & 255) / 255;
								int green = (color >>> 8 & 255) * (tint >>> 8 & 255) / 255;
								int blue = (color >>> 16 & 255) * (tint & 255) / 255;
								skin.blendPixel(x, y, alpha << 24 | blue << 16 | green << 8 | red);
							}
					}
				}
				return minecraft.getTextureManager().register("pswg_species", new DynamicTexture(skin));
			}
			catch (IOException | RuntimeException e)
			{
				skin.close();
				Galaxies.LOGGER.error("Could not compose species appearance {}", selected, e);
				return null;
			}
		});
	}
}
