package com.lhy.mest.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.widgets.Scrollbar;

import com.lhy.mest.MESplicedterminal;

/**
 * Minecraft 1.20.1 has no GUI sprite atlas ({@code blitSprite}); these draw the same artwork from
 * plain textures with nine-slice scaling.
 */
public final class GuiSprites {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation VERTICAL_BUTTONS_BG =
            new ResourceLocation(MESplicedterminal.MODID, "textures/guis/vertical_buttons_bg.png");
    private static final int VERTICAL_BUTTONS_BG_WIDTH = 21;
    private static final int VERTICAL_BUTTONS_BG_HEIGHT = 26;

    private GuiSprites() {
    }

    /** Vanilla/AE2 text button background (the {@code button} / {@code button_highlighted} sprites). */
    public static void button(GuiGraphics g, int x, int y, int width, int height, boolean highlighted) {
        g.blitNineSliced(WIDGETS, x, y, width, height, 20, 4, 200, 20, 0, highlighted ? 86 : 66);
    }

    /** AE2's vertical toolbar background, stretched with a 2px top and 4px bottom border. */
    public static void verticalButtonsBackground(GuiGraphics g, int x, int y, int width, int height) {
        g.blitNineSliced(VERTICAL_BUTTONS_BG, x, y, width, height, 0, 2, 0, 4,
                VERTICAL_BUTTONS_BG_WIDTH, VERTICAL_BUTTONS_BG_HEIGHT, 0, 0);
    }

    /** AE2's small scrollbar handle at native size. */
    public static void smallScroller(GuiGraphics g, int x, int y, boolean enabled) {
        (enabled ? Scrollbar.SMALL.enabledBlitter() : Scrollbar.SMALL.disabledBlitter())
                .copy()
                .dest(x, y)
                .blit(g);
    }
}
