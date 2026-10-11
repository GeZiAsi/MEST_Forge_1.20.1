package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import com.lhy.mest.compat.MestWtlibSupport;

/** Middle-click pick from ME using the spliced terminal's pick-block setting. */
public record MestPickBlockPacket(ItemStack stack) {
    /** Middle-click is a human-paced action; a burst of packets within one tick is never legitimate. */
    private static final PlayerTickBudgets BUDGETS = new PlayerTickBudgets(4);

    public void write(FriendlyByteBuf buf) {
        buf.writeItem(stack);
    }

    public static MestPickBlockPacket read(FriendlyByteBuf buf) {
        return new MestPickBlockPacket(buf.readItem());
    }

    public static void handle(MestPickBlockPacket packet, PacketContext context) {
        if (packet.stack().isEmpty()) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && BUDGETS.tryAcquire(player)) {
                MestWtlibSupport.pickBlock(player, packet.stack());
            }
        });
    }
}
