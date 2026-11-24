package dev.pswg.compat.jei;

import dev.pswg.Galaxies;
import dev.pswg.gadgets.Demolitions;
import dev.pswg.gadgets.WorkstationRecipes;
import dev.pswg.gadgets.Workstations;
import dev.pswg.gadgets.scrapping.CuttingRecipe;
import dev.pswg.gadgets.scrapping.ScrappingRecipe;
import dev.pswg.utilities.LegacyUtilities;
import dev.pswg.utilities.VaporatorRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.function.BiConsumer;
import java.util.function.Function;

@JeiPlugin
public final class GalaxiesJei implements IModPlugin
{
	private static final RecipeType<RecipeHolder<VaporatorRecipe>> VAPORATING = RecipeType.createRecipeHolderType(Galaxies.id("vaporator"));
	private static final RecipeType<RecipeHolder<ScrappingRecipe>> SCRAPPING = RecipeType.createRecipeHolderType(Demolitions.id("scrapping"));
	private static final RecipeType<RecipeHolder<CuttingRecipe>> CUTTING = RecipeType.createRecipeHolderType(Demolitions.id("laser_cutting"));

	@Override public ResourceLocation getPluginUid() { return Galaxies.id("recipes"); }

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration)
	{
		var gui = registration.getJeiHelpers().getGuiHelper();
		registration.addRecipeCategories(new Category<>(VAPORATING, new ItemStack(LegacyUtilities.VAPORATOR.get()), gui, (slots, recipe) -> {
			slots.addSlot(RecipeIngredientRole.INPUT, 5, 6).addIngredients(recipe.ingredient());
			slots.addSlot(RecipeIngredientRole.OUTPUT, 66, 6).addItemStack(recipe.result());
		}, recipe -> Component.translatable("jei.pswg.duration", recipe.duration() / 20F)),
				new Category<>(SCRAPPING, new ItemStack(Workstations.SCRAPPING_TABLE.get()), gui, (slots, recipe) -> {
					slots.addSlot(RecipeIngredientRole.CATALYST, 5, 6).addIngredients(recipe.tool());
					slots.addSlot(RecipeIngredientRole.INPUT, 26, 6).addIngredients(recipe.ingredient());
					slots.addSlot(RecipeIngredientRole.OUTPUT, 66, 6).addItemStack(recipe.primary());
					slots.addSlot(RecipeIngredientRole.OUTPUT, 87, 6).addItemStack(recipe.secondary());
				}, recipe -> bonusChance(recipe.secondaryChance())),
				new Category<>(CUTTING, new ItemStack(Workstations.CUTTER.get()), gui, (slots, recipe) -> {
					slots.addSlot(RecipeIngredientRole.INPUT, 5, 6).addIngredients(recipe.ingredient());
					slots.addSlot(RecipeIngredientRole.OUTPUT, 66, 6).addItemStack(recipe.primary());
					slots.addSlot(RecipeIngredientRole.OUTPUT, 87, 6).addItemStack(recipe.secondary());
				}, recipe -> bonusChance(recipe.secondaryChance())));
	}

	private static Component bonusChance(float chance)
	{
		return Component.translatable("jei.pswg.bonus_chance", Math.round(chance * 100));
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration)
	{
		var connection = Minecraft.getInstance().getConnection();
		if (connection == null) return;
		var recipes = connection.getRecipeManager();
		registration.addRecipes(VAPORATING, recipes.getAllRecipesFor(LegacyUtilities.VAPORATING.get()));
		registration.addRecipes(SCRAPPING, recipes.getAllRecipesFor(WorkstationRecipes.SCRAPPING.get()));
		registration.addRecipes(CUTTING, recipes.getAllRecipesFor(WorkstationRecipes.CUTTING.get()));
	}

	@Override
	public void registerRecipeCatalysts(IRecipeCatalystRegistration registration)
	{
		registration.addRecipeCatalyst(new ItemStack(LegacyUtilities.VAPORATOR.get()), VAPORATING);
		registration.addRecipeCatalyst(new ItemStack(Workstations.SCRAPPING_TABLE.get()), SCRAPPING);
		registration.addRecipeCatalyst(new ItemStack(Workstations.CUTTER.get()), CUTTING);
	}

	private record Category<R extends Recipe<?>>(RecipeType<RecipeHolder<R>> type, ItemStack catalyst,
			IDrawable icon, IDrawable arrow, BiConsumer<IRecipeLayoutBuilder, R> slots, Function<R, Component> details)
			implements IRecipeCategory<RecipeHolder<R>>
	{
		Category(RecipeType<RecipeHolder<R>> type, ItemStack catalyst, IGuiHelper gui, BiConsumer<IRecipeLayoutBuilder, R> slots,
				Function<R, Component> details)
		{
			this(type, catalyst, gui.createDrawableItemStack(catalyst), gui.getRecipeArrow(), slots, details);
		}
		@Override public RecipeType<RecipeHolder<R>> getRecipeType() { return type; }
		@Override public Component getTitle() { return catalyst.getHoverName(); }
		@Override public IDrawable getIcon() { return icon; }
		@Override public int getWidth() { return 120; }
		@Override public int getHeight() { return 42; }
		@Override public ResourceLocation getRegistryName(RecipeHolder<R> recipe) { return recipe.id(); }
		@Override public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<R> recipe, IFocusGroup focuses) { slots.accept(builder, recipe.value()); }
		@Override public void draw(RecipeHolder<R> recipe, IRecipeSlotsView view, GuiGraphics graphics, double mouseX, double mouseY)
		{
			arrow.draw(graphics, 44, 6);
			graphics.drawString(Minecraft.getInstance().font, details.apply(recipe.value()), 5, 29, 0x454545, false);
		}
	}
}
