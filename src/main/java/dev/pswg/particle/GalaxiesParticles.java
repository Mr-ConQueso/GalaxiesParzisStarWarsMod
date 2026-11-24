package dev.pswg.particle;

import com.mojang.serialization.MapCodec;
import dev.pswg.Galaxies;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class GalaxiesParticles
{
	private static final DeferredRegister<ParticleType<?>> TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, Galaxies.MODID);

	public static final Supplier<SimpleParticleType> SCORCH = TYPES.register("scorch", () -> new SimpleParticleType(true));
	public static final Supplier<SimpleParticleType> SPARK = TYPES.register("spark", () -> new SimpleParticleType(true));
	public static final Supplier<SimpleParticleType> SLUG_TRAIL = TYPES.register("slug_trail", () -> new SimpleParticleType(true));
	public static final Supplier<SimpleParticleType> WATER_WAKE = TYPES.register("water_wake", () -> new SimpleParticleType(true));
	public static final Supplier<SimpleParticleType> SHORT_FLAME = TYPES.register("short_flame", () -> new SimpleParticleType(false));
	public static final Supplier<SimpleParticleType> SMALL_SHORT_FLAME = TYPES.register("small_short_flame", () -> new SimpleParticleType(false));
	public static final Supplier<ParticleType<ColorParticleOption>> SMALL_FLASH = TYPES.register("small_flash", () -> colorType(true));
	public static final Supplier<ParticleType<ColorParticleOption>> SEQUOIA_LEAF = TYPES.register("sequoia_leaf", () -> colorType(false));

	public static final Supplier<ParticleType<BlockParticleOption>> WAKE = TYPES.register("wake", () -> new ParticleType<BlockParticleOption>(true)
	{
		@Override public MapCodec<BlockParticleOption> codec() { return BlockParticleOption.codec(this); }
		@Override public StreamCodec<? super RegistryFriendlyByteBuf, BlockParticleOption> streamCodec() { return BlockParticleOption.streamCodec(this); }
	});

	private static ParticleType<ColorParticleOption> colorType(boolean alwaysVisible)
	{
		return new ParticleType<>(alwaysVisible)
		{
			@Override public MapCodec<ColorParticleOption> codec() { return ColorParticleOption.codec(this); }
			@Override public StreamCodec<? super RegistryFriendlyByteBuf, ColorParticleOption> streamCodec() { return ColorParticleOption.streamCodec(this); }
		};
	}

	public static void register(IEventBus bus)
	{
		TYPES.register(bus);
	}

	private GalaxiesParticles() {}
}
