package net.mrconqueso.pswg.component;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class PlayerData
{
	public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "pswg");

	public static final Supplier<AttachmentType<PersistentPublicPlayerData>> PERSISTENT_PUBLIC = ATTACHMENT_TYPES.register(
			"per_pub",
			() -> AttachmentType.builder((holder) -> new PersistentPublicPlayerData((Player) holder))
					.copyOnDeath()
					.build()
	);

	public static final Supplier<AttachmentType<VolatilePublicPlayerData>> VOLATILE_PUBLIC = ATTACHMENT_TYPES.register(
			"vol_pub",
			() -> AttachmentType.builder((holder) -> new VolatilePublicPlayerData((Player) holder))
					.build()
	);

	public static final Supplier<AttachmentType<VolatilePrivatePlayerData>> VOLATILE_PRIVATE = ATTACHMENT_TYPES.register(
			"vol_prv",
			() -> AttachmentType.builder((holder) -> new VolatilePrivatePlayerData((Player) holder))
					.build()
	);

	public static PersistentPublicPlayerData getPersistentPublic(Player player)
	{
		return player.getData(PERSISTENT_PUBLIC);
	}

	public static VolatilePublicPlayerData getVolatilePublic(Player player)
	{
		return player.getData(VOLATILE_PUBLIC);
	}

	public static VolatilePrivatePlayerData getVolatilePrivate(Player player)
	{
		return player.getData(VOLATILE_PRIVATE);
	}
}