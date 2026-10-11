package com.lhy.mest.client.dock;

import com.lhy.mest.util.MestLists;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.Upgrades;
import appeng.client.Point;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.GuiText;
import appeng.menu.slot.AppEngSlot;

import de.mari_023.ae2wtlib.AE2wtlib;

/**
 * AE2wtlib {@code ScrollingUpgradesPanel} hung off the anchored group: same {@link UpgradeBackground}
 * sprites, small scroller, and CompatibleUpgrades tooltip. Slot list is whatever the menu exposes
 * under the UPGRADE semantic so other mods' extra upgrade slots still appear.
 */
public class ScrollingUpgradeColumn implements ExtraChrome {
    private static final int SLOT = 18;
    private static final int PADDING = 5;
    private static final int FIXED_WIDTH = 23;
    /** {@code UPGRADE_BACKGROUND_SCROLLING_*} sprites are 29px wide; the track is the right 6px. */
    private static final int SCROLLING_WIDTH = 29;
    /** AE2 {@code small_scroller} is 7×15; sit it on the 6px track that starts at x+23. */
    private static final int TRACK_X = 19;
    private static final int DEFAULT_MAX_ROWS = 2;

    private final List<Slot> slots;
    private final IUpgradeableObject upgradeable;
    private final Scrollbar scrollbar = new Scrollbar(Scrollbar.SMALL);
    private Rect2i bounds = new Rect2i(0, 0, 0, 0);
    private int maxRows = DEFAULT_MAX_ROWS;
    private int x;
    private int y;
    private boolean draggingScrollbar;

    public ScrollingUpgradeColumn(List<Slot> slots, IUpgradeableObject upgradeable) {
        this.slots = slots;
        this.upgradeable = upgradeable;
        this.scrollbar.setCaptureMouseWheel(false);
    }

    public void setMaxRows(int rows) {
        this.maxRows = Math.max(1, rows);
    }

    @Override
    public boolean hasSlots() {
        return upgradeSlotCount() > 0;
    }

    @Override
    public boolean isVisible() {
        return bounds.getWidth() > 0 && bounds.getHeight() > 0;
    }

    @Override
    public Rect2i bounds() {
        return bounds;
    }

    @Override
    public int nextColumnX() {
        if (scrolling()) {
            return bounds.getX() + 26;
        }
        return bounds.getX() + FIXED_WIDTH - 2;
    }

    @Override
    public boolean contains(double mx, double my) {
        return isVisible()
                && mx >= bounds.getX()
                && mx < bounds.getX() + bounds.getWidth()
                && my >= bounds.getY()
                && my < bounds.getY() + bounds.getHeight();
    }

