package dev.pswg.blasters;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;

public final class BlasterBolt extends Projectile implements IEntityWithComplexSpawn
{
	private static final double SPEED = 5;
	private float damage;
	private double range;

	public BlasterBolt(EntityType<BlasterBolt> type, Level level)
	{
		super(type, level);
	}

	public void launch(LivingEntity owner, float damage, int range)
	{
		this.damage = Math.max(0, damage);
		this.range = Math.max(0, range);
		setOwner(owner);
		setPos(owner.getEyePosition());
		setYRot(owner.getYHeadRot());
		setXRot(owner.getXRot());
		yRotO = getYRot();
		xRotO = getXRot();
		setDeltaMovement(Vec3.directionFromRotation(getXRot(), getYRot()).scale(SPEED));
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder)
	{
	}

	@Override
	public void tick()
	{
		super.tick();
		if (range <= 0 || tickCount > 20)
		{
			discard();
			return;
		}
		var velocity = getDeltaMovement();
		if (velocity.lengthSqr() > range * range)
			setDeltaMovement(velocity.normalize().scale(range));
		var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit))
		{
			setPos(hit.getLocation());
			if (!level().isClientSide)
				hitTargetOrDeflectSelf(hit);
			discard();
			return;
		}
		var travel = getDeltaMovement();
		setPos(position().add(travel));
		range -= travel.length();
	}

	@Override
	protected boolean canHitEntity(net.minecraft.world.entity.Entity entity)
	{
		return entity != getOwner() && super.canHitEntity(entity);
	}

	@Override
	protected void onHitEntity(EntityHitResult hit)
	{
		hit.getEntity().hurt(damageSources().thrown(this, getOwner()), damage);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putFloat("Damage", damage);
		tag.putDouble("Range", range);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		damage = tag.getFloat("Damage");
		range = tag.getDouble("Range");
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer)
	{
		var velocity = getDeltaMovement();
		buffer.writeDouble(velocity.x);
		buffer.writeDouble(velocity.y);
		buffer.writeDouble(velocity.z);
		buffer.writeDouble(range);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer)
	{
		setDeltaMovement(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		range = buffer.readDouble();
	}
}
