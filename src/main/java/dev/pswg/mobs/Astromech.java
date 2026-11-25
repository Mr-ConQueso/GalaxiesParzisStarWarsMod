package dev.pswg.mobs;

import dev.pswg.Galaxies;
import dev.pswg.gadgets.WorkstationMenu;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.resources.ResourceLocation;

public final class Astromech extends PathfinderMob
{
	private static final String PAINT_TAG = "pswg:astromech_parameters";
	private static final int LEG_TICKS = 18;
	private static final EntityDataAccessor<Integer> LEG = SynchedEntityData.defineId(Astromech.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<CompoundTag> PAINT = SynchedEntityData.defineId(Astromech.class, EntityDataSerializers.COMPOUND_TAG);
	private final Paint defaultPaint;
	private Paint paint;
	private int lastLeg;
	private final SimpleContainer cargo = new SimpleContainer(5)
	{
		@Override public boolean stillValid(Player player) { return isAlive() && player.canInteractWithEntity(Astromech.this, 0); }
	};

	public Astromech(EntityType<? extends PathfinderMob> type, Level level, String paint)
	{
		super(type, level);
		defaultPaint = Paint.preset(paint);
		setPaint(defaultPaint);
		moveControl = new MoveControl(this)
		{
			@Override protected float rotlerp(float from, float to, float maximum) { return super.rotlerp(from, to, Math.min(maximum, 6)); }

			@Override
			public void tick()
			{
				if (getNavigation().isDone() || entityData.get(LEG) == LEG_TICKS || isInWater())
					super.tick();
				else
					setSpeed(0);
			}
		};
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data)
	{
		super.defineSynchedData(data);
		data.define(LEG, 0);
		data.define(PAINT, new CompoundTag());
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key)
	{
		super.onSyncedDataUpdated(key);
		if (key.equals(PAINT))
			paint = Paint.read(entityData.get(PAINT), defaultPaint);
	}

	public Paint paint() { return paint; }
	public Container cargo() { return cargo; }
	public float extension(float partial) { return Mth.lerp(partial, lastLeg, entityData.get(LEG)) / LEG_TICKS; }

	private void setPaint(Paint paint)
	{
		this.paint = paint;
		entityData.set(PAINT, paint.save());
	}

	@Override
	protected void registerGoals()
	{
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.2, 100));
		goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6));
		goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	@Override
	public void tick()
	{
		lastLeg = entityData.get(LEG);
		super.tick();
		if (!level().isClientSide)
			entityData.set(LEG, Mth.clamp(lastLeg + (isLeashed() || !getNavigation().isDone() ? 1 : -1), 0, LEG_TICKS));
	}

	@Override
	protected InteractionResult mobInteract(Player player, InteractionHand hand)
	{
		if (player.isSecondaryUseActive())
		{
			if (!level().isClientSide)
				player.openMenu(new SimpleMenuProvider((id, slots, viewer) -> new Menu(id, slots, cargo), getDisplayName()));
			return InteractionResult.sidedSuccess(level().isClientSide);
		}
		return super.mobInteract(player, hand);
	}

	@Override public boolean removeWhenFarAway(double distance) { return false; }

	@Override
	protected void dropEquipment()
	{
		super.dropEquipment();
		Containers.dropContents(level(), this, cargo);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag)
	{
		super.addAdditionalSaveData(tag);
		ContainerHelper.saveAllItems(tag, cargo.getItems(), registryAccess());
		tag.put(PAINT_TAG, paint.save());
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		cargo.clearContent();
		ContainerHelper.loadAllItems(tag, cargo.getItems(), registryAccess());
		setPaint(Paint.read(tag.getCompound(PAINT_TAG), defaultPaint));
	}

	public record Paint(String series, boolean usingPaintPreset, String paintPreset, int baseTint, int domeTint, int paintTint)
	{
		private static Paint preset(String name) { return new Paint("r", true, name, 0, 0, 0); }

		private static Paint read(CompoundTag tag, Paint fallback)
		{
			if (!tag.contains("series", Tag.TAG_STRING))
				return fallback;
			String name = tag.getString("paintPreset");
			var texture = ResourceLocation.tryBuild(Galaxies.MODID, name);
			if (texture == null || !name.matches("[a-z0-9_-]+"))
				name = fallback.paintPreset;
			return new Paint(tag.getString("series"), tag.getBoolean("usingPaintPreset"), name,
					tag.getInt("baseTint") & 0xffffff, tag.getInt("domeTint") & 0xffffff, tag.getInt("paintTint") & 0xffffff);
		}

		private CompoundTag save()
		{
			var tag = new CompoundTag();
			tag.putString("series", series);
			tag.putBoolean("usingPaintPreset", usingPaintPreset);
			tag.putString("paintPreset", paintPreset);
			tag.putInt("baseTint", baseTint);
			tag.putInt("domeTint", domeTint);
			tag.putInt("paintTint", paintTint);
			return tag;
		}
	}

	public static final class Menu extends WorkstationMenu
	{
		public Menu(int id, Inventory slots) { this(id, slots, new SimpleContainer(5)); }

		Menu(int id, Inventory slots, Container cargo)
		{
			super(Creatures.DROID_MENU.get(), id, cargo);
			for (int i = 0; i < 5; i++)
				addMachineSlot(i, 8 + i * 18, 18, 64, stack -> true);
			addPlayerSlots(slots, 84);
		}
	}
}
