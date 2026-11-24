package dev.pswg.container;

import com.mojang.serialization.Codec;
import dev.pswg.Galaxies;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CoreComponents
{
	private static final DeferredRegister<DataComponentType<?>> TYPES = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Galaxies.MODID);

	public static final DataComponentType<Boolean> SCRAP = register("scrap", Codec.BOOL);
	public static final DataComponentType<Integer> METAL_COMPONENT = register("metal_component", Codec.INT);
	public static final DataComponentType<Integer> TECH_COMPONENT = register("tech_component", Codec.INT);
	public static final DataComponentType<Integer> PLASTIC_COMPONENT = register("plastic_component", Codec.INT);
	public static final DataComponentType<Integer> ENERGY_COMPONENT = register("energy_component", Codec.INT);

	public static void register(IEventBus bus)
	{
		TYPES.register(bus);
	}

	private static <T> DataComponentType<T> register(String name, Codec<T> codec)
	{
		var type = DataComponentType.<T>builder().persistent(codec).build();
		TYPES.register(name, () -> type);
		return type;
	}

	private CoreComponents()
	{
	}
}
