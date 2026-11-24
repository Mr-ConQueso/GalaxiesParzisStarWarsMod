package net.mrconqueso.pswg.client.blaster;

import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.mrconqueso.pswg.container.SwgSounds;
import net.mrconqueso.pswg.entity.custom.BlasterBoltEntity;
import net.mrconqueso.pswg.util.sound.DopplerSoundInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlasterBoltHissSoundInstance extends DopplerSoundInstance
{
	public BlasterBoltHissSoundInstance(BlasterBoltEntity entity)
	{
		super(entity, SwgSounds.Blaster.HISS, SoundSource.PLAYERS, RandomSource.create());
		this.looping = true;
		this.delay = 0;
		this.volume = 0.0f;
	}

	@Override
	public boolean canPlaySound() // Replaces canPlay()
	{
		return !this.source.isSilent();
	}

	@Override
	public boolean canStartSilent() // Replaces shouldAlwaysPlay()
	{
		return true;
	}

	@Override
	public void tick()
	{
		super.tick();

		if (this.source.isRemoved())
		{
			this.stop(); // Replaces setDone()
			return;
		}

		this.volume = 1;
		this.x = (float)this.source.getX();
		this.y = (float)this.source.getY();
		this.z = (float)this.source.getZ();
	}
}