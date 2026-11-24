package dev.pswg.utilities;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record VaporatorRecipe(Ingredient ingredient, int duration, ItemStack result) implements Recipe<SingleRecipeInput>
{
	public static final MapCodec<VaporatorRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(VaporatorRecipe::ingredient),
			Codec.intRange(1, Integer.MAX_VALUE).fieldOf("duration").forGetter(VaporatorRecipe::duration),
			ItemStack.STRICT_CODEC.fieldOf("result").forGetter(VaporatorRecipe::result)
	).apply(builder, VaporatorRecipe::new));
	public static final StreamCodec<RegistryFriendlyByteBuf, VaporatorRecipe> STREAM = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, VaporatorRecipe::ingredient, ByteBufCodecs.VAR_INT, VaporatorRecipe::duration,
			ItemStack.STREAM_CODEC, VaporatorRecipe::result, VaporatorRecipe::new);

	@Override public boolean matches(SingleRecipeInput input, Level level) { return ingredient.test(input.item()); }
	@Override public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) { return result.copy(); }
	@Override public ItemStack getResultItem(HolderLookup.Provider registries) { return result.copy(); }
	@Override public NonNullList<Ingredient> getIngredients() { return NonNullList.of(Ingredient.EMPTY, ingredient); }
	@Override public boolean canCraftInDimensions(int width, int height) { return width >= 1 && height >= 1; }
	@Override public boolean isSpecial() { return true; }
	@Override public RecipeSerializer<?> getSerializer() { return LegacyUtilities.VAPORATOR_SERIALIZER.get(); }
	@Override public RecipeType<?> getType() { return LegacyUtilities.VAPORATING.get(); }
	@Override public ItemStack getToastSymbol() { return new ItemStack(LegacyUtilities.VAPORATOR.get()); }
}
