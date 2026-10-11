package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

import appeng.api.client.AEKeyRendering;
import appeng.api.config.Settings;
import appeng.api.config.TypeFilter;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.ViewItems;
import appeng.api.stacks.AEKeyType;
import appeng.api.stacks.AEKeyTypes;
import appeng.api.stacks.AmountFormat;
import appeng.api.util.IConfigManager;
import appeng.client.Point;
import appeng.client.gui.me.common.Repo;
import appeng.client.gui.me.common.PendingCraftingJobs;
import appeng.client.gui.me.common.PinnedKeys;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ISortSource;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.localization.GuiText;
import appeng.integration.abstraction.ItemListMod;
import appeng.menu.me.common.GridInventoryEntry;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.client.MestPinnedKeysCap;
import com.lhy.mest.client.TerminalSearchRestore;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.terminal.MESTMenu;

/**
 * Floating ME inventory that reuses AE2's {@link Repo}, {@link AETextField}, big scroller and
 * terminal.png row art so the module tracks the stock wireless terminal instead of a custom grid.
 */
public class MEListPanel extends ModulePanel implements ISortSource {
    private static final int SLOT = 18;
    private static final int SEARCH_WIDTH = 89;
    private static final int RAIL_WIDTH = 20;
    private static final int RAIL_SPRITE_WIDTH = 21;
    private static final int RAIL_OVERLAP = 2;
    private static final int RAIL_SHIFT_X = 2;
    private static final int SCROLLBAR_INSET = 2;
    private static final int TRACK_WIDTH = 12;
    private static final int INSIDE_TRACK_GAP = 6;
    private static final int INSIDE_GUTTER = INSIDE_TRACK_GAP + TRACK_WIDTH;
    private static final int NATIVE_SLOT_COLS = 9;
    private static final int CRAFT_STATUS_SIZE = 20;
    private static final int CRAFT_STATUS_OVERHANG = 4;
    private static final int TITLE_SEARCH_TOP = 4;
    /** Pinned-row strip inside {@code guis/terminal.png}: 9 slots wide, one row tall, at y=204. */
    private static final int PINNED_ROW_SRC_Y = 204;
    private static final int PINNED_ROW_NATIVE_WIDTH = 162;
    private static final Blitter TERMINAL = Blitter.texture("guis/terminal.png", 256, 256);
    private static final Set<String> REPORTED_RENDER_FAILURES = new HashSet<>();

    private static String rememberedSearch = "";

    private final MESTMenu menu;
    private final IConfigManager configSrc;
    private final Repo repo;
    private final List<RepoSlot> repoSlots = new ArrayList<>();
    private final Set<Slot> repoSlotSet = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Scrollbar scrollbar = new Scrollbar(Scrollbar.DEFAULT);

    private AETextField searchField;
    private TabButton craftingStatusBtn;
    private CraftingTerminalPanel utilitySource;
    private boolean scrollbarDragging;

    private int cols = 9;
    private int rows = 4;

    public MEListPanel(MESTMenu menu) {
        this.menu = menu;
        this.configSrc = menu.getConfigManager();
        this.scrollbar.setCaptureMouseWheel(false);
        this.repo = new Repo(scrollbar, this);
        menu.setClientRepo(this.repo);
        this.repo.setRowSize(cols);
    }

    public Repo repo() {
        return repo;
    }

    public void attachSearch(AETextField field) {
        this.searchField = field;
        field.setPlaceholder(GuiText.SearchPlaceholder.text());
        field.setResponder(this::onSearchChanged);
        field.setTooltipMessage(List.of(
                GuiText.SearchTooltip.text(),
                GuiText.SearchTooltipModId.text(),
                GuiText.SearchTooltipTag.text(),
                GuiText.SearchTooltipToolTips.text(),
                GuiText.SearchTooltipItemId.text()));
        String restore = TerminalSearchRestore.valueToRestore(
                rememberedSearch,
                menu.isReturnedFromSubScreen(),
                AEConfig.instance().isRememberLastSearch());
        if (!restore.isEmpty()) {
            field.setValue(restore);
            field.selectAll();
            onSearchChanged(restore);
        }
    }

    public void attachCraftingStatus(TabButton button) {
        this.craftingStatusBtn = button;
    }

    public void setUtilitySource(CraftingTerminalPanel source) {
        this.utilitySource = source;
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 0, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "me_list";
    }