    @Override
    public boolean ownsSlot(Slot slot) {
        for (Slot owned : slots) {
            if (owned == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void layoutAgainst(int attachX, int attachY, boolean show) {
        if (!show || !hasSlots()) {
            hide();
            return;
        }
        this.x = attachX;
        this.y = attachY;
        int visible = visibleSlotCount();
        int width = scrolling() ? SCROLLING_WIDTH : FIXED_WIDTH;
        int height = 2 * PADDING + visible * SLOT;
        bounds = new Rect2i(attachX, attachY, width, height);
        setScrollbarRange();
        scrollbar.setVisible(scrolling());
        scrollbar.setHeight(Math.max(1, visible * SLOT - 2));
        scrollbar.setPosition(new Point(attachX + TRACK_X, attachY + PADDING + 1));
        placeSlots();
    }

    @Override
    public void hide() {
        bounds = new Rect2i(0, 0, 0, 0);
        scrollbar.setVisible(false);
        for (Slot slot : slots) {
            slot.x = -9999;
            slot.y = -9999;
        }
    }

    @Override
    public void renderBackground(GuiGraphics g) {
        int visible = visibleSlotCount();
        if (!isVisible() || visible <= 0) {
            return;
        }
        int slotOriginX = x;
        int slotOriginY = y + PADDING;
        UpgradeBackground bg = UpgradeBackground.get(scrolling());
        bg.top().getBlitter().dest(slotOriginX, slotOriginY - PADDING).blit(g);
        for (int i = 1; i < visible - 1; i++) {
            bg.middle().getBlitter().dest(slotOriginX, slotOriginY + i * SLOT).blit(g);
        }
        if (visible >= 1) {
            bg.bottom().getBlitter().dest(slotOriginX, slotOriginY + (visible - 1) * SLOT).blit(g);
        }
        if (scrolling()) {
            scrollbar.drawForegroundLayer(g, new Rect2i(0, 0, 0, 0), Point.ZERO);
        }
    }

    @Override
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
        return compatibleUpgradeTooltip();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!scrolling() || !overScrollbar(mx, my)) {
            return false;
        }
        boolean handled = scrollbar.onMouseDown(new Point((int) mx, (int) my), button);
        draggingScrollbar = handled;
        return handled;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        boolean wasDragging = draggingScrollbar;
        draggingScrollbar = false;
        scrollbar.onMouseUp(new Point((int) mx, (int) my), button);
        return wasDragging;
    }

    @Override
    public boolean mouseDragged(double mx, double my) {
        if (!draggingScrollbar) {
            return false;
        }
        boolean handled = scrollbar.onMouseDrag(new Point((int) mx, (int) my), 0);
        if (handled) {
            placeSlots();
        }
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!contains(mx, my) || !scrolling()) {
            return false;
        }
        boolean handled = scrollbar.onMouseWheel(new Point((int) mx, (int) my), delta);
        if (handled) {
            placeSlots();
        }
        return handled;
    }

    private boolean overScrollbar(double mx, double my) {
        Rect2i bar = scrollbar.getBounds();
        return mx >= bar.getX()
                && mx < bar.getX() + bar.getWidth()
                && my >= bar.getY()
                && my < bar.getY() + bar.getHeight();
    }

    private List<Component> compatibleUpgradeTooltip() {
        var lines = new ArrayList<Component>();
        lines.add(GuiText.CompatibleUpgrades.text());
        if (upgradeable == null) {
            return lines;
        }
        var seen = new LinkedHashSet<String>();
        for (Component line : Upgrades.getTooltipLinesForMachine(upgradeable.getUpgrades().getUpgradableItem())) {
            if (seen.add(line.getString())) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void placeSlots() {
        int slotOriginX = x;
        int slotOriginY = y + PADDING;
        int currentFirstSlot = scrollbar.getCurrentScroll();
        setScrollbarRange();
        int i = 0;
        for (Slot s : slots) {
            if (!(s instanceof AppEngSlot slot)) {
                continue;
            }
            if (singularitySlotHidden(slot)) {
                slot.x = -9999;
                slot.y = -9999;
                continue;
            }
            boolean show = currentFirstSlot <= i && currentFirstSlot + maxRows > i;
            i++;
            if (!show) {
                slot.x = -9999;
                slot.y = -9999;
                continue;
            }
            slot.x = slotOriginX + 1;
            slot.y = slotOriginY + 1;
            slotOriginY += SLOT;
        }
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

    private boolean singularitySlotHidden(Slot slot) {
        if (slots.isEmpty() || slot != MestLists.first(slots)) {
            return false;
        }
        if (!(slot instanceof AppEngSlot appEngSlot)) {
            return false;
        }
        return appEngSlot.getItem().isEmpty()
                && !upgradeable.getUpgrades().isInstalled(AE2wtlib.QUANTUM_BRIDGE_CARD);
    }

    private int upgradeSlotCount() {
        int count = 0;
        for (Slot slot : slots) {
            if (!(slot instanceof AppEngSlot)) {
                continue;
            }
            if (singularitySlotHidden(slot)) {
                continue;
            }
            count++;
        }
        return count;
    }

    private int visibleSlotCount() {
        return Math.min(maxRows, upgradeSlotCount());
    }

    private boolean scrolling() {
        return upgradeSlotCount() > maxRows;
    }

    private void setScrollbarRange() {
        scrollbar.setRange(0, Math.max(0, upgradeSlotCount() - visibleSlotCount()), 1);
    }
}
