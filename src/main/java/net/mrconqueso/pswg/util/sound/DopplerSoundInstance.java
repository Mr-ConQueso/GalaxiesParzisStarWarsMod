package net.mrconqueso.pswg.util.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.mrconqueso.pswg.util.math.MathUtil;

public class DopplerSoundInstance extends AbstractTickableSoundInstance
{
	protected final Entity source;

	protected DopplerSoundInstance(Entity source, SoundEvent soundEvent, SoundSource soundCategory, RandomSource random)
	{
		super(soundEvent, soundCategory, random);
		this.source = source;
	}

	@Override
	public void tick()
	{
		var mc = Minecraft.getInstance();
		if (mc.getCameraEntity() != null)
			this.pitch = 1 + MathUtil.calculateDopplerShift(source, mc.getCameraEntity());
	}
}