    @Override
    public Component title() {
        return Component.translatable("gui.ae2.WirelessTerminal");
    }

    @Override
    public int defaultWidth() {
        return 2 * CONTENT_PADDING + 9 * SLOT + preferredContentRightInset();
    }

    @Override
    public int defaultHeight() {
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + 4 * SLOT;
    }

    @Override
    public int minWidth() {
        return 2 * CONTENT_PADDING + 3 * SLOT + preferredContentRightInset();
    }

    @Override
    public int minHeight() {
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + 2 * SLOT;
    }

    @Override
    public boolean expandsVertically() {
        return true;
    }

    @Override
    public boolean fillsContentArea() {
        return true;
    }

    @Override
    protected int titleRightInset() {
        return SEARCH_WIDTH + 8 + chromeButtonsReserve()
                + (contentChromeVisible() ? 28 : 0) + utilityBarWidth();
    }

    @Override
    public int outsideHitWidth() {
        if (!scrollerOutside()) {
            return 0;
        }
        return Math.max(0, railLeft() - 2 + RAIL_SPRITE_WIDTH - (x + width));
    }

    @Override
    public boolean hasJoinableOutsideRail() {
        return scrollerOutside();
    }

    @Override
    public int preferredContentRightInset() {
        return visible && !scrollerOutside() ? INSIDE_GUTTER : 0;
    }

    @Override
    public int outsideHitTop() {
        return visible && craftingStatusBtn != null ? CRAFT_STATUS_OVERHANG : 0;
    }

