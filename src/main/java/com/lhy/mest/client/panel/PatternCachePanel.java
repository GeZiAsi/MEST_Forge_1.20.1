package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import com.lhy.mest.network.MestPackets;

import appeng.client.Point;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.AppEng;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.network.PatternCacheActionPacket;
import com.lhy.mest.terminal.MESTMenu;
import com.lhy.mest.terminal.MESTMenuHost;

/**
 * Encoded-pattern cache: wireless-terminal grid without search, gray pattern wells, in-panel
 * small scroller, batch multiplier row, and title-row substitution toggles.
 */
public class PatternCachePanel extends ModulePanel {
    private static final int SLOT = 18;
    private static final int COLS = 9;
    private static final int DEFAULT_ROWS = 2;
    private static final int MIN_ROWS = 2;
    private static final int BUTTON_W = 17;
    private static final int BUTTON_H = 16;
    private static final int BUTTON_ROW = BUTTON_H + 1;
    private static final int BUTTON_GAP = 1;
    private static final int TOGGLE_SIZE = 14;
    private static final int TRACK_WIDTH = 5;
    private static final int TRACK_INNER = 3;
    private static final int INSIDE_TRACK_GAP = 2;
    private static final int TRACK_SHIFT_X = 3;
    private static final int INSIDE_GUTTER = INSIDE_TRACK_GAP + TRACK_WIDTH + TRACK_SHIFT_X;
    private static final int TRACK_BORDER = 0xFFF2F2F2;
    private static final int TRACK_FILL = 0xFF9A9FB4;
    private static final ResourceLocation STATES = new ResourceLocation(
            MESplicedterminal.MODID, "textures/guis/pattern_cache_states.png");
    private static final ResourceLocation CHECKBOX = AppEng.makeId("textures/guis/checkbox.png");

    private final List<Slot> cacheSlots;
    private final Scrollbar scrollbar = new Scrollbar(Scrollbar.SMALL);
    private final MultiplierButton[] multipliers = new MultiplierButton[8];
    private final SubstitutionButton itemSubstitution = new SubstitutionButton(
            true, Component.translatable("gui.mesplicedterminal.pattern_cache.item_substitution"));
    private final SubstitutionButton fluidSubstitution = new SubstitutionButton(
            false, Component.translatable("gui.mesplicedterminal.pattern_cache.fluid_substitution"));
    private int rows = DEFAULT_ROWS;
    private int scrollRows;
    private boolean scrollbarDragging;

    public PatternCachePanel(MESTMenu menu) {
        this.cacheSlots = menu.getPatternCacheSlots();
        for (Slot slot : cacheSlots) {
            registerSlot(slot);
        }
        this.scrollbar.setCaptureMouseWheel(false);
        PatternCacheActionPacket.Action[] order = {
                PatternCacheActionPacket.Action.SWAP,
                PatternCacheActionPacket.Action.EQUALS_1,
                PatternCacheActionPacket.Action.TIMES_2,
                PatternCacheActionPacket.Action.DIVIDE_2,
                PatternCacheActionPacket.Action.TIMES_3,
                PatternCacheActionPacket.Action.DIVIDE_3,
                PatternCacheActionPacket.Action.TIMES_5,
                PatternCacheActionPacket.Action.DIVIDE_5
        };
        PatternCacheActionPacket.Action[] shiftActions = {
                null,
                null,
                PatternCacheActionPacket.Action.TIMES_8,
                PatternCacheActionPacket.Action.DIVIDE_8,
                PatternCacheActionPacket.Action.TIMES_16,
                PatternCacheActionPacket.Action.DIVIDE_16,
                PatternCacheActionPacket.Action.TIMES_32,
                PatternCacheActionPacket.Action.DIVIDE_32
        };
        String[] labels = {"⇄", "=1", "×2", "÷2", "×3", "÷3", "×5", "÷5"};
        String[] shiftLabels = {null, null, "×8", "÷8", "×16", "÷16", "×32", "÷32"};
        String[] keys = {"swap", "equals1", "times2", "divide2", "times3", "divide3", "times5", "divide5"};
        String[] shiftKeys = {null, null, "times8", "divide8", "times16", "divide16", "times32", "divide32"};
        for (int i = 0; i < order.length; i++) {
            multipliers[i] = new MultiplierButton(
                    labels[i], shiftLabels[i], keys[i], shiftKeys[i], order[i], shiftActions[i]);
        }
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 4, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "pattern_cache";
    }

