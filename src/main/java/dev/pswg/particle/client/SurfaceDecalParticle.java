package dev.pswg.particle.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
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
	private final Vec3 normal;
	private final Quaternionf facing;
	private final float heat;
	private final boolean blaster;

	private SurfaceDecalParticle(ClientLevel level, double x, double y, double z, Vec3 encodedNormal,
	                             SpriteSet sprites, float size, int minLife, int maxLife, int variants, boolean blaster)
	{
		super(level, x, y, z);
		float magnitude = Mth.clamp((float) encodedNormal.length(), 0, 1);
		normal = magnitude == 0 ? new Vec3(0, 1, 0) : encodedNormal.normalize();
		host = BlockPos.containing(new Vec3(x, y, z).subtract(normal.scale(0.1)));
		facing = normal.z == -1 ? new Quaternionf().rotationY(Mth.PI)
				: new Quaternionf().rotationTo(0, 0, 1, (float) normal.x, (float) normal.y, (float) normal.z);
		facing.rotateZ(blaster ? random.nextFloat() * Mth.PI : facing.angle());
		heat = magnitude;
		this.blaster = blaster;
		quadSize = size;
		lifetime = random.nextIntBetweenInclusive(minLife, maxLife);
		hasPhysics = false;
		setSprite(sprites.get(random.nextInt(variants), variants));
		updateHeat();
	}

	static ParticleProvider<SimpleParticleType> blasterFactory(SpriteSet sprites)
	{
		return (type, level, x, y, z, dx, dy, dz) ->
				new SurfaceDecalParticle(level, x, y, z, new Vec3(dx, dy, dz), sprites, 0.1F, 200, 200, 3, true);
	}

	static ParticleProvider<SimpleParticleType> factory(SpriteSet sprites, float size, int minLife, int maxLife, int variants)
	{
		return (type, level, x, y, z, dx, dy, dz) ->
				new SurfaceDecalParticle(level, x, y, z, new Vec3(dx, dy, dz), sprites, size, minLife, maxLife, variants, false);
	}

	@Override
	public void render(VertexConsumer output, Camera camera, float partial)
	{
		double offset = (1F - (float) age / lifetime) * 0.005F * quadSize;
		var eye = camera.getPosition();
		renderRotatedQuad(output, facing, (float) (x - eye.x + normal.x * offset),
				(float) (y - eye.y + normal.y * offset), (float) (z - eye.z + normal.z * offset), partial);
	}

	@Override
	public ParticleRenderType getRenderType()
	{
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	public int getLightColor(float partial)
	{
		return LightTexture.FULL_BRIGHT;
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
		float progress = Mth.clamp(2F * age / lifetime, 0, 1);
		// The 1.20.2 blaster encodes heat as the amount of cooling left. Gadget decals encode the starting point.
		float cooling = 5 * (blaster ? Mth.lerp(heat, 1, progress) : Mth.lerp(progress, heat, 1));
		float squared = cooling * cooling;
		setColor(Mth.clamp(-0.0581F * squared + 0.0982F * cooling + 0.963F, 0, 1),
				Mth.clamp(0.0292F * squared - 0.314F * cooling + 0.837F, 0, 1),
				Mth.clamp(0.0166F * squared - 0.152F * cooling + 0.348F, 0, 1));
	}
}
