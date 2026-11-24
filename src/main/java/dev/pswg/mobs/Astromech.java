package dev.pswg.mobs;

import dev.pswg.gadgets.WorkstationMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
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

public final class Astromech extends PathfinderMob
{
	private static final EntityDataAccessor<Boolean> EXTENDED = SynchedEntityData.defineId(Astromech.class, EntityDataSerializers.BOOLEAN);
	private final String paint;
	private float leg;
	private float lastLeg;
	private final SimpleContainer cargo = new SimpleContainer(5)
	{
		@Override public boolean stillValid(Player player) { return isAlive() && player.canInteractWithEntity(Astromech.this, 0); }
	};

	public Astromech(EntityType<? extends PathfinderMob> type, Level level, String paint)
	{
		super(type, level);
		this.paint = paint;
		moveControl = new MoveControl(this)
		{
			@Override protected float rotlerp(float from, float to, float maximum) { return super.rotlerp(from, to, Math.min(maximum, 6)); }
		};
	}

	@Override protected void defineSynchedData(SynchedEntityData.Builder data) { super.defineSynchedData(data); data.define(EXTENDED, false); }
	public String paint() { return paint; }
	public Container cargo() { return cargo; }
	public float extension(float partial) { return Mth.lerp(partial, lastLeg, leg); }

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
		super.tick();
		if (!level().isClientSide)
			entityData.set(EXTENDED, isLeashed() || !getNavigation().isDone());
		lastLeg = leg;
		leg = Mth.clamp(leg + (entityData.get(EXTENDED) ? 1 : -1) / 18F, 0, 1);
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
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag)
	{
		super.readAdditionalSaveData(tag);
		cargo.clearContent();
		ContainerHelper.loadAllItems(tag, cargo.getItems(), registryAccess());
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
