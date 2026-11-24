package dev.pswg.mobs;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;

public final class Bantha extends Animal
{
	private static final EntityDataAccessor<Boolean> BRED = SynchedEntityData.defineId(Bantha.class, EntityDataSerializers.BOOLEAN);
	public final AnimationState forage = new AnimationState();
	public final AnimationState call = new AnimationState();
	private int brayDelay = 1200;
	private int forageDelay = 10;
	private int foraging;

	public Bantha(EntityType<? extends Animal> type, Level level) { super(type, level); }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data)
	{
		super.defineSynchedData(data);
		data.define(BRED, false);
	}

	@Override
	protected void registerGoals()
	{
		goalSelector.addGoal(0, new FloatGoal(this));
		goalSelector.addGoal(1, new PanicGoal(this, 1.8));
		goalSelector.addGoal(2, new BreedGoal(this, 1));
		goalSelector.addGoal(3, new TemptGoal(this, 1, Ingredient.of(Creatures.BANTHA_FOOD), false));
		goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 1));
		goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
	}

	public boolean playerBred() { return entityData.get(BRED); }
	@Override public boolean isFood(ItemStack stack) { return stack.is(Creatures.BANTHA_FOOD); }

	@Override
	public Bantha getBreedOffspring(ServerLevel level, AgeableMob other)
	{
		var newborn = Creatures.BANTHA.get().create(level);
		if (newborn != null)
			newborn.entityData.set(BRED, true);
		return newborn;
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand)
	{
		var held = player.getItemInHand(hand);
		if (isFood(held))
		{
			if (getHealth() < getMaxHealth())
			{
				if (!level().isClientSide)
				{
					heal(2);
					held.consume(1, player);
					if (getAge() == 0 && canFallInLove()) setInLove(player);
					if (isBaby()) ageUp(10, true);
				}
				return InteractionResult.sidedSuccess(level().isClientSide);
			}
			return super.mobInteract(player, hand);
		}
		if (!isBaby() && !player.isSecondaryUseActive() && getPassengers().size() < 2)
		{
			if (held.isEmpty() && (level().isClientSide || player.startRiding(this)))
				return InteractionResult.sidedSuccess(level().isClientSide);
		}
		return super.mobInteract(player, hand);
	}

	@Override protected boolean canAddPassenger(Entity entity) { return !isBaby() && getPassengers().size() < 2 && entity instanceof Player; }
	@Override public LivingEntity getControllingPassenger() { return getFirstPassenger() instanceof Player pilot ? pilot : null; }

	@Override
	protected void tickRidden(Player pilot, Vec3 movement)
	{
		if (!playerBred())
		{
			if (!level().isClientSide) ejectPassengers();
			return;
		}
		setRot(pilot.getYRot(), pilot.getXRot() * 0.5F);
		yBodyRot = yHeadRot = getYRot();
	}

	@Override
	protected Vec3 getRiddenInput(Player pilot, Vec3 movement)
	{
		return playerBred() ? new Vec3(movement.x * 0.5, 0, movement.z <= 0 ? movement.z * 0.25 : movement.z) : Vec3.ZERO;
	}

	@Override
	protected float getRiddenSpeed(Player pilot)
	{
		return (float) getAttributeValue(Attributes.MOVEMENT_SPEED) + (pilot.isSprinting() ? 0.1F : 0);
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction move)
	{
		int seat = getPassengers().indexOf(passenger);
		var offset = new Vec3(0, getBbHeight() * 0.85, seat == 0 ? 0.35 : -0.65).yRot(-getYRot() * net.minecraft.util.Mth.DEG_TO_RAD);
		move.accept(passenger, getX() + offset.x, getY() + offset.y, getZ() + offset.z);
	}

	@Override
	protected void customServerAiStep()
	{
		super.customServerAiStep();
		if (isVehicle() || !onGround() || isPanicking())
		{
			foraging = 0;
			return;
		}
		if (--brayDelay <= 0)
		{
			brayDelay = random.nextIntBetweenInclusive(2400, 6000);
			playSound(SoundEvents.CAMEL_SIT, 3, 0.5F);
			level().broadcastEntityEvent(this, (byte) 4);
		}
		var ground = BlockPos.containing(position().add(getLookAngle().scale(1.4))).below();
		if (foraging > 0)
		{
			getNavigation().stop();
			if (--foraging == 0 && level().getBlockState(ground).is(Blocks.GRASS_BLOCK) && EventHooks.canEntityGrief(level(), this))
			{
				level().setBlock(ground, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
				level().levelEvent(2001, ground, Block.getId(Blocks.GRASS_BLOCK.defaultBlockState()));
			}
		}
		else if (--forageDelay <= 0 && level().getBlockState(ground).is(Blocks.GRASS_BLOCK))
		{
			forageDelay = random.nextIntBetweenInclusive(2400, 6000);
			foraging = 26;
			level().broadcastEntityEvent(this, (byte) 6);
		}
	}

	@Override
	public void handleEntityEvent(byte event)
	{
		if (event == 4) call.start(tickCount);
		else if (event == 6) forage.start(tickCount);
		else super.handleEntityEvent(event);
	}

	@Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putBoolean("PlayerBred", playerBred()); }
	@Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(BRED, tag.getBoolean("PlayerBred")); }
}