    @Override
    public void layoutSlots() {
        List<Slot> slots = menu.slots;
        if (!visible) {
            slots.removeAll(repoSlots);
            repoSlots.clear();
            repoSlotSet.clear();
            hideChrome();
            return;
        }

        int gridWidth = contentWidth();
        int gridHeight = contentHeight();
        int previousCols = this.cols;
        this.cols = Math.max(1, gridWidth / SLOT);
        this.rows = Math.max(1, gridHeight / SLOT);
        this.repo.setRowSize(cols);
        MestPinnedKeysCap.setVisibleColumns(cols);
        if (previousCols != this.cols) {
            this.repo.updateView();
        }

        int requiredSlots = rows * cols;
        if (repoSlots.size() != requiredSlots) {
            slots.removeAll(repoSlots);
            repoSlots.clear();
            repoSlotSet.clear();
            for (int repoIndex = 0; repoIndex < requiredSlots; repoIndex++) {
                var repoSlot = new RepoSlot(this.repo, repoIndex, 0, 0);
                repoSlots.add(repoSlot);
                repoSlotSet.add(repoSlot);
                slots.add(repoSlot);
            }
        }

        int gridLeft = contentLeft();
        int gridTop = contentTop();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                RepoSlot slot = repoSlots.get(row * cols + col);
                placeSlot(slot, gridLeft + col * SLOT + 1, gridTop + row * SLOT + 1);
            }
        }
        layoutChrome();
        updateScrollbar();
    }

    @Override
    public void renderSlots(GuiGraphics g, PanelSlotRenderer renderer) {
        for (RepoSlot slot : repoSlots) {
            renderer.drawPanelSlot(g, slot);
        }
    }

    @Override
    public boolean ownsSlot(Slot slot) {
        return repoSlotSet.contains(slot);
    }

    public void tick(boolean paused) {
        repo.setPaused(paused);
        layoutChrome();
        updateExternalSearch();
        updateScrollbar();
    }

    /**
     * AE2's {@code MEStorageScreen.storeState()}: always snapshot the current search so a later
     * constructor can restore it (sub-screen return, or "remember last search").
     */
    public void storeSearch() {
        if (searchField != null) {
            rememberedSearch = searchField.getValue();
        }
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        layoutChrome();
        updateScrollbar();
        drawTerminalRows(g);
        drawPinnedRowStrip(g);
        drawScrollerRail(g);
        // blitSprite is batched; flush so the well/handle are not covered by the rail.
        g.flush();
        drawScrollerTrack(g);
        if (craftingStatusBtn != null && craftingStatusBtn.visible) {
            craftingStatusBtn.render(g, mouseX, mouseY, partialTicks);
            int jobs = menu.activeCraftingJobs;
            if (jobs != -1) {
                int x = craftingStatusBtn.getX() + (craftingStatusBtn.getWidth() - 18) / 2;
                int y = craftingStatusBtn.getY() + (craftingStatusBtn.getHeight() - 18) / 2;
                StackSizeRenderer.renderSizeLabel(g, font, x, y, String.valueOf(jobs));
            }
        }
        if (utilitySource != null && utilitySource.hostUtilitiesOnMeList()) {
            utilitySource.renderUtilities(g, mouseX, mouseY, partialTicks);
        }
        if (searchField != null && searchField.isVisible()) {
            searchField.render(g, mouseX, mouseY, partialTicks);
        }
        scrollbar.drawForegroundLayer(g, new Rect2i(0, 0, 0, 0), new Point(mouseX, mouseY));
    }

    @Override
    public void renderForegroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (hosted) {
            return;
        }
        renderPinnedRowDecorations(g);
        AbstractWidget hovered = hoveredChrome(mouseX, mouseY);
        if (hovered == craftingStatusBtn) {
            return;
        }
        if (hovered instanceof ITooltip tooltip && !tooltip.getTooltipMessage().isEmpty()) {
            g.renderComponentTooltip(font, tooltip.getTooltipMessage(), mouseX, mouseY);
        }
    }

    /**
     * The "pinned crafted items" row background: AE2's {@code MEStorageScreen} paints an animated
     * molecular-assembler light over every slot whose item is currently being crafted. Replicated
     * here because this screen is a custom dock UI and cannot inherit that screen.
     */
    private void renderPinnedRowDecorations(GuiGraphics g) {
        if (!repo.hasPinnedRow()) {
            return;
        }
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        for (RepoSlot repoSlot : repoSlots) {
            GridInventoryEntry entry = repoSlot.getEntry();
            if (entry == null || !PendingCraftingJobs.hasPendingJob(entry.getWhat())) {
                continue;
            }
            TextureAtlasSprite sprite = atlas.apply(AppEng.makeId("block/molecular_assembler_lights"));
            Blitter.sprite(sprite)
                    .src(sprite.getX() + 2, sprite.getY() + 2,
                            sprite.contents().width() - 4, sprite.contents().height() - 4)
                    .dest(repoSlot.x - 1, repoSlot.y - 1, 18, 18)
                    .blit(g);
        }
    }

    public void renderRepoSlot(GuiGraphics g, Font font, RepoSlot slot) {
        if (!menu.isLinked()) {
            return;
        }
        GridInventoryEntry entry = slot.getEntry();
        if (entry == null) {
            return;
        }
        try {
            AEKeyRendering.drawInGui(Minecraft.getInstance(), g, slotScreenX(slot), slotScreenY(slot), entry.getWhat());
        } catch (Exception e) {
            String keyId = entry.getWhat().getId().toString();
            if (REPORTED_RENDER_FAILURES.add(keyId)) {
                com.lhy.mest.MESplicedterminal.LOGGER.warn(
                        "Failed to render ME entry {} in terminal grid", keyId, e);
            }
            return;
        }
        long storedAmount = entry.getStoredAmount();
        boolean craftable = entry.isCraftable();
        if (craftable && (isViewOnlyCraftable() || storedAmount <= 0)) {
            StackSizeRenderer.renderSizeLabel(g, font, slotScreenX(slot), slotScreenY(slot), "+");
        } else {
            String text = entry.getWhat().formatAmount(storedAmount, AmountFormat.SLOT);
            StackSizeRenderer.renderSizeLabel(g, font, slotScreenX(slot), slotScreenY(slot), text, false);
            if (craftable) {
                StackSizeRenderer.renderSizeLabel(g, font, slotScreenX(slot) - 11, slotScreenY(slot) - 11, "+", false);
            }
        }
    }

    public boolean isViewOnlyCraftable() {
        return getSortDisplay() == ViewItems.CRAFTABLE;
    }

    public boolean inGrid(double mx, double my) {
        int gridLeft = contentLeft();
        int gridTop = contentTop();
        return mx >= gridLeft && mx < gridLeft + cols * SLOT
                && my >= gridTop && my < gridTop + rows * SLOT;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!visible || scrollY == 0 || (!inGrid(mx, my) && !inScroller(mx, my))) {
            return false;
        }
        scrollbar.setCurrentScroll(scrollbar.getCurrentScroll() - (int) Math.signum(scrollY));
        return true;
    }

    public boolean inTitleBarControls(double mx, double my) {
        return super.inTitleBarControls(mx, my) || inSearchField(mx, my) || hoveredUtility(mx, my) != null;
    }

    public boolean inSearchField(double mx, double my) {
        return searchField != null && searchField.visible && searchField.isMouseOver(mx, my);
    }

    public void setSearchValue(String value) {
        if (searchField == null || value == null) {
            return;
        }
        searchField.setValue(value);
        repo.setSearchString(value);
        repo.updateView();
    }

    public void setSearchFocused(boolean focused) {
        if (searchField == null) {
            return;
        }
        searchField.setFocused(focused);
        if (Minecraft.getInstance().screen instanceof com.lhy.mest.client.MESTScreen screen) {
            if (focused) {
                screen.setFocused(searchField);
            } else if (screen.getFocused() == searchField) {
                screen.setFocused(null);
            }
        }
    }

    public boolean isSearchFocused() {
        return searchField != null && searchField.isFocused();
    }

    public boolean searchCharTyped(char character, int modifiers) {
        return isSearchFocused() && searchField.charTyped(character, modifiers);
    }

    public boolean searchKeyPressed(int keyCode, int scanCode, int modifiers) {
        return isSearchFocused() && searchField.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) {
            return false;
        }
        layoutChrome();
        if (searchField != null && searchField.visible && searchField.isMouseOver(mx, my)) {
            if (button == 1) {
                searchField.setValue("");
                onSearchChanged("");
                searchField.setFocused(true);
                return true;
            }
            searchField.mouseClicked(mx, my, button);
            searchField.setFocused(true);
            return true;
        }
        if (utilitySource != null && utilitySource.mouseClickedUtilities(mx, my, button)) {
            return true;
        }
        setSearchFocused(false);
        if (craftingStatusBtn != null && craftingStatusBtn.visible
                && craftingStatusBtn.mouseClicked(mx, my, button)) {
            return true;
        }
        return false;
    }

    public boolean inChrome(double mx, double my) {
        if (!visible) {
            return false;
        }
        if (inSearchField(mx, my) || hoveredUtility(mx, my) != null) {
            return true;
        }
        return craftingStatusBtn != null && craftingStatusBtn.visible && craftingStatusBtn.isMouseOver(mx, my);
    }

    @Override
    public boolean scrollbarPressed(double mx, double my) {
        if (!visible || !inScrollbar(mx, my)) {
            return false;
        }
        boolean consumed = scrollbar.onMouseDown(new Point((int) mx, (int) my), 0);
        scrollbarDragging = consumed;
        return consumed;
    }

    @Override
    public boolean scrollbarDragged(double mx, double my) {
        if (!scrollbarDragging) {
            return false;
        }
        return scrollbar.onMouseDrag(new Point((int) mx, (int) my), 0);
    }

    @Override
    public void scrollbarReleased() {
        if (scrollbarDragging) {
            scrollbar.onMouseUp(Point.ZERO, 0);
        }
        scrollbarDragging = false;
    }

    @Override
    public boolean scrollbarDragging() {
        return scrollbarDragging;
    }

    public GridInventoryEntry entryAt(double mx, double my) {
        RepoSlot slot = repoSlotAt(mx, my);
        return slot == null ? null : slot.getEntry();
    }

    public RepoSlot repoSlotAt(double mx, double my) {
        if (!visible) {
            return null;
        }
        for (RepoSlot slot : repoSlots) {
            if (mx >= slotScreenX(slot) && mx < slotScreenX(slot) + 16
                    && my >= slotScreenY(slot) && my < slotScreenY(slot) + 16) {
                return slot;
            }
        }
        return null;
    }

    @Override
    public SortOrder getSortBy() {
        return configSrc.getSetting(Settings.SORT_BY);
    }

    @Override
    public SortDir getSortDir() {
        return configSrc.getSetting(Settings.SORT_DIRECTION);
    }

    @Override
    public ViewItems getSortDisplay() {
        return configSrc.getSetting(Settings.VIEW_MODE);
    }

    @Override
    public TypeFilter getTypeFilter() {
        return configSrc.getSetting(Settings.TYPE_FILTER);
    }

    private void onSearchChanged(String text) {
        repo.setSearchString(text == null ? "" : text);
        repo.updateView();
        updateScrollbar();
    }

    private void updateScrollbar() {
        int maxScroll = Math.max(0, totalRows() - rows);
        scrollbar.setRange(0, maxScroll, Math.max(1, rows / 6));
        scrollbar.setHeight(Math.max(1, rows * SLOT - 2));
        scrollbar.setPosition(new Point(trackLeft(), contentTop() + 1));
    }

    private int totalRows() {
        if (cols == 0) {
            return 0;
        }
        int total = (repo.size() + cols - 1) / cols;
        if (repo.hasPinnedRow()) {
            total++;
        }
        return total;
    }

    public void markFinishedCraftingPinsPrunable() {
        for (GridInventoryEntry entry : repo.getPinnedEntries()) {
            PinnedKeys.PinInfo info = PinnedKeys.getPinInfo(entry.getWhat());
            if (info != null && info.reason == PinnedKeys.PinReason.CRAFTING
                    && !PendingCraftingJobs.hasPendingJob(entry.getWhat())) {
                info.canPrune = true;
            }
        }
    }

    private boolean scrollerOutside() {
        return visible && rightmostInWindow;
    }

    private int railLeft() {
        if (scrollerOutside()) {
            return x + width - RAIL_OVERLAP + RAIL_SHIFT_X;
        }
        return trackLeft() - SCROLLBAR_INSET;
    }

    private int trackLeft() {
        if (scrollerOutside()) {
            return railLeft() + SCROLLBAR_INSET;
        }
        return contentLeft() + cols * SLOT + INSIDE_TRACK_GAP;
    }

    private int trackHeight() {
        return Math.max(1, rows * SLOT - 2);
    }

    private boolean inScroller(double mx, double my) {
        if (inScrollbar(mx, my)) {
            return true;
        }
        if (scrollerOutside()) {
            int left = railLeft() - 2;
            return mx >= left && mx < left + RAIL_SPRITE_WIDTH
                    && my >= y - 1 && my < y + height;
        }
        int left = trackLeft();
        int top = contentTop() + 1;
        int trackH = trackHeight();
        return mx >= left && mx < left + TRACK_WIDTH
                && my >= top && my < top + trackH;
    }

    private boolean inScrollbar(double mx, double my) {
        Rect2i bounds = scrollbar.getBounds();
        return mx >= bounds.getX() && mx < bounds.getX() + bounds.getWidth()
                && my >= bounds.getY() && my < bounds.getY() + bounds.getHeight();
    }

    private void layoutChrome() {
        boolean show = visible;
        if (searchField != null) {
            boolean external = AEConfig.instance().isUseExternalSearch();
            searchField.setVisible(show && !external);
            int searchX = contentLeft() + cols * SLOT - SEARCH_WIDTH;
            int searchY = y + TITLE_SEARCH_TOP;
            if (searchField.visible) {
                searchField.move(new Point(searchX, searchY));
            }
            if (utilitySource != null && utilitySource.hostUtilitiesOnMeList()) {
                utilitySource.layoutUtilitiesOnSearch(
                        searchX, searchY, CraftingTerminalPanel.SEARCH_BUTTON);
            }
        }
        if (craftingStatusBtn != null) {
            craftingStatusBtn.visible = show;
            craftingStatusBtn.setWidth(CRAFT_STATUS_SIZE);
            craftingStatusBtn.setHeight(CRAFT_STATUS_SIZE);
            craftingStatusBtn.setX(scrollerOutside()
                    ? railLeft() + RAIL_WIDTH - 24
                    : trackLeft() + TRACK_WIDTH - CRAFT_STATUS_SIZE);
            craftingStatusBtn.setY(y - CRAFT_STATUS_OVERHANG);
        }
    }

    private void hideChrome() {
        if (searchField != null) {
            searchField.setVisible(false);
            searchField.setFocused(false);
        }
        if (craftingStatusBtn != null) {
            craftingStatusBtn.visible = false;
        }
    }

    private void updateExternalSearch() {
        if (searchField == null) {
            return;
        }
        var config = AEConfig.instance();
        if (config.isUseExternalSearch()) {
            searchField.setVisible(false);
            String externalSearchText = ItemListMod.getSearchText();
            if (!Objects.equals(repo.getSearchString(), externalSearchText)) {
                onSearchChanged(externalSearchText);
            }
            return;
        }
        if (visible) {
            searchField.setVisible(true);
        }
        if (config.isSyncWithExternalSearch()) {
            if (searchField.isFocused()) {
                ItemListMod.setSearchText(searchField.getValue());
            } else if (ItemListMod.hasSearchFocus()) {
                String externalSearchText = ItemListMod.getSearchText();
                if (!Objects.equals(externalSearchText, searchField.getValue())) {
                    searchField.setValue(externalSearchText);
                }
            }
        }
    }

    private void drawTerminalRows(GuiGraphics g) {
        int gridLeft = contentLeft();
        int gridTop = contentTop();
        int nativeCols = Math.min(cols, NATIVE_SLOT_COLS);
        for (int row = 0; row < rows; row++) {
            int srcY = rowSrcY(row);
            int destY = gridTop + row * SLOT;
            int nativeWidth = cols <= NATIVE_SLOT_COLS ? nativeCols * SLOT : nativeCols * SLOT - 1;
            TERMINAL.src(7, srcY, nativeWidth, SLOT)
                    .dest(gridLeft, destY)
                    .blit(g);
            for (int col = nativeCols; col < cols; col++) {
                TERMINAL.src(7 + SLOT, srcY, SLOT, SLOT)
                        .dest(gridLeft + col * SLOT, destY)
                        .blit(g);
            }
            if (cols > NATIVE_SLOT_COLS) {
                g.vLine(gridLeft + cols * SLOT - 1, destY, destY + SLOT - 1, 0xFFF2F2F2);
            }
        }
    }

    /**
     * The pinned row's background strip, as AE2's {@code MEStorageScreen.drawBG} blits it:
     * {@code guis/terminal.png} at (0, 204), one row tall, over the row backgrounds. AE2 only ever
     * needs the native nine columns; wider terminals repeat the 162px art (18px periodic, so the
     * cells stay on the same grid as the row backgrounds) and close it with the same right rule
     * {@link #drawTerminalRows} draws.
     */
    private void drawPinnedRowStrip(GuiGraphics g) {
        if (!repo.hasPinnedRow() || cols <= 0 || rows <= 0) {
            return;
        }
        int gridLeft = contentLeft();
        int gridTop = contentTop();
        int width = cols * SLOT;
        for (int x = 0; x < width; x += PINNED_ROW_NATIVE_WIDTH) {
            int chunk = Math.min(PINNED_ROW_NATIVE_WIDTH, width - x);
            TERMINAL.copy()
                    .src(0, PINNED_ROW_SRC_Y, chunk, SLOT)
                    .dest(gridLeft + x, gridTop)
                    .blit(g);
        }
        if (cols > NATIVE_SLOT_COLS) {
            g.vLine(gridLeft + width - 1, gridTop, gridTop + SLOT - 1, 0xFFF2F2F2);
        }
    }

    /** Right-hand rail. Outside chrome unless a sibling occupies this leaf's right edge. */
    private void drawScrollerRail(GuiGraphics g) {
        if (!scrollerOutside() || !drawOutsideRail) {
            return;
        }
        com.lhy.mest.client.GuiSprites.verticalButtonsBackground(
                g,
                railLeft() - 2,
                joinedRailY,
                RAIL_SPRITE_WIDTH,
                joinedRailH);
    }

    private void drawScrollerTrack(GuiGraphics g) {
        com.lhy.mest.client.dock.Scrollbar.drawTerminalTrack(
                g,
                trackLeft(),
                contentTop() + 1,
                trackHeight());
    }

    private int rowSrcY(int row) {
        if (row == 0) {
            return 17;
        }
        if (row + 1 == rows) {
            return 53;
        }
        return 35;
    }

    public ITooltip hoveredCraftingStatusTooltip(int mouseX, int mouseY) {
        if (craftingStatusBtn != null && craftingStatusBtn.visible
                && craftingStatusBtn.isMouseOver(mouseX, mouseY)) {
            return craftingStatusBtn;
        }
        return null;
    }

    private AbstractWidget hoveredChrome(int mouseX, int mouseY) {
        if (searchField != null && searchField.visible && searchField.isMouseOver(mouseX, mouseY)) {
            return searchField;
        }
        if (craftingStatusBtn != null && craftingStatusBtn.visible
                && craftingStatusBtn.isMouseOver(mouseX, mouseY)) {
            return craftingStatusBtn;
        }
        return hoveredUtility(mouseX, mouseY);
    }

    private AbstractWidget hoveredUtility(double mx, double my) {
        return utilitySource == null ? null : utilitySource.hoveredUtility((int) mx, (int) my);
    }

    private int utilityBarWidth() {
        if (utilitySource == null || !utilitySource.hostUtilitiesOnMeList()) {
            return 0;
        }
        return utilitySource.utilityBarWidth(CraftingTerminalPanel.SEARCH_BUTTON);
    }
}