package dev.pswg.mobs;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public final class Worrt extends PathfinderMob
{
	private static final EntityDataAccessor<Integer> TONGUE_TARGET = SynchedEntityData.defineId(Worrt.class, EntityDataSerializers.INT);

	public Worrt(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data)
	{
		super.defineSynchedData(data);
		data.define(TONGUE_TARGET, -1);
	}

	public int tongueTarget() { return entityData.get(TONGUE_TARGET); }

	@Override
	protected void registerGoals()
	{
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 2));
		goalSelector.addGoal(2, new TongueGoal());
		goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1));
		goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 6));
		targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, SandSkitter.class, 5, true, false, entity -> entity.getType().is(Creatures.WORRT_FOOD)));
	}

	private final class TongueGoal extends Goal
	{
		private int remaining;
		private int cooldown;

		TongueGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }
		@Override public boolean canUse()
		{
			return --cooldown <= 0 && getTarget() != null && getTarget().isAlive() && distanceToSqr(getTarget()) < 36;
		}
		@Override public boolean canContinueToUse() { return remaining > 0 && getTarget() != null && getTarget().isAlive(); }
		@Override public boolean requiresUpdateEveryTick() { return true; }

		@Override
		public void start()
		{
			remaining = 20;
			getNavigation().stop();
			entityData.set(TONGUE_TARGET, getTarget().getId());
			playSound(SoundEvents.FROG_TONGUE, 1, 1);
		}

		@Override
		public void tick()
		{
			remaining--;
			var target = getTarget();
			if (target == null || !hasLineOfSight(target))
				return;
			getLookControl().setLookAt(target);
			if (distanceToSqr(target) <= 0.25)
			{
				target.discard();
				playSound(SoundEvents.PLAYER_BURP, 1, 1);
				remaining = 0;
			}
			else
				target.setDeltaMovement(position().subtract(target.position()).normalize().scale(0.75));
		}

		@Override
		public void stop()
		{
			entityData.set(TONGUE_TARGET, -1);
			setTarget(null);
			cooldown = 20;
		}
	}
}
