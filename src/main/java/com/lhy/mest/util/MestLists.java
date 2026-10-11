package com.lhy.mest.util;

import java.util.List;

/** Java 17 stand-ins for the Java 21 {@code SequencedCollection} list accessors. */
public final class MestLists {
    private MestLists() {
    }

    public static <T> T first(List<T> list) {
        if (list.isEmpty()) {
            throw new java.util.NoSuchElementException();
        }
        return list.get(0);
    }

    public static <T> T last(List<T> list) {
        if (list.isEmpty()) {
            throw new java.util.NoSuchElementException();
        }
        return list.get(list.size() - 1);
    }

    public static <T> T removeLast(List<T> list) {
        if (list.isEmpty()) {
            throw new java.util.NoSuchElementException();
        }
        return list.remove(list.size() - 1);
    }
}
