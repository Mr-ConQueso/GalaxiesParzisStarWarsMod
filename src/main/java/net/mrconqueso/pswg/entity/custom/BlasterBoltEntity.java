package net.mrconqueso.pswg.entity.custom;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class BlasterBoltEntity extends Entity implements SpawnGroupData {
    public BlasterBoltEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public void tick() {
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHit);
        Vec3 nextPos;

        if (hitResult.getType() != HitResult.Type.MISS)
            nextPos = hitResult.getLocation();
        else
            nextPos = this.position().add(this.getDeltaMovement());

        this.setPos(nextPos);
        super.tick();

        if (hitResult.getType() != HitResult.Type.MISS && this.isAlive()) {
            this.hitOrDeflect(hitResult);
        }

        if (this.tickCount > 20) discard();
    }

    protected void hitOrDeflect(HitResult hitResult) {
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) hitResult;
            Entity entity = entityHitResult.getEntity();
            // TODO: deflect against blocking player
        }
        this.onCollision(hitResult);
    }

    protected void onCollision(HitResult hitResult) {
        discard();
    }

    protected boolean canHit(Entity entity) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {}
}
