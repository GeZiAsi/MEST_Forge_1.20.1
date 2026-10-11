package com.lhy.mest.client.dock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.style.Blitter;

import com.lhy.mest.MESplicedterminal;

/**
 * Skin for the network-tool panels (the terminal's dock module and the machine-screen chrome).
 *
 * <p>Drop a texture at {@code assets/mesplicedterminal/textures/guis/network_tool_panel.png} to
 * reskin them. It is drawn as a nine-slice and uses the same layout as AE2's own window background
 * ({@code assets/ae2/textures/guis/background.png}): 256×256 with a 4px border, so the middle
 * 248×248 area tiles. When the file is absent the AE2 generated window background is used instead.
 */
public final class MestPanelSkin {
    /** {@code mesplicedterminal:textures/guis/network_tool_panel.png} */
    public static final ResourceLocation PANEL = new ResourceLocation(
            MESplicedterminal.MODID, "textures/guis/network_tool_panel.png");
    private static final Blitter TEXTURE = Blitter.texture(PANEL, 256, 256);
    private static final int BORDER = 4;
    private static final int TILE = 248;

    private MestPanelSkin() {
    }

    /** True when a custom skin is installed in the current resource packs. */
    public static boolean isCustom() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }
        return minecraft.getResourceManager().getResource(PANEL).isPresent();
    }

    /** Panel background: the custom skin when present, AE2's window chrome otherwise. */
    public static void drawFrame(GuiGraphics g, int x, int y, int width, int height) {
        if (width < 8 || height < 8) {
            return;
        }
        if (!isCustom()) {
            ModulePanel.drawGeneratedBackground(g, x, y, width, height);
            return;
        }
        int right = x + width;
        int bottom = y + height;
        TEXTURE.copy().src(0, 0, BORDER, BORDER).dest(x, y).blit(g);
        TEXTURE.copy().src(252, 0, BORDER, BORDER).dest(right - BORDER, y).blit(g);
        TEXTURE.copy().src(0, 252, BORDER, BORDER).dest(x, bottom - BORDER).blit(g);
        TEXTURE.copy().src(252, 252, BORDER, BORDER).dest(right - BORDER, bottom - BORDER).blit(g);
        int innerX = x + BORDER;
        int innerY = y + BORDER;
        int innerW = width - 2 * BORDER;
        int innerH = height - 2 * BORDER;
        for (int cx = 0; cx < innerW; cx += TILE) {
            int tileW = Math.min(TILE, innerW - cx);
            TEXTURE.copy().src(BORDER, 0, tileW, BORDER).dest(innerX + cx, y).blit(g);
            TEXTURE.copy().src(BORDER, 252, tileW, BORDER).dest(innerX + cx, bottom - BORDER).blit(g);
            for (int cy = 0; cy < innerH; cy += TILE) {
                int tileH = Math.min(TILE, innerH - cy);
                TEXTURE.copy().src(BORDER, BORDER, tileW, tileH).dest(innerX + cx, innerY + cy).blit(g);
            }
        }
        for (int cy = 0; cy < innerH; cy += TILE) {
            int tileH = Math.min(TILE, innerH - cy);
            TEXTURE.copy().src(0, BORDER, BORDER, tileH).dest(x, innerY + cy).blit(g);
            TEXTURE.copy().src(252, BORDER, BORDER, tileH).dest(right - BORDER, innerY + cy).blit(g);
        }
    }
}
