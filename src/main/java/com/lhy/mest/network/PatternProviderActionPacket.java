package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.server.level.ServerPlayer;

import com.lhy.mest.terminal.MESTMenu;

/** A versioned action against one provider slot in the current menu session. */
public record PatternProviderActionPacket(
        int containerId,
        long epoch,
        long providerId,
        long revision,
        int providerSlot,
        Action action) {

    public enum Action {
        PICKUP_OR_SET_DOWN,
        QUICK_MOVE_TO_PLAYER,
        OPEN_PROVIDER_UI,
        INSERT_INTO_PROVIDER,
        DROP,
        DROP_STACK
    }

    private static final Action[] ACTIONS = Action.values();

    public static PatternProviderActionPacket read(FriendlyByteBuf buf) {
        int containerId = -1;
        try {
            containerId = buf.readVarInt();
            long epoch = buf.readVarLong();
            long providerId = buf.readVarLong();
            long revision = buf.readVarLong();
            int providerSlot = buf.readVarInt();
            int actionOrdinal = buf.readUnsignedByte();
            if (actionOrdinal >= ACTIONS.length || buf.isReadable()) {
                throw new IllegalArgumentException("Malformed pattern provider action");
            }
            return new PatternProviderActionPacket(
                    containerId, epoch, providerId, revision, providerSlot, ACTIONS[actionOrdinal]);
        } catch (RuntimeException exception) {
            discardRemaining(buf);
            return invalid(containerId);
        }
    }

    public void write(FriendlyByteBuf buf) {
        if (!isWellFormed()) {
            throw new IllegalStateException("Cannot encode a malformed pattern provider action");
        }
        buf.writeVarInt(containerId);
        buf.writeVarLong(epoch);
        buf.writeVarLong(providerId);
        buf.writeVarLong(revision);
        buf.writeVarInt(providerSlot);
        buf.writeByte(action.ordinal());
    }

    boolean isWellFormed() {
        return containerId >= 0
                && epoch > 0
                && providerId > 0
                && revision > 0
                && providerSlot >= 0
                && providerSlot < PatternProviderClientState.MAX_INVENTORY_SIZE
                && action != null;
    }

    private static PatternProviderActionPacket invalid(int containerId) {
        return new PatternProviderActionPacket(
                containerId, 0, 0, 0, -1, Action.PICKUP_OR_SET_DOWN);
    }

    private static void discardRemaining(FriendlyByteBuf buf) {
        try {
            buf.skipBytes(buf.readableBytes());
        } catch (RuntimeException ignored) {
            // Decoding must still return a rejected sentinel if the buffer itself is already invalid.
        }
    }

    public static void handle(PatternProviderActionPacket packet, PacketContext context) {
        if (!packet.isWellFormed()) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof MESTMenu menu
                    && menu.containerId == packet.containerId()
                    && menu.canUsePatternAccess(player)) {
                menu.getPatternAccessSession().handleAction(player, packet);
            }
        });
    }
}
