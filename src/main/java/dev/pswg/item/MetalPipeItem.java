package dev.pswg.item;

import dev.pswg.container.CoreSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class MetalPipeItem extends Item
{
	public MetalPipeItem(Properties properties)
	{
		super(properties);
	}

	@Override
	public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker)
	{
		var level = attacker.level();
		if (!level.isClientSide)
		{
			var random = level.getRandom();
			level.playSound(null, target.blockPosition(), CoreSounds.METAL_PIPE.get(), SoundSource.PLAYERS,
					random.nextFloat() * 0.25F + 0.75F, random.nextFloat() + 0.5F);
		}
		return super.hurtEnemy(stack, target, attacker);
	}
}
