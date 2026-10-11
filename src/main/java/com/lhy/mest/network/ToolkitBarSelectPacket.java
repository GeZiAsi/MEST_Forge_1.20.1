package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.lhy.mest.terminal.ToolkitBarActions;

/**
 * Selects one of the 27 visible quick-bar cells. Extra bars only change page; slot is Inventory.selected.
 *
 * <p>Applied immediately on the main thread, ahead of a use packet that arrived after it. A fast
 * scroll can emit more than a handful of these per tick; dropping one would leave the server on
 * the previous page and the following equip would hit the vanilla hotbar instead.
 */
public record ToolkitBarSelectPacket(int index) {
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(index);
    }

    public static ToolkitBarSelectPacket read(FriendlyByteBuf buf) {
        return new ToolkitBarSelectPacket(buf.readVarInt());
    }

    public static void handle(ToolkitBarSelectPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                ToolkitBarActions.selectVisibleCell(player, packet.index());
            }
        });
    }
}
