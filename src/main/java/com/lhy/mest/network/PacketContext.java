package com.lhy.mest.network;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

/**
 * Minimal stand-in for NeoForge's {@code IPayloadContext}: handlers run on the network thread and
 * must {@link #enqueueWork} anything that touches game state.
 */
public final class PacketContext {
    /** Installed by the physical client; the dedicated server never links client classes. */
    private static volatile Supplier<Player> clientPlayer = () -> null;

    private final NetworkEvent.Context context;

    PacketContext(NetworkEvent.Context context) {
        this.context = context;
    }

    public static void installClientPlayer(Supplier<Player> supplier) {
        clientPlayer = supplier;
    }

    /** The sending player on the server, the local player on the client. */
    @Nullable
    public Player player() {
        Player sender = context.getSender();
        return sender != null ? sender : clientPlayer.get();
    }

    public void enqueueWork(Runnable work) {
        context.enqueueWork(work);
    }
}
