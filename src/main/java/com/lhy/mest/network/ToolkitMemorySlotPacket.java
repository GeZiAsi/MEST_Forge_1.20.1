package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import com.lhy.mest.terminal.MESTMenu;

public record ToolkitMemorySlotPacket(int slotIndex, boolean remember) {
    private static final PlayerTickBudgets BUDGETS = new PlayerTickBudgets(16);

    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeBoolean(remember);
    }

    public static ToolkitMemorySlotPacket read(FriendlyByteBuf buf) {
        return new ToolkitMemorySlotPacket(buf.readVarInt(), buf.readBoolean());
    }

    public static void handle(ToolkitMemorySlotPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof MESTMenu menu
                    && BUDGETS.tryAcquire(player)) {
                menu.setToolkitMemorySlot(packet.slotIndex(), packet.remember());
            }
        });
    }
}
