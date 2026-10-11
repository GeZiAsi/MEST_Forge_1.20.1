package com.lhy.mest.network;

import com.lhy.mest.util.MestLists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class PatternProviderClientStateTest {
    private static final int CONTAINER = 7;

    @Test
    void staleResetCannotReplaceNewerEpoch() {
        var state = state();
        assertEquals(PatternProviderClientState.Outcome.CHANGED, state.apply(reset(20)).outcome());
        assertEquals(PatternProviderClientState.Outcome.CHANGED,
                state.apply(full(20, 1, 1, 0, 1, 4, Map.of(0, "first"))).outcome());

        assertEquals(PatternProviderClientState.Outcome.IGNORED, state.apply(reset(19)).outcome());
        var result = state.apply(delta(20, 1, 2, 0, 1, Map.of(1, "second")));

        assertEquals(PatternProviderClientState.Outcome.CHANGED, result.outcome());
        assertEquals(20, onlyProvider(result).epoch());
        assertEquals(Map.of(0, "first", 1, "second"), onlyProvider(result).slots());
    }

    @Test
    void newSubscriptionMayStartAtLowerEpochAfterServerReconnect() {
        var state = state();
        state.apply(reset(500));
        state.apply(full(500, 1, 1, 0, 1, 4, Map.of(0, "old-server")));

        state.beginSession(CONTAINER);
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(full(1, 1, 1, 0, 1, 4, Map.of(0, "before-reset"))).outcome());
        assertEquals(PatternProviderClientState.Outcome.CHANGED, state.apply(reset(1)).outcome());
        var result = state.apply(full(1, 1, 1, 0, 1, 4, Map.of(0, "new-server")));

        assertEquals("new-server", onlyProvider(result).slots().get(0));
    }

    @Test
    void endedSessionDropsLatePacketsAndAllowsLowerReconnectEpoch() {
        var state = state();
        state.apply(reset(500));
        state.apply(full(500, 1, 1, 0, 1, 4, Map.of(0, "old-server")));

        state.endSession();
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(delta(500, 1, 2, 0, 1, Map.of(1, "late"))).outcome());
        assertEquals(PatternProviderClientState.Outcome.IGNORED, state.apply(reset(501)).outcome());

        state.beginSession(CONTAINER);
        assertEquals(PatternProviderClientState.Outcome.CHANGED, state.apply(reset(1)).outcome());
        var result = state.apply(full(1, 1, 1, 0, 1, 4, Map.of(0, "new-server")));
        assertEquals("new-server", onlyProvider(result).slots().get(0));
    }

    @Test
    void resubscriptionToSameMenuWaitsForStrictlyNewerReset() {
        var state = state();
        state.apply(reset(50));
        state.apply(full(50, 1, 1, 0, 1, 4, Map.of(0, "old")));

        state.beginSession(CONTAINER, true);
        assertEquals(PatternProviderClientState.Outcome.IGNORED, state.apply(reset(50)).outcome());
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(full(50, 1, 1, 0, 1, 4, Map.of(0, "late"))).outcome());
        assertEquals(PatternProviderClientState.Outcome.CHANGED, state.apply(reset(51)).outcome());
        var result = state.apply(full(51, 1, 1, 0, 1, 4, Map.of(0, "fresh")));

        assertEquals("fresh", onlyProvider(result).slots().get(0));
    }

    @Test
    void missingResetForNewerEpochRequestsResyncOnce() {
        var state = state();
        state.apply(reset(20));

        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED,
                state.apply(full(21, 1, 1, 0, 1, 4, Map.of())).outcome());
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(full(21, 1, 1, 0, 1, 4, Map.of())).outcome());
    }

    @Test
    void interleavedRevisionsDrainInRevisionOrder() {
        var state = state();
        state.apply(reset(30));
        state.apply(full(30, 1, 1, 0, 1, 130, Map.of(0, "base")));

        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(delta(30, 1, 2, 0, 2, Map.of(1, "two-a"))).outcome());
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(delta(30, 1, 3, 0, 1, Map.of(3, "three"))).outcome());
        var result = state.apply(delta(30, 1, 2, 1, 2, Map.of(2, "two-b")));

        assertEquals(PatternProviderClientState.Outcome.CHANGED, result.outcome());
        assertEquals(3, onlyProvider(result).revision());
        assertEquals(Map.of(0, "base", 1, "two-a", 2, "two-b", 3, "three"),
                onlyProvider(result).slots());
    }

    @Test
    void byteSplitFullUpdateMayUseOneChunkPerSlot() {
        var state = state();
        state.apply(reset(45));

        // Byte-based chunk splitting on the server may put a single large slot into each chunk, so a
        // full update may legitimately consist of inventorySize chunks.
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(full(45, 1, 1, 0, 3, 3, Map.of(0, "a"))).outcome());
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(full(45, 1, 1, 1, 3, 3, Map.of(1, "b"))).outcome());
        var result = state.apply(full(45, 1, 1, 2, 3, 3, Map.of(2, "c")));

        assertEquals(PatternProviderClientState.Outcome.CHANGED, result.outcome());
        assertEquals(Map.of(0, "a", 1, "b", 2, "c"), onlyProvider(result).slots());
    }

    @Test
    void identicalDuplicateChunkIsIdempotent() {
        var state = state();
        state.apply(reset(40));
        var firstChunk = full(40, 1, 1, 0, 2, 65, Map.of(0, "zero"));

        assertEquals(PatternProviderClientState.Outcome.IGNORED, state.apply(firstChunk).outcome());
        assertEquals(PatternProviderClientState.Outcome.IGNORED, state.apply(firstChunk).outcome());
        var result = state.apply(full(40, 1, 1, 1, 2, 65, Map.of(64, "last")));

        assertEquals(PatternProviderClientState.Outcome.CHANGED, result.outcome());
        assertEquals(Map.of(0, "zero", 64, "last"), onlyProvider(result).slots());
    }

    @Test
    void conflictingDuplicateChunkForcesResync() {
        var state = state();
        state.apply(reset(50));
        state.apply(full(50, 1, 1, 0, 2, 65, Map.of(0, "original")));

        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED,
                state.apply(full(50, 1, 1, 0, 2, 65, Map.of(0, "conflict"))).outcome());
    }

    @Test
    void overlappingSlotsAcrossChunksForceResync() {
        var state = state();
        state.apply(reset(60));
        state.apply(full(60, 1, 1, 0, 2, 65, Map.of(5, "first")));

        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED,
                state.apply(full(60, 1, 1, 1, 2, 65, Map.of(5, "second"))).outcome());
    }

    @Test
    void revisionGapWithoutEarlierUpdateForcesResync() {
        var state = state();
        state.apply(reset(70));
        state.apply(full(70, 1, 1, 0, 1, 4, Map.of(0, "base")));

        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED,
                state.apply(delta(70, 1, 3, 0, 1, Map.of(1, "gap"))).outcome());
    }

    @Test
    void removalRetiresProviderIdAgainstDelayedFullUpdate() {
        var state = state();
        state.apply(reset(80));
        state.apply(full(80, 8, 1, 0, 1, 4, Map.of(0, "pattern")));

        var removed = state.apply(remove(80, 8));
        assertEquals(PatternProviderClientState.Outcome.CHANGED, removed.outcome());
        assertTrue(removed.providers().isEmpty());
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(full(80, 8, 2, 0, 1, 4, Map.of(0, "late"))).outcome());
    }

    @Test
    void newerFullReordersMetadataAndOlderFullCannotRollItBack() {
        var state = state();
        state.apply(reset(90));
        state.apply(full(90, 3, 1, 0, 1, 4, 100, "old", Map.of()));

        var changed = state.apply(full(90, 3, 2, 0, 1, 4, 5, "new", Map.of()));
        assertEquals(5, onlyProvider(changed).sortOrder());
        assertEquals("new", onlyProvider(changed).metadata());
        assertEquals(PatternProviderClientState.Outcome.IGNORED,
                state.apply(full(90, 3, 1, 0, 1, 4, 100, "old", Map.of())).outcome());
    }

    @Test
    void invalidSizesAndSlotIndexesForceResync() {
        var tooLarge = state();
        tooLarge.apply(reset(100));
        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED,
                tooLarge.apply(full(100, 1, 1, 0, 1,
                        PatternProviderClientState.MAX_INVENTORY_SIZE + 1, Map.of())).outcome());

        var badSlot = state();
        badSlot.apply(reset(101));
        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED,
                badSlot.apply(full(101, 1, 1, 0, 1, 4, Map.of(4, "outside"))).outcome());

        var tooManyChunks = state();
        tooManyChunks.apply(reset(102));
        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED,
                tooManyChunks.apply(full(102, 1, 1, 0,
                        PatternProviderClientState.MAX_CHUNKS_PER_UPDATE + 1,
                        PatternProviderClientState.MAX_INVENTORY_SIZE, Map.of())).outcome());
    }

    @Test
    void pendingUpdateCountIsBounded() {
        var state = state();
        state.apply(reset(110));

        PatternProviderClientState.Result<String, String> result = null;
        for (int provider = 1; provider <= 129; provider++) {
            result = state.apply(full(110, provider, 1, 0, 2, 65, Map.of()));
        }

        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED, result.outcome());
    }

    @Test
    void totalAdvertisedInventorySizeIsBounded() {
        var state = state();
        state.apply(reset(120));

        PatternProviderClientState.Result<String, String> result = null;
        int providerSize = PatternProviderClientState.MAX_INVENTORY_SIZE;
        int allowedProviders = PatternProviderClientState.MAX_TOTAL_INVENTORY_SLOTS / providerSize;
        for (int provider = 1; provider <= allowedProviders; provider++) {
            result = state.apply(full(120, provider, 1, 0, 1, providerSize, Map.of()));
            assertEquals(PatternProviderClientState.Outcome.CHANGED, result.outcome());
        }
        result = state.apply(full(120, allowedProviders + 1, 1, 0, 1, providerSize, Map.of()));

        assertEquals(PatternProviderClientState.Outcome.RESYNC_REQUIRED, result.outcome());
    }

    private static PatternProviderClientState<String, String> state() {
        var state = new PatternProviderClientState<String, String>(
                new PatternProviderClientState.ValueOps<String>() {
                    @Override
                    public String copy(String value) {
                        return value;
                    }

                    @Override
                    public boolean isEmpty(String value) {
                        return value.isEmpty();
                    }

                    @Override
                    public boolean matches(String left, String right) {
                        return left.equals(right);
                    }
                });
        state.beginSession(CONTAINER);
        return state;
    }

    private static PatternProviderClientState.Provider<String, String> onlyProvider(
            PatternProviderClientState.Result<String, String> result) {
        assertEquals(1, result.providers().size());
        return MestLists.first(result.providers());
    }

    private static PatternProviderClientState.Update<String, String> reset(long epoch) {
        return update(epoch, PatternProviderClientState.Operation.RESET, 0, 0,
                0, 1, 0, 0, null, Map.of());
    }

    private static PatternProviderClientState.Update<String, String> remove(long epoch, long providerId) {
        return update(epoch, PatternProviderClientState.Operation.REMOVE, providerId, 0,
                0, 1, 0, 0, null, Map.of());
    }

    private static PatternProviderClientState.Update<String, String> full(
            long epoch, long providerId, long revision, int chunkIndex, int chunkCount,
            int inventorySize, Map<Integer, String> slots) {
        return full(epoch, providerId, revision, chunkIndex, chunkCount, inventorySize, 0, "group", slots);
    }

    private static PatternProviderClientState.Update<String, String> full(
            long epoch, long providerId, long revision, int chunkIndex, int chunkCount,
            int inventorySize, long sortOrder, String metadata, Map<Integer, String> slots) {
        return update(epoch, PatternProviderClientState.Operation.FULL, providerId, revision,
                chunkIndex, chunkCount, inventorySize, sortOrder, metadata, slots);
    }

    private static PatternProviderClientState.Update<String, String> delta(
            long epoch, long providerId, long revision, int chunkIndex, int chunkCount,
            Map<Integer, String> slots) {
        return update(epoch, PatternProviderClientState.Operation.DELTA, providerId, revision,
                chunkIndex, chunkCount, 0, 0, null, slots);
    }

    private static PatternProviderClientState.Update<String, String> update(
            long epoch, PatternProviderClientState.Operation operation, long providerId, long revision,
            int chunkIndex, int chunkCount, int inventorySize, long sortOrder,
            String metadata, Map<Integer, String> slots) {
        return new PatternProviderClientState.Update<>(CONTAINER, epoch, operation, providerId, revision,
                chunkIndex, chunkCount, inventorySize, sortOrder, metadata, slots);
    }
}
