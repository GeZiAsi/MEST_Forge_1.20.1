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
        // Not GuiGraphics.blitNineSliced: in 1.20.1 it assumes a 256×256 texture, which samples
        // only the top rows of this 21×26 one and tiles them into dark/white stripes.
        nineSlice(g, VERTICAL_BUTTONS_BG, VERTICAL_BUTTONS_BG_WIDTH, VERTICAL_BUTTONS_BG_HEIGHT,
                x, y, width, height, 0, 2, 0, 4);
    }

    /**
     * Nine-slice of a whole texture of any size: corners at native size, edges and centre
     * stretched. The artwork here has flat edges and centre, so stretching matches 1.21's sprites.
     */
    static void nineSlice(GuiGraphics g, ResourceLocation texture, int texW, int texH,
            int x, int y, int width, int height, int left, int top, int right, int bottom) {
        if (width <= 0 || height <= 0) {
            return;
        }
        left = Math.min(left, width / 2);
        right = Math.min(right, width - left);
        top = Math.min(top, height / 2);
        bottom = Math.min(bottom, height - top);
        int[] dx = { x, x + left, x + width - right };
        int[] dw = { left, width - left - right, right };
        int[] su = { 0, left, texW - right };
        int[] sw = { left, texW - left - right, right };
        int[] dy = { y, y + top, y + height - bottom };
        int[] dh = { top, height - top - bottom, bottom };
        int[] sv = { 0, top, texH - bottom };
        int[] sh = { top, texH - top - bottom, bottom };
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (dw[col] > 0 && dh[row] > 0 && sw[col] > 0 && sh[row] > 0) {
                    g.blit(texture, dx[col], dy[row], dw[col], dh[row], su[col], sv[row], sw[col], sh[row],
                            texW, texH);
                }
            }
        }
    }

    /** AE2's small scrollbar handle at native size. */
    public static void smallScroller(GuiGraphics g, int x, int y, boolean enabled) {
        (enabled ? Scrollbar.SMALL.enabledBlitter() : Scrollbar.SMALL.disabledBlitter())
                .copy()
                .dest(x, y)
                .blit(g);
    }
}
