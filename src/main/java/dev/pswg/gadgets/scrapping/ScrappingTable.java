package dev.pswg.gadgets.scrapping;

import dev.pswg.container.CoreComponents;
import dev.pswg.gadgets.WorkstationInventory;
import dev.pswg.gadgets.WorkstationRecipes;
import dev.pswg.gadgets.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;

public final class ScrappingTable extends WorkstationInventory
{
	public static final int MAX_PROGRESS = 480;
	static final int TOOL_COUNT = 3;
	static final int INPUT = 3;
	static final int FIRST_OUTPUT = 4;
	static final int SIZE = 10;
	private static final List<DataComponentType<Integer>> MATERIALS = List.of(CoreComponents.METAL_COMPONENT,
			CoreComponents.TECH_COMPONENT, CoreComponents.PLASTIC_COMPONENT, CoreComponents.ENERGY_COMPONENT);
	private static final int[][] COSTS = { { 2, 0, 1, 0 }, { 1, 2, 0, 0 }, { 0, 1, 0, 2 } };

	private final int[] progress = { -1, -1, -1 };
	private final RecipeManager.CachedCheck<ScrappingRecipe.Input, ScrappingRecipe> recipes =
			RecipeManager.createCheck(WorkstationRecipes.SCRAPPING.get());

	public ScrappingTable(BlockPos pos, BlockState state)
	{
		super(Workstations.SCRAPPING_ENTITY.get(), pos, state, SIZE);
	}

	public static DataComponentType<Integer> material(int index)
	{
		return MATERIALS.get(index);
	}

	public static int cost(int tool, int material)
	{
		return COSTS[tool][material];
	}

	static boolean isTool(int index, ItemStack stack)
	{
		return switch (index)
		{
			case 0 -> stack.is(Workstations.CUTTER.get());
			case 1 -> stack.is(Workstations.SPANNER.get());
			case 2 -> stack.is(Workstations.CALIBRATOR.get());
			default -> false;
		};
	}

	public static void tick(Level level, BlockPos pos, BlockState state, ScrappingTable table)
	{
		for (int tool = 0; tool < TOOL_COUNT; tool++)
		{
			var recipe = table.availableRecipe((ServerLevel) level, tool);
			int previous = table.progress[tool];
			if (recipe == null)
				table.progress[tool] = -1;
			else if (previous >= MAX_PROGRESS)
			{
				table.complete(tool, recipe);
				table.progress[tool] = 0;
			}
			else
				table.progress[tool] = Math.max(0, previous - 10);
			if (previous != table.progress[tool])
				table.setChanged();
		}
	}

	private ScrappingRecipe availableRecipe(ServerLevel server, int tool)
	{
		ItemStack target = getItem(INPUT);
		ItemStack implement = getItem(tool);
		if (target.isEmpty() || !isTool(tool, implement))
			return null;
		var input = new ScrappingRecipe.Input(implement, target);
		var recipe = recipes.getRecipeFor(input, server).map(holder -> holder.value()).orElse(null);
		int output = FIRST_OUTPUT + tool * 2;
		return recipe != null && fits(output, recipe.primary()) && fits(output + 1, recipe.secondary()) ? recipe : null;
	}

	private boolean fits(int slot, ItemStack result)
	{
		ItemStack current = getItem(slot);
		int limit = Math.min(getMaxStackSize(), result.getMaxStackSize());
		return result.getCount() <= limit && (current.isEmpty()
				|| ItemStack.isSameItemSameComponents(current, result) && current.getCount() <= limit - result.getCount());
	}

	private void complete(int tool, ScrappingRecipe recipe)
	{
		int output = FIRST_OUTPUT + tool * 2;
		deposit(output, recipe.primary());
		// The source table treats this value as a threshold, while the cutter uses it as a probability.
		if (level.random.nextFloat() >= recipe.secondaryChance())
			deposit(output + 1, recipe.secondary());

		ItemStack target = getItem(INPUT);
		boolean charged = false;
		boolean depleted = false;
		for (int material = 0; material < MATERIALS.size(); material++)
		{
			int cost = cost(tool, material);
			var component = material(material);
			if (cost == 0 || !target.has(component))
				continue;
			int remaining = Math.max(0, target.getOrDefault(component, 0) - cost);
			target.set(component, remaining);
			charged = true;
			depleted |= remaining == 0;
		}
		if (depleted || !charged)
			target.shrink(1);

		ItemStack toolStack = getItem(tool);
		toolStack.setDamageValue(Math.min(toolStack.getMaxDamage() - 1, toolStack.getDamageValue() + 1));
	}

	private void deposit(int slot, ItemStack result)
	{
		ItemStack existing = getItem(slot);
		if (existing.isEmpty())
			setItem(slot, result.copy());
		else
			existing.grow(result.getCount());
	}

	boolean work(int tool)
	{
		if (tool < 0 || tool >= TOOL_COUNT || !(level instanceof ServerLevel server) || availableRecipe(server, tool) == null)
			return false;
		var stack = getItem(tool);
		int efficiency = (int) (100 - 75F * stack.getDamageValue() / Math.max(1, stack.getMaxDamage()));
		progress[tool] = Math.min(MAX_PROGRESS, Math.max(0, progress[tool]) + efficiency);
		setChanged();
		return true;
	}

	@Override
	public void setItem(int slot, ItemStack stack)
	{
		if (slot <= INPUT && !ItemStack.matches(getItem(slot), stack))
			resetProgress(slot);
		super.setItem(slot, stack);
	}

	@Override
	public ItemStack removeItem(int slot, int count)
	{
		var removed = super.removeItem(slot, count);
		if (slot <= INPUT && !removed.isEmpty())
			resetProgress(slot);
		return removed;
	}

	private void resetProgress(int slot)
	{
		if (slot == INPUT)
			Arrays.fill(progress, -1);
		else
			progress[slot] = -1;
	}

	@Override
	protected Component getDefaultName()
	{
		return Component.translatable("block.pswg_gadgets.scrapping_table");
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory)
	{
		ContainerData data = new SimpleContainerData(TOOL_COUNT)
		{
			@Override
			public int get(int index)
			{
				return progress[index];
			}
		};
		return new ScrappingMenu(id, inventory, this, data);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.putIntArray("tool_progress", progress.clone());
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		Arrays.fill(progress, -1);
		int[] saved = tag.getIntArray("tool_progress");
		for (int i = 0; i < Math.min(saved.length, TOOL_COUNT); i++)
			progress[i] = Mth.clamp(saved[i], -1, MAX_PROGRESS);
	}
}
