package com.lhy.mest.client;

import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;

/**
 * Small square button backgrounds. The 1.21 build borrowed ExtendedAE's terminal-button sprites,
 * which ExtendedAE 1.20 does not have; the bundled AE2WTLib button art replaces them.
 */
public final class MestButtonSprites {
    private MestButtonSprites() {
    }

    public static Blitter terminalButton(boolean hovered, boolean focused) {
        com.lhy.mest.client.wt.Icon icon = hovered
                ? com.lhy.mest.client.wt.Icon.BUTTON_BACKGROUND_HOVERED
                : focused ? com.lhy.mest.client.wt.Icon.BUTTON_BACKGROUND_FOCUSED
                        : com.lhy.mest.client.wt.Icon.BUTTON_BACKGROUND;
        return icon.getBlitter();
    }

    /** Pattern-access search scopes; ExtendedAE 1.20 has no dedicated glyphs for these. */
    public static Blitter searchInput() {
        return Icon.ARROW_DOWN.getBlitter();
    }

    public static Blitter searchOutput() {
        return Icon.ARROW_UP.getBlitter();
    }

    public static Blitter searchBoth() {
        return Icon.SEARCH_DEFAULT.getBlitter();
    }
}
