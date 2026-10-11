package com.lhy.mest.client;

import appeng.client.gui.Icon;

/**
 * AE2 19 icons the layout uses that AE2 15 (GTL) does not have, mapped onto the closest AE2 15
 * icon so every AE2 widget that takes an {@link Icon} keeps working.
 */
public final class Ae2Icons {
    private Ae2Icons() {
    }

    public static final Icon BACK = Icon.ARROW_LEFT;
    public static final Icon COG = Icon.WRENCH;
    public static final Icon TOOLBAR_BUTTON_BACKGROUND_HOVER = Icon.TOOLBAR_BUTTON_BACKGROUND;
    public static final Icon TOOLBAR_BUTTON_BACKGROUND_FOCUS = Icon.TOOLBAR_BUTTON_BACKGROUND;
    public static final Icon S_STORAGE = Icon.VIEW_MODE_STORED;
    public static final Icon S_SUBSTITUTION_ENABLED = Icon.SUBSTITUTION_ENABLED;
    public static final Icon S_SUBSTITUTION_DISABLED = Icon.SUBSTITUTION_DISABLED;
    public static final Icon S_FLUID_SUBSTITUTION_ENABLED = Icon.FLUID_SUBSTITUTION_ENABLED;
    public static final Icon S_FLUID_SUBSTITUTION_DISABLED = Icon.FLUID_SUBSTITUTION_DISABLED;
}
