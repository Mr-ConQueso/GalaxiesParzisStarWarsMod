package net.mrconqueso.pswg.data.blaster;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.mrconqueso.pswg.Resources;
import net.mrconqueso.pswg.component.ModDataComponents;
import net.mrconqueso.pswg.item.custom.BlasterItem;

import java.util.Objects;
import java.util.function.Consumer;

public class BlasterData
{
	public static final byte COOLING_MODE_NONE = 0;
	public static final byte COOLING_MODE_OVERHEAT = 1;
	public static final byte COOLING_MODE_FORCED_BYPASS = 2;
	public static final byte COOLING_MODE_PENALTY_BYPASS = 3;

	public byte firingMode;
	public byte coolingMode;
	public boolean isAimingDownSights;
	public boolean canBypassCooling;
	public int shotsRemaining;
	public int heat;
	public int ventingHeat;
	public short burstCounter;
	public short shotTimer;
	public short timeSinceLastShot;
	public short passiveCooldownTimer;
	public short overchargeTimer;
	public short readyTimer;
	public long uid;
	public int attachmentBitmask;

	// A default empty state used when an item doesn't have the component yet
	public static final BlasterData EMPTY = new BlasterData();

	public BlasterData() {}

	public BlasterData(byte firingMode, byte coolingMode, boolean isAimingDownSights, boolean canBypassCooling, int shotsRemaining, int heat, int ventingHeat, short burstCounter, short shotTimer, short timeSinceLastShot, short passiveCooldownTimer, short overchargeTimer, short readyTimer, long uid, int attachmentBitmask)
	{
		this.firingMode = firingMode;
		this.coolingMode = coolingMode;
		this.isAimingDownSights = isAimingDownSights;
		this.canBypassCooling = canBypassCooling;
		this.shotsRemaining = shotsRemaining;
		this.heat = heat;
		this.ventingHeat = ventingHeat;
		this.burstCounter = burstCounter;
		this.shotTimer = shotTimer;
		this.timeSinceLastShot = timeSinceLastShot;
		this.passiveCooldownTimer = passiveCooldownTimer;
		this.overchargeTimer = overchargeTimer;
		this.readyTimer = readyTimer;
		this.uid = uid;
		this.attachmentBitmask = attachmentBitmask;
	}

	/**
	 * Creates a duplicate of this data.
	 * Data Components must be immutable once attached to an ItemStack.
	 * To change values, we copy it, modify the copy, and apply it back.
	 */
	public BlasterData copy()
	{
		return new BlasterData(firingMode, coolingMode, isAimingDownSights, canBypassCooling, shotsRemaining, heat, ventingHeat, burstCounter, shotTimer, timeSinceLastShot, passiveCooldownTimer, overchargeTimer, readyTimer, uid, attachmentBitmask);
	}

	/**
	 * Extracts the current BlasterData from the stack, copies it, passes it to the action for modification,
	 * and saves the updated copy back to the ItemStack.
	 */
	public static void mutate(ItemStack stack, Consumer<BlasterData> action)
	{
		BlasterData data = stack.getOrDefault(ModDataComponents.BLASTER_DATA.get(), BlasterData.EMPTY).copy();
		action.accept(data);
		stack.set(ModDataComponents.BLASTER_DATA.get(), data);
	}

	public void tick(BlasterDescriptor bd)
	{
		if (uid == 0) uid = Resources.RANDOM.nextLong();
		if (passiveCooldownTimer > 0) passiveCooldownTimer--;
		if (shotTimer > 0) shotTimer--;
		if (overchargeTimer > 0) overchargeTimer--;
		if (readyTimer > 0) readyTimer--;

		if (timeSinceLastShot < 100)
			timeSinceLastShot++;

		if (ventingHeat > 0)
		{
			ventingHeat -= (int)Math.ceil(bd.heat.overheatDrainSpeed * BlasterItem.getCoolingMultiplier(bd, attachmentBitmask));
			if (ventingHeat <= 0)
			{
				ventingHeat = 0;
				coolingMode = COOLING_MODE_NONE;
			}
		}

		if (heat > 0 && passiveCooldownTimer == 0)
		{
			heat -= bd.heat.drainSpeed;
			if (heat < 0) heat = 0;
		}
	}

	public boolean isCooling()
	{
		return coolingMode != COOLING_MODE_NONE;
	}

