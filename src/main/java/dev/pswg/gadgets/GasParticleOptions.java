package dev.pswg.gadgets;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record GasParticleOptions(ParticleType<GasParticleOptions> type, int cloudId, int slot) implements ParticleOptions
{
	static ParticleType<GasParticleOptions> createType()
	{
		return new ParticleType<>(true)
		{
			@Override
			public MapCodec<GasParticleOptions> codec()
			{
				return RecordCodecBuilder.mapCodec(builder -> builder.group(
						Codec.INT.optionalFieldOf("cloud", -1).forGetter(GasParticleOptions::cloudId),
						Codec.INT.optionalFieldOf("slot", 0).forGetter(GasParticleOptions::slot)
				).apply(builder, (cloud, slot) -> new GasParticleOptions(this, cloud, slot)));
			}

			@Override
			public StreamCodec<? super RegistryFriendlyByteBuf, GasParticleOptions> streamCodec()
			{
				return StreamCodec.composite(ByteBufCodecs.VAR_INT, GasParticleOptions::cloudId, ByteBufCodecs.VAR_INT,
						GasParticleOptions::slot, (cloud, slot) -> new GasParticleOptions(this, cloud, slot));
			}
		};
	}

	@Override public ParticleType<GasParticleOptions> getType() { return type; }
}
