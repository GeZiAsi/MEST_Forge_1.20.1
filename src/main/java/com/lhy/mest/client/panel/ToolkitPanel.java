package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import com.lhy.mest.client.dock.model.ModuleDefaults;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import com.lhy.mest.network.MestPackets;

import appeng.client.Point;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.Scrollbar;


import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.config.MestConfig;
import com.lhy.mest.network.ToolkitMemorySlotPacket;
import com.lhy.mest.terminal.MESTMenu;
import com.lhy.mest.terminal.ToolkitSlot;

/**
 * Toolkit: unstackable tools in a scrollable slot grid. The slot frame and scrollbar follow the
 * actual visible slots instead of stretching to the whole panel.
 */
public class ToolkitPanel extends ModulePanel {
    private static final int SLOT = 18;
    private static final int MIN_COLS = 9;
    private static final int MIN_ROWS = 2;
    private static final int LOCK = 12;
    private static final int TRACK_WIDTH = 5;
    private static final int TRACK_INNER = 3;
    /** Pin sits 4px from the right edge; keep the same column so lock lines up under it. */
    private static final int INSIDE_GUTTER = LOCK + 4 - CONTENT_PADDING;
    private static final int TRACK_BORDER = 0xFFF2F2F2;
    private static final int TRACK_FILL = 0xFF9A9FB4;
    private static final float MEMORY_GHOST_ALPHA = 0.38F;

    private final MESTMenu menu;
    private final List<Slot> toolkitSlots;
    private final Scrollbar scrollbar = new Scrollbar(Scrollbar.SMALL);
    private final LockButton lockButton;
    private int cols = MIN_COLS;
    private int rows = MIN_ROWS;
    private int scrollRows;
    private boolean scrollbarDragging;
    private boolean memoryMode;

    public ToolkitPanel(MESTMenu menu) {
        this.menu = menu;
        this.toolkitSlots = menu.getToolkitSlots();
        for (Slot slot : toolkitSlots) {
            registerSlot(slot);
        }
        this.scrollbar.setCaptureMouseWheel(false);
        this.lockButton = new LockButton(button -> memoryMode = !memoryMode);
    }

    @Override
    public ModuleDefaults defaults() {
        return new ModuleDefaults(false, true, false, true);
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 11, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "toolkit";
    }

    @Override
    public Component title() {
        return Component.translatable("gui.mesplicedterminal.toolkit");
    }

    @Override
    public int defaultWidth() {
        return 2 * CONTENT_PADDING + MIN_COLS * SLOT + INSIDE_GUTTER;
    }

    @Override
    public int defaultHeight() {
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + MIN_ROWS * SLOT;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return defaultHeight();
    }

    @Override
    public boolean expandsVertically() {
        return true;
    }

    @Override
    public boolean fillsContentArea() {
        return false;
    }

    @Override
    public int preferredContentWidth() {
        return Math.max(MIN_COLS, cols) * SLOT + INSIDE_GUTTER;
    }

    @Override
    public int preferredContentHeight() {
        return Math.max(MIN_ROWS * SLOT, occupiedHeight());
    }

    @Override
    public int preferredContentRightInset() {
        return visible ? INSIDE_GUTTER : 0;
    }

    @Override
    public void layoutSlots() {
        this.cols = Math.max(MIN_COLS, contentWidth() / SLOT);
        this.rows = Math.max(MIN_ROWS, contentHeight() / SLOT);
        layoutScrollbar();
        layoutLock();
        if (!visible) {
            for (Slot slot : toolkitSlots) {
                hideSlot(slot);
            }
            return;
        }
        int gridLeft = contentLeft();
        int gridTop = contentTop();
        int first = scrollRows * cols;
        int displayRows = displayRows();
        for (int index = 0; index < toolkitSlots.size(); index++) {
            Slot slot = toolkitSlots.get(index);
            int visibleIndex = index - first;
            int row = cols == 0 ? 0 : visibleIndex / cols;
            int col = cols == 0 ? 0 : visibleIndex % cols;
            if (visibleIndex < 0 || row >= displayRows) {
                hideSlot(slot);
            } else {
                placeSlot(slot, gridLeft + col * SLOT + 1, gridTop + row * SLOT + 1);
            }
        }
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        layoutScrollbar();
        layoutLock();
        int gridLeft = contentLeft();
        int gridTop = contentTop();
        int visibleCells = visibleCells();
        int occupiedRows = occupiedRows();
        int lastCols = lastRowCols();
        for (int visibleIndex = 0; visibleIndex < visibleCells; visibleIndex++) {
            int row = cols == 0 ? 0 : visibleIndex / cols;
            int col = cols == 0 ? 0 : visibleIndex % cols;
            drawSlot(g, gridLeft + col * SLOT, gridTop + row * SLOT);
        }
        drawOccupiedBorder(g, gridLeft, gridTop, cols, occupiedRows, lastCols);
        lockButton.render(g, mouseX, mouseY, partialTicks);
        g.flush();
        int trackHeight = trackHeight();
        if (trackHeight > 0 && maxScroll() > 0) {
            drawTrack(g, trackDrawLeft(), trackTop(), trackHeight);
            scrollbar.drawForegroundLayer(g, new Rect2i(0, 0, 0, 0), new Point(mouseX, mouseY));
        }
    }

