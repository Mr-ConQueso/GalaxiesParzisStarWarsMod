package net.mrconqueso.pswg.util.math;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import org.joml.Math;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public class QuatUtil
{
	public static final Quaternionf ROT_X_POS45 = new Quaternionf().rotationX(Math.toRadians(45));

	public static final Quaternionf ROT_Y_POS10 = new Quaternionf().rotationY(Math.toRadians(10));
	public static final Quaternionf ROT_Y_POS90 = new Quaternionf().rotationY(Math.toRadians(90));
	public static final Quaternionf ROT_Y_180 = new Quaternionf().rotationY(Math.toRadians(180));

	public static final Quaternionf ROT_Z_POS80 = new Quaternionf().rotationZ(Math.toRadians(80));

	public static final Quaternionf IDENTITY = new Quaternionf();

	private static final Vector3d UP = new Vector3d(0, 1, 0);
	private static final Vector3d FORWARD = new Vector3d(0, 0, 1);

	public static EulerAngle toEulerAngles(Quaternionf q)
	{
		var forward = rotate(MathUtil.V3D_NEG_Z, q);
		return MathUtil.lookToAngles(forward);
	}

	public static Quaternionf lookAt(Vector3d sourcePoint, Vector3d destPoint)
	{
		var forwardVector = destPoint.sub(sourcePoint).normalize();

		var dot = FORWARD.dot(forwardVector);

		if (Math.abs(dot - (-1.0f)) < 0.000001f)
			return new Quaternionf().rotationAxis(Mth.PI, new Vector3f((float)UP.x, (float)UP.y, (float)UP.z));
		if (Math.abs(dot - (1.0f)) < 0.000001f)
			return new Quaternionf(QuatUtil.IDENTITY);

		var rotAngle = Math.acos(dot);
		var rotAxis = FORWARD.cross(forwardVector);
		rotAxis = rotAxis.normalize();

		return new Quaternionf().rotationAxis((float)rotAngle, new Vector3f((float)rotAxis.x, (float)rotAxis.y, (float)rotAxis.z));
	}

	public static Vector3d rotate(Vector3d self, Quaternionf q)
	{
		var u = new Vector3d(q.x, q.y, q.z);
		var s = q.w;
		return u.mul(2.0f * u.dot(self))
		        .add(self.mul(s * s - u.dot(u)))
		        .add(u.cross(self).mul(2.0f * s));
	}

	public static void putQuaternion(CompoundTag tag, String key, Quaternionf q)
	{
		ListTag list = new ListTag();
		list.add(FloatTag.valueOf(q.w));
		list.add(FloatTag.valueOf(q.x));
		list.add(FloatTag.valueOf(q.y));
		list.add(FloatTag.valueOf(q.z));
		tag.put(key, list);
	}

	public static Quaternionf getQuaternion(CompoundTag tag, String key)
	{
		var list = tag.getList(key, Tag.TAG_FLOAT);
		return new Quaternionf(list.getFloat(1), list.getFloat(2), list.getFloat(3), list.getFloat(0));
	}

	public static void rotateTowards(Quaternionf self, Vector3d orientation, float speed)
	{
		self.normalize();
		var vec2 = rotate(orientation, self);
		var cross = orientation.cross(vec2).mul(-1.0);
		var axis = cross.normalize();
		var f1 = (float)cross.length();
		var axisf = new Vector3f((float)axis.x, (float)axis.y, (float)axis.z);
		var other = new Quaternionf().rotationAxis(speed * f1, axisf);
		other.mul(self);
		self.set(other);
	}

	public static Quaternionf getRotationTowards(Vector3d from, Vector3d to)
	{
		var cross = from.cross(to);
		var w = (float)(Math.sqrt(from.lengthSquared() * to.lengthSquared()) + from.dot(to));
		var q = new Quaternionf(w, (float)cross.x, (float)cross.y, (float)cross.z);
		q.normalize();
		return q;
	}

	/**
	 * Finds a global vector in local terms
	 */
	public static Vector3d project(Vector3d v, Quaternionf q)
	{
		var c = new Quaternionf(q);
		c.conjugate();
		return rotate(v, c);
	}

	public static Quaternionf slerp(Quaternionf start, Quaternionf end, float t)
	{
		// Only unit quaternions are valid rotations.
		// Normalize to avoid undefined behavior.
		start.normalize();
		end.normalize();

		// Compute the cosine of the angle between the two vectors.
		double dot = start.dot(end);

		// If the dot product is negative, slerp won't take
		// the shorter path. Note that end and -end are equivalent when
		// the negation is applied to all four components. Fix by
		// reversing one quaternion.
		if (dot < 0.0f)
		{
			end.scale(-1);
			dot = -dot;
		}

		if (dot > 0.9995)
		{
			// If the inputs are too close for comfort, linearly interpolate
			// and normalize the result.

			var f = 1 - t;

			var a = f * start.w + t * end.w;
			var b = f * start.x + t * end.x;
			var c = f * start.y + t * end.y;
			var d = f * start.z + t * end.z;

			var result = new Quaternionf(b, c, d, a);
			result.normalize();
			return result;
		}

		// Since dot is in range [0, DOT_THRESHOLD], acos is safe
		var theta_0 = Math.acos(dot);        // theta_0 = angle between input vectors
		var theta = theta_0 * t;          // theta = angle between start and result
		var sin_theta = Math.sin(theta);     // compute this value only once
		var sin_theta_0 = Math.sin(theta_0); // compute this value only once

		var f1 = Math.cos(theta) - dot * sin_theta / sin_theta_0;  // == sin(theta_0 - theta) / sin(theta_0)
		var f2 = sin_theta / sin_theta_0;

		var a = (float)(f1 * start.w + f2 * end.w);
		var b = (float)(f1 * start.x + f2 * end.x);
		var c = (float)(f1 * start.y + f2 * end.y);
		var d = (float)(f1 * start.z + f2 * end.z);

		var result = new Quaternionf(b, c, d, a);
		result.normalize();
		return result;
	}
}
