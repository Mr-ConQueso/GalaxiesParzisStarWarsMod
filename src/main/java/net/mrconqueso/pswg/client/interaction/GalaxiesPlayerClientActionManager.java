package net.mrconqueso.pswg.client.interaction;

import net.minecraft.client.player.LocalPlayer;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.networking.GalaxiesPlayerActionS2CPacket;

/**
 * Dispatches handlers for S2C player actions
 */
public final class GalaxiesPlayerClientActionManager
{
    /**
     * Invokes the handler within the specified subsystem based on the packet's action.
     * (Call this directly from your S2C packet's handle() method!)
     */
    public static void handlePlayerAction(GalaxiesPlayerActionS2CPacket packet, LocalPlayer player)
    {
        switch (packet.action())
        {
            case INVALID -> PSWG.LOGGER.warn("Received invalid player action packet!");
            case CONSUME_LEFT_ITEM -> GalaxiesEntityLeftClickClientManager.handleConsumeItem(player);
        }
    }
}