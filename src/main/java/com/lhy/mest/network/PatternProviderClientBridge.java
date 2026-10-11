package com.lhy.mest.network;

import java.util.Objects;

import java.util.function.BiConsumer;

/**
 * Common-side registration target for a clientbound payload.
 *
 * <p>The dedicated server registers the payload without linking client classes. The physical
 * client installs its UI handler during client initialization before any play payload can arrive.
 */
public final class PatternProviderClientBridge {
    private static volatile BiConsumer<PatternProviderListPacket, PacketContext> handler = (packet, context) -> {
    };

    private PatternProviderClientBridge() {
    }

    public static void install(BiConsumer<PatternProviderListPacket, PacketContext> clientHandler) {
        handler = Objects.requireNonNull(clientHandler, "clientHandler");
    }

    public static void handle(PatternProviderListPacket packet, PacketContext context) {
        if (packet.isWellFormed()) {
            handler.accept(packet, context);
        }
    }
}