	// --- DATA COMPONENT REQUIREMENTS ---
	// Data components absolutely require equals() and hashCode() to stack items properly.

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BlasterData that = (BlasterData) o;
		return firingMode == that.firingMode && coolingMode == that.coolingMode && isAimingDownSights == that.isAimingDownSights && canBypassCooling == that.canBypassCooling && shotsRemaining == that.shotsRemaining && heat == that.heat && ventingHeat == that.ventingHeat && burstCounter == that.burstCounter && shotTimer == that.shotTimer && timeSinceLastShot == that.timeSinceLastShot && passiveCooldownTimer == that.passiveCooldownTimer && overchargeTimer == that.overchargeTimer && readyTimer == that.readyTimer && uid == that.uid && attachmentBitmask == that.attachmentBitmask;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(firingMode, coolingMode, isAimingDownSights, canBypassCooling, shotsRemaining, heat, ventingHeat, burstCounter, shotTimer, timeSinceLastShot, passiveCooldownTimer, overchargeTimer, readyTimer, uid, attachmentBitmask);
	}

	// Codec for saving to disk (NBT)
	public static final Codec<BlasterData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BYTE.optionalFieldOf("firingMode", (byte)0).forGetter(d -> d.firingMode),
			Codec.BYTE.optionalFieldOf("coolingMode", (byte)0).forGetter(d -> d.coolingMode),
			Codec.BOOL.optionalFieldOf("isAimingDownSights", false).forGetter(d -> d.isAimingDownSights),
			Codec.BOOL.optionalFieldOf("canBypassCooling", false).forGetter(d -> d.canBypassCooling),
			Codec.INT.optionalFieldOf("shotsRemaining", 0).forGetter(d -> d.shotsRemaining),
			Codec.INT.optionalFieldOf("heat", 0).forGetter(d -> d.heat),
			Codec.INT.optionalFieldOf("ventingHeat", 0).forGetter(d -> d.ventingHeat),
			Codec.SHORT.optionalFieldOf("burstCounter", (short)0).forGetter(d -> d.burstCounter),
			Codec.SHORT.optionalFieldOf("shotTimer", (short)0).forGetter(d -> d.shotTimer),
			Codec.SHORT.optionalFieldOf("timeSinceLastShot", (short)0).forGetter(d -> d.timeSinceLastShot),
			Codec.SHORT.optionalFieldOf("passiveCooldownTimer", (short)0).forGetter(d -> d.passiveCooldownTimer),
			Codec.SHORT.optionalFieldOf("overchargeTimer", (short)0).forGetter(d -> d.overchargeTimer),
			Codec.SHORT.optionalFieldOf("readyTimer", (short)0).forGetter(d -> d.readyTimer),
			Codec.LONG.optionalFieldOf("uid", 0L).forGetter(d -> d.uid),
			Codec.INT.optionalFieldOf("attachmentBitmask", 0).forGetter(d -> d.attachmentBitmask)
	).apply(instance, BlasterData::new));

	// StreamCodec for syncing across the network smoothly
	public static final StreamCodec<ByteBuf, BlasterData> STREAM_CODEC = new StreamCodec<>() {
		@Override
		public BlasterData decode(ByteBuf buf) {
			return new BlasterData(
					buf.readByte(), buf.readByte(), buf.readBoolean(), buf.readBoolean(),
					buf.readInt(), buf.readInt(), buf.readInt(), buf.readShort(),
					buf.readShort(), buf.readShort(), buf.readShort(), buf.readShort(),
					buf.readShort(), buf.readLong(), buf.readInt()
			);
		}

		@Override
		public void encode(ByteBuf buf, BlasterData d) {
			buf.writeByte(d.firingMode);
			buf.writeByte(d.coolingMode);
			buf.writeBoolean(d.isAimingDownSights);
			buf.writeBoolean(d.canBypassCooling);
			buf.writeInt(d.shotsRemaining);
			buf.writeInt(d.heat);
			buf.writeInt(d.ventingHeat);
			buf.writeShort(d.burstCounter);
			buf.writeShort(d.shotTimer);
			buf.writeShort(d.timeSinceLastShot);
			buf.writeShort(d.passiveCooldownTimer);
			buf.writeShort(d.overchargeTimer);
			buf.writeShort(d.readyTimer);
			buf.writeLong(d.uid);
			buf.writeInt(d.attachmentBitmask);
		}
	};
}