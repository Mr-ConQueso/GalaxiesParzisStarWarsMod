package dev.pswg.vehicles;

import dev.pswg.Galaxies;
import dev.pswg.blasters.BlasterBolt;
import dev.pswg.blasters.Blasters;
import dev.pswg.particle.GalaxiesParticles;
import dev.pswg.rendering.p3d.P3dResources;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Ship extends Entity
{
	private static final EntityDataAccessor<Quaternionf> ROTATION = SynchedEntityData.defineId(Ship.class, EntityDataSerializers.QUATERNION);
	private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(Ship.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> WINGS = SynchedEntityData.defineId(Ship.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> COCKPIT = SynchedEntityData.defineId(Ship.class, EntityDataSerializers.INT);
	private static final String[] CANNONS = { "CannonTopLeft", "CannonBottomLeft", "CannonTopRight", "CannonBottomRight" };
	private static final String[] X_WING_WAKE_SOURCES = { "CannonTopLeft", "CannonBottomLeft", "CannonTopRight", "CannonBottomRight",
			"EngineTopLeft", "EngineBottomLeft", "EngineTopRight", "EngineBottomRight" };
	private static final ResourceLocation X_WING_MODEL = Galaxies.id("ship/xwing_t65b");
	private static final Vec3[] X_WING_HULL = { new Vec3(0, 0, -4.3), new Vec3(0, 0, -2), new Vec3(0, 0, 3.2),
			new Vec3(-4.1, 0, -1.8), new Vec3(4.1, 0, -1.8), new Vec3(-2.2, 0, 0), new Vec3(2.2, 0, 0) };
	private static final Vec3[] SPEEDER_HULL = { new Vec3(0, 0, -1.2), new Vec3(0, 0, 1.35) };

	private final Vehicles.Kind kind;
	private Quaternionf previousRotation = new Quaternionf();
	private Quaternionf viewRotation = new Quaternionf();
	private int previousWings;
	private int previousCockpit;
	private int clientWings;
	private int clientCockpit;
	private int cannon;
	private int controls;
	private float turnMomentum;
	private boolean wingsOpen;
	private boolean canopyOpen;
	private long lastInput = -100;
	private long lastShot = -100;
	private Vec3 lerpTarget;
	private int lerpSteps;

	Ship(EntityType<? extends Ship> type, Level level, Vehicles.Kind kind)
	{
		super(type, level);
		this.kind = kind;
		blocksBuilding = true;
		setNoGravity(true);
	}

	public Vehicles.Kind kind() { return kind; }
	public float throttle() { return entityData.get(THROTTLE); }
	public Entity pilot() { return getFirstPassenger(); }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder)
	{
		builder.define(ROTATION, new Quaternionf()).define(THROTTLE, 0F).define(WINGS, 0).define(COCKPIT, 0);
	}

	@Override
	public boolean isPickable() { return isAlive(); }

	@Override
	public boolean canBeCollidedWith() { return true; }

	@Override
	public ItemStack getPickResult() { return Vehicles.spawner(kind).toStack(); }

	@Override
	public boolean hurt(DamageSource source, float amount)
	{
		if (level().isClientSide || isInvulnerableTo(source) || source.getDirectEntity() == null)
			return false;
		ejectPassengers();
		kill();
		return true;
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand)
	{
		if (player.isSecondaryUseActive() || player.isSpectator() || !canAddPassenger(player))
			return InteractionResult.PASS;
		return level().isClientSide || player.startRiding(this) ? InteractionResult.sidedSuccess(level().isClientSide) : InteractionResult.PASS;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger)
	{
		return passenger instanceof Player && getPassengers().size() < kind.seats;
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction move)
	{
		int seat = getPassengers().indexOf(passenger);
		if (seat < 0)
			return;
		var point = position().add(rotated(kind.seat(seat)));
		move.accept(passenger, point.x, point.y, point.z);
		if (kind.flying())
		{
			passenger.setYRot(getYRot());
			passenger.setXRot(getXRot());
			passenger.setYHeadRot(getYRot());
		}
		else if (passenger instanceof LivingEntity rider)
			rider.setYBodyRot(getYRot());
	}

	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger)
	{
		for (double side : new double[] { 1, -1 })
		{
			var candidate = position().add(rotated(new Vec3(side * 1.65, 0.2, 0)));
			if (level().noCollision(passenger, passenger.getBoundingBox().move(candidate.subtract(passenger.position()))))
				return candidate;
		}
		return super.getDismountLocationForPassenger(passenger);
	}

	public void setHeading(float yaw)
	{
		var rotation = new Quaternionf().rotationY((180 - yaw) * Mth.DEG_TO_RAD);
		setOrientation(rotation);
		previousRotation = new Quaternionf(rotation);
	}

	private void setOrientation(Quaternionf rotation)
	{
		entityData.set(ROTATION, rotation.normalize());
		var heading = rotated(new Vec3(0, 0, -1));
		setYRot((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
		setXRot((float) -Math.toDegrees(Math.asin(Mth.clamp(heading.y, -1, 1))));
	}

	public Quaternionf orientation(float partial)
	{
		return level().isClientSide ? new Quaternionf(previousRotation).slerp(viewRotation, partial) : new Quaternionf(entityData.get(ROTATION));
	}

	public Vec3 rotated(Vec3 offset)
	{
		var vec = entityData.get(ROTATION).transform(offset.toVector3f());
		return new Vec3(vec);
	}

	public Matrix4f partTransform(String name, float partial)
	{
		var transform = new Matrix4f();
		if (!kind.flying())
			return transform;
		float open = eased(Mth.lerp(partial, previousWings, entityData.get(WINGS)) / 40F);
		float canopy = eased(Mth.lerp(partial, previousCockpit, entityData.get(COCKPIT)) / 20F);
		switch (name)
		{
			case "WingTopLeft", "WingBottomRight" -> transform.rotateZ(-open * 13 * Mth.DEG_TO_RAD);
			case "WingBottomLeft", "WingTopRight" -> transform.rotateZ(open * 13 * Mth.DEG_TO_RAD);
			case "Cockpit" -> transform.rotateX(canopy * 50 * Mth.DEG_TO_RAD);
		}
		return transform;
	}

	private static float eased(float t)
	{
		return t < 0.5F ? 4 * t * t * t : 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
	}

	@Override
	public void tick()
	{
		super.tick();
		previousRotation = new Quaternionf(viewRotation);
		viewRotation.slerp(entityData.get(ROTATION), tickCount < 2 ? 1F : 0.65F);
		if (level() instanceof ServerLevel server)
		{
			previousWings = entityData.get(WINGS);
			previousCockpit = entityData.get(COCKPIT);
			tickServer(server);
		}
		else
		{
			previousWings = clientWings;
			previousCockpit = clientCockpit;
			clientWings = entityData.get(WINGS);
			clientCockpit = entityData.get(COCKPIT);
			if (lerpSteps > 0)
			{
				var start = position();
				setPos(start.lerp(lerpTarget, 1D / lerpSteps--));
				if (kind == Vehicles.Kind.X_WING)
					wake(start, position());
				else if (throttle() > 0.5F)
					speederWake();
			}
		}
	}

	private void tickServer(ServerLevel level)
	{
		int input = pilot() instanceof Player && level.getGameTime() - lastInput < 10 ? controls : 0;
		float throttle = throttle();
		if (pilot() == null)
			throttle = throttle > 0.5F ? throttle * 0.8F : 0;
		else
			throttle = Mth.clamp(throttle + ((input & VehicleNetwork.FORWARD) != 0 ? 0.3F : 0)
					- ((input & VehicleNetwork.BACK) != 0 ? 0.3F : 0), 0, 3);

		if (kind.flying())
		{
			wingsOpen = animate(WINGS, 40, wingsOpen, (input & VehicleNetwork.RIGHT) != 0);
			canopyOpen = animate(COCKPIT, 20, canopyOpen, (input & VehicleNetwork.LEFT) != 0);
			if ((input & VehicleNetwork.FIRE) != 0 && pilot() instanceof Player player)
				fire(level, player);
		}
		else
		{
			turnMomentum += (input & VehicleNetwork.LEFT) != 0 ? 1.75F : (input & VehicleNetwork.RIGHT) != 0 ? -1.75F : 0;
			if (Math.abs(turnMomentum) > 0.02F)
			{
				setOrientation(new Quaternionf(entityData.get(ROTATION)).rotateY(turnMomentum * Mth.DEG_TO_RAD));
				for (var passenger : getPassengers())
					passenger.setYRot(passenger.getYRot() - turnMomentum);
				turnMomentum *= 0.6F;
			}
		}

		entityData.set(THROTTLE, throttle);
		var direction = rotated(new Vec3(0, 0, -1));
		var speed = kind.flying() ? direction.scale(throttle)
				: new Vec3(direction.x * throttle / 2, hoverMotion(direction, throttle), direction.z * throttle / 2);
		var start = position();
		int steps = Math.max(1, Mth.ceil(speed.length() / 0.35));
		var step = speed.scale(1D / steps);
		for (int i = 0; i < steps; i++)
		{
			if (!hullClear(position().add(step)))
			{
				entityData.set(THROTTLE, 0F);
				break;
			}
			move(MoverType.SELF, step);
		}
		setDeltaMovement(position().subtract(start));
	}

	private boolean animate(EntityDataAccessor<Integer> key, int maximum, boolean open, boolean pressed)
	{
		int frame = entityData.get(key);
		if (pressed && frame == (open ? maximum : 0))
			open = !open;
		entityData.set(key, Mth.clamp(frame + (open ? 1 : -1), 0, maximum));
		return open;
	}

	boolean hullClear(Vec3 center)
	{
		if (!level().hasChunkAt(BlockPos.containing(center)) || !level().getWorldBorder().isWithinBounds(BlockPos.containing(center)))
			return false;
		if (level().getBlockCollisions(this, getBoundingBox().move(center.subtract(position())).deflate(0.005)).iterator().hasNext())
			return false;
		var hull = kind.flying() ? X_WING_HULL : SPEEDER_HULL;
		for (Vec3 offset : hull)
		{
			var point = center.add(rotated(offset)).add(0, kind.flying() ? 0.32 : 0.30, 0);
			if (!level().hasChunkAt(BlockPos.containing(point)) || level().getBlockCollisions(this, AABB.ofSize(point, 0.45, 0.4, 0.55)).iterator().hasNext())
				return false;
		}
		return true;
	}

	private double hoverMotion(Vec3 direction, float throttle)
	{
		double highest = getY() - 3;
		var side = new Vec3(-direction.z, 0, direction.x);
		for (int column = -1; column <= 1; column++)
			for (int row = 0; row <= 2; row++)
			{
				var start = position().add(side.scale(column * 1.4)).add(direction.scale(row * (0.5 + throttle / 2))).add(0, 3, 0);
				var hit = level().clip(new ClipContext(start, start.add(0, -6, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this));
				if (hit.getType() != HitResult.Type.MISS)
					highest = Math.max(highest, hit.getLocation().y);
			}
		return Mth.clamp((highest + kind.hover + 0.08 - getY()) / 5, -0.32, 0.35);
	}

	private void wake(Vec3 from, Vec3 to)
	{
		double speed = Math.min(3, from.distanceTo(to));
		int maxParticles = (int) (15 * speed * speed);
		if (maxParticles == 0)
			return;
		var rig = P3dResources.MODELS.get(X_WING_MODEL);
		if (rig == null)
			return;
		double steps = 5 * speed + 1;
		var offset = new Vector3f();
		for (int step = 0; step <= steps; step++)
		{
			float partial = (float) (step / steps);
			var center = from.lerp(to, partial);
			var rotation = orientation(partial);
			for (String emitter : X_WING_WAKE_SOURCES)
			{
				var socket = rig.transform(emitter, part -> partTransform(part, partial));
				if (socket == null)
					continue;
				rotation.transform(socket.getTranslation(offset));
				surfaceWake(center.add(offset.x, offset.y, offset.z), speed, steps, maxParticles);
			}
		}
	}

	private void surfaceWake(Vec3 source, double speed, double steps, int maxParticles)
	{
		var hit = level().clip(new ClipContext(source, source.add(0, -10, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		if (hit.getType() == HitResult.Type.MISS)
			return;
		var state = level().getBlockState(hit.getBlockPos());
		ParticleOptions particle = state.getFluidState().is(FluidTags.WATER) ? GalaxiesParticles.WATER_WAKE.get()
				: new BlockParticleOption(GalaxiesParticles.WAKE.get(), state).setPos(hit.getBlockPos());
		var point = hit.getLocation();
		double altitude = Math.max(0.5, point.distanceTo(source));
		double count = Math.min(15 / altitude, maxParticles) / steps;
		double lateral = speed * speed / (3.24 * altitude);
		double lift = speed * speed / (6.25 * altitude);
		for (int i = 0; i < count; i++)
			if (random.nextFloat() * 2 <= speed)
				level().addParticle(particle, point.x + random.nextGaussian() / 4, point.y + 0.01,
						point.z + random.nextGaussian() / 4, lateral, lift, lateral);
	}

	private void speederWake()
	{
		var start = position();
		var hit = level().clip(new ClipContext(start, start.add(0, -8, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
		if (hit.getType() == HitResult.Type.MISS)
			return;
		var point = hit.getLocation().add(0, 0.04, 0);
		for (int i = 0; i < Math.min(8, throttle() * 3); i++)
			level().addParticle(ParticleTypes.CLOUD, point.x + random.nextGaussian(), point.y,
					point.z + random.nextGaussian(), random.nextGaussian() * 0.08, 0.05, random.nextGaussian() * 0.08);
	}

	void input(Player sender, int bits, float yaw, float pitch)
	{
		if (pilot() != sender || !(level() instanceof ServerLevel) || !Float.isFinite(yaw) || !Float.isFinite(pitch))
			return;
		long time = level().getGameTime();
		if (lastInput == time)
			return;
		lastInput = time;
		controls = bits & VehicleNetwork.MASK;
		if (kind.flying() && (yaw != 0 || pitch != 0))
		{
			var rotation = new Quaternionf(entityData.get(ROTATION));
			float radians = Mth.clamp(yaw, -24, 24) * Mth.DEG_TO_RAD;
			if ((controls & VehicleNetwork.ROLL) != 0)
				rotation.rotateZ(radians);
			else
			{
				rotation.rotateY(radians);
				stabilizeRoll(rotation);
			}
			rotation.rotateX(Mth.clamp(pitch, -24, 24) * Mth.DEG_TO_RAD);
			setOrientation(rotation);
		}
	}

	private static void stabilizeRoll(Quaternionf rotation)
	{
		var forward = rotation.transform(new Vector3f(0, 0, -1));
		var up = rotation.transform(new Vector3f(0, 1, 0));
		var target = new Vector3f(0, 1, 0).fma(-forward.y, forward);
		if (target.lengthSquared() < 0.0001F)
			return;
		target.normalize();
		if (up.y < 0) target.negate();
		float angle = (float) Math.acos(Mth.clamp(up.dot(target), -1, 1));
		if (angle <= 0.01F)
			return;
		float speed = 1 - Math.abs((float) Math.asin(Mth.clamp(forward.y, -1, 1)) / Mth.HALF_PI);
		var axis = up.cross(target).normalize();
		rotation.premul(new Quaternionf().rotationAxis(angle * 0.005F * speed * speed, axis));
	}

	private void fire(ServerLevel level, Player player)
	{
		if (level.getGameTime() - lastShot < 5)
			return;
		lastShot = level.getGameTime();
		var rig = P3dResources.RIGS.get(Galaxies.id("ship/" + kind.id));
		var matrix = rig == null ? null : rig.transform(CANNONS[cannon & 3], name -> partTransform(name, 1F));
		var anchor = matrix == null ? new Vec3((cannon & 2) == 0 ? 4.2 : -4.2, 0.2, -4.8)
				: new Vec3(matrix.m30(), matrix.m31(), matrix.m32());
		cannon++;
		var start = position().add(rotated(anchor));
		var target = position().add(rotated(new Vec3(0, 0, -40)));
		var bolt = new BlasterBolt(Blasters.BOLT.get(), level);
		bolt.launchFrom(player, start, target.subtract(start), 50, 100);
		if (level.addFreshEntity(bolt))
			level.playSound(null, blockPosition(), Vehicles.CANNON.get(), SoundSource.PLAYERS, 1, 1);
	}

	@Override
	public void lerpTo(double x, double y, double z, float yaw, float pitch, int steps)
	{
		lerpTarget = new Vec3(x, y, z);
		lerpSteps = Math.min(3, Math.max(1, steps));
		setYRot(yaw);
		setXRot(pitch);
	}

	@Override
	protected void addAdditionalSaveData(CompoundTag tag)
	{
		var rotation = entityData.get(ROTATION);
		tag.putFloat("Qx", rotation.x);
		tag.putFloat("Qy", rotation.y);
		tag.putFloat("Qz", rotation.z);
		tag.putFloat("Qw", rotation.w);
		tag.putFloat("Throttle", throttle());
		tag.putInt("Wings", entityData.get(WINGS));
		tag.putInt("Canopy", entityData.get(COCKPIT));
		tag.putBoolean("WingsOpen", wingsOpen);
		tag.putBoolean("CanopyOpen", canopyOpen);
		tag.putInt("Cannon", cannon);
	}

	@Override
	protected void readAdditionalSaveData(CompoundTag tag)
	{
		var rotation = new Quaternionf(tag.getFloat("Qx"), tag.getFloat("Qy"), tag.getFloat("Qz"), tag.getFloat("Qw"));
		if (!rotation.isFinite() || rotation.lengthSquared() < 0.001F)
			rotation.identity();
		setOrientation(rotation);
		previousRotation = new Quaternionf(rotation);
		entityData.set(THROTTLE, Mth.clamp(tag.getFloat("Throttle"), 0, 3));
		entityData.set(WINGS, Mth.clamp(tag.getInt("Wings"), 0, 40));
		entityData.set(COCKPIT, Mth.clamp(tag.getInt("Canopy"), 0, 20));
		wingsOpen = tag.contains("WingsOpen") ? tag.getBoolean("WingsOpen") : entityData.get(WINGS) == 40;
		canopyOpen = tag.contains("CanopyOpen") ? tag.getBoolean("CanopyOpen") : entityData.get(COCKPIT) == 20;
		cannon = tag.getInt("Cannon");
	}
}
