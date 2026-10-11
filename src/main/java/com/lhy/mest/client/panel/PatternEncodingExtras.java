package com.lhy.mest.client.panel;

import java.util.List;

import net.minecraft.client.gui.components.AbstractWidget;
import com.lhy.mest.terminal.MESTMenu;

/** Optional processing-scale chrome. Implementations may live in the Plus compat package. */
public interface PatternEncodingExtras {
    static PatternEncodingExtras create(MESTMenu menu) {
        // The only implementation drove ExtendedAE Plus, which the GTL stack does not ship.
        return null;
    }

    List<AbstractWidget> widgets();

    void layout(int bgX, int bgY, int encodeX, int encodeY, boolean visible, boolean processing);

    boolean scaleButtonsVisible();
}
