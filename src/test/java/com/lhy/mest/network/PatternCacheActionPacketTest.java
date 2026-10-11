package com.lhy.mest.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;


import org.junit.jupiter.api.Test;

class PatternCacheActionPacketTest {

    @Test
    void codecRoundTripsValidPacket() {
        var expected = new PatternCacheActionPacket(PatternCacheActionPacket.Action.TIMES_2, true);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            expected.write(buf);

            var decoded = PatternCacheActionPacket.read(buf);

            assertTrue(decoded.isWellFormed());
            assertEquals(expected, decoded);
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void codecRejectsInvalidOrdinal() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(99);
            buf.writeBoolean(true);

            var decoded = PatternCacheActionPacket.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void codecRejectsTrailingData() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(0);
            buf.writeBoolean(false);
            buf.writeByte(0);

            var decoded = PatternCacheActionPacket.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void codecRejectsTruncatedPacket() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(0);

            var decoded = PatternCacheActionPacket.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }
}
