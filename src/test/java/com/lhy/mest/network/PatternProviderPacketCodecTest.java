package com.lhy.mest.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

import net.minecraft.network.FriendlyByteBuf;

class PatternProviderPacketCodecTest {
    @Test
    void actionCodecRoundTripsValidPacket() {
        var expected = new PatternProviderActionPacket(
                7, 11, 13, 17, 19,
                PatternProviderActionPacket.Action.QUICK_MOVE_TO_PLAYER);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            expected.write(buf);

            assertEquals(expected, PatternProviderActionPacket.read(buf));
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void actionCodecRejectsInvalidOrdinalWithoutThrowing() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            writeActionEnvelope(buf);
            buf.writeByte(255);

            var decoded = PatternProviderActionPacket.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void actionCodecRejectsTruncatedPacketWithoutThrowing() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(7);

            var decoded = PatternProviderActionPacket.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(7, decoded.containerId());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void listCodecRoundTripsValidReset() {
        var expected = PatternProviderListPacket.reset(23, 29);
        var buf = registryBuffer();
        try {
            expected.write(buf);

            var decoded = PatternProviderListPacket.read(buf);

            assertTrue(decoded.isWellFormed());
            assertEquals(expected.containerId(), decoded.containerId());
            assertEquals(expected.epoch(), decoded.epoch());
            assertEquals(expected.operation(), decoded.operation());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void listCodecRejectsInvalidOperationWithoutThrowing() {
        var buf = registryBuffer();
        try {
            buf.writeVarInt(23);
            buf.writeVarLong(29);
            buf.writeVarInt(99);

            var decoded = PatternProviderListPacket.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(23, decoded.containerId());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void listCodecRejectsTruncatedPacketWithoutThrowing() {
        var buf = registryBuffer();
        try {
            buf.writeVarInt(23);

            var decoded = PatternProviderListPacket.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(23, decoded.containerId());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void listEnvelopeValidationRejectsMalformedNumericFields() {
        assertFalse(PatternProviderListPacket.reset(1, 0).isWellFormed());
        assertFalse(new PatternProviderListPacket(
                1, 2, PatternProviderListPacket.Operation.DELTA,
                3, 4, 1, 1, 0, 0, null, new Int2ObjectArrayMap<>(), null).isWellFormed());
        assertFalse(new PatternProviderListPacket(
                1, 2, PatternProviderListPacket.Operation.FULL,
                3, 4, 0, 1, PatternProviderClientState.MAX_INVENTORY_SIZE + 1,
                0, null, new Int2ObjectArrayMap<>(), null).isWellFormed());
    }

    @Test
    void fullChunkCountBoundsMatchSparseServerChunking() {
        int maximumInventory = PatternProviderClientState.MAX_INVENTORY_SIZE;
        int maximumChunks = PatternProviderClientState.MAX_CHUNKS_PER_UPDATE;

        assertTrue(PatternProviderListPacket.isPlausibleFullChunkCount(1, maximumInventory));
        assertTrue(PatternProviderListPacket.isPlausibleFullChunkCount(maximumChunks, maximumInventory));
        assertFalse(PatternProviderListPacket.isPlausibleFullChunkCount(0, maximumInventory));
        assertFalse(PatternProviderListPacket.isPlausibleFullChunkCount(maximumChunks + 1, maximumInventory));
        assertFalse(PatternProviderListPacket.isPlausibleFullChunkCount(2, 1));
    }

    @Test
    void malformedListPacketIsRejectedBeforeHandlerEnqueue() {
        var calls = new AtomicInteger();
        PatternProviderClientBridge.install((packet, context) -> calls.incrementAndGet());

        PatternProviderClientBridge.handle(PatternProviderListPacket.reset(23, 0), null);

        assertEquals(0, calls.get());
    }

    @Test
    void requestCodecRejectsTruncatedPacketWithoutThrowing() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeVarInt(31);

            var decoded = PatternProviderListPacket.Request.read(buf);

            assertFalse(decoded.isWellFormed());
            assertEquals(0, buf.readableBytes());
        } finally {
            buf.release();
        }
    }

    @Test
    void requestValidationRejectsNegativeContainerId() {
        assertTrue(new PatternProviderListPacket.Request(0, true, (byte) 0).isWellFormed());
        assertFalse(new PatternProviderListPacket.Request(-1, false, (byte) 0).isWellFormed());
        assertFalse(new PatternProviderListPacket.Request(0, true, (byte) 3).isWellFormed());
    }

    private static void writeActionEnvelope(FriendlyByteBuf buf) {
        buf.writeVarInt(7);
        buf.writeVarLong(11L);
        buf.writeVarLong(13L);
        buf.writeVarLong(17L);
        buf.writeVarInt(19);
    }

    private static FriendlyByteBuf registryBuffer() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }
}
