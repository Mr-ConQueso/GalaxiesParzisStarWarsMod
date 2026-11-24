package net.mrconqueso.pswg.interaction;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.mrconqueso.pswg.item.IPrimaryActionHandler;

/**
 * Handles player non-primary item interactions
 */
public final class GalaxiesEntityItemActionManager
{
    /**
     * Invokes the primary item action for the given player's held item
     */
    public static void handlePrimaryItemAction(ServerPlayer player)
    {
        player.resetLastActionTime();

        var hand = player.getUsedItemHand();
        var activeStack = player.getItemInHand(hand);

        if (!(activeStack.getItem() instanceof IPrimaryActionHandler item))
            return;

        ItemStack resultStack = item.invokePrimaryAction(activeStack, player.level(), player);
        if (resultStack != activeStack)
        {
            player.setItemInHand(hand, resultStack);
        }
    }
}