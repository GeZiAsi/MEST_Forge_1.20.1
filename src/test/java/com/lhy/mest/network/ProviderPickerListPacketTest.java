package com.lhy.mest.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import org.junit.jupiter.api.Test;

class ProviderPickerListPacketTest {

    @Test
    void roundTripsEmptyList() {
        var expected = new ProviderPickerListPacket(List.of(), List.of(), List.of(), false);
        var buf = buf();
        try {
            expected.write(buf);

            var decoded = ProviderPickerListPacket.read(buf);

            assertEquals(0, buf.readableBytes());
            assertTrue(decoded.ids().isEmpty());
            assertFalse(decoded.applyPreset());
        } finally {
            buf.release();
        }
    }

    // The full-row roundtrip is covered structurally by {@code roundTripsEmptyList} and the
    // {@code PatternProviderPacketCodecTest} list codec patterns. A non-empty {@code Component}
    // row cannot be encoded through {@code ComponentSerialization.TRUSTED_STREAM_CODEC} with
    // {@code RegistryAccess.EMPTY} (the style/hover codec needs a real registry), so the
    // safety properties below deliberately exercise only the byte-level bounds.

    @Test
    void decoderRejectsOversizedRowCount() {
        var buf = buf();
        try {
            buf.writeVarInt(ProviderPickerListPacket.MAX_ROWS + 1);

            var decoded = ProviderPickerListPacket.read(buf);

            // Oversized length must not pre-size an ArrayList; the decoder falls back to empty.
            assertTrue(decoded.ids().isEmpty());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void decoderRejectsNegativeRowCount() {
        var buf = buf();
        try {
            buf.writeVarInt(-1);

            var decoded = ProviderPickerListPacket.read(buf);

            assertTrue(decoded.ids().isEmpty());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void decoderRejectsTrailingData() {
        var buf = buf();
        try {
            buf.writeVarInt(0);
            buf.writeBoolean(true);
            buf.writeByte(0);

            var decoded = ProviderPickerListPacket.read(buf);

            assertTrue(decoded.ids().isEmpty());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void encoderRefusesToEmitOversizedList() {
        int size = ProviderPickerListPacket.MAX_ROWS + 1;
        var ids = new ArrayList<Long>(size);
        var names = new ArrayList<Component>(size);
        var slots = new ArrayList<Integer>(size);
        for (int i = 0; i < size; i++) {
            ids.add((long) i);
            names.add(Component.empty());
            slots.add(0);
        }
        var huge = new ProviderPickerListPacket(ids, names, slots, false);
        var buf = buf();
        try {
            assertThrows(IllegalStateException.class,
                    () -> huge.write(buf));
        } finally {
            buf.release();
        }
    }

    private static FriendlyByteBuf buf() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }
}
