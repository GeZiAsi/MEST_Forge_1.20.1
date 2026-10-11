package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.lhy.mest.terminal.ToolkitBarActions;

/** Persist the toolkit-hotbar toggle on the spliced terminal item. */
public record ToolkitBarTogglePacket(boolean enabled) {
    private static final PlayerTickBudgets BUDGETS = new PlayerTickBudgets(4);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(enabled);
    }

    public static ToolkitBarTogglePacket read(FriendlyByteBuf buf) {
        return new ToolkitBarTogglePacket(buf.readBoolean());
    }

    public static void handle(ToolkitBarTogglePacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && BUDGETS.tryAcquire(player)) {
                ToolkitBarActions.setBarEnabled(player, packet.enabled());
            }
        });
    }
}
