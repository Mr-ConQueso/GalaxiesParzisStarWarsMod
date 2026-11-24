package net.mrconqueso.pswg.util;

import net.minecraft.world.InteractionHand;

public enum BlasterWield
{
	Invalid(null, false, false, false),
	None(null, false, false, false),
	SingleMain(InteractionHand.MAIN_HAND, true, false, true),
	SingleOff(InteractionHand.OFF_HAND, false, true, true),
	DoubleMain(InteractionHand.MAIN_HAND, true, true, true),
	DoubleOff(InteractionHand.OFF_HAND, true, true, true),
	Dual(InteractionHand.MAIN_HAND, true, true, false);

	private final InteractionHand baseHand;

	public final boolean hasBlaster;
	public final boolean mainHandOccupied;
	public final boolean offHandOccupied;
	public final boolean oneWeapon;

	BlasterWield(InteractionHand baseHand, boolean mainHandOccupied, boolean offHandOccupied, boolean oneWeapon)
	{
		this.hasBlaster = baseHand != null;
		this.baseHand = baseHand;
		this.mainHandOccupied = mainHandOccupied;
		this.offHandOccupied = offHandOccupied;
		this.oneWeapon = oneWeapon;
	}

	public boolean isBaseHand(InteractionHand hand)
	{
		if (hand == null)
			return false;
		return this.baseHand == hand;
	}

	public boolean areBothHandsOccupied()
	{
		return mainHandOccupied && offHandOccupied;
	}
}
