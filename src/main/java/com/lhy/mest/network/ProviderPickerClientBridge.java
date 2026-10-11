package com.lhy.mest.network;

import java.util.Objects;

import java.util.function.BiConsumer;

public final class ProviderPickerClientBridge {
    private static volatile BiConsumer<ProviderPickerListPacket, PacketContext> handler = (packet, context) -> {
    };

    private ProviderPickerClientBridge() {
    }

    public static void install(BiConsumer<ProviderPickerListPacket, PacketContext> clientHandler) {
        handler = Objects.requireNonNull(clientHandler, "clientHandler");
    }

    public static void handle(ProviderPickerListPacket packet, PacketContext context) {
        handler.accept(packet, context);
    }
}
