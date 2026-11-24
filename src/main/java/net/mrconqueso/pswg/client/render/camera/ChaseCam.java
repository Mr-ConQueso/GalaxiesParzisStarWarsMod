package net.mrconqueso.pswg.client.render.camera;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.entity.ShipEntity;
import net.mrconqueso.pswg.util.math.MathUtil;
import net.mrconqueso.pswg.util.math.QuatUtil;
import org.joml.Quaternionf;
import org.joml.Vector3d;

public class ChaseCam
{
	public Vector3d prevPos;
	public Vec3 pos;

	public ChaseCam()
	{
		prevPos = new Vector3d(Double.NaN, Double.NaN, Double.NaN);
		pos = new Vec3(Double.NaN, Double.NaN, Double.NaN);
	}

	public void tick(ShipEntity parent)
	{
		if (parent == null || parent.isRemoved())
			return;

		if (Double.isNaN(pos.x) || Double.isNaN(pos.y) || Double.isNaN(pos.z))
			pos = parent.position();

		prevPos = new Vector3d(pos.x, pos.y, pos.z);

		var world = parent.level();

		var lerpAmount = parent.getCameraLerp();

		var q = parent.getViewRotation(1);

		var camDistTarget = getCamDistTarget(parent, q);

		var camTargetPosition = parent.position().add(QuatUtil.rotate(new Vector3d(0, 0, camDistTarget), q));
		var camDpos = camTargetPosition.subtract(pos);

		var lerpPos = pos.add(camDpos.multiply(lerpAmount));

		var parentPos = parent.position();
		var dist = lerpPos.distanceTo(parentPos);
		var targetDist = dist;

		for (var i = 0; i < 8; ++i)
		{
			var f = (float)((i & 1) * 2 - 1);
			var g = (float)((i >> 1 & 1) * 2 - 1);
			var h = (float)((i >> 2 & 1) * 2 - 1);

			f *= 0.1F;
			g *= 0.1F;
			h *= 0.1F;

			var start = parentPos.add(f, g, h);
			var end = lerpPos.add(f, g, h);
			HitResult hitResult = world.ray(new RaycastContext(start, end, RaycastContext.ShapeType.VISUAL, RaycastContext.FluidHandling.NONE, parent));

			if (hitResult.getType() != HitResult.Type.MISS)
			{
				var d = hitResult.getPos().distanceTo(parentPos);
				if (d < dist)
					dist = (float)d;
			}
		}

		pos = MathUtil.lerp((float)(dist / targetDist), parentPos, lerpPos);
	}

	private float getCamDistTarget(ShipEntity parent, Quaternionf q)
	{
		var minecraft = MinecraftClient.getInstance();

		var perspective = minecraft.options.getPerspective();
		var scalar = 1;
		if (perspective == Perspective.FIRST_PERSON)
			return 0;
		else if (perspective == Perspective.THIRD_PERSON_FRONT)
			scalar = -1;
		var throttle = parent.getThrottle();

		var baseMult = Mth.clamp(Resources.CONFIG.get().view.shipCameraBaseDistance, 0.1f, 10f);
		var speedMult = Mth.clamp(Resources.CONFIG.get().view.shipCameraSpeedDistance, 0.1f, 10f);

		return scalar * (13 * baseMult + 3 * speedMult * throttle);
	}
}