    @Override
    public Component title() {
        return Component.translatable("gui.mesplicedterminal.pattern_cache");
    }

    @Override
    public int defaultWidth() {
        return 2 * CONTENT_PADDING + COLS * SLOT + INSIDE_GUTTER;
    }

    @Override
    public int defaultHeight() {
        return TITLE_BAR_HEIGHT + BUTTON_ROW + CONTENT_PADDING + DEFAULT_ROWS * SLOT;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return TITLE_BAR_HEIGHT + BUTTON_ROW + MIN_ROWS * SLOT;
    }

    @Override
    public boolean expandsVertically() {
        return false;
    }

    @Override
    public int preferredContentRightInset() {
        return visible ? INSIDE_GUTTER : 0;
    }

    @Override
    public int contentTop() {
        return super.contentTop() + BUTTON_ROW;
    }

    @Override
    public int contentHeight() {
        return Math.max(0, super.contentHeight() - BUTTON_ROW);
    }

    @Override
    protected int titleRightInset() {
        return super.titleRightInset() + 2 * (TOGGLE_SIZE + 2);
    }

    @Override
    public boolean inTitleBarControls(double mx, double my) {
        return super.inTitleBarControls(mx, my)
                || (itemSubstitution.visible && itemSubstitution.isMouseOver(mx, my))
                || (fluidSubstitution.visible && fluidSubstitution.isMouseOver(mx, my));
    }

    @Override
    public void layoutSlots() {
        this.rows = Math.max(MIN_ROWS, contentHeight() / SLOT);
        layoutScrollbar();
        layoutButtons();
        if (!visible) {
            for (Slot slot : cacheSlots) {
                hideSlot(slot);
            }
            return;
        }
        int gridLeft = contentLeft();
        int gridTop = contentTop();
        int first = scrollRows * COLS;
        for (int index = 0; index < cacheSlots.size(); index++) {
            Slot slot = cacheSlots.get(index);
            int visibleIndex = index - first;
            int row = visibleIndex / COLS;
            int col = visibleIndex % COLS;
            if (visibleIndex < 0 || row >= rows) {
                hideSlot(slot);
            } else {
                placeSlot(slot, gridLeft + col * SLOT + 1, gridTop + row * SLOT + 1);
            }
        }
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        layoutScrollbar();
        layoutButtons();
        int gridLeft = contentLeft();
        int gridTop = contentTop();
        int visible = Math.min(cacheSlots.size() - scrollRows * COLS, rows * COLS);
        for (int visibleIndex = 0; visibleIndex < Math.max(0, visible); visibleIndex++) {
            int row = visibleIndex / COLS;
            int col = visibleIndex % COLS;
            int px = gridLeft + col * SLOT;
            int py = gridTop + row * SLOT;
            drawSlot(g, px, py);
            Icon.BACKGROUND_ENCODED_PATTERN.getBlitter().dest(px + 1, py + 1).blit(g);
        }
        drawSlotGroupBorder(g, gridLeft, gridTop, COLS, rows);
        for (MultiplierButton button : multipliers) {
            button.render(g, mouseX, mouseY, partialTicks);
        }
        itemSubstitution.render(g, mouseX, mouseY, partialTicks);
        fluidSubstitution.render(g, mouseX, mouseY, partialTicks);
        g.flush();
        drawCacheTrack(g, trackDrawLeft(), contentTop(), Math.max(1, rows * SLOT));
        scrollbar.drawForegroundLayer(g, new Rect2i(0, 0, 0, 0), new Point(mouseX, mouseY));
    }

