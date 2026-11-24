package net.mrconqueso.pswg.client.blaster;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.Util;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.client.PSWGClient;
import net.mrconqueso.pswg.data.blaster.BlasterAttachmentFunction;
import net.mrconqueso.pswg.data.blaster.BlasterDescriptor;
import net.mrconqueso.pswg.data.blaster.BlasterData;
import net.mrconqueso.pswg.item.custom.BlasterItem;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class BlasterZoomHandler
{
	private static final HashMap<BlasterAttachmentFunction, ZoomOverlay> ZOOM_OVERLAYS = Util.make(new HashMap<>(), (h) -> {
		h.put(BlasterAttachmentFunction.DEFAULT_SCOPE, PSWGClient.blasterZoomOverlayDefault);
		h.put(BlasterAttachmentFunction.SNIPER_SCOPE, PSWGClient.blasterZoomOverlaySniper);
	});

	public static void tick(Minecraft mc)
	{
		if (mc.player == null)
			return;

		var stack = mc.player.getMainHandItem();

		var blasterZoomInstance = PSWGClient.blasterZoomInstance;

		// Note: Adapt Resources.CONFIG call to your NeoForge config implementation
		var forceFirstPersonAds = Resources.CONFIG.get().view.forceFirstPersonAds;
		var isFirstPerson = mc.options.getCameraType().isFirstPerson();

		if (stack.getItem() instanceof BlasterItem b)
		{
			// Requires NBT->Data Component migration in BlasterTag (see below)
			var bt = new BlasterData(stack);
			var bd = BlasterItem.getBlasterDescriptor(stack);

			if (bt.isAimingDownSights && !isFirstPerson && forceFirstPersonAds)
			{
				mc.options.setCameraType(CameraType.FIRST_PERSON);
				isFirstPerson = true;
			}

			blasterZoomInstance.setZoom(bt.isAimingDownSights && isFirstPerson);
			blasterZoomInstance.setZoomDivisor(b.getFovMultiplier(stack, mc.level, mc.player));

			ZoomOverlay overlay = null;

			if (isFirstPerson)
				overlay = getZoomOverlay(bd, bt.attachmentBitmask);

			blasterZoomInstance.setZoomOverlay(overlay);
		}
		else
			blasterZoomInstance.setZoom(false);
	}

	@Nullable
	public static ZoomOverlay getZoomOverlay(BlasterDescriptor bd, int attachmentBitmask)
	{
		return bd.mapWithAttachment(attachmentBitmask, ZOOM_OVERLAYS).orElse(null);
	}
}