package net.mrconqueso.pswg.mixin.events;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.mrconqueso.pswg.util.ModTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin
{
    /*
    @Shadow
    protected float lastHurt;

    // We explicitly define the method signature so the IDE maps it perfectly,
    // and inject at HEAD to bypass the missing target instruction issue.
    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("HEAD"))
    public void pswg$damage(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir)
    {
        if (source.is(ModTags.Damage.IGNORES_INVULNERABLE_FRAMES))
        {
            this.lastHurt = 0;

            // Optional tip: If you want to bypass I-frames entirely,
            // you can also set `this.invulnerableTime = 0;` here, OR just add
            // your damage types to the vanilla 'minecraft:bypasses_invulnerability' tag!
        }
    }
    */
}