    public boolean isLockEditing() {
        return memoryMode;
    }

    /** In lock-editing, a remembered slot shows only the ghost, not the live item. */
    public boolean hideLiveItem(Slot slot) {
        return memoryMode
                && slot instanceof ToolkitSlot toolkitSlot
                && menu.hasToolkitMemory(toolkitSlot.toolkitIndex());
    }

    public void renderMemoryGhost(GuiGraphics g, Slot slot) {
        if (slot.x <= -1000 || slot.y <= -1000 || !(slot instanceof ToolkitSlot toolkitSlot)) {
            return;
        }
        if (!menu.hasToolkitMemory(toolkitSlot.toolkitIndex())) {
            return;
        }
        ItemStack memory = menu.getToolkitMemoryStack(toolkitSlot.toolkitIndex());
        if (memory.isEmpty()) {
            return;
        }
        g.setColor(1.0F, 1.0F, 1.0F, MEMORY_GHOST_ALPHA);
        g.renderItem(memory, slot.x, slot.y);
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public void renderForegroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
    }

    public ITooltip hoveredTooltip(int mouseX, int mouseY) {
        layoutLock();
        if (lockButton.visible && lockButton.isMouseOver(mouseX, mouseY)) {
            return lockButton;
        }
        if (!memoryMode) {
            return null;
        }
        Slot hovered = slotAt(mouseX, mouseY);
        if (hovered == null) {
            return null;
        }
        Component line;
        if (hovered.getItem().isEmpty()) {
            boolean locked = hovered instanceof ToolkitSlot slot && menu.hasToolkitMemory(slot.toolkitIndex());
            line = Component.translatable(locked
                    ? "gui.mesplicedterminal.toolkit.lock.slot.clear"
                    : "gui.mesplicedterminal.toolkit.lock.slot.empty");
        } else {
            line = Component.translatable("gui.mesplicedterminal.toolkit.lock.slot.set");
        }
        return new SlotLockTooltip(hovered, line);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) {
            return false;
        }
        layoutLock();
        if (lockButton.visible && lockButton.mouseClicked(mx, my, button)) {
            return true;
        }
        if (!memoryMode || (button != 0 && button != 1)) {
            return false;
        }
        Slot slot = slotAt(mx, my);
        if (!(slot instanceof ToolkitSlot toolkitSlot)) {
            return false;
        }
        if (button == 0 && slot.getItem().isEmpty()) {
            return true;
        }
        boolean remember = button == 0;
        menu.setToolkitMemorySlot(toolkitSlot.toolkitIndex(), remember);
        MestPackets.sendToServer(new ToolkitMemorySlotPacket(toolkitSlot.toolkitIndex(), remember));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!visible || scrollY == 0 || (!inGrid(mx, my) && !inScroller(mx, my))) {
            return false;
        }
        scrollbar.setCurrentScroll(scrollbar.getCurrentScroll() - (int) Math.signum(scrollY));
        scrollRows = scrollbar.getCurrentScroll();
        layoutSlots();
        return true;
    }

    @Override
    public boolean scrollbarPressed(double mx, double my) {
        if (!visible || maxScroll() <= 0 || !inScrollbar(mx, my)) {
            return false;
        }
        boolean consumed = scrollbar.onMouseDown(new Point((int) mx, (int) my), 0);
        scrollbarDragging = consumed;
        scrollRows = scrollbar.getCurrentScroll();
        layoutSlots();
        return consumed;
    }

    @Override
    public boolean scrollbarDragged(double mx, double my) {
        if (!scrollbarDragging) {
            return false;
        }
        boolean consumed = scrollbar.onMouseDrag(new Point((int) mx, (int) my), 0);
        scrollRows = scrollbar.getCurrentScroll();
        layoutSlots();
        return consumed;
    }

    @Override
    public void scrollbarReleased() {
        if (scrollbarDragging) {
            scrollbar.onMouseUp(Point.ZERO, 0);
        }
        scrollbarDragging = false;
        scrollRows = scrollbar.getCurrentScroll();
        layoutSlots();
    }

    @Override
    public boolean scrollbarDragging() {
        return scrollbarDragging;
    }

    private void layoutLock() {
        lockButton.visible = visible;
        lockButton.active = visible;
        lockButton.setX(pinButtonX());
        lockButton.setY(contentTop());
    }

    private void layoutScrollbar() {
        int shown = displayRows();
        int max = Math.max(0, neededRows() - shown);
        scrollbar.setRange(0, max, 1);
        int height = trackHeight();
        scrollbar.setHeight(Math.max(1, height - 2));
        // Same inset as TrashPanel: thumb sits in the 3px inner fill of the 5px track.
        scrollbar.setPosition(new Point(trackDrawLeft() - 1, trackTop() + 1));
        scrollbar.setCurrentScroll(Math.min(scrollRows, max));
        scrollRows = scrollbar.getCurrentScroll();
    }

    private int maxScroll() {
        return Math.max(0, neededRows() - displayRows());
    }

    private int neededRows() {
        int size = Math.max(toolkitSlots.size(), MestConfig.toolkitSlots());
        return Math.max(1, (size + cols - 1) / Math.max(1, cols));
    }

    private int displayRows() {
        return Math.max(1, Math.min(rows, neededRows()));
    }

    private int visibleCells() {
        return Math.min(Math.max(0, toolkitSlots.size() - scrollRows * cols), displayRows() * cols);
    }

    private int occupiedRows() {
        int cells = visibleCells();
        return cells <= 0 ? 0 : (cells + Math.max(1, cols) - 1) / Math.max(1, cols);
    }

    private int lastRowCols() {
        int cells = visibleCells();
        if (cells <= 0 || cols <= 0) {
            return 0;
        }
        int rem = cells % cols;
        return rem == 0 ? cols : rem;
    }

    private int occupiedHeight() {
        return Math.max(0, occupiedRows() * SLOT);
    }

    private int trackDrawLeft() {
        return pinButtonX() + (LOCK - TRACK_WIDTH) / 2;
    }

    private int trackTop() {
        return contentTop() + LOCK + 1;
    }

    private int trackHeight() {
        return Math.max(0, occupiedHeight() - LOCK - 1);
    }

    private boolean inGrid(double mx, double my) {
        int occupied = occupiedRows();
        int last = lastRowCols();
        if (occupied <= 0) {
            return false;
        }
        int y = (int) my - contentTop();
        if (y < 0 || y >= occupied * SLOT) {
            return false;
        }
        int row = y / SLOT;
        int width = row == occupied - 1 ? last * SLOT : cols * SLOT;
        return mx >= contentLeft() && mx < contentLeft() + width;
    }

    private boolean inScroller(double mx, double my) {
        if (inScrollbar(mx, my) || inLock(mx, my)) {
            return true;
        }
        int height = trackHeight();
        if (height <= 0) {
            return false;
        }
        int left = trackDrawLeft();
        int top = trackTop();
        return mx >= left && mx < left + TRACK_WIDTH && my >= top && my < top + height;
    }

    private boolean inLock(double mx, double my) {
        return lockButton.visible && mx >= lockButton.getX() && mx < lockButton.getX() + lockButton.getWidth()
                && my >= lockButton.getY() && my < lockButton.getY() + lockButton.getHeight();
    }

    private boolean inScrollbar(double mx, double my) {
        if (maxScroll() <= 0) {
            return false;
        }
        Rect2i bounds = scrollbar.getBounds();
        return mx >= bounds.getX() && mx < bounds.getX() + bounds.getWidth()
                && my >= bounds.getY() && my < bounds.getY() + bounds.getHeight();
    }

    private Slot slotAt(double mx, double my) {
        for (Slot slot : toolkitSlots) {
            if (slot.x <= -1000 || slot.y <= -1000) {
                continue;
            }
            if (mx >= slot.x && mx < slot.x + 16 && my >= slot.y && my < slot.y + 16) {
                return slot;
            }
        }
        return null;
    }

    private static void drawOccupiedBorder(GuiGraphics g, int px, int py, int cols, int occupiedRows, int lastCols) {
        if (occupiedRows <= 0 || lastCols <= 0) {
            return;
        }
        if (occupiedRows == 1 || lastCols == cols) {
            drawRectBorder(g, px, py, (occupiedRows == 1 ? lastCols : cols) * SLOT, occupiedRows * SLOT);
            return;
        }
        int fullBottom = py + (occupiedRows - 1) * SLOT;
        int fullRight = px + cols * SLOT;
        int lastRight = px + lastCols * SLOT;
        int bottom = py + occupiedRows * SLOT;
        int color = 0xFFF2F2F2;
        g.hLine(px, fullRight - 1, py, color);
        g.vLine(px, py, bottom - 1, color);
        g.vLine(fullRight - 1, py, fullBottom - 1, color);
        g.hLine(lastRight - 1, fullRight - 1, fullBottom - 1, color);
        g.vLine(lastRight - 1, fullBottom - 1, bottom - 1, color);
        g.hLine(px, lastRight - 1, bottom - 1, color);
    }

    private static void drawRectBorder(GuiGraphics g, int px, int py, int width, int height) {
        int x1 = px + width;
        int y1 = py + height;
        g.hLine(px, x1 - 1, py, 0xFFF2F2F2);
        g.hLine(px, x1 - 1, y1 - 1, 0xFFF2F2F2);
        g.vLine(px, py, y1 - 1, 0xFFF2F2F2);
        g.vLine(x1 - 1, py, y1 - 1, 0xFFF2F2F2);
    }

    private static void drawTrack(GuiGraphics g, int x, int y, int height) {
        if (height <= 0) {
            return;
        }
        int x1 = x + TRACK_WIDTH - 1;
        int y1 = y + height - 1;
        g.hLine(x, x1, y, TRACK_BORDER);
        g.hLine(x, x1, y1, TRACK_BORDER);
        g.vLine(x, y, y1, TRACK_BORDER);
        g.vLine(x1, y, y1, TRACK_BORDER);
        if (height > 2) {
            g.fill(x + 1, y + 1, x + 1 + TRACK_INNER, y1, TRACK_FILL);
        }
    }

    private final class LockButton extends Button implements ITooltip {
        private LockButton(OnPress onPress) {
            super(0, 0, LOCK, LOCK, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            int px = getX();
            int py = getY() + yOffset;
            Blitter background = com.lhy.mest.client.MestButtonSprites.terminalButton(isHovered(), false);
            background.dest(px, py, getWidth(), getHeight()).blit(graphics);
            Icon icon = memoryMode ? Icon.LOCKED : Icon.UNLOCKED;
            icon.getBlitter().dest(px, py, getWidth(), getHeight()).blit(graphics);
        }

        @Override
        public List<Component> getTooltipMessage() {
            if (memoryMode) {
                return List.of(
                        Component.translatable("gui.mesplicedterminal.toolkit.lock.editing"),
                        Component.translatable("gui.mesplicedterminal.toolkit.lock.editing.hint1"),
                        Component.translatable("gui.mesplicedterminal.toolkit.lock.editing.hint2"));
            }
            return List.of(
                    Component.translatable("gui.mesplicedterminal.toolkit.lock"),
                    Component.translatable("gui.mesplicedterminal.toolkit.lock.hint1"),
                    Component.translatable("gui.mesplicedterminal.toolkit.lock.hint2"));
        }

        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(getX(), getY(), getWidth(), getHeight());
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return visible;
        }
    }

    private record SlotLockTooltip(Slot slot, Component line) implements ITooltip {
        @Override
        public List<Component> getTooltipMessage() {
            return List.of(line);
        }

        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(slot.x, slot.y, 16, 16);
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return true;
        }
    }
}
