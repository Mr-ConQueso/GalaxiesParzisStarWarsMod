package dev.pswg.gadgets.scrapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.pswg.gadgets.WorkstationRecipes;
import net.minecraft.core.HolderLookup;
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

public record CuttingRecipe(Ingredient ingredient, ItemStack primary, ItemStack secondary, float secondaryChance)
		implements Recipe<SingleRecipeInput>
{
	public static final MapCodec<CuttingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(CuttingRecipe::ingredient),
			ItemStack.STRICT_CODEC.fieldOf("primary_result").forGetter(CuttingRecipe::primary),
			ItemStack.STRICT_CODEC.fieldOf("secondary_result").forGetter(CuttingRecipe::secondary),
			Codec.floatRange(0F, 1F).fieldOf("secondary_chance").forGetter(CuttingRecipe::secondaryChance)
	).apply(builder, CuttingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, CuttingRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, CuttingRecipe::ingredient,
			ItemStack.STREAM_CODEC, CuttingRecipe::primary,
			ItemStack.STREAM_CODEC, CuttingRecipe::secondary,
			ByteBufCodecs.FLOAT, CuttingRecipe::secondaryChance,
			CuttingRecipe::new);

	@Override
	public boolean matches(SingleRecipeInput input, Level level)
	{
		return ingredient.test(input.item());
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries)
	{
		return primary.copy();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider registries)
	{
		return primary.copy();
	}

	@Override
	public boolean canCraftInDimensions(int width, int height)
	{
		return false;
	}

	@Override
	public boolean isSpecial()
	{
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer()
	{
		return WorkstationRecipes.CUTTING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType()
	{
		return WorkstationRecipes.CUTTING.get();
	}
}
