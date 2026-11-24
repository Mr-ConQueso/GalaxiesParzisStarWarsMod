package net.mrconqueso.pswg.util.math;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public record EntityHitResult(Entity entity, Vec3d hit)
{
}
