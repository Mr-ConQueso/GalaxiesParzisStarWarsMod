package net.mrconqueso.pswg.event;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.client.blaster.BlasterRecoilManager;
import net.mrconqueso.pswg.client.PSWGClient;
import net.mrconqueso.pswg.client.blaster.BlasterZoomHandler;
import net.mrconqueso.pswg.client.input.KeyHandler;
import net.mrconqueso.pswg.debug.DebugUtil;
import net.mrconqueso.pswg.item.ModItems;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.util.client.TextUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

@EventBusSubscriber(modid = PSWG.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ModClientEvents
{
    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event)
    {
        KeyHandler.tick(Minecraft.getInstance());
        BlasterRecoilManager.tick(Minecraft.getInstance()); // Updated to pass Minecraft instance if needed
    }

    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event)
    {
        BlasterZoomHandler.tick(Minecraft.getInstance());
        DebugUtil.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void onComputeFovModifierEvent(ComputeFovModifierEvent event)
    {
        if (event.getPlayer().isUsingItem() && event.getPlayer().getUseItem().is(ModItems.KAUPEN_BOW.get()))
        {
            float fovModifier = 1f;
            int ticksUsingItem = event.getPlayer().getTicksUsingItem();
            float deltaTicks = (float) ticksUsingItem / 20f;

            if (deltaTicks > 1f) {
                deltaTicks = 1f;
            } else {
                deltaTicks *= deltaTicks;
            }

            fovModifier *= 1f - deltaTicks * 0.15f;
            event.setNewFovModifier(fovModifier);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(ClientPlayerNetworkEvent.LoggingIn event)
    {
        if (event.getPlayer() != null)
        {
            // Configuration fetching should be adapted to NeoForge config spec.
            var config = Resources.CONFIG.get();
            if (config.client.showCharacterCustomizeTip)
            {
                event.getPlayer().displayClientMessage(Component.translatable("msg.pswg.tip.customize_character", TextUtil.stylizeKeybind(PSWGClient.KEY_SPECIES_SELECT.getTranslatedKeyMessage())), false);
                config.client.showCharacterCustomizeTip = false;
                Resources.CONFIG.save();
            }
        }
    }
}