package dev.pswg.mobs;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.level.Level;

public final class SandSkitter extends PathfinderMob
{
	public final AnimationState nibble = new AnimationState();
	private long nextNibble;
	private int nibbling;

	public SandSkitter(EntityType<? extends PathfinderMob> type, Level level) { super(type, level); }

	@Override
	protected void registerGoals()
	{
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 2.5));
		goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Worrt.class, 8, 1.5, 2.5));
		goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, 1));
		goalSelector.addGoal(4, new RandomLookAroundGoal(this));
	}

	@Override
	protected void customServerAiStep()
	{
		super.customServerAiStep();
		var ground = getBlockStateOn();
		if (nibbling > 0)
		{
			if (getLastHurtByMob() != null || !onGround() || !ground.is(Creatures.SKITTER_DIGGABLE))
				nibbling = 0;
			else
			{
				getNavigation().stop();
				if (--nibbling < 14 && nibbling % 5 == 0 && level() instanceof ServerLevel server)
				{
					var point = position().add(getLookAngle().scale(0.5));
					server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), point.x, getY(), point.z, 10, 0.05, 0.05, 0.05, 0.1);
					playSound(ground.getSoundType().getBreakSound());
				}
			}
		}
		else if (level().getGameTime() >= nextNibble && onGround() && ground.is(Creatures.SKITTER_DIGGABLE) && getLastHurtByMob() == null)
		{
			nextNibble = level().getGameTime() + random.nextIntBetweenInclusive(2400, 6000);
			nibbling = 36;
			level().broadcastEntityEvent(this, (byte) 6);
		}
	}

	@Override
	public void handleEntityEvent(byte event)
	{
		if (event == 6) nibble.start(tickCount);
		else super.handleEntityEvent(event);
	}
}
