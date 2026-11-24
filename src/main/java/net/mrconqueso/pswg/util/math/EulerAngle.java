package net.mrconqueso.pswg.util.math;

/**
 * Represents rotational components in degrees,
 * consistent with Minecraft's Pitch/Yaw/Roll coordinate system.
 */
public class EulerAngle {
    public float pitch;
    public float yaw;
    public float roll;

    public EulerAngle(float pitch, float yaw, float roll) {
        this.pitch = pitch;
        this.yaw = yaw;
        this.roll = roll;
    }

    public static EulerAngle of(float pitch, float yaw, float roll) {
        return new EulerAngle(pitch, yaw, roll);
    }

    // Helper to get Pitch as radians, commonly needed for Quaternion math
    public float getPitchRad() {
        return (float) Math.toRadians(pitch);
    }

    // Helper to get Yaw as radians
    public float getYawRad() {
        return (float) Math.toRadians(yaw);
    }

    // Helper to get Roll as radians
    public float getRollRad() {
        return (float) Math.toRadians(roll);
    }
}