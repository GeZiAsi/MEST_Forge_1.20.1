package com.lhy.mest.client;

import net.minecraft.client.Minecraft;
import com.lhy.mest.network.PacketContext;

import com.lhy.mest.network.ProviderPickerListPacket;

public final class ProviderPickerClientHandler {
    private ProviderPickerClientHandler() {
    }

    public static void handle(ProviderPickerListPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (Minecraft.getInstance().screen instanceof MESTScreen screen) {
                screen.applyProviderPickerList(packet);
            }
        });
    }
}
