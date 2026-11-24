package net.mrconqueso.pswg.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.mrconqueso.pswg.character.SwgSpecies;
import net.mrconqueso.pswg.container.SwgSpeciesRegistry;
import org.jetbrains.annotations.UnknownNullability;

public class PersistentPublicPlayerData implements INBTSerializable<CompoundTag>
{
	private final Player provider;

	private static final int CHARACTER_SYNCOP = 1;
	private String character = "";

	public PersistentPublicPlayerData(Player provider)
	{
		this.provider = provider;
	}

	public SwgSpecies getCharacter()
	{
		if (character.isEmpty())
			return null;

		return SwgSpeciesRegistry.deserialize(character);
	}

	public void setCharacter(SwgSpecies character)
	{
		this.character = "";

		if (character != null)
			this.character = character.serialize();

		// In NeoForge, you must send a CustomPacketPayload to sync data.
		// Example: PacketDistributor.sendToPlayersTrackingEntityAndSelf(provider, new YourSyncPacket(provider.getId(), CHARACTER_SYNCOP, this.character));
		sync(CHARACTER_SYNCOP);
	}

	@Override
	public @UnknownNullability CompoundTag serializeNBT(HolderLookup.Provider lookupProvider)
	{
		CompoundTag tag = new CompoundTag();
		tag.putString("character", character);
		return tag;
	}

	@Override
	public void deserializeNBT(HolderLookup.Provider lookupProvider, CompoundTag tag)
	{
		character = tag.getString("character");
		onCharacterChange();
	}

	private void onCharacterChange()
	{
		provider.refreshDimensions();
	}

	public void syncAll()
	{
		sync(0);
	}

	private void sync(int syncOp)
	{
		if (provider instanceof ServerPlayer serverPlayer)
		{
			// TODO: Fire your NeoForge CustomPacketPayload here to sync to clients
		}
	}

	// Preserved for your new CustomPacketPayload implementation
	public void writeSyncPacket(FriendlyByteBuf buf, int syncOp)
	{
		buf.writeInt(syncOp);

		switch (syncOp)
		{
			case 0 ->
			{ // Full sync
				CompoundTag tag = new CompoundTag();
				tag.putString("character", character);
				buf.writeNbt(tag);
			}
			case CHARACTER_SYNCOP -> buf.writeUtf(character);
		}
	}

	// Preserved for your new CustomPacketPayload implementation
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
			case CHARACTER_SYNCOP ->
			{
				character = buf.readUtf();
				onCharacterChange();
			}
		}
	}
}