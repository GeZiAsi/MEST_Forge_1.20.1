package com.lhy.mest.network;

import com.lhy.mest.util.MestLists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;

class PatternAccessChunkPlannerTest {
    @Test
    void returnsOneEmptyChunkForAnEmptySnapshot() {
        var chunks = PatternAccessChunkPlanner.chunk(
                new Int2ObjectArrayMap<ArrayList<Integer>>(),
                4,
                100,
                value -> value.size(),
                ArrayList::new);

        assertEquals(1, chunks.size());
        assertEquals(0, MestLists.first(chunks).size());
    }

    @Test
    void splitsByValueCountAndCopiesValues() {
        var source = new Int2ObjectArrayMap<ArrayList<Integer>>();
        for (int slot = 0; slot < 5; slot++) {
            source.put(slot, new ArrayList<>(java.util.List.of(slot)));
        }

        var chunks = PatternAccessChunkPlanner.chunk(source, 2, 100, value -> 1, ArrayList::new);

        assertEquals(java.util.List.of(2, 2, 1), chunks.stream().map(java.util.Map::size).toList());
        assertNotSame(source.get(0), MestLists.first(chunks).get(0));
    }

    @Test
    void keepsOversizedSingleValueInItsOwnChunk() {
        var source = new Int2ObjectArrayMap<String>();
        source.put(0, "oversized");
        source.put(1, "small");

        var chunks = PatternAccessChunkPlanner.chunk(source, 4, 4, String::length, value -> value);

        assertEquals(2, chunks.size());
        assertEquals(1, chunks.get(0).size());
        assertEquals(1, chunks.get(1).size());
    }

    @Test
    void rejectsNonPositiveLimits() {
        assertThrows(IllegalArgumentException.class, () -> PatternAccessChunkPlanner.chunk(
                new Int2ObjectArrayMap<String>(), 0, 1, String::length, value -> value));
    }
}