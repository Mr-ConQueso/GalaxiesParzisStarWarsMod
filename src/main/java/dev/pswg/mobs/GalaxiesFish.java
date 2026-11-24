package dev.pswg.mobs;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class GalaxiesFish extends AbstractFish
{
	public GalaxiesFish(EntityType<? extends AbstractFish> type, Level level) { super(type, level); }
	@Override public ItemStack getBucketItemStack()
	{
		return new ItemStack(getType() == Creatures.FAA.get() ? Creatures.FAA_BUCKET.get() : Creatures.LAA_BUCKET.get());
	}
	@Override protected SoundEvent getFlopSound() { return SoundEvents.COD_FLOP; }
}
