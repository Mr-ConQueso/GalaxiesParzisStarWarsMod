package dev.pswg.particle.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

final class SurfaceDecalParticle extends TextureSheetParticle
{
	private final BlockPos host;
	private final Quaternionf facing;
	private final float heat;

	private SurfaceDecalParticle(ClientLevel level, double x, double y, double z, Vec3 encodedNormal,
	                             SpriteSet sprites, float size, int minLife, int maxLife, float rotationStep)
	{
		super(level, x, y, z);
		var normal = encodedNormal.normalize();
		host = BlockPos.containing(new Vec3(x, y, z).subtract(normal.scale(0.1)));
		facing = new Quaternionf().rotationTo(0, 0, 1, (float) normal.x, (float) normal.y, (float) normal.z);
		if (rotationStep > 0F)
			facing.rotateZ(rotationStep < Mth.TWO_PI ? random.nextInt(4) * rotationStep : random.nextFloat() * Mth.TWO_PI);
		heat = (float) Math.min(1, encodedNormal.length());
		quadSize = size;
		lifetime = random.nextIntBetweenInclusive(minLife, maxLife);
		hasPhysics = false;
		pickSprite(sprites);
		updateHeat();
	}

	static ParticleProvider<SimpleParticleType> factory(SpriteSet sprites, float size, int minLife, int maxLife, float rotationStep)
	{
		return (type, level, x, y, z, dx, dy, dz) ->
				new SurfaceDecalParticle(level, x, y, z, dx == 0 && dy == 0 && dz == 0
						? new Vec3(0, 1, 0) : new Vec3(dx, dy, dz), sprites, size, minLife, maxLife, rotationStep);
	}

	@Override
	public FacingCameraMode getFacingCameraMode()
	{
		return (rotation, camera, partialTick) -> rotation.set(facing);
	}

	@Override
	public ParticleRenderType getRenderType()
	{
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public int getLightColor(float partial)
	{
		int light = super.getLightColor(partial);
		int block = Math.min(15, LightTexture.block(light) + (int) (15 * (age + partial) / lifetime));
		return LightTexture.pack(block, LightTexture.sky(light));
	}

	@Override
	public void tick()
	{
		if (++age >= lifetime || level.isEmptyBlock(host))
		{
			remove();
			return;
		}
		setAlpha(Math.min(1F, 2F * (1F - (float) age / lifetime)));
		updateHeat();
	}

	private void updateHeat()
	{
		float cooling = 5 * Mth.lerp(Mth.clamp(2F * age / lifetime, 0, 1), heat, 1);
		float squared = cooling * cooling;
		setColor(Mth.clamp(-0.0581F * squared + 0.0982F * cooling + 0.963F, 0, 1),
				Mth.clamp(0.0292F * squared - 0.314F * cooling + 0.837F, 0, 1),
				Mth.clamp(0.0166F * squared - 0.152F * cooling + 0.348F, 0, 1));
	}
}
