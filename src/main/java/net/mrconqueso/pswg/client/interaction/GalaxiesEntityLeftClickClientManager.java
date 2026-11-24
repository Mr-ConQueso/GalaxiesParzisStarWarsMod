package net.mrconqueso.pswg.client.interaction;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.mrconqueso.pswg.interaction.ClientPlayerAction;
import net.mrconqueso.pswg.interaction.GalaxiesEntityLeftClickManager;
import net.mrconqueso.pswg.interaction.ILeftClickUsable;
import net.mrconqueso.pswg.interaction.ILeftClickingEntity;
import net.mrconqueso.pswg.networking.GalaxiesPlayerActionC2SPacket;
import net.mrconqueso.pswg.networking.PlayerInteractItemLeftC2SPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.mrconqueso.pswg.PSWG;

import java.util.Objects;

/**
 * Handles left-use item interactions on the client
 */
@EventBusSubscriber(modid = PSWG.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class GalaxiesEntityLeftClickClientManager
{
    private static int itemUseCooldownLeft;
    private static boolean repeatEvent;

    /**
     * Handles per-tick left-use item actions automatically via the EventBusSubscriber
     */
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event)
    {
        if (itemUseCooldownLeft > 0)
        {
            itemUseCooldownLeft--;
        }
    }

//    /**
//     * NeoForge automatically calls this at the end of every client tick.
//     * We use this to trigger your custom input handler!
//     */
//    @SubscribeEvent
//    public static void onClientTickPost(ClientTickEvent.Post event)
//    {
//        Minecraft client = Minecraft.getInstance();
//        if (client.player != null && client.level != null)
//        {
//            handleInputEvents(client);
//        }
//    }

    /**
     * Emulates the standard use-item keybind handler,
     * except using the attack key for left-click interactions
     */
    public static void handleInputEvents(Minecraft client)
    {
        assert client.player != null;

        var handItem = client.player.getItemInHand(client.player.getUsedItemHand());
        var isHoldingLeftClickableItem = handItem.getItem() instanceof ILeftClickUsable;

        if (!isHoldingLeftClickableItem || !(client.player instanceof ILeftClickingEntity leftClickingEntity))
            return;

        if (leftClickingEntity.pswg$isLeftUsingItem())
        {
            if (!client.options.keyAttack.isDown())
                stopUsingItemLeft(client.player);
        }
        else
        {
            var startedUsing = false;

            while (client.options.keyAttack.consumeClick())
            {
                doItemUseLeft(client);
                startedUsing = true;
            }

            if (startedUsing)
                repeatEvent = true;
        }

        if (!client.options.keyAttack.isDown())
        {
            itemUseCooldownLeft = 0;
            repeatEvent = false;
        }

        if (client.options.keyAttack.isDown() && itemUseCooldownLeft == 0 && !leftClickingEntity.pswg$isLeftUsingItem())
        {
            doItemUseLeft(client);
        }
    }

    /**
     * Emulates the MultiPlayerGameMode releaseUsingItem functionality for left-use items
     */
    private static void stopUsingItemLeft(LocalPlayer player)
    {
        CarriedItemSyncHelper.ensureHasSentCarriedItem(player);

        PacketDistributor.sendToServer(new GalaxiesPlayerActionC2SPacket(ClientPlayerAction.RELEASE_USE_LEFT_ITEM));

        if (!(player instanceof ILeftClickingEntity leftClickingEntity))
            return;

        leftClickingEntity.pswg$stopLeftUsingItem();
    }

    /**
     * Emulates the standard startUseItem functionality for left-use items
     */
    private static void doItemUseLeft(Minecraft client)
    {
        assert client.gameMode != null;
        assert client.player != null;
        assert client.level != null;

        if (client.gameMode.isDestroying())
            return;

        itemUseCooldownLeft = 4;

        for (var hand : InteractionHand.values())
        {
            var stack = client.player.getItemInHand(hand);

            if (!stack.isItemEnabled(client.level.enabledFeatures()))
                return;

            if (stack.isEmpty())
                continue;

            var result = interactItemLeft(client, client.gameMode, client.player, hand);
            if (!result.consumesAction())
                continue;

            // Natively swing the player's arm if the interaction succeeded
            client.player.swing(hand);
            return;
        }
    }

    /**
     * Emulates the MultiPlayerGameMode useItem functionality for left-use items
     */
    private static InteractionResult interactItemLeft(Minecraft client, MultiPlayerGameMode interactionManager, LocalPlayer player, InteractionHand hand)
    {
        if (interactionManager.getPlayerMode() == GameType.SPECTATOR)
        {
            return InteractionResult.PASS;
        }
        else
        {
            CarriedItemSyncHelper.ensureHasSentCarriedItem(player);

            var packet = new PlayerInteractItemLeftC2SPacket(hand, player.getYRot(), player.getXRot(), repeatEvent);
            var itemStack = player.getItemInHand(hand);

            var actionResult = GalaxiesEntityLeftClickManager.useLeft(client.level, player, hand, itemStack, repeatEvent);

            // Check the inventory directly instead of using the removed Fabric extension methods
            ItemStack resultStack = player.getItemInHand(hand);

            if (resultStack != itemStack)
            {
                player.setItemInHand(hand, resultStack);
            }

            PacketDistributor.sendToServer(packet);

            return actionResult;
        }
    }

    /**
     * Handles packets notifying the client that the server has consumed the item
     */
    public static void handleConsumeItem(LocalPlayer player)
    {
        if (!(player instanceof ILeftClickingEntity lce))
            return;

        lce.pswg$consumeLeftItem();
    }
}