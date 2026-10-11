package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.lhy.mest.terminal.ToolkitBarState;

public record ToolkitQuickMovePacket(boolean enabled) {
    private static final PlayerTickBudgets BUDGETS = new PlayerTickBudgets(4);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public static ToolkitQuickMovePacket read(FriendlyByteBuf buf) {
        return new ToolkitQuickMovePacket(buf.readBoolean());
    }

    public static void handle(ToolkitQuickMovePacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !BUDGETS.tryAcquire(player)) {
                return;
            }
            if (ToolkitBarState.hasTerminal(player)) {
                ToolkitBarState.setQuickMove(player, packet.enabled());
            }
        });
    }
}
