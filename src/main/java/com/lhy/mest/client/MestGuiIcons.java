package com.lhy.mest.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.style.Blitter;

import com.lhy.mest.MESplicedterminal;

/** 16×16 icons from {@code layout_preset_icons.png} and the spliced-terminal WUT icon. */
public final class MestGuiIcons {
    public static final ResourceLocation ATLAS = new ResourceLocation(
            MESplicedterminal.MODID, "textures/guis/layout_preset_icons.png");
    public static final ResourceLocation SPLICED_TERMINAL = new ResourceLocation(
            MESplicedterminal.MODID, "textures/guis/spliced_terminal_icon.png");
    public static final int SIZE = 16;

    private MestGuiIcons() {
    }

    public static void blit(GuiGraphics graphics, int col, int row, int x, int y, int w, int h) {
        blit(graphics, col, row, x, y, w, h, 1.0F);
    }

    public static void blit(
            GuiGraphics graphics, int col, int row, int x, int y, int w, int h, float opacity) {
        var blitter = Blitter.texture(ATLAS, 256, 256)
                .src(col * SIZE, row * SIZE, SIZE, SIZE)
                .dest(x, y, w, h)
;
        if (opacity < 1.0F) {
            blitter.opacity(opacity);
        }
        blitter.blit(graphics);
    }

    public static void blitHalf(
            GuiGraphics graphics, int col, int row, boolean rightHalf, int x, int y, int w, int h) {
        int srcW = SIZE / 2;
        int u = col * SIZE + (rightHalf ? srcW : 0);
        Blitter.texture(ATLAS, 256, 256)
                .src(u, row * SIZE, srcW, SIZE)
                .dest(x, y, w, h)

                .blit(graphics);
    }

    /** Combined module 1 to 3 reuses the layout-preset 1 to 3 icons (row 2, icons 10 to 12). */
    public static void blitCombined(GuiGraphics graphics, int group, int x, int y, int w, int h, float opacity) {
        var blitter = Blitter.texture(ATLAS, 256, 256)
                .src(144 + (group - 1) * SIZE, SIZE, SIZE, SIZE)
                .dest(x, y, w, h)
;
        if (opacity < 1.0F) {
            blitter.opacity(opacity);
        }
        blitter.blit(graphics);
    }

    public static void blitSplicedTerminal(GuiGraphics graphics, int x, int y, int w, int h) {
        Blitter.texture(SPLICED_TERMINAL, SIZE, SIZE)
                .src(0, 0, SIZE, SIZE)
                .dest(x, y, w, h)
                .blit(graphics);
    }
}
