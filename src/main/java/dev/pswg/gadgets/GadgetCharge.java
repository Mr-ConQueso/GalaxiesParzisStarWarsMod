package dev.pswg.gadgets;

import dev.pswg.Galaxies;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

public final class GadgetCharge extends Projectile
{
	private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(GadgetCharge.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(GadgetCharge.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DETONATING = SynchedEntityData.defineId(GadgetCharge.class, EntityDataSerializers.BOOLEAN);
	private static final TagKey<net.minecraft.world.level.block.Block> BOUNCY = TagKey.create(Registries.BLOCK, Galaxies.id("bouncy"));
	private static final TagKey<net.minecraft.world.level.block.Block> SOFT = TagKey.create(Registries.BLOCK, Galaxies.id("soft"));
	private static final TagKey<net.minecraft.world.damagesource.DamageType> IGNITES = TagKey.create(Registries.DAMAGE_TYPE, Demolitions.id("ignites_explosives"));

	private static final ExplosionDamageCalculator FRAG_BLOCKS = new ExplosionDamageCalculator()
	{
		@Override
		public boolean shouldBlockExplode(Explosion explosion, BlockGetter level, BlockPos pos, BlockState state, float power)
		{
			return state.is(Demolitions.FRAGMENTATION_DESTROY);
		}
	};

	private final Demolitions.Variant variant;
	private float power;
	private int age;
	private int settledTicks;
	private int detonationTicks;
	private int chainDelay = -1;

	GadgetCharge(EntityType<GadgetCharge> type, Level level, Demolitions.Variant variant)
	{
		super(type, level);
		this.variant = variant;
		this.power = variant.power;
	}

	public static GadgetCharge create(Demolitions.Variant variant, Level level, Vec3 pos, @Nullable Entity owner)
	{
		var charge = variant.entityType().create(level);
		if (charge == null)
			throw new IllegalStateException("Missing entity factory for " + variant);
		charge.setPos(pos);
		charge.setOwner(owner);
		return charge;
	}

	public boolean isPrimed()
	{
		return entityData.get(FUSE) >= 0;
	}

	public boolean isDetonating()
	{
		return entityData.get(DETONATING);
	}

	public Demolitions.Variant variant()
	{
		return variant;
	}

	public void arm(int remaining)
	{
		entityData.set(FUSE, Math.max(0, remaining));
	}

	void setPower(float power)
	{
		this.power = power;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(FUSE, -1).define(SETTLED, false).define(DETONATING, false);
	}

	@Override
	public void tick()
	{
		super.tick();
		if (level() instanceof ServerLevel server)
		{
			age++;
			if (isDetonating())
			{
				tickDetonation(server);
				return;
			}
			if (isInLava() || level().getBlockState(blockPosition()).is(Demolitions.DETONATES_GRENADE)
					|| chainDelay >= 0 && chainDelay-- == 0)
			{
				beginDetonation(server);
				return;
			}
			if (variant == Demolitions.Variant.PRESSURE)
			{
				if (age >= variant.delay && !isPrimed())
				{
					arm(0);
					playSound(Demolitions.sound("shared.arm"), 1F, 1F);
				}
				if (isPrimed() && !server.getEntities(this, getBoundingBox().inflate(0.08),
						entity -> entity.isAlive() && !entity.isSpectator()).isEmpty())
				{
					beginDetonation(server);
					return;
				}
			}
			else if (isPrimed())
			{
				int fuse = entityData.get(FUSE);
				if (fuse == 0)
				{
					beginDetonation(server);
					return;
				}
				entityData.set(FUSE, fuse - 1);
			}

			if (variant.canCluster() && !isPrimed() && entityData.get(SETTLED) && ++settledTicks >= 300)
			{
				if (variant.cluster().acceptCharge(server, blockPosition(), this))
					return;
				settledTicks = 0;
			}
		}

		if (isDetonating())
			return;
		if (entityData.get(SETTLED))
		{
			if (!level().noCollision(this, getBoundingBox().move(0, -0.025, 0)))
				return;
			entityData.set(SETTLED, false);
			settledTicks = 0;
		}
		moveCharge();
	}

	private void moveCharge()
	{
		var velocity = getDeltaMovement().add(0, variant == Demolitions.Variant.PRESSURE ? -0.075 : -0.03, 0);
		setDeltaMovement(velocity);
		var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit))
		{
			if (variant == Demolitions.Variant.IMPACT && isPrimed() && level() instanceof ServerLevel server)
			{
				setPos(hit.getLocation());
				beginDetonation(server);
				return;
			}
			if (hit instanceof BlockHitResult blockHit)
			{
				bounce(blockHit, velocity);
				return;
			}
		}
		setPos(position().add(velocity));
		setDeltaMovement(velocity.scale(isInWater() ? 0.8 : 0.975));
		checkInsideBlocks();
		updateRotation();
	}

