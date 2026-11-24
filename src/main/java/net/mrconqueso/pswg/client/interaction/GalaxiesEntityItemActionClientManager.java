package net.mrconqueso.pswg.client.interaction;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.mrconqueso.pswg.interaction.ClientPlayerAction;
import net.mrconqueso.pswg.item.IPrimaryActionHandler;
import net.mrconqueso.pswg.networking.GalaxiesPlayerActionC2SPacket;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Handles player non-primary item interactions on the client
 */
public final class GalaxiesEntityItemActionClientManager
{
    /**
     * Invokes the primary item action for the given player's held item
     */
    public static void handlePrimaryItemAction()
    {
        var client = Minecraft.getInstance();
        var interactionManager = client.gameMode;
        var player = client.player;
        if (interactionManager == null || player == null)
            return;

        CarriedItemSyncHelper.ensureHasSentCarriedItem(player);
        var hand = player.getUsedItemHand();
        var activeStack = player.getItemInHand(hand);
        if (!(activeStack.getItem() instanceof IPrimaryActionHandler item))
            return;

        // NeoForge: Replaced ClientPlayNetworking with PacketDistributor
        PacketDistributor.sendToServer(new GalaxiesPlayerActionC2SPacket(ClientPlayerAction.PRIMARY_ITEM_ACTION));

        ItemStack resultStack = item.invokePrimaryAction(activeStack, player.level(), player);
        if (resultStack != activeStack)
        {
            player.setItemInHand(hand, resultStack);
        }
    }
}