package dev.pswg.gadgets;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

final class Intoxicated extends MobEffect
{
	private static final ResourceKey<DamageType> DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, Demolitions.id("nerve_gas"));

	Intoxicated()
	{
		super(MobEffectCategory.HARMFUL, 100000);
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier)
	{
		return amplifier > 0;
	}

	@Override
	public boolean applyEffectTick(LivingEntity target, int amplifier)
	{
		target.hurt(target.damageSources().source(DAMAGE), amplifier / 8F);
		return true;
	}
}
