package dev.pswg.mixin.client;

import dev.pswg.rendering.g3d.G3dWornArmor;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
abstract class PlayerArmorOverlayMixin
{
	@Inject(method = "setModelProperties", at = @At("TAIL"))
	private void pswg$hideArmorSkinOverlays(AbstractClientPlayer player, CallbackInfo callback)
	{
		if (player.isSpectator())
			return;

		var model = ((PlayerRenderer) (Object) this).getModel();
		if (G3dWornArmor.hidesSkinOverlay(player.getItemBySlot(EquipmentSlot.HEAD)))
			model.hat.visible = false;
		if (G3dWornArmor.hidesSkinOverlay(player.getItemBySlot(EquipmentSlot.CHEST)))
		{
			model.jacket.visible = false;
			model.leftSleeve.visible = false;
			model.rightSleeve.visible = false;
		}
		if (G3dWornArmor.hidesSkinOverlay(player.getItemBySlot(EquipmentSlot.LEGS))
				|| G3dWornArmor.hidesSkinOverlay(player.getItemBySlot(EquipmentSlot.FEET)))
		{
			model.leftPants.visible = false;
			model.rightPants.visible = false;
		}
	}
}
