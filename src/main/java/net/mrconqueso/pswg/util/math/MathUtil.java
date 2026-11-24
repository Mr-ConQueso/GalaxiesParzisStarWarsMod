package net.mrconqueso.pswg.util.math;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.joml.*;

import java.lang.Math;

public class MathUtil
{
	public static final int TICKS_PER_SECOND = 20;

	public static final float fPI = (float)Math.PI;

	public static final double ONE_OVER_GOLDEN_RATIO = 0.61803398875;

	public static final float SPEED_OF_SOUND = 275f / TICKS_PER_SECOND; // m/tick

	public static final Vector3d V3D_POS_X = new Vector3d(1, 0, 0);
	public static final Vector3d V3D_NEG_X = new Vector3d(-1, 0, 0);
	public static final Vector3d V3D_POS_Y = new Vector3d(0, 1, 0);
	public static final Vector3d V3D_NEG_Y = new Vector3d(0, -1, 0);
	public static final Vector3d V3D_POS_Z = new Vector3d(0, 0, 1);
	public static final Vector3d V3D_NEG_Z = new Vector3d(0, 0, -1);

	public static final Vector3f V3F_POS_X = new Vector3f(1, 0, 0);
	public static final Vector3f V3F_NEG_X = new Vector3f(-1, 0, 0);
	public static final Vector3f V3F_POS_Y = new Vector3f(0, 1, 0);
	public static final Vector3f V3F_NEG_Y = new Vector3f(0, -1, 0);
	public static final Vector3f V3F_POS_Z = new Vector3f(0, 0, 1);
	public static final Vector3f V3F_NEG_Z = new Vector3f(0, 0, -1);

	public static final Matrix4f MAT4_IDENTITY = new Matrix4f();
	public static final Matrix4f MAT4_SCALE_10_16THS = new Matrix4f().scale(10 / 16f, 10 / 16f, 10 / 16f);

	public static float fract(double d)
	{
		return (float)(d - Math.floor(d));
	}

	public static Vector2f fract(Vector2f v)
	{
		return new Vector2f(fract(v.x), fract(v.y));
	}

	public static Vector3d fract(Vector3d v)
	{
		return new Vector3d(fract(v.x), fract(v.y), fract(v.z));
	}

	public static double seed(double d, long seed)
	{
		return Double.longBitsToDouble(Double.doubleToLongBits(d) ^ seed);
	}

	public static Vector2f floor(Vector2f v)
	{
		return new Vector2f((float)Math.floor(v.x), (float)Math.floor(v.y));
	}

	public static Vector3d floor(Vector3d v)
	{
		return new Vector3d(Math.floor(v.x), Math.floor(v.y), Math.floor(v.z));
	}

	public static Vector3i floorInt(Vector3d v) {
		return new Vector3i(Mth.floor(v.x), Mth.floor(v.y), Mth.floor(v.z));
	}

	public static Vector3d lerp(float tickDelta, Vector3d a, Vector3d b)
	{
		return new Vector3d(Mth.lerp(tickDelta, a.x, b.x), Mth.lerp(tickDelta, a.y, b.y), Mth.lerp(tickDelta, a.z, b.z));
	}

	public static Vector2f add(Vector2f a, Vector2f b)
	{
		return new Vector2f(a.x + b.x, a.y + b.y);
	}

	public static Vector2f sub(Vector2f a, Vector2f b)
	{
		return new Vector2f(a.x - b.x, a.y - b.y);
	}

	public static double length(Vector2f v)
	{
		return Math.sqrt(v.x * v.x + v.y * v.y);
	}

	public static int clamp(int i, int min, int max)
	{
		return Math.max(min, Math.min(i, max));
	}

	public static float remap(float x, float iMin, float iMax, float oMin, float oMax)
	{
		return (x - iMin) / (iMax - iMin) * (oMax - oMin) + oMin;
	}

	// TODO: move doppler handling to OpenAL through SoundSystem's updateListenerPosition call?
	public static float calculateDopplerShift(Entity a, Entity b)
	{
		var velA = a.position().subtract(a.xOld, a.yOld, a.zOld);
		var velB = b.position().subtract(b.xOld, b.yOld, b.zOld);

		var posA = a.getEyePosition();
		var posB = b.getEyePosition();

		var relativeSpeed = posA.distanceTo(posB) - posA.add(velA).distanceTo(posB.add(velB));

		return Mth.clamp((float)(relativeSpeed / SPEED_OF_SOUND), -1.0F, 1.0F);
	}

	public static Vector3d project(Vector3d v, Vector3d onto)
	{
		return onto.mul(v.dot(onto) / onto.dot(onto));
	}

	@NotNull
	public static Vector3d anglesToLook(float pitch, float yaw)
	{
		var x = -Mth.sin(yaw * Mth.RAD_TO_DEG) * Mth.cos(pitch * Mth.RAD_TO_DEG);
		var y = -Mth.sin(pitch * Mth.RAD_TO_DEG);
		var z = Mth.cos(yaw * Mth.RAD_TO_DEG) * Mth.cos(pitch * Mth.RAD_TO_DEG);

		return new Vector3d(x, y, z).normalize();
	}

	@NotNull
	public static EulerAngle lookToAngles(Vector3d forward)
	{
		forward = forward.normalize();

		var yaw = -(float)Math.atan2(forward.x, forward.z);
		var pitch = -(float)Math.asin(forward.y);

		return new EulerAngle(pitch * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0);
	}

	public static boolean rectContains(int l, int t, int w, int h, double x, double y)
	{
		return x >= l && x < l + w && y >= t && y < t + h;
	}

	public static float toRadians(float degrees)
	{
		return degrees * Mth.RAD_TO_DEG;
	}

	public static Vector3d transform(Vector3d v, Matrix4f transform)
	{
		var Vector3d = new Vector3d(v.x, v.y, v.z).mulPosition(transform);
		return new Vector3d(Vector3d.x, Vector3d.y, Vector3d.z);
	}

	public static void scalePos(PoseStack stack, float x, float y, float z)
	{
		var entry = stack.last();
		entry.pose().scale(x, y, z);
	}

	public static Quaternionf getRotation(Direction direction)
	{
		return switch (direction)
		{
			case DOWN -> new Quaternionf().rotationXYZ(0, 0, (float)(Math.PI / -2));
			case UP -> new Quaternionf().rotationXYZ(0, 0, (float)(Math.PI / 2));
			case NORTH -> new Quaternionf().rotationXYZ(0, (float)(Math.PI / 2), 0);
			case SOUTH -> new Quaternionf().rotationXYZ(0, (float)(Math.PI / -2), 0);
			case WEST -> new Quaternionf().rotationXYZ(0, (float)Math.PI, 0);
			case EAST -> new Quaternionf().rotationXYZ(0, 0, 0);
		};
	}

	public static Vector3d reflect(Vector3d incident, Vector3d normal)
	{
		var reflection = normal.mul(2 * normal.dot(incident)).sub(incident);
		return reflection.mul(-1);
	}
}
