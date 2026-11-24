package net.mrconqueso.pswg.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.player.Player;
import net.mrconqueso.pswg.util.client.CameraUtil;
import net.mrconqueso.pswg.util.client.PositionNormal3f;
import org.joml.Vector3f;

import java.util.HashMap;

public class PlayerSocket {
	private static final HashMap<String, PositionNormal3f> SOCKETS = new HashMap<>();

	private static String getSocketId(Player player, String socketName) {
		return "%s/%s".formatted(player.getStringUUID(), socketName);
	}

	public static void save(Player player, String socketName, PoseStack poseStack, Vector3f normal) {
		SOCKETS.put(getSocketId(player, socketName), CameraUtil.unproject(poseStack, normal));
	}

	public static PositionNormal3f getSocket(Player player, String socketName) {
		return SOCKETS.get(getSocketId(player, socketName));
	}
}