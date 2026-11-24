package net.mrconqueso.pswg.client.render.camera;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.mrconqueso.pswg.client.PSWGClient;
import net.mrconqueso.pswg.entity.ShipEntity;
import net.mrconqueso.pswg.util.entity.EntityUtil;
import org.joml.Quaternionf;

public class MutableCameraEntity extends Entity
{
	private boolean forceEnable = false;

	public MutableCameraEntity()
	{
		super(EntityType.ITEM, null);
	}

	@Override
	protected void initDataTracker()
	{

	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound tag)
	{

	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound tag)
	{

	}

	@Override
	public Packet<ClientPlayPacketListener> createSpawnPacket()
	{
		return null;
	}

	public boolean shouldForce()
	{
		return forceEnable;
	}

	public MutableCameraEntity with(World world, Vec3d position, Quaternionf rotation, boolean forceEnable)
	{
		this.setWorld(world);

		var pos = this.getPos();
		this.prevX = pos.x;
		this.prevY = pos.y;
		this.prevZ = pos.z;
		setPosition(position);

		this.prevPitch = this.getPitch();
		this.prevYaw = this.getYaw();
		EntityUtil.updateEulerRotation(this, rotation);

		this.forceEnable = forceEnable;

		return this;
	}

	public MutableCameraEntity with(ShipEntity parent, ChaseCam camera)
	{
		this.setWorld(parent.getWorld());

		this.prevX = camera.prevPos.x;
		this.prevY = camera.prevPos.y;
		this.prevZ = camera.prevPos.z;
		setPosition(camera.pos.x, camera.pos.y, camera.pos.z);

		this.prevPitch = this.getPitch();
		this.prevYaw = this.getYaw();
		EntityUtil.updateEulerRotation(this, parent.getViewRotation(PSWGClient.getTickDelta()));

		this.forceEnable = false;

		return this;
	}
}
