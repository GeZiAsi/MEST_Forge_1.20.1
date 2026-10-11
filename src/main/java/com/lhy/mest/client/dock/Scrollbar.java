package com.lhy.mest.client.dock;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;

import appeng.client.gui.style.Blitter;
import appeng.core.AppEng;

/**
 * Shared scrollbar state + rendering for the floating panels. Holds the current scroll offset and
 * the thumb-drag state machine; geometry is supplied per call so the helper tracks a resizable panel.
 *
 * <p>The scroll model is generic "visible / total / maxScroll": the thumb covers a fraction of the
 * track proportional to {@code visible/total}, and dragging maps the thumb position back to a scroll
 * row. This fits the ME list (rows of RepoSlots), pattern access (provider rows) and the processing
 * encoder (slot rows) without per-panel drag code.
 */
public class Scrollbar {
    private static final int HANDLE_HEIGHT = 15;
    private static final int TRACK_SRC_X = 175;
    private static final int TRACK_SRC_W = 12;
    private static final Blitter TERMINAL = Blitter.texture("guis/terminal.png", 256, 256);

    private int scroll;
    private boolean dragging;
    // Vertical offset between the mouse and the thumb's top edge at drag start, so the thumb doesn't
    // snap to the cursor.
    private double grabOffset;

    public int scroll() {
        return scroll;
    }

    public void setScroll(int value) {
        this.scroll = value;
    }

    public void clamp(int maxScroll) {
        scroll = Math.max(0, Math.min(maxScroll, scroll));
    }

    /** Fixed AE2-style thumb height, clamped to the track. */
    private static int thumbHeight(int trackH) {
        return Math.min(HANDLE_HEIGHT, trackH);
    }

    private static int thumbY(int trackY, int trackH, int thumbH, int scroll, int maxScroll) {
        if (maxScroll <= 0) {
            return trackY;
        }
        int travel = trackH - thumbH;
        return trackY + travel * scroll / maxScroll;
    }

    /** Current thumb rect, for hit-testing. */
    public Rect2i thumbRect(int trackX, int trackY, int trackW, int trackH, int maxScroll) {
        int thumbH = thumbHeight(trackH);
        int thumbY = thumbY(trackY, trackH, thumbH, scroll, maxScroll);
        return new Rect2i(trackX, thumbY, trackW, thumbH);
    }

    /** Draw the AE2 scroller handle at native sprite size. The track belongs in the panel texture. */
    public void render(GuiGraphics g, int trackX, int trackY, int trackW, int trackH, int maxScroll) {
        render(g, trackX, trackY, trackW, trackH, maxScroll, false);
    }

    public void render(GuiGraphics g, int trackX, int trackY, int trackW, int trackH, int maxScroll,
            boolean drawTrack) {
        if (drawTrack) {
            drawTerminalTrack(g, trackX, trackY, trackH);
        }
        int thumbH = thumbHeight(trackH);
        int thumbY = thumbY(trackY, trackH, thumbH, scroll, maxScroll);
        com.lhy.mest.client.GuiSprites.smallScroller(g, trackX, thumbY, maxScroll > 0);
    }

    public static void drawTerminalTrack(GuiGraphics g, int x, int y, int height) {
        int remaining = height;
        int destY = y;
        int top = Math.min(18, remaining);
        TERMINAL.src(TRACK_SRC_X, 17, TRACK_SRC_W, top).dest(x, destY, TRACK_SRC_W, top).blit(g);
        destY += top;
        remaining -= top;
        if (remaining <= 0) {
            return;
        }
        int bottom = Math.min(18, remaining);
        int middle = remaining - bottom;
        while (middle > 0) {
            int slice = Math.min(18, middle);
            TERMINAL.src(TRACK_SRC_X, 35, TRACK_SRC_W, slice).dest(x, destY, TRACK_SRC_W, slice).blit(g);
            destY += slice;
            middle -= slice;
        }
        if (bottom > 0) {
            TERMINAL.src(TRACK_SRC_X, 53 + 18 - bottom, TRACK_SRC_W, bottom)
                    .dest(x, destY, TRACK_SRC_W, bottom).blit(g);
        }
    }

    /**
     * Handle a press inside the track. Returns true if consumed (thumb grab or page-jump). When
     * consumed, the caller must route subsequent {@link #mouseDragged} / {@link #mouseReleased} here.
     */
    public boolean mousePressed(double mx, double my, int trackX, int trackY, int trackW, int trackH,
            int visible, int maxScroll) {
        if (maxScroll <= 0) {
            return false;
        }
        if (!(mx >= trackX && mx < trackX + trackW && my >= trackY && my < trackY + trackH)) {
            return false;
        }
        int thumbH = thumbHeight(trackH);
        int thumbY = thumbY(trackY, trackH, thumbH, scroll, maxScroll);
        if (my >= thumbY && my < thumbY + thumbH) {
            // Grab the thumb.
            dragging = true;
            grabOffset = my - thumbY;
        } else if (my < thumbY) {
            // Page up.
            scroll = Math.max(0, scroll - visible);
        } else {
            // Page down.
            scroll = Math.min(maxScroll, scroll + visible);
        }
        return true;
    }

    /** Drag the thumb. Only meaningful after a thumb-grabbing {@link #mousePressed}. */
    public boolean mouseDragged(double my, int trackY, int trackH, int maxScroll) {
        if (!dragging || maxScroll <= 0) {
            return false;
        }
        int thumbH = thumbHeight(trackH);
        int travel = trackH - thumbH;
        if (travel <= 0) {
            return true;
        }
        double thumbTop = my - grabOffset;
        double ratio = (thumbTop - trackY) / travel;
        scroll = Math.max(0, Math.min(maxScroll, (int) Math.round(ratio * maxScroll)));
        return true;
    }

    public void mouseReleased() {
        dragging = false;
    }

    public boolean isDragging() {
        return dragging;
    }
}
