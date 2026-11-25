package dev.pswg.mobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class Worrt extends Rabbit
{
	public Worrt(EntityType<? extends Worrt> type, Level level)
	{
		super(type, level);
	}

	@Override
	protected void registerGoals()
	{
		goalSelector.addGoal(1, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 0.5)
		{
			@Override
			public void tick()
			{
				super.tick();
				setSpeedModifier(0.5);
			}
		});
		goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 8, 0.3, 0.7));
		goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Monster.class, 4, 0.3, 0.7));
		goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7, 5000));
		goalSelector.addGoal(11, new LookAtPlayerGoal(this, Player.class, 10));
	}

	@Override
	protected float getJumpPower()
	{
		if (horizontalCollision || moveControl.hasWanted() && moveControl.getWantedY() > getY() + 0.5)
			return 0.75F;
		var path = navigation.getPath();
		if (path != null && !path.isDone() && path.getNextEntityPos(this).y > getY() + 0.5)
			return 0.5F;
		return (float) moveControl.getSpeedModifier() * 0.5F;
	}

	public float airborneProgress(float partial)
	{
		var position = getPosition(partial);
		var ground = level().clip(new ClipContext(position, position.add(0, -1, 0),
				ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
		return ground.getType() == HitResult.Type.MISS ? 1F : (float) Math.min(1, position.distanceTo(ground.getLocation()));
	}

	@Override protected SoundEvent getJumpSound() { return SoundEvents.EMPTY; }
	@Override protected SoundEvent getAmbientSound() { return null; }
	@Override protected SoundEvent getHurtSound(DamageSource source) { return null; }
	@Override protected SoundEvent getDeathSound() { return null; }
	@Override public boolean canSpawnSprintParticle() { return false; }
	@Override public boolean isFood(ItemStack stack) { return false; }
	@Override public Worrt getBreedOffspring(ServerLevel level, AgeableMob other) { return null; }
}
