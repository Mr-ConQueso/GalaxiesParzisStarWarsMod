package net.mrconqueso.pswg.mixin.client.attributes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.mrconqueso.pswg.PSWG;
import net.mrconqueso.pswg.attributes.ModEntityAttributes;
import net.mrconqueso.pswg.interaction.IRecoilEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerEntityMixin
{
	/**
	 * Modifies the local variable 'f' (the FOV multiplier) before it is passed
	 * to NeoForge's ClientHooks and returned.
	 * * @param f The current FOV multiplier calculated by vanilla
	 * @return The modified FOV multiplier
	 */
	@ModifyVariable(method = "getFieldOfViewModifier()F", at = @At(value = "STORE", ordinal = 0), name = "f")
	private float pswg$applyBlasterFovModifiers(float f)
	{
		Player self = (Player)(Object)this;

		// 1. Apply Blaster Attribute Zoom
		// Note: Division because FOV works inversely (lower value = more zoom)
		float zoomAttr = (float) self.getAttributeValue(ModEntityAttributes.FIELD_OF_VIEW_ZOOM);
		if (zoomAttr != 0) {
			f /= zoomAttr;
		}

		// 2. Apply Recoil FOV "Kick"
		if (self instanceof IRecoilEntity recoilEntity)
		{
			int recoilTime = Math.toIntExact(recoilEntity.pswg$getRecoilTime());
			if (recoilTime > 0)
			{
				// In this specific method, we don't have partialTicks passed as an argument.
				// We fetch it from Minecraft's timer instead.
				float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
				f /= recoilEntity.pswg$getRecoilFovMultiplier(self, partialTick);
			}
		}

		return f;
	}
}