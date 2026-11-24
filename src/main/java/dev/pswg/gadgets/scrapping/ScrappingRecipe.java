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
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record ScrappingRecipe(Ingredient tool, Ingredient ingredient, ItemStack primary, ItemStack secondary, float secondaryChance)
		implements Recipe<ScrappingRecipe.Input>
{
	public static final MapCodec<ScrappingRecipe> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Ingredient.CODEC_NONEMPTY.fieldOf("tool").forGetter(ScrappingRecipe::tool),
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ScrappingRecipe::ingredient),
			ItemStack.STRICT_CODEC.fieldOf("primary_result").forGetter(ScrappingRecipe::primary),
			ItemStack.STRICT_CODEC.fieldOf("secondary_result").forGetter(ScrappingRecipe::secondary),
			Codec.floatRange(0F, 1F).fieldOf("secondary_chance").forGetter(ScrappingRecipe::secondaryChance)
	).apply(builder, ScrappingRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, ScrappingRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC, ScrappingRecipe::tool,
			Ingredient.CONTENTS_STREAM_CODEC, ScrappingRecipe::ingredient,
			ItemStack.STREAM_CODEC, ScrappingRecipe::primary,
			ItemStack.STREAM_CODEC, ScrappingRecipe::secondary,
			ByteBufCodecs.FLOAT, ScrappingRecipe::secondaryChance,
			ScrappingRecipe::new);

	@Override
	public boolean matches(Input input, Level level)
	{
		return tool.test(input.tool()) && ingredient.test(input.scrap());
	}

	@Override
	public ItemStack assemble(Input input, HolderLookup.Provider registries)
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
		return WorkstationRecipes.SCRAPPING_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType()
	{
		return WorkstationRecipes.SCRAPPING.get();
	}

	public record Input(ItemStack tool, ItemStack scrap) implements RecipeInput
	{
		@Override
		public ItemStack getItem(int index)
		{
			return switch (index)
			{
				case 0 -> tool;
				case 1 -> scrap;
				default -> throw new IndexOutOfBoundsException(index);
			};
		}

		@Override
		public int size()
		{
			return 2;
		}
	}
}
