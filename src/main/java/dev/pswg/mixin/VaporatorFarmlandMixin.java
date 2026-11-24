package dev.pswg.mixin;

import dev.pswg.utilities.MoistureVaporator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FarmBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FarmBlock.class)
abstract class VaporatorFarmlandMixin
{
	@Inject(method = "isNearWater", at = @At("RETURN"), cancellable = true)
	private static void pswg$vaporatorHydration(LevelReader level, BlockPos farmland, CallbackInfoReturnable<Boolean> callback)
	{
		if (callback.getReturnValue()) return;
		for (var pos : BlockPos.betweenClosed(farmland.offset(-4, 1, -4), farmland.offset(4, 1, 4)))
			if (MoistureVaporator.providesMoisture(level.getBlockState(pos)))
			{
				callback.setReturnValue(true);
				return;
			}
	}
}
