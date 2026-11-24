package dev.pswg.gadgets;

import dev.pswg.Galaxies;
import dev.pswg.particle.GalaxiesParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

public final class GadgetCharge extends Projectile
{
	private static final byte FRAGMENTATION_BURST = 61;
	private static final float BEAM_START = 1 / 16F;
	private static final float BEAM_RANGE = 3F;
	private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(GadgetCharge.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> FACE = SynchedEntityData.defineId(GadgetCharge.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> SETTLED = SynchedEntityData.defineId(GadgetCharge.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Float> BEAM_LENGTH = SynchedEntityData.defineId(GadgetCharge.class, EntityDataSerializers.FLOAT);
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
	private Inferno inferno;
	private int chainDelay = -1;
	private BlockPos support;
	private Vec3 lerpTarget = Vec3.ZERO;
	private int lerpSteps;
	private float targetYaw;
	private float targetPitch;

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
		builder.define(FUSE, -1).define(SETTLED, false).define(DETONATING, false).define(FACE, -1).define(BEAM_LENGTH, 0F);
	}

	@Override
	public void tick()
	{
		super.tick();
		if (!(level() instanceof ServerLevel server))
		{
			tickClient();
			return;
		}

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
		if (variant.isMine())
		{
			if (age >= variant.delay && !isPrimed())
			{
				arm(0);
				playSound(Demolitions.sound("shared.arm"), 1F, 1F);
			}
			if (isPrimed() && (variant == Demolitions.Variant.TRIPWIRE ? crossesBeam(server)
					: !server.getEntities(this, getBoundingBox().inflate(0.08),
							entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()))
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

		if (entityData.get(SETTLED))
		{
			if (variant == Demolitions.Variant.TRIPWIRE && mountedFace() != null)
			{
				if (support != null && !level().getBlockState(support).isAir())
					return;
				entityData.set(FACE, -1);
				entityData.set(BEAM_LENGTH, 0F);
				support = null;
			}
			if (!level().noCollision(this, getBoundingBox().move(0, -0.025, 0)))
				return;
			entityData.set(SETTLED, false);
			settledTicks = 0;
		}
		moveCharge();
	}

	@Override
	public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps)
	{
		lerpTarget = new Vec3(x, y, z);
		lerpSteps = 2;
		targetYaw = yaw;
		targetPitch = pitch;
	}

	private void tickClient()
	{
		var start = position();
		if (lerpSteps > 0)
		{
			float step = 1F / lerpSteps--;
			setPos(start.lerp(lerpTarget, step));
			setRot(Mth.rotLerp(step, getYRot(), targetYaw), Mth.rotLerp(step, getXRot(), targetPitch));
		}

		var face = mountedFace();
		if (face != null && isPrimed() && !isDetonating() && tickCount % 2 == 0)
		{
			var direction = Vec3.atLowerCornerOf(face.getNormal());
			for (double distance = BEAM_START; distance < beamLength(); distance += 0.015)
			{
				var point = position().add(direction.scale(distance));
				level().addParticle(Demolitions.WIRE_LASER.get(), point.x, point.y, point.z,
						direction.x * 0.001, direction.y * 0.001, direction.z * 0.001);
			}
		}
		else if (isInWater() && !entityData.get(SETTLED) && !isDetonating())
		{
			var motion = position().subtract(start);
			for (int i = 0; i < 4 && motion.lengthSqr() > 0.0001; i++)
			{
				var point = position().subtract(motion.scale(0.25 * i));
				level().addParticle(ParticleTypes.BUBBLE, point.x, point.y, point.z, motion.x, motion.y, motion.z);
			}
		}
	}

	private void moveCharge()
	{
		var velocity = getDeltaMovement().add(0, variant.isMine() ? -0.075 : -0.03, 0);
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
		if (variant == Demolitions.Variant.TRIPWIRE)
		{
			entityData.set(FACE, hit.getDirection().get3DDataValue());
			support = hit.getBlockPos().immutable();
			entityData.set(SETTLED, true);
			setDeltaMovement(Vec3.ZERO);
			return;
		}
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
		if (velocity.lengthSqr() > 0.05 * 0.05)
			playSound(block.getSoundType().getHitSound(), 0.6F, 1F);
	}

	@Override
	protected boolean canHitEntity(Entity entity)
	{
		return entity != getOwner() && !variant.isMine() && super.canHitEntity(entity);
	}

	public Direction mountedFace()
	{
		int face = entityData.get(FACE);
		return face < 0 ? null : Direction.from3DDataValue(face);
	}

	public float beamLength()
	{
		return entityData.get(BEAM_LENGTH);
	}

	private boolean crossesBeam(ServerLevel level)
	{
		var face = mountedFace();
		if (face == null || support == null || level.getBlockState(support).isAir())
		{
			entityData.set(BEAM_LENGTH, 0F);
			return false;
		}
		var direction = Vec3.atLowerCornerOf(face.getNormal());
		var start = position().add(direction.scale(BEAM_START));
		var hit = level.clip(new ClipContext(start, position().add(direction.scale(BEAM_RANGE)),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		var end = hit.getLocation();
		entityData.set(BEAM_LENGTH, (float) position().distanceTo(end));
		var bounds = new AABB(start, end).inflate(0.08);
		return !level.getEntities(this, bounds, entity -> entity.isAlive() && !entity.isSpectator()
				&& (entity.getBoundingBox().contains(start) || entity.getBoundingBox().inflate(0.05).clip(start, end).isPresent())).isEmpty();
	}

	private void beginDetonation(ServerLevel level)
	{
		entityData.set(DETONATING, true);
		setDeltaMovement(Vec3.ZERO);
		if (variant == Demolitions.Variant.SMOKE || variant == Demolitions.Variant.NERVE)
		{
			var cloud = (variant == Demolitions.Variant.SMOKE ? Demolitions.SMOKE_CLOUD : Demolitions.NERVE_CLOUD).get().create(level);
			if (cloud != null)
			{
				cloud.setPos(position());
				level.addFreshEntity(cloud);
			}
		}
		else if (variant == Demolitions.Variant.FRAGMENTATION)
		{
			playSound(Demolitions.sound("fragmentationgrenade.explode" + (random.nextInt(4) + 1)), 4F, 1F);
			level.sendParticles(Demolitions.FRAGMENTATION_WAVE.get(), getX(), getY() + 0.05, getZ(), 1, 0, 0, 0, 0);
		}
		else if (variant == Demolitions.Variant.INFERNO)
			playSound(SoundEvents.FIRECHARGE_USE, 3, 0.8F);
		else
			explode(level);
	}

	private void tickDetonation(ServerLevel level)
	{
		detonationTicks++;
		if (variant == Demolitions.Variant.INFERNO)
		{
			if (inferno == null) inferno = new Inferno();
			inferno.tick(level, this, detonationTicks);
			if (detonationTicks >= 20)
				discard();
			return;
		}
		if (variant == Demolitions.Variant.SMOKE || variant == Demolitions.Variant.NERVE)
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
			level.broadcastEntityEvent(this, FRAGMENTATION_BURST);
		if (detonationTicks >= 15)
			explode(level);
	}

	@Override
	public void handleEntityEvent(byte event)
	{
		if (event != FRAGMENTATION_BURST)
		{
			super.handleEntityEvent(event);
			return;
		}
		boolean onSurface = !level().noCollision(this, getBoundingBox().move(0, -0.08, 0));
		int count = random.nextIntBetweenInclusive(70, 100);
		for (int i = 0; i < count; i++)
		{
			double rise = onSurface ? Math.abs(random.nextGaussian() * 0.8) : random.nextGaussian() * 0.4;
			level().addParticle(Demolitions.FRAGMENTATION_SPARK.get(), getX(), getY() + 0.05, getZ(),
					random.nextGaussian() * 0.5, rise, random.nextGaussian() * 0.5);
		}
	}

	private void explode(ServerLevel level)
	{
		var smallFlash = ColorParticleOption.create(GalaxiesParticles.SMALL_FLASH.get(), -1);
		ParticleOptions visual = switch (variant)
		{
			case THERMAL -> ParticleTypes.FLASH;
			case IMPACT -> power > 2 ? ParticleTypes.FLASH : smallFlash;
			// Fragmentation already played its wave and spark burst. The explosion packet still carries knockback.
			case FRAGMENTATION -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AIR.defaultBlockState());
			default -> smallFlash;
		};
		var sound = switch (variant)
		{
			case THERMAL -> Holder.direct(Demolitions.sound("thermaldetonator.explode"));
			case FRAGMENTATION -> Holder.direct(SoundEvents.EMPTY);
			default -> SoundEvents.GENERIC_EXPLODE;
		};
		level.explode(this, damageSources().explosion(this, getOwner()),
				variant == Demolitions.Variant.FRAGMENTATION ? FRAG_BLOCKS : null,
				getX(), getY() + 0.05, getZ(), power, false, Level.ExplosionInteraction.TNT, visual, visual, sound);
		if (variant == Demolitions.Variant.THERMAL || variant == Demolitions.Variant.IMPACT)
		{
			int bursts = (int) power * 2;
			double spread = power / 4;
			int flameSpread = (int) spread;
			level.sendParticles(Demolitions.EXPLOSION_SMOKE.get(), getX(), getY(), getZ(), bursts * 10, spread, spread, spread, 0);
			level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), bursts * 4, flameSpread, flameSpread, flameSpread, 0);
			level.sendParticles(ParticleTypes.SMALL_FLAME, getX(), getY(), getZ(), bursts * 5,
					flameSpread * 1.5, flameSpread * 1.5, flameSpread * 1.5, 0);
		}
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
		if (variant.isMine() || isPrimed() || age < 40 || !player.getItemInHand(hand).isEmpty())
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
		if (inferno != null) tag.putLongArray("BurnedBlocks", inferno.save());
		tag.putInt("ChainDelay", chainDelay);
		tag.putInt("Face", entityData.get(FACE));
		if (support != null)
			tag.putLong("Support", support.asLong());
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
		if (variant == Demolitions.Variant.INFERNO && isDetonating())
		{
			inferno = new Inferno();
			inferno.load(tag.getLongArray("BurnedBlocks"), blockPosition());
		}
		chainDelay = tag.contains("ChainDelay") ? tag.getInt("ChainDelay") : -1;
		entityData.set(FACE, tag.contains("Face") ? tag.getInt("Face") : -1);
		support = tag.contains("Support") ? BlockPos.of(tag.getLong("Support")) : null;
	}
}
