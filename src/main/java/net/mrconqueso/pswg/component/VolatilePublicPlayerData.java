package net.mrconqueso.pswg.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

public class VolatilePublicPlayerData implements INBTSerializable<CompoundTag>
{
	private final Player provider;

	private static final int PATROL_POSTURE_SYNCOP = 1;
	private boolean patrolPosture = false;

	public VolatilePublicPlayerData(Player provider)
	{
		this.provider = provider;
	}

	public boolean isPatrolPosture()
	{
		return patrolPosture;
	}

	public void setPatrolPosture(boolean patrolPosture)
	{
		this.patrolPosture = patrolPosture;
		sync(PATROL_POSTURE_SYNCOP);
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider lookupProvider)
	{
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("patrol_posture", patrolPosture);
		return tag;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag tag)
	{
		patrolPosture = tag.getBoolean("patrol_posture");
	}

	public void syncAll()
	{
		sync(0);
	}

	private void sync(int syncOp)
	{
		if (provider instanceof ServerPlayer serverPlayer)
		{
			// TODO: Send packet via PacketDistributor.sendToPlayersTrackingEntityAndSelf(...)
		}
	}

	public void writeSyncPacket(FriendlyByteBuf buf, int syncOp)
	{
		buf.writeInt(syncOp);

		switch (syncOp)
		{
			case 0 ->
			{ // Full sync
				CompoundTag tag = new CompoundTag();
				tag.putBoolean("patrol_posture", patrolPosture);
				buf.writeNbt(tag);
			}
			case PATROL_POSTURE_SYNCOP -> buf.writeBoolean(patrolPosture);
		}
	}

	public void applySyncPacket(FriendlyByteBuf buf)
	{
		var syncOp = buf.readInt();

		switch (syncOp)
		{
			case 0 ->
			{ // Full sync
				var tag = buf.readNbt();
				if (tag != null)
					this.deserializeNBT(null, tag);
			}
			case PATROL_POSTURE_SYNCOP ->
			{
				patrolPosture = buf.readBoolean();
			}
		}
	}
}