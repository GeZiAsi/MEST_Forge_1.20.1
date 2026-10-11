package com.lhy.mest.network;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraft.server.level.ServerPlayer;

import com.lhy.mest.terminal.MESTMenu;

/** Client action against every encoded pattern in the cache panel. */
public record PatternCacheActionPacket(Action action, boolean value) {

    private static final Action[] ACTIONS = Action.values();

    /** One packet rewrites the whole cache, so the quota is far tighter than for single-slot actions. */
    private static final PlayerTickBudgets BUDGETS = new PlayerTickBudgets(4);

    public enum Action {
        SWAP,
        TIMES_2,
        TIMES_3,
        TIMES_5,
        EQUALS_1,
        DIVIDE_2,
        DIVIDE_3,
        DIVIDE_5,
        TIMES_8,
        DIVIDE_8,
        TIMES_16,
        DIVIDE_16,
        TIMES_32,
        DIVIDE_32,
        ITEM_SUBSTITUTION,
        FLUID_SUBSTITUTION
    }

    public static PatternCacheActionPacket read(FriendlyByteBuf buf) {
        try {
            int actionOrdinal = buf.readVarInt();
            boolean value = buf.readBoolean();
            if (actionOrdinal < 0 || actionOrdinal >= ACTIONS.length || buf.isReadable()) {
                throw new IllegalArgumentException("Malformed pattern cache action");
            }
            return new PatternCacheActionPacket(ACTIONS[actionOrdinal], value);
        } catch (RuntimeException exception) {
            discardRemaining(buf);
            return invalid();
        }
    }

    public void write(FriendlyByteBuf buf) {
        if (!isWellFormed()) {
            throw new IllegalStateException("Cannot encode a malformed pattern cache action");
        }
        buf.writeVarInt(action.ordinal());
        buf.writeBoolean(value);
    }

    boolean isWellFormed() {
        return action != null;
    }

    private static PatternCacheActionPacket invalid() {
        return new PatternCacheActionPacket(null, false);
    }

    private static void discardRemaining(FriendlyByteBuf buf) {
        try {
            buf.skipBytes(buf.readableBytes());
        } catch (RuntimeException ignored) {
            // Decoding must still return a rejected sentinel if the buffer itself is already invalid.
        }
    }

    public static void handle(PatternCacheActionPacket packet, PacketContext context) {
        if (!packet.isWellFormed()) {
            return;
        }
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof MESTMenu menu
                    && menu.canUseTerminal(player)
                    && BUDGETS.tryAcquire(player)) {
                menu.handlePatternCacheAction(packet.action(), packet.value());
            }
        });
    }
}
