package com.lhy.mest.client.dock;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import appeng.menu.slot.AppEngSlot;

/**
 * AE2 {@link appeng.client.gui.widgets.UpgradesPanel}-style column, hung off the spliced group like
 * {@link PanelSideBar}. Not a dock module.
 *
 * <p>The upgrade-background slot well starts at the piece's left edge, so items sit at panel
 * {@code x + 1} like vanilla — not at {@code x + PADDING + 1}.
 */
public class ExtraSlotColumn implements ExtraChrome {
    private static final int SLOT = 18;
    private static final int PADDING = 5;
    private static final int MAX_ROWS = 8;
    /** Right chrome of an upgrade-background piece beyond its 18px slot well. */
    private static final int RIGHT_CHROME = 5;
    /** Top piece's 5px above the first well plus the bottom piece's 7px below the last. */
    private static final int VERTICAL_CHROME = 12;

    private final List<Slot> slots;
    private final Supplier<List<Component>> tooltipSupplier;
    private Rect2i bounds = new Rect2i(0, 0, 0, 0);

    public ExtraSlotColumn(List<Slot> slots, Supplier<List<Component>> tooltipSupplier) {
        this.slots = slots;
        this.tooltipSupplier = tooltipSupplier != null ? tooltipSupplier : Collections::emptyList;
    }

    public ExtraSlotColumn(List<Slot> slots) {
        this(slots, Collections::emptyList);
    }

    public boolean hasSlots() {
        return enabledSlotCount() > 0;
    }

    public Rect2i bounds() {
        return bounds;
    }

    public int right() {
        return bounds.getX() + bounds.getWidth();
    }

    /**
     * Left of the next extra panel so its 5px left chrome overlaps this panel's 5px right chrome
     * and the wells sit 18px apart.
     */
    public int nextColumnX() {
        return bounds.getX() + PADDING + columns() * SLOT - 1;
    }

    public boolean isVisible() {
        return bounds.getWidth() > 0 && bounds.getHeight() > 0;
    }

    public boolean contains(double mx, double my) {
        return isVisible()
                && mx >= bounds.getX()
                && mx < bounds.getX() + bounds.getWidth()
                && my >= bounds.getY()
                && my < bounds.getY() + bounds.getHeight();
    }

    public boolean ownsSlot(Slot slot) {
        for (Slot owned : slots) {
            if (owned == slot) {
                return true;
            }
        }
        return false;
    }

    public void layoutAgainst(int attachX, int attachY, boolean show) {
        if (!show || !hasSlots()) {
            hide();
            return;
        }
        int slotCount = enabledSlotCount();
        int rows = Math.max(1, Math.min(MAX_ROWS, slotCount));
        int width = columns() * SLOT + RIGHT_CHROME;
        int height = rows * SLOT + VERTICAL_CHROME;
        bounds = new Rect2i(attachX, attachY, width, height);
        placeSlots();
    }

    public void hide() {
        bounds = new Rect2i(0, 0, 0, 0);
        for (Slot slot : slots) {
            slot.x = -9999;
            slot.y = -9999;
        }
    }

    public void renderBackground(GuiGraphics g) {
        int slotCount = enabledSlotCount();
        if (!isVisible() || slotCount <= 0) {
            return;
        }
        // AE2 15's extra_panels.png has a different layout from AE2 19's, so the column is built
        // from the same AE2WTLib pieces as the upgrade column next to it. Each piece is open on
        // the left, so a following column overlaps the previous one's right edge and joins it.
        UpgradeBackground bg = UpgradeBackground.get(false);
        for (int col = 0; col < columns(); col++) {
            int count = Math.min(MAX_ROWS, slotCount - col * MAX_ROWS);
            int x = bounds.getX() + col * SLOT;
            int slotOriginY = bounds.getY() + PADDING;
            bg.top().getBlitter().dest(x, bounds.getY()).blit(g);
            for (int row = 1; row < count - 1; row++) {
                bg.middle().getBlitter().dest(x, slotOriginY + row * SLOT).blit(g);
            }
            bg.bottom().getBlitter().dest(x, slotOriginY + (count - 1) * SLOT).blit(g);
        }
    }

    public void renderSlots(GuiGraphics g, ModulePanel.PanelSlotRenderer renderer) {
        if (!isVisible()) {
            return;
        }
        for (Slot slot : slots) {
            if (slot.x <= -1000 || slot.y <= -1000) {
                continue;
            }
            renderer.drawPanelSlot(g, slot);
        }
    }

    @Override
    public List<Component> chromeTooltipAt(double mx, double my) {
        if (!contains(mx, my)) {
            return List.of();
        }
        Slot slot = slotAt(mx, my);
        if (slot != null && !slot.getItem().isEmpty()) {
            return List.of();
        }
        return tooltipSupplier.get();
    }

    private Slot slotAt(double mx, double my) {
        for (Slot slot : slots) {
            if (slot.x <= -1000 || slot.y <= -1000) {
                continue;
            }
            if (mx >= slot.x - 1 && mx < slot.x + 17 && my >= slot.y - 1 && my < slot.y + 17) {
                return slot;
            }
        }
        return null;
    }

    private void placeSlots() {
        // Same as UpgradesPanel.updateBeforeRender: items sit on the well at panel x+1.
        int slotOriginX = bounds.getX();
        int slotOriginY = bounds.getY() + PADDING;
        int index = 0;
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot && !appEngSlot.isSlotEnabled()) {
                slot.x = -9999;
                slot.y = -9999;
                continue;
            }
            int row = index % MAX_ROWS;
            int col = index / MAX_ROWS;
            slot.x = slotOriginX + col * SLOT + 1;
            slot.y = slotOriginY + row * SLOT + 1;
            index++;
        }
    }

    private int columns() {
        return Math.max(1, (enabledSlotCount() + MAX_ROWS - 1) / MAX_ROWS);
    }

    private int enabledSlotCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (slot instanceof AppEngSlot appEngSlot) {
                if (appEngSlot.isSlotEnabled()) {
                    count++;
                }
            } else {
                count++;
            }
        }
        return count;
    }
}
