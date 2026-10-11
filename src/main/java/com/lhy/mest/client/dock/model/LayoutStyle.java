package com.lhy.mest.client.dock.model;

import java.util.Objects;

/**
 * Geometry constants supplied by the eventual renderer integration.
 *
 * @param bottomLeafPadding extra height a leaf needs when it sits on its window's bottom edge:
 *                          spliced sections are drawn tight, and only the bottom-most one gets the
 *                          content padding below it, which its own minimum does not include
 */
public record LayoutStyle(DockInsets rootInsets, int dividerThickness, int bottomLeafPadding) {
    public LayoutStyle {
        Objects.requireNonNull(rootInsets, "rootInsets");
        if (dividerThickness < 0) {
            throw new IllegalArgumentException("dividerThickness must be non-negative");
        }
        if (bottomLeafPadding < 0) {
            throw new IllegalArgumentException("bottomLeafPadding must be non-negative");
        }
    }

    public LayoutStyle(DockInsets rootInsets, int dividerThickness) {
        this(rootInsets, dividerThickness, 0);
    }
}
