package dev.pswg.species;

import dev.pswg.species.Species.Appearance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class Mannequin extends ArmorStand
{
	private static final EntityDataAccessor<CompoundTag> APPEARANCE = SynchedEntityData.defineId(Mannequin.class, EntityDataSerializers.COMPOUND_TAG);
	private Appearance cachedAppearance = Species.DEFAULT;

	public Mannequin(EntityType<? extends ArmorStand> type, Level level) { super(type, level); }

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder data)
	{
		super.defineSynchedData(data);
		data.define(APPEARANCE, new CompoundTag());
	}

	public Appearance appearance() { return cachedAppearance; }

	public void appearance(Appearance appearance)
	{
		var encoded = (CompoundTag) Appearance.CODEC.encodeStart(NbtOps.INSTANCE, appearance).getOrThrow();
		entityData.set(APPEARANCE, encoded);
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key)
	{
		super.onSyncedDataUpdated(key);
		if (APPEARANCE.equals(key))
			cachedAppearance = Appearance.CODEC.parse(NbtOps.INSTANCE, entityData.get(APPEARANCE)).result()
					.flatMap(Species::validate).orElse(Species.DEFAULT);
	}

	@Override
	public InteractionResult interactAt(Player player, Vec3 hit, InteractionHand hand)
	{
		if (!isMarker() && player.getItemInHand(hand).is(SpeciesItems.WIZARD.get()))
		{
			if (player instanceof ServerPlayer server)
				SpeciesNetwork.open(server, getId(), appearance());
			return InteractionResult.sidedSuccess(level().isClientSide);
		}
		return super.interactAt(player, hit, hand);
	}

	@Override public ItemStack getPickResult() { return new ItemStack(SpeciesItems.MANNEQUIN.get()); }

	@Override
	public void addAdditionalSaveData(CompoundTag data)
	{
		super.addAdditionalSaveData(data);
		data.put("species", entityData.get(APPEARANCE));
	}

	@Override
	public void readAdditionalSaveData(CompoundTag data)
	{
		super.readAdditionalSaveData(data);
		entityData.set(APPEARANCE, data.getCompound("species"));
	}
}
