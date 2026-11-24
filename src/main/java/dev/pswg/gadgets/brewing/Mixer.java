package dev.pswg.gadgets.brewing;

import com.mojang.datafixers.util.Pair;
import dev.pswg.container.DrinkItems;
import dev.pswg.gadgets.WorkstationInventory;
import dev.pswg.gadgets.Workstations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Mixer extends WorkstationInventory
{
	static final int FUEL = 0;
	static final int INGREDIENT = 1;
	static final int VESSEL = 2;
	public static final int MAX_PRESSURE = 180;
	private static final float CENTER = BrewingMap.SIZE / 2F;
	private static final int MAX_ADDITIONS = 3;

	private float x = CENTER;
	private float y = CENTER;
	private int fuel;
	private int fuelDuration;
	private int pressure;
	private int pumping;
	private int danger;
	private final List<Pair<Float, Float>> path = new ArrayList<>();
	private final List<MobEffectInstance> effects = new ArrayList<>();
	private final List<Integer> dyes = new ArrayList<>();
	private final List<ItemStack> foods = new ArrayList<>();

	public Mixer(BlockPos pos, BlockState state)
	{
		super(Workstations.MIXER_ENTITY.get(), pos, state, 3);
	}

	static boolean isVessel(ItemStack stack)
	{
		return stack.is(DrinkItems.DRINK_CONTAINER) && filledItem(stack) != Items.AIR;
	}

	static boolean isIngredient(ItemStack stack)
	{
		return stack.getItem() instanceof DyeItem || stack.is(MixerIngredients.FOOD) || !MixerIngredients.path(stack).isEmpty();
	}

	private static Item filledItem(ItemStack stack)
	{
		for (var pair : DrinkItems.CONTAINERS)
			if (stack.is(pair.empty().get()))
				return pair.filled().get();
		return stack.is(Items.GLASS_BOTTLE) ? Items.POTION : Items.AIR;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, Mixer mixer)
	{
		mixer.mix((ServerLevel) level);
	}

	private void mix(ServerLevel level)
	{
		boolean hasVessel = isVessel(getItem(VESSEL));
		boolean changed = fuel > 0 || !isReset();
		if (!hasVessel && !isReset())
			reset();

		if (fuel > 0)
			fuel--;
		pumping = Math.max(0, pumping - 1);
		if (hasVessel)
			pressure = fuel > 0 ? Math.min(MAX_PRESSURE, pressure + (pumping == 0 ? 5 : 0)) : Math.max(0, pressure - 2);

		ItemStack ingredient = getItem(INGREDIENT);
		if (hasVessel && fuel == 0 && (!path.isEmpty() || canConsume(ingredient)))
		{
			ItemStack stack = getItem(FUEL);
			int burnTime = stack.getBurnTime(RecipeType.SMELTING);
			if (burnTime > 0)
			{
				fuel = fuelDuration = burnTime;
				ItemStack remainder = stack.getCraftingRemainingItem();
				stack.shrink(1);
				if (stack.isEmpty())
					setItem(FUEL, remainder);
				changed = true;
			}
		}
		if (hasVessel && fuel > 0 && canConsume(ingredient))
		{
			var steps = MixerIngredients.path(ingredient);
			if (!steps.isEmpty())
				path.addAll(steps);
			else if (ingredient.getItem() instanceof DyeItem dye)
				dyes.add(dye.getDyeColor().getTextureDiffuseColor());
			else
				foods.add(ingredient.copyWithCount(1));
			ingredient.shrink(1);
			changed = true;
		}

		boolean moving = hasVessel && pumping > 0 && pressure > 5 && !path.isEmpty();
		var cell = BrewingMap.at(x, y);
		if (moving)
		{
			float speed = switch (cell.kind())
			{
				case DANGER -> 1.25F;
				case EFFECT -> 0.9F;
				case CORNER -> 0.95F;
				case EMPTY -> 1F;
			};
			var step = path.removeLast();
			float distance = Math.min(speed, step.getSecond());
			x = Mth.clamp(x + distance * Mth.cos(step.getFirst()), 0, BrewingMap.SIZE);
			y = Mth.clamp(y - distance * Mth.sin(step.getFirst()), 0, BrewingMap.SIZE);
			if (distance < step.getSecond())
				path.add(Pair.of(step.getFirst(), step.getSecond() - distance));
		}
		if (moving && (cell.kind() == BrewingMap.Kind.DANGER || cell.kind() == BrewingMap.Kind.CORNER))
		{
			danger++;
			if (cell.kind() == BrewingMap.Kind.DANGER && danger >= 12)
			{
				var effect = new MobEffectInstance(cell.effect());
				if (effects.isEmpty())
					effects.add(effect);
				else
					effects.set(0, effect);
				bottle(null);
				failParticles(level);
			}
			else if (cell.kind() == BrewingMap.Kind.CORNER && danger >= 8)
			{
				reset();
				failParticles(level);
			}
		}
		else if (cell.kind() != BrewingMap.Kind.DANGER && cell.kind() != BrewingMap.Kind.CORNER)
			danger = Math.max(0, danger - 1);

		if (changed || moving)
			setChanged();
	}

	private boolean canConsume(ItemStack stack)
	{
		if (!MixerIngredients.path(stack).isEmpty())
			return path.isEmpty();
		if (stack.getItem() instanceof DyeItem)
			return dyes.size() < MAX_ADDITIONS;
		return stack.is(MixerIngredients.FOOD) && foods.size() < MAX_ADDITIONS;
	}

	private void failParticles(ServerLevel level)
	{
		var pos = Vec3.atCenterOf(worldPosition);
		level.sendParticles(ParticleTypes.SMOKE, pos.x, pos.y, pos.z, 20, 0, 0, 0, 0.05);
	}

	boolean canAddEffect()
	{
		var cell = BrewingMap.at(x, y);
		return cell.kind() == BrewingMap.Kind.EFFECT && effects.size() < MAX_ADDITIONS
				&& effects.stream().noneMatch(effect -> effect.getEffect().equals(cell.effect().getEffect()));
	}

	private boolean canBottle()
	{
		return !effects.isEmpty() || !dyes.isEmpty() || canAddEffect();
	}

	private boolean addEffect()
	{
		if (!canAddEffect())
			return false;
		effects.add(new MobEffectInstance(BrewingMap.at(x, y).effect()));
		return true;
	}

	private boolean bottle(Player player)
	{
		Item filled = filledItem(getItem(VESSEL));
		if (filled == Items.AIR || !canBottle())
			return false;
		var vessel = new ItemStack(filled);
		addEffect();
		Optional<Integer> color = dyes.isEmpty() ? Optional.empty() : Optional.of(blendDyes());
		vessel.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), color, List.copyOf(effects)));
		int nutrition = 0;
		float saturation = 0;
		for (var stack : foods)
		{
			var food = stack.get(DataComponents.FOOD);
			if (food != null)
			{
				nutrition += food.nutrition();
				saturation += food.saturation();
			}
		}
		if (!foods.isEmpty())
			vessel.set(DataComponents.FOOD, new FoodProperties((int) (nutrition / 1.5F), saturation / 1.5F, true,
					1.6F, Optional.empty(), List.of()));

		if (player != null && !effects.isEmpty())
		{
			var discoveries = new ArrayList<>(player.getData(Workstations.BREWING_DISCOVERIES));
			for (var effect : effects)
				effect.getEffect().unwrapKey().map(key -> key.location()).filter(id -> !discoveries.contains(id)).ifPresent(discoveries::add);
			player.setData(Workstations.BREWING_DISCOVERIES, List.copyOf(discoveries));
		}

		setItem(VESSEL, vessel);
		return true;
	}

	private int blendDyes()
	{
		int red = 0;
		int green = 0;
		int blue = 0;
		for (int color : dyes)
		{
			red += color >> 16 & 255;
			green += color >> 8 & 255;
			blue += color & 255;
		}
		int count = dyes.size();
		return (red / count << 16) | (green / count << 8) | (blue / count);
	}

	boolean act(Player player, int action)
	{
		if (!isVessel(getItem(VESSEL)))
			return false;
		boolean changed = switch (action)
		{
			case 0 -> pump();
			case 1 -> bottle(player);
			case 2 -> addEffect();
			case 3 -> reset();
			default -> false;
		};
		if (changed)
			setChanged();
		return changed;
	}

	private boolean pump()
	{
		if (fuel == 0 || pressure <= 5)
			return false;
		pumping = 4;
		pressure -= 2;
		return true;
	}

	private boolean isReset()
	{
		return x == CENTER && y == CENTER && pressure == 0 && pumping == 0 && danger == 0
				&& path.isEmpty() && effects.isEmpty() && dyes.isEmpty() && foods.isEmpty();
	}

	private boolean reset()
	{
		if (isReset())
			return false;
		x = y = CENTER;
		pressure = pumping = danger = 0;
		path.clear();
		effects.clear();
		dyes.clear();
		foods.clear();
		return true;
	}

	int menuData(int index)
	{
		return switch (index)
		{
			case 0 -> Math.round(x * 10);
			case 1 -> Math.round(y * 10);
			case 2 -> fuel == 0 ? 0 : Math.max(1, (int) (1000F * fuel / Math.max(1, fuelDuration)));
			case 3 -> pressure;
			case 4 -> (canBottle() ? 1 : 0) | (canAddEffect() ? 2 : 0) | (!isReset() ? 4 : 0);
			default -> swatchWord(index - 5);
		};
	}

	private int swatchWord(int word)
	{
		int slot = word / 2;
		int entry = slot % MAX_ADDITIONS;
		int color = switch (slot / MAX_ADDITIONS)
		{
			case 0 -> entry < effects.size() ? effects.get(entry).getEffect().value().getColor() : -1;
			case 1 -> entry < dyes.size() ? dyes.get(entry) : -1;
			case 2 -> entry < foods.size() ? MixerIngredients.color(foods.get(entry)) : -1;
			default -> -1;
		};
		if (word % 2 == 0)
			return color == -1 ? -1 : color >> 16 & 255;
		return (short) color;
	}

	@Override
	public ItemStack removeItem(int slot, int count)
	{
		var removed = super.removeItem(slot, count);
		if (slot == VESSEL && !removed.isEmpty())
			reset();
		return removed;
	}

	@Override
	public void setItem(int slot, ItemStack stack)
	{
		if (slot == VESSEL && !ItemStack.matches(getItem(slot), stack))
			reset();
		super.setItem(slot, stack);
	}

	@Override
	protected Component getDefaultName()
	{
		return Component.translatable("block.pswg_gadgets.mixer");
	}

	@Override
	protected AbstractContainerMenu createMenu(int id, Inventory inventory)
	{
		return new MixerMenu(id, inventory, this);
	}

	@Override
	protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.saveAdditional(tag, registries);
		tag.putFloat("map_x", x);
		tag.putFloat("map_y", y);
		tag.putInt("fuel", fuel);
		tag.putInt("fuel_duration", fuelDuration);
		tag.putInt("pressure", pressure);
		tag.putInt("pumping", pumping);
		tag.putInt("danger", danger);
		ListTag steps = new ListTag();
		for (var step : path)
		{
			CompoundTag entry = new CompoundTag();
			entry.putFloat("angle", step.getFirst());
			entry.putFloat("distance", step.getSecond());
			steps.add(entry);
		}
		tag.put("path", steps);
		ListTag savedEffects = new ListTag();
		effects.forEach(effect -> savedEffects.add(effect.save()));
		tag.put("effects", savedEffects);
		tag.putIntArray("dyes", dyes);
		ListTag savedFoods = new ListTag();
		foods.forEach(stack -> savedFoods.add(stack.save(registries)));
		tag.put("foods", savedFoods);
	}

	@Override
	protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries)
	{
		super.loadAdditional(tag, registries);
		reset();
		if (tag.contains("map_x"))
			x = Mth.clamp(tag.getFloat("map_x"), 0, BrewingMap.SIZE);
		if (tag.contains("map_y"))
			y = Mth.clamp(tag.getFloat("map_y"), 0, BrewingMap.SIZE);
		fuel = Math.max(0, tag.getInt("fuel"));
		fuelDuration = Math.max(fuel, tag.getInt("fuel_duration"));
		pressure = Mth.clamp(tag.getInt("pressure"), 0, MAX_PRESSURE);
		pumping = Mth.clamp(tag.getInt("pumping"), 0, 4);
		danger = Mth.clamp(tag.getInt("danger"), 0, 11);
		var steps = tag.getList("path", Tag.TAG_COMPOUND);
		for (int i = 0; i < steps.size(); i++)
		{
			var step = steps.getCompound(i);
			float angle = step.getFloat("angle");
			float distance = step.getFloat("distance");
			if (Float.isFinite(angle) && Float.isFinite(distance) && distance > 0)
				path.add(Pair.of(angle, distance));
		}
		var savedEffects = tag.getList("effects", Tag.TAG_COMPOUND);
		for (int i = 0; i < Math.min(MAX_ADDITIONS, savedEffects.size()); i++)
		{
			var effect = MobEffectInstance.load(savedEffects.getCompound(i));
			if (effect != null)
				effects.add(effect);
		}
		int[] savedDyes = tag.getIntArray("dyes");
		for (int i = 0; i < Math.min(MAX_ADDITIONS, savedDyes.length); i++)
			dyes.add(savedDyes[i]);
		var savedFoods = tag.getList("foods", Tag.TAG_COMPOUND);
		for (int i = 0; i < Math.min(MAX_ADDITIONS, savedFoods.size()); i++)
			ItemStack.parse(registries, savedFoods.getCompound(i)).ifPresent(foods::add);
	}
}
