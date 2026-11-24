package net.mrconqueso.pswg.interaction;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.mrconqueso.pswg.networking.PlayerInteractItemLeftC2SPacket;

/**
 * Handles left-use item interactions on the server
 */
public final class GalaxiesEntityLeftClickManager
{
    /**
     * Handles {@link PlayerInteractItemLeftC2SPacket} packets to left-use an item
     */
    public static void handleInteractItemLeft(PlayerInteractItemLeftC2SPacket packet, ServerPlayer player)
    {
        ServerLevel serverWorld = player.serverLevel();
        InteractionHand hand = packet.hand();
        ItemStack itemStack = player.getItemInHand(hand);
        player.resetLastActionTime();

        if (!itemStack.isEmpty() && itemStack.getItem() instanceof ILeftClickUsable)
        {
            float yaw = packet.yaw();
            float pitch = packet.pitch();

            // Replaces the old Fabric absSnapRotationTo
            if (pitch != player.getXRot() || yaw != player.getYRot())
            {
                player.setXRot(pitch);
                player.setYRot(yaw);
                player.xRotO = pitch;
                player.yRotO = yaw;
            }

            var result = interactItemLeft(player.gameMode, player, serverWorld, itemStack, hand, packet.repeat());

            if (result.consumesAction())
            {
                player.swing(hand, true);
            }
        }
    }

    private static InteractionResult interactItemLeft(ServerPlayerGameMode gameMode, ServerPlayer player, ServerLevel world, ItemStack stack, InteractionHand hand, boolean repeatEvent)
    {
        if (gameMode.getGameModeForPlayer() == GameType.SPECTATOR)
            return InteractionResult.PASS;

        if (player.getCooldowns().isOnCooldown(stack.getItem()))
            return InteractionResult.PASS;

        InteractionResult actionResult = useLeft(world, player, hand, stack, repeatEvent);
        ItemStack itemStack = player.getItemInHand(hand);

        ILeftClickingEntity leftClickingEntity = (ILeftClickingEntity) player;

        if (actionResult.consumesAction())
        {
            player.inventoryMenu.sendAllDataToRemote();
            return actionResult;
        }
        else if (actionResult == InteractionResult.FAIL && ((ILeftClickUsable) stack.getItem()).getMaxUseLeftTime(itemStack, player) > 0 && !leftClickingEntity.pswg$isLeftUsingItem())
        {
            return actionResult;
        }
        else
        {
            if (!leftClickingEntity.pswg$isLeftUsingItem())
            {
                player.inventoryMenu.sendAllDataToRemote();
            }

            return actionResult;
        }
    }

    /**
     * Emulates the {@link ItemStack#use} functionality for left-use items
     */
    public static InteractionResult useLeft(Level world, Player user, InteractionHand hand, ItemStack stack, boolean repeatEvent)
    {
        if (!(stack.getItem() instanceof ILeftClickUsable leftItem))
            return InteractionResult.PASS;

        return leftItem.useLeft(world, user, hand, repeatEvent);
    }

    /**
     * Invokes the emulated {@link Player#releaseUsingItem} functionality for left-use items
     */
    public static void handleReleaseUseItem(ServerPlayer player)
    {
        player.resetLastActionTime();

        if (player instanceof ILeftClickingEntity leftClickingEntity)
        {
            leftClickingEntity.pswg$stopLeftUsingItem();
        }
    }
}