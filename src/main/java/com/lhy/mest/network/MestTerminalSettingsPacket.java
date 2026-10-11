package com.lhy.mest.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import de.mari_023.ae2wtlib.AE2wtlibTags;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

import com.lhy.mest.terminal.MESTMenu;

/**
 * Wireless-terminal settings of the open spliced terminal. AE2WTLib 15's own settings packet
 * replaces the whole item tag with the client's copy, which would roll back the pattern cache,
 * trash and encoder state MEST keeps on the same stack; this one only writes the four flags.
 */
public record MestTerminalSettingsPacket(boolean pickBlock, boolean craftIfMissing, boolean restock,
        byte magnetMode) {
    private static final PlayerTickBudgets BUDGETS = new PlayerTickBudgets(8);

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(pickBlock);
        buf.writeBoolean(craftIfMissing);
        buf.writeBoolean(restock);
        buf.writeByte(magnetMode);
    }

    public static MestTerminalSettingsPacket read(FriendlyByteBuf buf) {
        return new MestTerminalSettingsPacket(buf.readBoolean(), buf.readBoolean(), buf.readBoolean(), buf.readByte());
    }

    public static void handle(MestTerminalSettingsPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !(player.containerMenu instanceof MESTMenu menu)
                    || !menu.canUseTerminal(player)
                    || !BUDGETS.tryAcquire(player)) {
                return;
            }
            ItemStack stack = menu.getMestHost().getItemStack();
            CompoundTag tag = stack.getOrCreateTag();
            tag.putBoolean(AE2wtlibTags.PICK_BLOCK, packet.pickBlock());
            tag.putBoolean(AE2wtlibTags.CRAFT_IF_MISSING, packet.craftIfMissing());
            tag.putBoolean(AE2wtlibTags.RESTOCK, packet.restock());
            // Only accepted while a magnet card is installed, same as AE2WTLib.
            MagnetHandler.saveMagnetMode(stack, MagnetMode.fromByte(packet.magnetMode()));
        });
    }
}
