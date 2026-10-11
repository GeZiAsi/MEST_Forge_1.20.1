package com.lhy.mest.network;

import java.util.HashMap;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;

import com.lhy.mest.client.MestRestockClient;

/** ME counts for hotbar overlay, same payload shape as wtlib RestockAmountPacket. */
public record MestRestockAmountPacket(HashMap<Item, Long> items) {
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(items.size());
        items.forEach((item, count) -> {
            buf.writeId(BuiltInRegistries.ITEM, item);
            buf.writeVarLong(count);
        });
    }

    public static MestRestockAmountPacket read(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        HashMap<Item, Long> items = new HashMap<>(Math.min(size, 256));
        for (int i = 0; i < size; i++) {
            Item item = buf.readById(BuiltInRegistries.ITEM);
            long count = buf.readVarLong();
            if (item != null) {
                items.put(item, count);
            }
        }
        return new MestRestockAmountPacket(items);
    }

    public static void handle(MestRestockAmountPacket packet, PacketContext context) {
        context.enqueueWork(() -> MestRestockClient.setAmounts(packet.items()));
    }
}