	private void bounce(BlockHitResult hit, Vec3 velocity)
	{
		var normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
		setPos(hit.getLocation().add(normal.scale(0.01)));
		if (hit.getDirection() == Direction.UP && (variant == Demolitions.Variant.PRESSURE || velocity.lengthSqr() < 0.01))
		{
			entityData.set(SETTLED, true);
			setDeltaMovement(Vec3.ZERO);
			return;
		}
		var block = level().getBlockState(hit.getBlockPos());
		float hardness = block.getDestroySpeed(level(), hit.getBlockPos());
		double elasticity = Mth.clamp(0.4 - 0.25 / hardness, 0.1, 1);
		double multiplier = block.is(BOUNCY) ? 2.5 : block.is(SOFT) ? 0.75 : 1;
		setDeltaMovement(velocity.subtract(normal.scale(2 * velocity.dot(normal))).scale(elasticity * multiplier));
		if (!level().isClientSide && velocity.lengthSqr() > 0.05 * 0.05)
			playSound(block.getSoundType().getHitSound(), 0.6F, 1F);
	}

	@Override
	protected boolean canHitEntity(Entity entity)
	{
		return entity != getOwner() && variant != Demolitions.Variant.PRESSURE && super.canHitEntity(entity);
	}

	private void beginDetonation(ServerLevel level)
	{
		entityData.set(DETONATING, true);
		setDeltaMovement(Vec3.ZERO);
		if (variant == Demolitions.Variant.SMOKE)
		{
			var cloud = Demolitions.SMOKE_CLOUD.get().create(level);
			if (cloud != null)
			{
				cloud.setPos(position());
				level.addFreshEntity(cloud);
			}
		}
		else if (variant == Demolitions.Variant.FRAGMENTATION)
		{
			playSound(Demolitions.sound("fragmentationgrenade.explode" + (random.nextInt(4) + 1)), 4F, 1F);
			level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 0.05, getZ(), 1, 0, 0, 0, 0);
		}
		else
		{
			explode(level);
		}
	}

	private void tickDetonation(ServerLevel level)
	{
		detonationTicks++;
		if (variant == Demolitions.Variant.SMOKE)
		{
			if (detonationTicks >= 30)
				discard();
			return;
		}
		if (detonationTicks == 6)
		{
			float radius = power * 0.75F;
			for (var target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius)))
				target.push((getX() - target.getX()) / radius, 0, (getZ() - target.getZ()) / radius);
		}
		if (detonationTicks == 7)
			level.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 85, 0.2, 0.1, 0.2, 0.4);
		if (detonationTicks >= 15)
			explode(level);
	}

	private void explode(ServerLevel level)
	{
		var explosionSound = variant == Demolitions.Variant.THERMAL
				? Holder.direct(Demolitions.sound("thermaldetonator.explode")) : SoundEvents.GENERIC_EXPLODE;
		level.explode(this, damageSources().explosion(this, getOwner()),
				variant == Demolitions.Variant.FRAGMENTATION ? FRAG_BLOCKS : null,
				getX(), getY() + 0.05, getZ(), power, false, Level.ExplosionInteraction.TNT,
				ParticleTypes.EXPLOSION, ParticleTypes.EXPLOSION_EMITTER, explosionSound);
		level.sendParticles(ParticleTypes.FLASH, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
		discard();
	}

	@Override
	public boolean hurt(DamageSource source, float damage)
	{
		if (level().isClientSide || isDetonating())
			return false;
		if (source.is(DamageTypeTags.IS_EXPLOSION) || source.is(IGNITES))
		{
			if (chainDelay < 0)
				chainDelay = source.is(DamageTypeTags.IS_EXPLOSION) ? 2 : 0;
			return true;
		}
		return false;
	}

	@Override
	public boolean isPickable()
	{
		return !isDetonating();
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand)
	{
		if (variant == Demolitions.Variant.PRESSURE || isPrimed() || age < 40 || !player.getItemInHand(hand).isEmpty())
			return InteractionResult.PASS;
		if (!level().isClientSide && player.addItem(new ItemStack(variant.item())))
			discard();
		return InteractionResult.sidedSuccess(level().isClientSide);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putInt("Fuse", entityData.get(FUSE));
		tag.putBoolean("Settled", entityData.get(SETTLED));
		tag.putBoolean("Detonating", isDetonating());
		tag.putFloat("Power", power);
		tag.putInt("Age", age);
		tag.putInt("SettledTime", settledTicks);
		tag.putInt("DetonationTime", detonationTicks);
		tag.putInt("ChainDelay", chainDelay);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		entityData.set(FUSE, tag.contains("Fuse") ? tag.getInt("Fuse") : -1);
		entityData.set(SETTLED, tag.getBoolean("Settled"));
		entityData.set(DETONATING, tag.getBoolean("Detonating"));
		power = tag.contains("Power") ? tag.getFloat("Power") : variant.power;
		age = tag.getInt("Age");
		settledTicks = tag.getInt("SettledTime");
		detonationTicks = tag.getInt("DetonationTime");
		chainDelay = tag.contains("ChainDelay") ? tag.getInt("ChainDelay") : -1;
	}
}
