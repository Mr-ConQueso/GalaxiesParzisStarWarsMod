package dev.pswg.container;

import dev.pswg.Galaxies;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoreSounds
{
	private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Galaxies.MODID);

	public static final DeferredHolder<SoundEvent, SoundEvent> METAL_PIPE = SOUNDS.register("metalpipe",
			() -> SoundEvent.createVariableRangeEvent(Galaxies.id("metalpipe")));

	public static void register(IEventBus bus)
	{
		SOUNDS.register(bus);
	}

	private CoreSounds()
	{
	}
}
