package net.mrconqueso.pswg.entity;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.client.PSWGClient;
import net.mrconqueso.pswg.client.input.ShipControls;
import net.mrconqueso.pswg.client.render.camera.ChaseCam;
import net.mrconqueso.pswg.networking.SwgPackets;
import net.mrconqueso.pswg.util.entity.EntityUtil;
import net.mrconqueso.pswg.util.entity.IFlyingVehicle;
import net.mrconqueso.pswg.util.math.MathUtil;
import net.mrconqueso.pswg.util.math.QuatUtil;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import org.joml.Quaternionf;
import org.joml.Vector3d;

import javax.annotation.Nullable;
import java.util.EnumSet;

public abstract class ShipEntity extends Entity implements IFlyingVehicle
{
    // Mapped from TrackedData to EntityDataAccessor[cite: 10]
    private static final EntityDataAccessor<Quaternionf> ROTATION = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Float> THROTTLE = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);

    // Note: If you don't have a custom SHORT serializer registered, use INT and cast appropriately.
    // Assuming custom serializers exist matching your previous TrackedDataHandlers[cite: 10].
    private static final EntityDataAccessor<Short> CONTROL_BITS = SynchedEntityData.defineId(ShipEntity.class, TrackedDataHandlers.SHORT);
    private static final EntityDataAccessor<Short> SHIELD_BITS = SynchedEntityData.defineId(ShipEntity.class, TrackedDataHandlers.SHORT);
    private static final EntityDataAccessor<Integer> FUEL_BITS = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);

    @OnlyIn(Dist.CLIENT)
    private ChaseCam camera;

    @OnlyIn(Dist.CLIENT)
    protected Quaternionf clientInstRotation = new Quaternionf(QuatUtil.IDENTITY);
    @OnlyIn(Dist.CLIENT)
    private Quaternionf clientRotation = new Quaternionf(QuatUtil.IDENTITY);
    @OnlyIn(Dist.CLIENT)
    private Quaternionf clientPrevRotation = new Quaternionf(QuatUtil.IDENTITY);
    @OnlyIn(Dist.CLIENT)
    private boolean firstRotationUpdate = true;

    private Quaternionf viewRotation = new Quaternionf(QuatUtil.IDENTITY);
    private Quaternionf viewPrevRotation = new Quaternionf(QuatUtil.IDENTITY);

    public ShipEntity(EntityType<?> type, Level level)
    {
        super(type, level);
        this.blocksBuilding = true; // Equivalent to intersectionChecked behavior logic in modern Forge
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean handleMouseInput(double cursorDeltaX, double cursorDeltaY)
    {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;

        assert player != null;

        var ship = getShip(player);

        if (ship != null && ship.isPilot(player) && ship.useMouseInput(player))
        {
            ship.acceptMouseInput(cursorDeltaX, cursorDeltaY);
            return !ship.usePlayerPerspective();
        }

        return false;
    }

    // Modern NeoForge Network Handler approach[cite: 10, 11]
    // These should ideally be moved to their respective custom payload classes.
    public static void handleFirePacket(IPayloadContext context)
    {
        context.enqueueWork(() -> {
            var player = (ServerPlayer) context.player();
            var ship = getShip(player);

            if (ship != null && ship.isPilot(player))
                ship.acceptFireInput();
        });
    }

    @Override
    public void recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket packet)
    {
        super.recreateFromPacket(packet);
        var sound = getExteriorSoundEvent();
        if (sound != null)
            SoundHelper.playShipExteriorSound(this, sound);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance)
    {
        return true; // Replaces shouldRender[cite: 10]
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state)
    {
    }

    @Override
    protected void playSwimSound(float volume)
    {
    }

    protected SoundEvent getExteriorSoundEvent()
    {
        return null;
    }

    // Assumes passing the Quaternionf inside a custom payload
    public static void handleRotationPacket(Quaternionf q, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            var player = (ServerPlayer) context.player();
            var ship = getShip(player);

            if (ship != null && ship.isPilot(player))
                ship.setRotation(q);
        });
    }

    // Assumes passing the controls short inside a custom payload
    public static void handleControlPacket(short controls, IPayloadContext context)
    {
        context.enqueueWork(() -> {
            var player = (ServerPlayer) context.player();
            var ship = getShip(player);

            if (ship != null && ship.isPilot(player))
                ship.acceptControlInput(ShipControls.unpack(controls));
        });
    }

    public static ShipEntity getShip(LivingEntity player)
    {
        if (player == null)
            return null;

        var vehicle = player.getVehicle();

        if (vehicle instanceof ShipEntity ship)
        {
            if (ship.getControllingPassenger() == player)
                return ship;
        }

        return null;
    }

    @Override
    public AABB getBoundingBoxForCulling()
    {
        return getBoundingBox().inflate(5); // Replaces getVisibilityBoundingBox[cite: 10]
    }

    @Override
    public boolean isPushable()
    {
        return true;
    }

    @Override
    public boolean isPickable()
    {
        return !this.isRemoved(); // Replaces canHit[cite: 10]
    }

    @Override
    public boolean hurt(DamageSource source, float amount)
    {
        if (this.isInvulnerableTo(source) || source.getEntity() == null)
            return false;

        this.kill();

        return true;
    }

    // 1.21.1 Standard for Data Trackers[cite: 10]
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder)
    {
        builder.define(ROTATION, new Quaternionf(QuatUtil.IDENTITY));
        builder.define(THROTTLE, 0f);
        builder.define(CONTROL_BITS, (short)0);
        builder.define(SHIELD_BITS, (short)0);
        builder.define(FUEL_BITS, 0);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        if (tag.contains("rotation"))
            setRotation(QuatUtil.getQuaternion(tag, "rotation"));
        setThrottle(tag.getFloat("throttle"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        QuatUtil.putQuaternion(tag, "rotation", getRotation());
        tag.putFloat("throttle", getThrottle());
    }

    @OnlyIn(Dist.CLIENT)
    public ChaseCam getCamera()
    {
        if (camera == null)
            camera = new ChaseCam();

        return camera;
    }

    public boolean usePlayerPerspective()
    {
        return false;
    }

    public boolean isPilot(Player player)
    {
        return getControllingPassenger() == player;
    }

    public boolean useMouseInput(Player player)
    {
        return isPilot(player);
    }

    @Override
    public void tick()
    {
        viewPrevRotation = new Quaternionf(viewRotation);

        super.tick();
        if (this.isControlledByLocalInstance())
            this.syncPacketPositionCodec(this.getX(), this.getY(), this.getZ());

        viewRotation = new Quaternionf(getRotation());

        EntityUtil.updateEulerRotation(this, viewRotation);

        if (level().isClientSide)
        {
            if (PSWGClient.isShipClientControlled(this))
            {
                clientPrevRotation = new Quaternionf(clientRotation);
                clientRotation = new Quaternionf(clientInstRotation);
            }
            else
            {
                clientPrevRotation = new Quaternionf(viewPrevRotation);
                clientRotation = new Quaternionf(viewRotation);
            }

            var camera = getCamera();
            camera.tick(this);
        }

        var throttle = getThrottle();

        var pilot = getControllingPassenger();
        if (pilot instanceof Player)
        {
            var controls = getControls();

            if (controls.contains(ShipControls.THROTTLE_UP))
                throttle += 0.3f;
            if (controls.contains(ShipControls.THROTTLE_DOWN))
                throttle -= 0.3f;

            throttle = Mth.clamp(throttle, 0, 3);
            setThrottle(throttle);
        }
        else if (throttle > 0)
        {
            if (throttle > 0.5f)
                throttle *= 0.8f;
            else
                throttle = 0;

            throttle = Mth.clamp(throttle, 0, 3);
            setThrottle(throttle);
        }

        var forward = getThrottleVelocity(throttle);
        setDeltaMovement(forward); // Replaces setVelocity[cite: 10]

        move(MoverType.SELF, getDeltaMovement());
    }

    protected Vec3 getThrottleVelocity(float throttle)
    {
        return QuatUtil.rotate(MathUtil.V3D_NEG_Z, getRotation()).scale(throttle);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand)
    {
        var config = Resources.CONFIG.get();
        if (!player.hasPermissions(config.server.vehiclePermissionLevel))
            return InteractionResult.FAIL;

        if (player.isSecondaryUseActive())
            return InteractionResult.FAIL;
        else
            return !this.level().isClientSide && player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    protected int getMaxPassengers()
    {
        return 1;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger)
    {
        return this.getPassengers().size() < getMaxPassengers();
    }

    public Vec3 getPassengerSocket(int passengerIndex)
    {
        return new Vec3(0, 0, 3);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction)
    {
        if (this.hasPassenger(passenger))
        {
            var q = getRotation();

            var vec3 = getPassengerSocket(this.getPassengers().indexOf(passenger));
            vec3 = QuatUtil.rotate(vec3, q);

            if (!usePlayerPerspective())
                EntityUtil.updateEulerRotation(passenger, q);
            else
                passenger.setYBodyRot(this.getYRot());

            moveFunction.accept(passenger, this.getX() + vec3.x, this.getY() + vec3.y, this.getZ() + vec3.z);
        }
    }

    protected void copyEntityData(Entity entity)
    {
        this.setXRot(entity.getXRot());
        this.setYRot(entity.getYRot());
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger()
    {
        if (getFirstPassenger() instanceof LivingEntity livingEntity)
            return livingEntity;
        return null;
    }

    protected void tickControlledAnim(EntityDataAccessor<Byte> data, byte animLength, boolean keyInput)
    {
        var anim = entityData.get(data);
        anim = TrackedAnimationValue.tick(anim);

        if (getControllingPassenger() instanceof Player)
        {
            if (keyInput && TrackedAnimationValue.isStopped(anim))
                anim = TrackedAnimationValue.startToggled(anim, animLength);
        }

        entityData.set(data, anim);
    }

    public EnumSet<ShipControls> getControls()
    {
        return ShipControls.unpack(getEntityData().get(CONTROL_BITS));
    }

    public void setControls(EnumSet<ShipControls> controls)
    {
        getEntityData().set(CONTROL_BITS, ShipControls.pack(controls));
    }

    public float getThrottle()
    {
        return getEntityData().get(THROTTLE);
    }

    public void setThrottle(float t)
    {
        if (t != getThrottle())
            getEntityData().set(THROTTLE, t);
    }

    public Quaternionf getRotation()
    {
        return getEntityData().get(ROTATION);
    }

    public void setRotation(Quaternionf q)
    {
        q.normalize();
        getEntityData().set(ROTATION, q);
    }

    @OnlyIn(Dist.CLIENT)
    public Quaternionf getViewRotation(float t)
    {
        var start = clientPrevRotation;
        var end = clientRotation;
        return QuatUtil.slerp(start, end, t);
    }

    public void acceptControlInput(EnumSet<ShipControls> controls)
    {
        if (ShipControls.pack(controls) == getEntityData().get(CONTROL_BITS))
            return;

        setControls(controls);

        if (this.level().isClientSide)
        {
            // NeoForge Modern Packet sending[cite: 10, 11]
            // Note: Replace ShipControlsPayload with your 1.21.1 CustomPacketPayload implementation
            PacketDistributor.sendToServer(new SwgPackets.ShipControlsPayload(ShipControls.pack(controls)));
        }
    }

    public void acceptFireInput()
    {
    }

    @OnlyIn(Dist.CLIENT)
    public void acceptMouseInput(double mouseDx, double mouseDy)
    {
        if (this.firstTick)
            return;

        if (!allowPitchMovement())
            mouseDy = 0;

        var rotation = new Quaternionf(clientInstRotation);
        if (firstRotationUpdate)
        {
            rotation = new Quaternionf(getRotation());
            firstRotationUpdate = false;
        }

        var shipRollPriority = Resources.CONFIG.get().input.shipRollPriority;

        if (!usePlayerPerspective() && PSWGClient.KEY_SHIP_INPUT_MODE_OVERRIDE.isDown())
            shipRollPriority = !shipRollPriority;

        if (shipRollPriority)
            rotation.rotateZ(MathUtil.toRadians(-(float)mouseDx * 0.1f));
        else
        {
            rotation.rotateY(MathUtil.toRadians(-(float)mouseDx * 0.1f));

            var ea = QuatUtil.toEulerAngles(rotation);
            var currentUp = QuatUtil.rotate(new Vector3d(0, 1, 0), rotation);
            var currentForward = QuatUtil.rotate(new Vector3d(0, 0, 1), rotation);

            var targetYaw = ea.getYaw();
            var targetPitch = ea.getPitch();

            if (currentUp.y < 0)
                targetPitch += 180;

            var zeroRollUp = MathUtil.anglesToLook(targetPitch - 90, targetYaw);

            var angle = Math.acos(currentUp.dot(zeroRollUp) / (currentUp.length() * zeroRollUp.length()));

            if (Math.abs(angle) > 0.01)
            {
                Vec3 vec3 = currentUp.cross(zeroRollUp).normalize();
                var zeroRollRotation = new Quaternionf().rotationAxis((float)angle, vec3.toVector3f());
                zeroRollRotation.mul(rotation);

                var maxRotationSpeed = (float)(1 - Math.abs(Math.asin(currentForward.y) / Mth.HALF_PI));
                maxRotationSpeed *= maxRotationSpeed;

                rotation = QuatUtil.slerp(rotation, zeroRollRotation, 0.005f * maxRotationSpeed);
            }
        }

        rotation.rotateX(MathUtil.toRadians(-(float)mouseDy * 0.1f));

        setRotation(rotation);

        clientInstRotation = new Quaternionf(rotation);

        // NeoForge Modern Packet sending[cite: 10, 11]
        // Note: Replace ShipRotationPayload with your 1.21.1 CustomPacketPayload implementation
        PacketDistributor.sendToServer(new SwgPackets.ShipRotationPayload(rotation));
    }

    protected boolean allowPitchMovement()
    {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean acceptLeftClick(Player player)
    {
        if (!isPilot(player))
            return false;

        // NeoForge Modern Packet sending[cite: 10, 11]
        PacketDistributor.sendToServer(new SwgPackets.ShipFirePayload());
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    public float getCameraLerp()
    {
        return Mth.clamp(Resources.CONFIG.get().view.shipCameraStiffness, 0.1f, 1f);
    }
}