package dev.pswg.blasters;

import dev.pswg.Galaxies;
import dev.pswg.blasters.LegacyBlaster.FiringMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.event.EventHooks;

public final class BlasterBolt extends Projectile implements IEntityWithComplexSpawn
{
	private static final double SPEED = 5;
	private static final TagKey<Block> REFLECTS = TagKey.create(Registries.BLOCK, Galaxies.id("blaster_reflect"));
	private float damage;
	private double range;
	private double initialRange;
	private float falloff = -1F;
	private int color = 0xFF2244;
	private float length = 1;
	private float radius = 1;
	private boolean ignoresWater = true;
	private boolean pyrotechnics;
	private HumanoidArm arm = HumanoidArm.RIGHT;
	private FiringMode mode = FiringMode.AUTOMATIC;

	public BlasterBolt(EntityType<BlasterBolt> type, Level level)
	{
		super(type, level);
	}

	public void launch(LivingEntity owner, float damage, int range)
	{
		launch(owner, damage, range, owner.getXRot(), owner.getYHeadRot(), SPEED);
	}

	void launch(LivingEntity owner, InteractionHand hand, float damage, int range)
	{
		arm = hand == InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite();
		launch(owner, damage, range);
	}

	public void launchFrom(LivingEntity owner, Vec3 origin, Vec3 direction, float damage, int range)
	{
		float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance()));
		float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
		launch(owner, damage, range, pitch, yaw, SPEED);
		color = Mth.hsvToRgb(0.98F, 1, 1);
		setPos(origin);
	}

	private void launch(LivingEntity owner, float damage, double range, float pitch, float yaw, double speed)
	{
		this.damage = Math.max(0, damage);
		this.range = this.initialRange = Math.max(0, range);
		setOwner(owner);
		setPos(owner.getEyePosition().subtract(0, getBbHeight() * 0.5, 0));
		setYRot(yaw);
		setXRot(pitch);
		yRotO = yaw;
		xRotO = pitch;
		setDeltaMovement(Vec3.directionFromRotation(pitch, yaw).scale(speed));
	}

	void launchLegacy(LivingEntity owner, InteractionHand hand, ItemStack stack, BlasterDefinition definition, LegacyBlaster legacy,
	                  FiringMode mode, float damage, int range)
	{
		this.mode = mode;
		this.color = legacy.beam().color();
		this.length = legacy.beam().length();
		this.radius = legacy.beam().radius();
		this.falloff = legacy.beam().falloff();
		this.pyrotechnics = legacy.beam().pyrotechnics();
		this.ignoresWater = !legacy.mechanics().water().equals("none") || BlasterItem.hasFunction(stack, definition, "waterproof_bolts");
		this.arm = hand == InteractionHand.MAIN_HAND ? owner.getMainArm() : owner.getMainArm().getOpposite();
		float spread = BlasterItem.state(stack).isAiming() ? 0 : BlasterItem.multiplier(stack, definition, "spread_multiplier");
		float pitch = owner.getXRot() + spread * legacy.mechanics().spreadY() * (2 * random.nextFloat() - 1);
		float yaw = owner.getYRot() + spread * legacy.mechanics().spreadX() * (2 * random.nextFloat() - 1);
		double reach = mode == FiringMode.STUN ? range * 0.1 : range;
		double speed = mode == FiringMode.STUN ? 1.25 : mode == FiringMode.ION ? 2 : SPEED;
		launch(owner, damage, reach, pitch, yaw, speed);
	}

	public int color() { return color; }
	public float length() { return length; }
	public float radius() { return radius; }
	public FiringMode mode() { return mode; }
	public HumanoidArm arm() { return arm; }

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder)
	{
	}

	@Override
	public void tick()
	{
		super.tick();
		if (range <= 0 || tickCount > (falloff < 0 ? 20 : 240))
		{
			discard();
			return;
		}
		var velocity = getDeltaMovement();
		if (velocity.lengthSqr() > range * range)
		{
			velocity = velocity.normalize().scale(range);
			setDeltaMovement(velocity);
		}
		var start = position();
		if (level().isClientSide && tickCount == 1 && pyrotechnics)
		{
			var forward = velocity.normalize();
			var right = forward.cross(new Vec3(0, 1, 0)).normalize();
			var muzzle = start.add(forward.scale(0.65)).add(right.scale(arm == HumanoidArm.RIGHT ? 0.25 : -0.25)).add(0, -0.15, 0);
			BlasterEffects.muzzle(level(), muzzle, forward);
		}
		var hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
		if (!ignoresWater)
		{
			var water = level().clip(new ClipContext(start, start.add(velocity), ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, this));
			if (water.getType() != HitResult.Type.MISS && water.getLocation().distanceToSqr(start) < hit.getLocation().distanceToSqr(start))
				hit = water;
		}
		boolean collided = hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, hit);
		if (mode == FiringMode.SLUGTHROWER && level().isClientSide)
			BlasterEffects.slugTrail(level(), start, collided ? hit.getLocation() : start.add(velocity));
		if (collided)
		{
			range -= hit.getLocation().distanceTo(start);
			setPos(hit.getLocation());
			if (hit instanceof BlockHitResult block && level().getBlockState(block.getBlockPos()).is(REFLECTS))
			{
				var normal = Vec3.atLowerCornerOf(block.getDirection().getNormal());
				reflect(position().add(normal.scale(0.04)), velocity.subtract(normal.scale(2 * normal.dot(velocity))));
				return;
			}
			if (hit instanceof EntityHitResult entityHit && deflectWithSaber(entityHit.getEntity(), velocity))
				return;
			if (!level().isClientSide)
				hitTargetOrDeflectSelf(hit);
			discard();
			return;
		}
		setPos(start.add(velocity));
		range -= velocity.length();
	}

	private boolean deflectWithSaber(Entity entity, Vec3 velocity)
	{
		if (!(entity instanceof LivingEntity defender) || !dev.pswg.sabers.SaberItem.blocks(defender))
			return false;
		var outgoing = Vec3.directionFromRotation(defender.getXRot(), defender.getYHeadRot()).normalize();
		setOwner(defender);
		var origin = defender.getEyePosition().add(outgoing.scale(defender.getBbWidth() + 0.2));
		reflect(origin, outgoing.scale(velocity.length()));
		return true;
	}

	private void reflect(Vec3 pos, Vec3 velocity)
	{
		setPos(pos);
		setDeltaMovement(velocity);
		if (!level().isClientSide)
			BlasterNetwork.sendBoltMotion(this, pos, velocity);
	}

	@Override
	protected boolean canHitEntity(Entity entity)
	{
		return entity != getOwner() && (mode != FiringMode.STUN || entity instanceof LivingEntity) && super.canHitEntity(entity);
	}

	void stunCone(LivingEntity shooter)
	{
		var start = shooter.getEyePosition();
		var axis = getDeltaMovement().normalize();
		double cosine = Math.cos(Math.toRadians(10));
		var bounds = new AABB(start, start.add(axis.scale(range))).inflate(range * 0.18);
		for (LivingEntity target : level().getEntitiesOfClass(LivingEntity.class, bounds, entity -> entity != shooter && !entity.isSpectator()))
		{
			var point = target.getBoundingBox().getCenter();
			var delta = point.subtract(start);
			if (delta.lengthSqr() > range * range || delta.normalize().dot(axis) < cosine)
				continue;
			var trace = level().clip(new ClipContext(start, point, ClipContext.Block.COLLIDER,
					ignoresWater ? ClipContext.Fluid.NONE : ClipContext.Fluid.WATER, shooter));
			if (trace.getType() == HitResult.Type.MISS && !dev.pswg.sabers.SaberItem.blocks(target))
				stun(target);
		}
	}

	private void stun(LivingEntity entity)
	{
		entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3, true, false), getOwner());
		entity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0, true, false), getOwner());
		entity.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0, true, false), getOwner());
		entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1, true, false), getOwner());
	}

	@Override
	protected void onHitEntity(EntityHitResult hit)
	{
		if (mode == FiringMode.ION)
			return;
		if (mode == FiringMode.STUN)
		{
			if (hit.getEntity() instanceof LivingEntity living)
				stun(living);
			return;
		}
		float scaled = damage;
		if (falloff >= 0)
		{
			double fraction = Math.clamp(1 - range / Math.max(0.01, initialRange), 0.00001, 1);
			scaled *= (float) (falloff == 0 ? 1 - fraction : 1 - Math.pow(fraction, falloff / fraction));
		}
		hit.getEntity().hurt(damageSources().thrown(this, getOwner()), scaled);
	}

	@Override
	protected void onHitBlock(BlockHitResult hit)
	{
		super.onHitBlock(hit);
		if (level() instanceof ServerLevel server && mode != FiringMode.STUN)
			BlasterNetwork.sendBoltImpact(server, hit, getDeltaMovement().normalize());
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		tag.putFloat("Damage", damage);
		tag.putDouble("Range", range);
		tag.putDouble("InitialRange", initialRange);
		tag.putFloat("Falloff", falloff);
		tag.putInt("Color", color);
		tag.putFloat("Length", length);
		tag.putFloat("Radius", radius);
		tag.putBoolean("IgnoresWater", ignoresWater);
		tag.putInt("Arm", arm.ordinal());
		tag.putInt("FiringMode", mode.ordinal());
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		damage = tag.getFloat("Damage");
		range = tag.getDouble("Range");
		initialRange = tag.getDouble("InitialRange");
		falloff = tag.contains("Falloff") ? tag.getFloat("Falloff") : -1;
		if (tag.contains("Color"))
		{
			color = tag.getInt("Color");
			length = tag.getFloat("Length");
			radius = tag.getFloat("Radius");
			ignoresWater = tag.getBoolean("IgnoresWater");
			arm = HumanoidArm.values()[Math.floorMod(tag.getInt("Arm"), 2)];
			mode = FiringMode.values()[Math.floorMod(tag.getInt("FiringMode"), FiringMode.values().length)];
		}
	}

	@Override
	public void writeSpawnData(RegistryFriendlyByteBuf buffer)
	{
		var velocity = getDeltaMovement();
		buffer.writeDouble(velocity.x);
		buffer.writeDouble(velocity.y);
		buffer.writeDouble(velocity.z);
		buffer.writeDouble(range);
		buffer.writeFloat(falloff);
		buffer.writeInt(color);
		buffer.writeFloat(length);
		buffer.writeFloat(radius);
		buffer.writeBoolean(ignoresWater);
		buffer.writeBoolean(pyrotechnics);
		buffer.writeEnum(arm);
		buffer.writeEnum(mode);
	}

	@Override
	public void readSpawnData(RegistryFriendlyByteBuf buffer)
	{
		setDeltaMovement(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
		range = buffer.readDouble();
		falloff = buffer.readFloat();
		color = buffer.readInt();
		length = buffer.readFloat();
		radius = buffer.readFloat();
		ignoresWater = buffer.readBoolean();
		pyrotechnics = buffer.readBoolean();
		arm = buffer.readEnum(HumanoidArm.class);
		mode = buffer.readEnum(FiringMode.class);
	}
}
