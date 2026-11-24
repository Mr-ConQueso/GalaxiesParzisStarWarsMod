package dev.pswg.gadgets;

import com.mojang.serialization.MapCodec;
import dev.pswg.gadgets.scrapping.CuttingRecipe;
import dev.pswg.gadgets.scrapping.ScrappingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class WorkstationRecipes
{
	private static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Demolitions.NAMESPACE);
	private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
			DeferredRegister.create(Registries.RECIPE_SERIALIZER, Demolitions.NAMESPACE);

	public static final Supplier<RecipeType<ScrappingRecipe>> SCRAPPING = TYPES.register("scrapping",
			() -> RecipeType.simple(Demolitions.id("scrapping")));
	public static final Supplier<RecipeSerializer<ScrappingRecipe>> SCRAPPING_SERIALIZER = SERIALIZERS.register("scrapping",
			() -> new Serializer<>(ScrappingRecipe.CODEC, ScrappingRecipe.STREAM_CODEC));

	public static final Supplier<RecipeType<CuttingRecipe>> CUTTING = TYPES.register("laser_cutting",
			() -> RecipeType.simple(Demolitions.id("laser_cutting")));
	public static final Supplier<RecipeSerializer<CuttingRecipe>> CUTTING_SERIALIZER = SERIALIZERS.register("laser_cutting",
			() -> new Serializer<>(CuttingRecipe.CODEC, CuttingRecipe.STREAM_CODEC));

	static void register(IEventBus bus)
	{
		TYPES.register(bus);
		SERIALIZERS.register(bus);
	}

	public record Serializer<T extends Recipe<?>>(MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec)
			implements RecipeSerializer<T>
	{
	}

	private WorkstationRecipes()
	{
	}
}
