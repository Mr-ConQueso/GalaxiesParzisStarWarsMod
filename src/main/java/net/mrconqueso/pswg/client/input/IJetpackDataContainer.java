package net.mrconqueso.pswg.client.input;

import org.joml.Vector3d;

import java.util.EnumSet;

public interface IJetpackDataContainer
{
	void pswg_setJetpackControls(EnumSet<JetpackControls> controls);

	EnumSet<JetpackControls> pswg_getJetpackControls();

	void pswg_setJetpackForce(Vector3d force);

	Vector3d pswg_getJetpackForce();

	void pswg_tryCancelFallFlying();
}
