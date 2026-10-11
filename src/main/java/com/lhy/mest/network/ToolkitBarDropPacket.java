package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.lhy.mest.terminal.ToolkitBarActions;

/** Drops the selected extra-bar toolkit cell. Does not use vanilla hotbar drop. */
public record ToolkitBarDropPacket(boolean all) {
    private static final PlayerTickBudgets BUDGETS = new PlayerTickBudgets(8);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(all);
    }

    public static ToolkitBarDropPacket read(FriendlyByteBuf buf) {
        return new ToolkitBarDropPacket(buf.readBoolean());
    }

    public static void handle(ToolkitBarDropPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && BUDGETS.tryAcquire(player)) {
                ToolkitBarActions.dropSelected(player, packet.all());
            }
        });
    }
}
