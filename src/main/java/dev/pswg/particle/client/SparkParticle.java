package dev.pswg.particle.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;
import org.joml.Vector3f;

final class SparkParticle extends TextureSheetParticle
{
	enum Style { BLASTER, FRAGMENTATION, TRIPWIRE }

	private final SpriteSet sprites;
	private final Style style;
	private final Vector3f direction = new Vector3f(1, 0, 0);

	SparkParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites, Style style)
	{
		super(level, x, y, z);
		this.sprites = sprites;
		this.style = style;
		setParticleSpeed(dx, dy, dz);
		friction = 1;
		switch (style)
		{
			case BLASTER -> {
				lifetime = random.nextIntBetweenInclusive(5, 14);
				quadSize = 0.06F + random.nextFloat() * 0.05F;
				setColor(1, 0, 0);
			}
			case FRAGMENTATION -> {
				lifetime = random.nextIntBetweenInclusive(10, 29);
				quadSize = 0.15F + random.nextFloat() * 0.25F;
				setColor(0, 0, 1);
			}
			case TRIPWIRE -> {
				lifetime = 3;
				quadSize = 0.0625F;
				setAlpha(0.1F);
				setColor(0.05F + random.nextFloat() * 0.1F, 0.65F + random.nextFloat() * 0.1F, 0.9F + random.nextFloat() * 0.1F);
				hasPhysics = false;
			}
		}
		setSpriteFromAge(sprites);
	}

	@Override
	public void tick()
	{
		super.tick();
		if (!isAlive() || style == Style.TRIPWIRE)
			return;
		setSpriteFromAge(sprites);
		float time = (float) age / lifetime;
		setAlpha(1 - Math.max(0, time - 0.5F));
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
		var rotation = new Quaternionf().rotationTo(1, 0, 0, direction.x, direction.y, direction.z).rotateX(Mth.PI / 4);
		for (int side = 0; side < 4; side++)
		{
			renderRotatedQuad(output, camera, rotation, partial);
			rotation.rotateX(Mth.HALF_PI);
		}
	}

	@Override public int getLightColor(float partial) { return LightTexture.FULL_BRIGHT; }
	@Override public ParticleRenderType getRenderType() { return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT; }
}
