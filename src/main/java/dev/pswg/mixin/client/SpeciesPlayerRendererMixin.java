package dev.pswg.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.pswg.species.client.SpeciesClient;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerRenderer.class)
abstract class SpeciesPlayerRendererMixin extends LivingEntityRenderer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>>
{
	protected SpeciesPlayerRendererMixin(EntityRendererProvider.Context context, PlayerModel<AbstractClientPlayer> model, float radius)
	{
		super(context, model, radius);
	}

	@WrapMethod(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
	private void pswg$renderSpecies(AbstractClientPlayer player, float yaw, float partial, PoseStack pose, MultiBufferSource buffers,
			int light, Operation<Void> render)
	{
		var previous = model;
		var species = SpeciesClient.model(player);
		try
		{
			if (species != null)
				model = species;
			render.call(player, yaw, partial, pose, buffers, light);
		}
		finally { model = previous; }
	}

	@WrapMethod(method = { "renderLeftHand", "renderRightHand" })
	private void pswg$renderSpeciesHands(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, Operation<Void> render)
	{
		var previous = model;
		var species = SpeciesClient.model(player);
		try
		{
			if (species != null)
				model = species;
			render.call(pose, buffers, light, player);
		}
		finally { model = previous; }
	}
}
