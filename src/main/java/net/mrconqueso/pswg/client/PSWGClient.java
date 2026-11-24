package net.mrconqueso.pswg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.entity.ShipEntity;
import net.mrconqueso.pswg.nem.NemManager;
import net.mrconqueso.pswg.util.client.texture.remote.RemoteTextureProvider;
import net.mrconqueso.pswg.util.client.texture.stacked.StackedTextureProvider;
import net.mrconqueso.pswg.util.client.texture.tinted.TintedTextureProvider;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class PSWGClient
{
	public static final KeyMapping KEY_PRIMARY_ITEM_ACTION = new KeyMapping(Resources.keyBinding("primary_item_action"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, "key.category.pswg");
	public static final KeyMapping KEY_SECONDARY_ITEM_ACTION = new KeyMapping(Resources.keyBinding("secondary_item_action"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, "key.category.pswg");
	public static final KeyMapping KEY_SHIP_INPUT_MODE_OVERRIDE = new KeyMapping(Resources.keyBinding("ship_input_mode_override"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.category.pswg");
	public static final KeyMapping KEY_PATROL_POSTURE = new KeyMapping(Resources.keyBinding("toggle_patrol_posture"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.category.pswg");
	public static final KeyMapping KEY_SPECIES_SELECT = new KeyMapping(Resources.keyBinding("species_select"), InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.category.pswg");

	public static final ResourceLocation TEX_TRANSPARENT = Resources.id("textures/effect/transparent.png");
	public static final NemManager NEM_MANAGER = new NemManager(Resources.id("nem_manager"));

	public static RemoteTextureProvider remoteSkinTextureProvider;
	public static StackedTextureProvider stackedTextureProvider;
	public static TintedTextureProvider tintedTextureProvider;

	public static ZoomInstance blasterZoomInstance;
	public static ZoomOverlay blasterZoomOverlayDefault;
	public static ZoomOverlay blasterZoomOverlaySniper;

	public static boolean isShipClientControlled(ShipEntity shipEntity)
	{
		var minecraft = Minecraft.getInstance();
		if (minecraft.player == null)
			return false;

		return ShipEntity.getShip(minecraft.player) == shipEntity;
	}

	public static void getRightDebugText(List<String> strings)
	{
	}

	public static float getTickDelta()
	{
		var mc = Minecraft.getInstance();
		if (mc.isPaused())
			return mc.pausePartialTick; // 1.21.1 mapping for paused tick delta
		return mc.getTimer().getGameTimeDeltaPartialTick(true);
	}

	public static ResourceLocation tintTexture(ResourceLocation texture, int color)
	{
		var textureId = texture.getNamespace() + "/" + texture.getPath() + "/" + Integer.toHexString(color);
		return tintedTextureProvider.tint(textureId, texture, color);
	}
}