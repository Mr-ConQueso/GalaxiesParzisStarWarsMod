package dev.pswg.particle.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class SparkParticle extends SimpleAnimatedParticle
{
	enum Style { BLASTER, FRAGMENTATION, TRIPWIRE }

	private final Style style;
	private final Vector3f direction = new Vector3f(0, 0, 1);

	SparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, Style style)
	{
		super(level, x, y, z, sprites, 0F);
		this.style = style;
		friction = 1;
		switch (style)
		{
			case BLASTER -> {
				quadSize = 0.06F + random.nextFloat() * 0.05F;
				lifetime = (int) (random.nextFloat() * 10 + 5);
				setParticleSpeed(dx, dy, dz);
				setColor(1, 0, 0);
			}
			case FRAGMENTATION -> {
				lifetime = (int) (random.nextFloat() * 20 + 10);
				quadSize = 0.15F + random.nextFloat() * 0.25F;
				setParticleSpeed(dx, dy, dz);
				setColor(0, 0, 1);
			}
			case TRIPWIRE -> {
				friction = 0.91F;
				lifetime = 3;
				quadSize = 0.0625F;
				setAlpha(0.1F);
				setColor(0.05F + random.nextInt(101) / 1000F, 0.65F + random.nextInt(101) / 1000F,
						0.9F + random.nextInt(101) / 1000F);
			}
		}
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick()
	{
		super.tick();
		if (!isAlive())
			return;
		if (style == Style.TRIPWIRE)
		{
			yd -= 0.00001;
			return;
		}
		float time = (float) age / lifetime;
		if (level.isEmptyBlock(BlockPos.containing(x, y, z)))
			yd -= style == Style.BLASTER ? 0.0245 : 0.0145;
		if (style == Style.BLASTER)
			setColor(1, brighten(3 * time), brighten(1.5F * time));
		else
			setColor(brighten(0.375F * time), brighten(0.75F * time), 1);
	}

	private static float brighten(float progress)
	{
		float rest = Math.max(0, 1 - progress);
		return 1 - rest * rest * rest;
	}

	@Override
	public void render(VertexConsumer output, Camera camera, float partial)
	{
		if (xd * xd + yd * yd + zd * zd > 1e-12)
			direction.set((float) xd, (float) yd, (float) zd).normalize();
		var rotation = direction.z < -0.999999F ? new Quaternionf().rotationY(Mth.PI)
				: new Quaternionf().rotationTo(0, 0, 1, direction.x, direction.y, direction.z);
		rotation.rotateY(Mth.HALF_PI).rotateX(Mth.PI / 4);
		if (style == Style.BLASTER)
		{
			renderBlasterPlane(output, camera, rotation, partial);
			renderBlasterPlane(output, camera, rotation.rotateX(-Mth.HALF_PI), partial);
			return;
		}
		renderRotatedQuad(output, camera, rotation, partial);
		renderRotatedQuad(output, camera, rotation.rotateX(Mth.PI), partial);
		renderRotatedQuad(output, camera, rotation.rotateX(Mth.HALF_PI), partial);
		renderRotatedQuad(output, camera, rotation.rotateX(Mth.PI), partial);
	}

	private void renderBlasterPlane(VertexConsumer output, Camera camera, Quaternionf rotation, float partial)
	{
		var eye = camera.getPosition();
		float dx = (float) (Mth.lerp(partial, xo, x) - eye.x);
		float dy = (float) (Mth.lerp(partial, yo, y) - eye.y);
		float dz = (float) (Mth.lerp(partial, zo, z) - eye.z);
		var vertex = new Vector3f();
		// Both windings keep the leading edge of the legacy sprite on the same end of the spark.
		for (int i = 0; i < 8; i++)
		{
			int corner = i < 4 ? i : 7 - i;
			boolean left = corner < 2;
			boolean top = corner == 1 || corner == 2;
			vertex.set(left ? -quadSize : quadSize, top ? quadSize : -quadSize, 0).rotate(rotation).add(dx, dy, dz);
			output.addVertex(vertex.x, vertex.y, vertex.z).setUv(left ? getU1() : getU0(), top ? getV0() : getV1())
					.setColor(rCol, gCol, bCol, alpha).setLight(getLightColor(partial));
		}
	}
}
