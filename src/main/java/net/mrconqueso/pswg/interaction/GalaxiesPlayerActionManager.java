package net.mrconqueso.pswg.interaction;

import net.minecraft.server.level.ServerPlayer;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.networking.GalaxiesPlayerActionC2SPacket;

/**
 * Dispatches handlers for C2S player actions
 */
public final class GalaxiesPlayerActionManager
{
    /**
     * Invokes the handler within the specified subsystem based on the packet's action
     */
    public static void handlePlayerAction(GalaxiesPlayerActionC2SPacket packet, ServerPlayer player)
    {
        switch (packet.action())
        {
            case INVALID -> PSWG.LOGGER.warn("Received invalid player action packet!");
            case RELEASE_USE_LEFT_ITEM -> GalaxiesEntityLeftClickManager.handleReleaseUseItem(player);
            case PRIMARY_ITEM_ACTION -> GalaxiesEntityItemActionManager.handlePrimaryItemAction(player);
        }
    }
}