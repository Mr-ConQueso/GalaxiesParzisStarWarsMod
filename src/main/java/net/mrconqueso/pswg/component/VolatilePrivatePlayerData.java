package net.mrconqueso.pswg.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.UnknownNullability;

public class VolatilePrivatePlayerData implements INBTSerializable<CompoundTag>
{
	private final Player provider;

	private static final int CREDITS_SYNCOP = 1;
	private int credits;

	public VolatilePrivatePlayerData(Player provider)
	{
		this.provider = provider;
	}

	public int getCredits()
	{
		return credits;
	}

	public void setCredits(int credits)
	{
		this.credits = credits;
		sync(CREDITS_SYNCOP);
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider lookupProvider)
	{
		CompoundTag tag = new CompoundTag();
		tag.putInt("credits", credits);
		return tag;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag tag)
	{
		credits = tag.getInt("credits");
	}

	public void syncAll()
	{
		sync(0);
	}

	private void sync(int syncOp)
	{
		if (provider instanceof ServerPlayer serverPlayer)
		{
			// TODO: Send packet via PacketDistributor.sendToPlayer(serverPlayer, new YourSyncPacket(...))
		}
	}

	public void writeSyncPacket(FriendlyByteBuf buf, int syncOp)
	{
		buf.writeInt(syncOp);

		switch (syncOp)
		{
			case 0 -> { // Full sync
				CompoundTag tag = new CompoundTag();
				tag.putInt("credits", credits);
				buf.writeNbt(tag);
			}
			case CREDITS_SYNCOP -> buf.writeInt(credits);
		}
	}

	public void applySyncPacket(FriendlyByteBuf buf)
	{
		var syncOp = buf.readInt();

		switch (syncOp)
		{
			case 0 -> { // Full sync
				var tag = buf.readNbt();
				if (tag != null)
					this.deserializeNBT(null, tag);
			}
			case CREDITS_SYNCOP -> credits = buf.readInt();
		}
	}
}