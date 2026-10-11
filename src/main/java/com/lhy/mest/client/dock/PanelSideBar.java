package com.lhy.mest.client.dock;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;

import appeng.client.gui.widgets.ITooltip;
import appeng.core.AppEng;

/**
 * AE2 {@code VerticalButtonBar} equivalent. It is not a dock module: the terminal screen
 * attaches it to the left of the non-floating spliced group at runtime, matching vanilla.
 */
public class PanelSideBar {
    public static final int BUTTON = 16;
    public static final int SPACING = 6;
    public static final int MARGIN = 2;
    public static final int ANCHOR_X = 3;
    public static final int ANCHOR_Y = 1;

    private final List<Button> buttons = new ArrayList<>();
    private Rect2i bounds = new Rect2i(0, 0, 0, 0);
    private boolean shown;

    public void add(Button button) {
        buttons.add(button);
    }

    public void clearFrom(int index) {
        if (index < 0) {
            buttons.clear();
            return;
        }
        if (index >= buttons.size()) {
            return;
        }
        buttons.subList(index, buttons.size()).clear();
    }

    public List<Button> buttons() {
        return buttons;
    }

    public Rect2i bounds() {
        return bounds;
    }

    public boolean isVisible() {
        return shown && bounds.getWidth() > 0 && bounds.getHeight() > 0;
    }

    public void layoutAgainst(int groupLeft, int groupTop, boolean show) {
        shown = show;
        if (!show) {
            bounds = new Rect2i(0, 0, 0, 0);
            return;
        }
        int currentY = groupTop + ANCHOR_Y + MARGIN;
        int maxWidth = 0;
        for (Button button : buttons) {
            if (!button.visible) {
                continue;
            }
            int width = Math.max(0, button.getWidth());
            int height = Math.max(0, button.getHeight());
            button.setX(groupLeft + ANCHOR_X - MARGIN - Math.max(width, BUTTON));
            button.setY(currentY);
            if (width <= 0 || height <= 0) {
                continue;
            }
            currentY += height + SPACING;
            maxWidth = Math.max(width, maxWidth);
        }
        if (maxWidth == 0) {
            bounds = new Rect2i(0, 0, 0, 0);
            return;
        }
        bounds = new Rect2i(
                groupLeft + ANCHOR_X - maxWidth - 2 * MARGIN,
                groupTop + ANCHOR_Y,
                maxWidth + 2 * MARGIN,
                currentY - (groupTop + ANCHOR_Y));
    }

    public void hide() {
        layoutAgainst(0, 0, false);
    }

    public void renderBackground(GuiGraphics g) {
        if (!isVisible()) {
            return;
        }
        com.lhy.mest.client.GuiSprites.verticalButtonsBackground(
                g,
                bounds.getX() - 2,
                bounds.getY() - 1,
                bounds.getWidth() + 1,
                bounds.getHeight() + 4);
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!isVisible()) {
            return false;
        }
        for (Button widget : buttons) {
            if (!widget.visible) {
                continue;
            }
            if (widget.isMouseOver(mouseX, mouseY)) {
                return true;
            }
            if (widget instanceof ITooltip tooltip && tooltip.getTooltipArea().contains((int) mouseX, (int) mouseY)) {
                return true;
            }
        }
        return false;
    }

    public AbstractWidget hovered(int mouseX, int mouseY) {
        if (!isVisible()) {
            return null;
        }
        for (Button widget : buttons) {
            if (!widget.visible) {
                continue;
            }
            if (widget.isMouseOver(mouseX, mouseY)) {
                return widget;
            }
            if (widget instanceof ITooltip tooltip && tooltip.getTooltipArea().contains(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    public ITooltip hoveredTooltip(int mouseX, int mouseY) {
        AbstractWidget widget = hovered(mouseX, mouseY);
        return widget instanceof ITooltip tooltip ? tooltip : null;
    }
}