    @Override
    public void renderForegroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        for (MultiplierButton button : multipliers) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                g.renderComponentTooltip(font, button.getTooltipMessage(), mouseX, mouseY);
                return;
            }
        }
        if (itemSubstitution.visible && itemSubstitution.isMouseOver(mouseX, mouseY)) {
            g.renderComponentTooltip(font, itemSubstitution.getTooltipMessage(), mouseX, mouseY);
        } else if (fluidSubstitution.visible && fluidSubstitution.isMouseOver(mouseX, mouseY)) {
            g.renderComponentTooltip(font, fluidSubstitution.getTooltipMessage(), mouseX, mouseY);
        }
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) {
            return false;
        }
        layoutButtons();
        for (MultiplierButton multiplier : multipliers) {
            if (multiplier.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        if (itemSubstitution.mouseClicked(mx, my, button) || fluidSubstitution.mouseClicked(mx, my, button)) {
            return true;
        }
        return false;
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
        if (!visible || !inScrollbar(mx, my)) {
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

    private void layoutButtons() {
        boolean show = visible;
        int buttonY = y + TITLE_BAR_HEIGHT + 1;
        int buttonX = contentLeft() + 1;
        for (int i = 0; i < multipliers.length; i++) {
            multipliers[i].visible = show;
            multipliers[i].active = show;
            multipliers[i].setX(buttonX + i * (BUTTON_W + BUTTON_GAP));
            multipliers[i].setY(buttonY);
        }
        int afterEighth = contentLeft() + 8 * (BUTTON_W + BUTTON_GAP) + 2;
        placeToggle(itemSubstitution, afterEighth, buttonY, show);
        placeToggle(fluidSubstitution, afterEighth + TOGGLE_SIZE + 2, buttonY, show);
    }

    private static void placeToggle(SubstitutionButton button, int x, int y, boolean show) {
        button.visible = show;
        button.active = show;
        button.setX(x);
        button.setY(y);
    }

    private void layoutScrollbar() {
        int max = Math.max(0, totalRows() - rows);
        scrollbar.setRange(0, max, 1);
        scrollbar.setHeight(Math.max(1, rows * SLOT - 2));
        scrollbar.setPosition(new Point(trackLeft() + 2, contentTop() + 1));
        scrollbar.setCurrentScroll(Math.min(scrollRows, max));
        scrollRows = scrollbar.getCurrentScroll();
    }

    private int totalRows() {
        return MESTMenuHost.patternCacheSize() / COLS;
    }

    private int trackLeft() {
        return contentLeft() + COLS * SLOT + INSIDE_TRACK_GAP;
    }

    private int trackDrawLeft() {
        return trackLeft() + TRACK_SHIFT_X;
    }

    private static void drawCacheTrack(GuiGraphics g, int x, int y, int height) {
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

    private boolean inGrid(double mx, double my) {
        return mx >= contentLeft() && mx < contentLeft() + COLS * SLOT
                && my >= contentTop() && my < contentTop() + rows * SLOT;
    }

    private boolean inScroller(double mx, double my) {
        if (inScrollbar(mx, my)) {
            return true;
        }
        int left = trackDrawLeft();
        int top = contentTop();
        int height = Math.max(1, rows * SLOT);
        return mx >= left && mx < left + TRACK_WIDTH && my >= top && my < top + height;
    }

    private boolean inScrollbar(double mx, double my) {
        Rect2i bounds = scrollbar.getBounds();
        return mx >= bounds.getX() && mx < bounds.getX() + bounds.getWidth()
                && my >= bounds.getY() && my < bounds.getY() + bounds.getHeight();
    }

    private static void drawSlotGroupBorder(GuiGraphics g, int px, int py, int cols, int rows) {
        int x1 = px + cols * SLOT;
        int y1 = py + rows * SLOT;
        g.hLine(px, x1 - 1, py, 0xFFF2F2F2);
        g.hLine(px, x1 - 1, y1 - 1, 0xFFF2F2F2);
        g.vLine(px, py, y1 - 1, 0xFFF2F2F2);
        g.vLine(x1 - 1, py, y1 - 1, 0xFFF2F2F2);
    }

    private static final class MultiplierButton extends Button implements ITooltip {
        private final String label;
        private final String shiftLabel;
        private final String tooltipKey;
        private final String shiftTooltipKey;

        private MultiplierButton(
                String label,
                String shiftLabel,
                String tooltipKey,
                String shiftTooltipKey,
                PatternCacheActionPacket.Action action,
                PatternCacheActionPacket.Action shiftAction) {
            super(0, 0, BUTTON_W, BUTTON_H, Component.empty(), button -> {
                PatternCacheActionPacket.Action send =
                        shiftAction != null && Screen.hasShiftDown() ? shiftAction : action;
                MestPackets.sendToServer(new PatternCacheActionPacket(send, false));
            }, Button.DEFAULT_NARRATION);
            this.label = label;
            this.shiftLabel = shiftLabel;
            this.tooltipKey = tooltipKey;
            this.shiftTooltipKey = shiftTooltipKey;
        }

        private String currentLabel() {
            return shiftLabel != null && Screen.hasShiftDown() ? shiftLabel : label;
        }

        private String currentTooltipKey() {
            return shiftTooltipKey != null && Screen.hasShiftDown() ? shiftTooltipKey : tooltipKey;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) {
                return;
            }
            int srcX = isHovered() ? 224 : 192;
            Blitter.texture(STATES, 256, 256)
                    .src(srcX, 160, BUTTON_W, BUTTON_H)
                    .dest(getX(), getY())
                    .blit(graphics);
            Font font = Minecraft.getInstance().font;
            String text = currentLabel();
            int color = isHovered() ? 0xA0A0A0 : 0xFFFFFF;
            int textX = getX() + (BUTTON_W - font.width(text)) / 2;
            int textY = getY() + (BUTTON_H - 8) / 2 + (isHovered() ? 1 : 0);
            graphics.drawString(font, text, textX, textY, color, true);
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(Component.translatable("gui.mesplicedterminal.pattern_cache." + currentTooltipKey()));
        }

        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(getX(), getY(), width, height);
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return visible;
        }
    }

    private static final class SubstitutionButton extends Button implements ITooltip {
        private boolean selected;
        private final Component tooltip;

        private SubstitutionButton(boolean item, Component tooltip) {
            super(0, 0, TOGGLE_SIZE, TOGGLE_SIZE, Component.empty(), button -> {
                SubstitutionButton self = (SubstitutionButton) button;
                self.selected = !self.selected;
                MestPackets.sendToServer(new PatternCacheActionPacket(
                        item ? PatternCacheActionPacket.Action.ITEM_SUBSTITUTION
                                : PatternCacheActionPacket.Action.FLUID_SUBSTITUTION,
                        self.selected));
            }, Button.DEFAULT_NARRATION);
            this.tooltip = tooltip;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) {
                return;
            }
            boolean hover = isHovered();
            int u = selected ? 42 : hover ? 42 : 28;
            int v = selected ? 14 : 0;
            int yOffset = hover || selected ? 1 : 0;
            Blitter.texture(CHECKBOX, 64, 64)
                    .src(u, v, 14, 14)
                    .dest(getX(), getY() + yOffset, TOGGLE_SIZE, TOGGLE_SIZE)
                    .blit(graphics);
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(tooltip);
        }

        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(getX(), getY(), width, height);
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return visible;
        }
    }
}
