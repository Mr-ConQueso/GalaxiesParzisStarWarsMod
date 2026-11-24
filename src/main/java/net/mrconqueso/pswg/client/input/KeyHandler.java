package net.mrconqueso.pswg.client.input;

import net.minecraft.client.Minecraft;
import net.mrconqueso.pswg.client.PSWGClient;
import net.mrconqueso.pswg.entity.ShipEntity;
import net.mrconqueso.pswg.networking.PlayerItemActionPayload;
import net.mrconqueso.pswg.util.item.ItemAction;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.EnumSet;

public class KeyHandler {
	public static void tick(Minecraft mc) {
		if (mc.player == null) return;

//		if (PSWGClient.KEY_SPECIES_SELECT.consumeClick())
//			mc.setScreen(new CharacterScreen(mc.screen));

		if (PSWGClient.KEY_PRIMARY_ITEM_ACTION.consumeClick())
			sendItemAction(ItemAction.PRIMARY);

		if (PSWGClient.KEY_SECONDARY_ITEM_ACTION.consumeClick())
			sendItemAction(ItemAction.SECONDARY);

		if (PSWGClient.KEY_PATROL_POSTURE.consumeClick()) {
			// NeoForge 1.21.1 uses PacketDistributor with custom Payloads
			// PacketDistributor.sendToServer(new TogglePatrolPosturePayload());
		}
		var ship = ShipEntity.getShip(mc.player);

		if (ship != null) {
			var controls = EnumSet.noneOf(ShipControls.class);

			if (mc.options.keyUp.isDown())
				controls.add(ShipControls.THROTTLE_UP);

			if (mc.options.keyDown.isDown())
				controls.add(ShipControls.THROTTLE_DOWN);

			if (mc.options.keyRight.isDown())
				controls.add(ShipControls.SPECIAL1);

			if (mc.options.keyLeft.isDown())
				controls.add(ShipControls.SPECIAL2);

			ship.acceptControlInput(controls);
		}

		/*
		var jetpack = JetpackItem.getEquippedJetpack(mc.player);
		if (!jetpack.isEmpty()) {
			var originalControls = ((IJetpackDataContainer)mc.player).pswg_getJetpackControls();
			var controls = EnumSet.noneOf(JetpackControls.class);

			if (mc.options.keyUp.isDown())
				controls.add(JetpackControls.FORWARD);

			if (mc.options.keyDown.isDown())
				controls.add(JetpackControls.BACKWARD);

			if (mc.options.keyRight.isDown())
				controls.add(JetpackControls.RIGHT);

			if (mc.options.keyLeft.isDown())
				controls.add(JetpackControls.LEFT);

			if (mc.options.keySprint.isDown())
				controls.add(JetpackControls.MODE);

			if (mc.options.keyJump.isDown())
				controls.add(JetpackControls.ASCEND);

			if (mc.options.keyShift.isDown())
				controls.add(JetpackControls.DESCEND);

			if (!controls.equals(originalControls)) {
				// PacketDistributor.sendToServer(new JetpackControlsPayload(JetpackControls.pack(controls)));
			}

			((IJetpackDataContainer)mc.player).pswg_setJetpackControls(controls);
		}
		 */
	}

	private static void sendItemAction(ItemAction action) {
		PacketDistributor.sendToServer(new PlayerItemActionPayload(action));
	}
}