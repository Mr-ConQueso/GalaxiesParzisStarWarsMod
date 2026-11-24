package dev.pswg.sabers;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public final class ThrownSaber extends ThrowableItemProjectile
{
	private boolean recoverable;
	private int elapsed;

	public ThrownSaber(EntityType<? extends ThrownSaber> type, Level level)
	{
		super(type, level);
		setNoGravity(true);
	}

	public void setRecoverable(boolean recoverable)
	{
		this.recoverable = recoverable;
	}

	@Override
	protected Item getDefaultItem()
	{
		return Sabers.ITEMS_BY_HILT.get("luke_rotj").get();
	}

	@Override
	public void tick()
	{
		super.tick();
		if (!level().isClientSide && ++elapsed >= 60 && !isRemoved())
			recover();
	}

	@Override
	protected void onHit(HitResult hit)
	{
		super.onHit(hit);
		if (!level().isClientSide)
			recover();
	}

	@Override
	protected void onHitEntity(EntityHitResult hit)
	{
		if (!level().isClientSide)
			hit.getEntity().hurt(damageSources().thrown(this, getOwner()), 22);
	}

	private void recover()
	{
		if (recoverable)
		{
			recoverable = false;
			var stack = getItem().copy();
			if (!(getOwner() instanceof Player player) || !player.getInventory().add(stack))
				spawnAtLocation(stack);
		}
		discard();
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putBoolean("Recoverable", recoverable);
		tag.putInt("Elapsed", elapsed);
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		recoverable = tag.getBoolean("Recoverable");
		elapsed = tag.getInt("Elapsed");
	}
}
