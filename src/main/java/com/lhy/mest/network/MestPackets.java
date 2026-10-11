package com.lhy.mest.network;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import com.lhy.mest.MESplicedterminal;

public final class MestPackets {
    private MestPackets() {}

    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MESplicedterminal.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int nextId;

    public static void register() {
        toServer(PatternProviderListPacket.Request.class,
                PatternProviderListPacket.Request::write,
                PatternProviderListPacket.Request::read,
                PatternProviderListPacket.Request::handle);
        toServer(PatternProviderActionPacket.class,
                PatternProviderActionPacket::write,
                PatternProviderActionPacket::read,
                PatternProviderActionPacket::handle);
        toServer(PatternCacheActionPacket.class,
                PatternCacheActionPacket::write,
                PatternCacheActionPacket::read,
                PatternCacheActionPacket::handle);
        toServer(MestPickBlockPacket.class,
                MestPickBlockPacket::write,
                MestPickBlockPacket::read,
                MestPickBlockPacket::handle);
        toServer(MestTerminalSettingsPacket.class,
                MestTerminalSettingsPacket::write,
                MestTerminalSettingsPacket::read,
                MestTerminalSettingsPacket::handle);
        toServer(ToolkitBarTogglePacket.class,
                ToolkitBarTogglePacket::write,
                ToolkitBarTogglePacket::read,
                ToolkitBarTogglePacket::handle);
        toServer(ToolkitQuickMovePacket.class,
                ToolkitQuickMovePacket::write,
                ToolkitQuickMovePacket::read,
                ToolkitQuickMovePacket::handle);
        toServer(ToolkitBarSelectPacket.class,
                ToolkitBarSelectPacket::write,
                ToolkitBarSelectPacket::read,
                ToolkitBarSelectPacket::handle);
        toServer(ToolkitBarDropPacket.class,
                ToolkitBarDropPacket::write,
                ToolkitBarDropPacket::read,
                ToolkitBarDropPacket::handle);
        toServer(ToolkitMemorySlotPacket.class,
                ToolkitMemorySlotPacket::write,
                ToolkitMemorySlotPacket::read,
                ToolkitMemorySlotPacket::handle);

        toClient(ToolkitBarSyncPacket.class,
                ToolkitBarSyncPacket::write,
                ToolkitBarSyncPacket::read,
                ToolkitBarSyncPacket::handle);
        toClient(PatternProviderListPacket.class,
                PatternProviderListPacket::write,
                PatternProviderListPacket::read,
                PatternProviderClientBridge::handle);
        toClient(ProviderPickerListPacket.class,
                ProviderPickerListPacket::write,
                ProviderPickerListPacket::read,
                ProviderPickerClientBridge::handle);
        toClient(MestRestockAmountPacket.class,
                MestRestockAmountPacket::write,
                MestRestockAmountPacket::read,
                MestRestockAmountPacket::handle);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    private static <T> void toServer(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
        register(type, encoder, decoder, handler, NetworkDirection.PLAY_TO_SERVER);
    }

    private static <T> void toClient(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler) {
        register(type, encoder, decoder, handler, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static <T> void register(Class<T> type, BiConsumer<T, FriendlyByteBuf> encoder,
            Function<FriendlyByteBuf, T> decoder, BiConsumer<T, PacketContext> handler,
            NetworkDirection direction) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, (packet, contextSupplier) -> {
            var context = contextSupplier.get();
            handler.accept(packet, new PacketContext(context));
            context.setPacketHandled(true);
        }, Optional.of(direction));
    }
}
