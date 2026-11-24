package net.mrconqueso.pswg.mixin.client.leftuse;

import net.minecraft.client.Minecraft;
import net.mrconqueso.pswg.client.interaction.GalaxiesEntityLeftClickClientManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client input support for left-use items
 */
@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin
{
	@Inject(method = "handleKeybinds()V", at = @At("HEAD"))
	public void handleInputEvents(CallbackInfo ci)
	{
		GalaxiesEntityLeftClickClientManager.handleInputEvents((Minecraft)(Object)this);
	}
}
