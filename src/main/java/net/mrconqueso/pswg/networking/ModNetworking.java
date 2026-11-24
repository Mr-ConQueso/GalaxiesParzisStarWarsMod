package net.mrconqueso.pswg.networking;

import net.mrconqueso.pswg.PSWG;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = PSWG.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class ModNetworking {

    @SubscribeEvent
    public static void registerPayloads(final RegisterPayloadHandlersEvent event) {
        // Sets up the registrar for your mod
        final PayloadRegistrar registrar = event.registrar(PSWG.MOD_ID);

        // Serverbound (C2S) packets
        registrar.playToServer(
                PlayerInteractItemLeftC2SPacket.ID,
                PlayerInteractItemLeftC2SPacket.CODEC,
                PlayerInteractItemLeftC2SPacket::handle // You must pass your handling method here!
        );

        registrar.playToServer(
                PlayerItemActionPayload.TYPE,
                PlayerItemActionPayload.STREAM_CODEC,
                (payload, context) -> {
                    // This runs on the server when the packet is received
                    context.enqueueWork(() -> {
                        // DO SERVER LOGIC HERE
                        // e.g., System.out.println("Received action: " + payload.action());
                    });
                }
        );

        registrar.playToServer(
                GalaxiesPlayerActionC2SPacket.ID,
                GalaxiesPlayerActionC2SPacket.CODEC,
                GalaxiesPlayerActionC2SPacket::handle
        );

        // Clientbound (S2C) packets
        registrar.playToClient(
                GalaxiesPlayerActionS2CPacket.ID,
                GalaxiesPlayerActionS2CPacket.CODEC,
                GalaxiesPlayerActionS2CPacket::handle
        );

        // PayloadTypeRegistry.clientboundPlay().register(GalaxiesEntitySpawnS2CPacket.ID, GalaxiesEntitySpawnS2CPacket.CODEC);

        // PayloadTypeRegistry.clientboundPlay().register(PreciseVelocityParticleS2CPayload.TYPE, PreciseVelocityParticleS2CPayload.CODEC);
    }
}