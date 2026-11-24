package dev.pswg.mixin.client;

import dev.pswg.species.client.SpeciesClient;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerRenderer.class)
abstract class SpeciesPlayerSkinMixin
{
	@Inject(method = "getTextureLocation(Lnet/minecraft/client/player/AbstractClientPlayer;)Lnet/minecraft/resources/ResourceLocation;",
			at = @At("RETURN"), cancellable = true)
	private void pswg$speciesSkin(AbstractClientPlayer player, CallbackInfoReturnable<ResourceLocation> callback)
	{
		var texture = SpeciesClient.texture(SpeciesClient.appearance(player));
		if (texture != null) callback.setReturnValue(texture);
	}

	@Redirect(method = "renderHand", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkin()Lnet/minecraft/client/resources/PlayerSkin;"))
	private PlayerSkin pswg$speciesHands(AbstractClientPlayer player)
	{
		var skin = player.getSkin();
		var texture = SpeciesClient.texture(SpeciesClient.appearance(player));
		return texture == null ? skin : new PlayerSkin(texture, skin.textureUrl(), skin.capeTexture(), skin.elytraTexture(), skin.model(), skin.secure());
	}
}
