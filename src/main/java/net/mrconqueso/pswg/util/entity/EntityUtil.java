package net.mrconqueso.pswg.util.entity;

import net.minecraft.world.entity.Entity;
import net.mrconqueso.pswg.util.math.MathUtil;
import net.mrconqueso.pswg.util.math.QuatUtil;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Arrays;

public class EntityUtil
{
	public static NbtCompound serializeEntity(Entity e)
	{
		var nbtCompound = new NbtCompound();
		nbtCompound.putString("id", EntityType.getId(e.getType()).toString());
		e.writeNbt(nbtCompound);
		return nbtCompound;
	}

	public static EntityHitResult raycastEntities(Class<? extends Entity> clazz, Vec3d startPos, Vec3d fromDir, double distance, Entity fromEntity, Entity[] exclude)
	{
		Entity pointedEntity = null;
		Vec3d hitLocation = null;

		var blacklist = Arrays.asList(exclude);

		fromDir = fromDir.normalize();

		var endPos = startPos.add(fromDir.multiply(distance));
		var list = fromEntity.getWorld().getEntitiesByClass(clazz, fromEntity.getBoundingBox().stretch(fromDir.x * distance, fromDir.y * distance, fromDir.z * distance).expand(1, 1, 1), EntityPredicates.EXCEPT_SPECTATOR);

		for (var entity : list)
		{
			if (blacklist.contains(entity))
				continue;

			if (entity.canHit())
			{
				var box = entity.getBoundingBox();
				var hitvec = box.raycast(startPos, endPos);

				if (hitvec.isPresent())
				{
					var distanceTo = startPos.distanceTo(hitvec.get());

					if (distanceTo < distance)
					{
						pointedEntity = entity;
						distance = distanceTo;
						hitLocation = hitvec.get();
					}
				}
			}
		}

		if (pointedEntity != null)
			return new EntityHitResult(pointedEntity, hitLocation);

		return null;
	}

	public static ArrayList<Entity> raycastEntitiesCone(Vec3d startPos, Vec3d fromDir, double maxAngleRad, double distance, Entity fromEntity, Entity[] exclude)
	{
		var blacklist = Arrays.asList(exclude);

		fromDir = fromDir.normalize();

		var list = fromEntity.getWorld().getEntitiesByClass(LivingEntity.class, fromEntity.getBoundingBox().stretch(fromDir.x * distance, fromDir.y * distance, fromDir.z * distance).expand(1, 1, 1), EntityPredicates.EXCEPT_SPECTATOR);

		var hit = new ArrayList<Entity>();

		for (var entity : list)
		{
			if (blacklist.contains(entity))
				continue;

			if (entity.canHit())
			{
				var entityDirVec = entity.getPos().subtract(startPos).normalize();
				if (Math.acos(entityDirVec.dotProduct(fromDir)) > maxAngleRad)
					continue;

				hit.add(entity);
			}
		}

		return hit;
	}

	public static BlockHitResult raycastBlocks(Vec3d startPos, Vec3d fromDir, double distance, Entity fromEntity, RaycastContext.ShapeType shapeType, RaycastContext.FluidHandling fluidHandling)
	{
		var end = startPos.add(fromDir.multiply(distance));
		return fromEntity.getWorld().raycast(new RaycastContext(startPos, end, shapeType, fluidHandling, fromEntity));
	}

	public static void setVelocityFromAngles(Entity entity, float pitch, float yaw, float scalar)
	{
		var look = MathUtil.anglesToLook(pitch, yaw);
		entity.setVelocity(scalar * look.x, scalar * look.y, scalar * look.z);
	}

	public static void updateEulerRotation(Entity entity, Quaternionf rotation)
	{
		entity.prevPitch = entity.getPitch();
		entity.prevYaw = entity.getYaw();

		var eulerAngle = QuatUtil.toEulerAngles(rotation);
		entity.setYaw(eulerAngle.getYaw());
		entity.setPitch(eulerAngle.getPitch());

		while (entity.getPitch() - entity.prevPitch >= 180.0F)
		{
			entity.prevPitch += 360.0F;
		}

		while (entity.getYaw() - entity.prevYaw < -180.0F)
		{
			entity.prevYaw -= 360.0F;
		}

		while (entity.getYaw() - entity.prevYaw >= 180.0F)
		{
			entity.prevYaw += 360.0F;
		}
	}
}
