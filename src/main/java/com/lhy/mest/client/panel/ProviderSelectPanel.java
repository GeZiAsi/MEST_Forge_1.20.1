package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import com.lhy.mest.client.dock.model.ModuleDefaults;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import com.lhy.mest.network.MestPackets;

import appeng.client.Point;
import appeng.client.gui.Icon;

import com.lhy.mest.client.Ae2Icons;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.Scrollbar;
import appeng.core.localization.GuiText;

import com.lhy.mest.compat.plus.PlusUploadShim;
import com.lhy.mest.compat.plus.PlusUploadShim.RecipeTypeMapping;
import com.lhy.mest.client.PinyinSearch;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.compat.plus.PlusPickerPrefs;
import com.lhy.mest.network.ProviderPickerListPacket;

/**
 * Compact Plus provider picker as a dock module. Uses Plus packets, mapping APIs, and the same
 * pinned-providers JSON — no mixin or reflection.
 */
public class ProviderSelectPanel extends ModulePanel {
    private static final int SEARCH_WIDTH = 58;
    private static final int SEARCH_HEIGHT = 10;
    private static final int ROW = 12;
    private static final int FOOTER = 14;
    private static final int BUTTON = 12;
    private static final int TRACK_WIDTH = 5;
    private static final int TRACK_INNER = 3;
    private static final int TRACK_GAP = 2;
    private static final int TRACK_SHIFT_X = 2;
    private static final int GUTTER = TRACK_GAP + TRACK_WIDTH + TRACK_SHIFT_X;
    private static final int TRACK_BORDER = 0xFFF2F2F2;
    private static final int TRACK_FILL = 0xFF9A9FB4;
    private static final int ROW_FILL = 0xFF9A9FB4;
    private static final int ROW_ALT = 0xFF8F94A8;
    private static final int ROW_HOVER = 0x669CD3FF;
    private static final int PINNED_MARK = 0xFFE8C547;
    private static final Pattern NATURAL = Pattern.compile("(\\D*)(\\d*)");

    private final ScreenStyle style;
    private final Scrollbar scrollbar = new Scrollbar(Scrollbar.SMALL);
    private final List<RawRow> raw = new ArrayList<>();
    private final List<GroupRow> visibleRows = new ArrayList<>();
    private final CompactToggle autoButton;
    private final CompactToggle processingButton;
    private final CompactToggle mappingButton;
    private final CompactToggle addMappingButton;
    private AETextField searchField;
    private AETextField mappingField;
    private String search = "";
    private boolean mappingMode;
    private boolean scrollbarDragging;
    private int scrollRows;
    private GroupRow hovered;
    private String editingMappingKey;

    public ProviderSelectPanel(ScreenStyle style) {
        this.style = style;
        this.scrollbar.setCaptureMouseWheel(false);
        this.autoButton = new CompactToggle(
                btn -> PlusPickerPrefs.toggleAutoUploadUniqueMatch(),
                6,
                () -> List.of(
                        Component.translatable("gui.mesplicedterminal.provider_picker.auto"),
                        Component.translatable(PlusPickerPrefs.autoUploadUniqueMatch()
                                ? "gui.mesplicedterminal.pattern_action.enabled"
                                : "gui.mesplicedterminal.provider_picker.auto.off")));
        this.processingButton = new CompactToggle(
                btn -> PlusPickerPrefs.toggleProcessingButtons(),
                7,
                () -> List.of(
                        Component.translatable("gui.mesplicedterminal.provider_picker.processing"),
                        Component.translatable(PlusPickerPrefs.showProcessingButtons()
                                ? "gui.mesplicedterminal.pattern_action.enabled"
                                : "gui.mesplicedterminal.provider_picker.processing.off")));
        this.mappingButton = new CompactToggle(
                btn -> {
                    mappingMode = !mappingMode;
                    if (!mappingMode) {
                        editingMappingKey = null;
                    }
                    rebuildVisible();
                    layoutSlots();
                },
                8,
                () -> List.of(Component.translatable("gui.mesplicedterminal.provider_picker.mapping")));
        this.addMappingButton = new CompactToggle(
                btn -> addMappingFromFields(),
                Icon.ARROW_RIGHT,
                () -> List.of(Component.translatable("extendedae_plus.screen.add_mapping")));
    }

    @Override
    public ModuleDefaults defaults() {
        return new ModuleDefaults(false, false, false, false);
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 6, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "provider_select";
    }

    @Override
    public Component title() {
        return Component.translatable("gui.mesplicedterminal.provider_select");
    }

    @Override
    public int defaultWidth() {
        return 148;
    }

    @Override
    public int defaultHeight() {
        return 118;
    }

    @Override
    public int minWidth() {
        return 128;
    }

    @Override
    public int minHeight() {
        return 80;
    }

    @Override
    public boolean expandsVertically() {
        return true;
    }

    @Override
    public boolean fillsContentArea() {
        return true;
    }

    public boolean applyList(ProviderPickerListPacket packet) {
        raw.clear();
        int size = Math.min(packet.ids().size(), Math.min(packet.names().size(), packet.emptySlots().size()));
        for (int i = 0; i < size; i++) {
            raw.add(new RawRow(packet.ids().get(i), packet.names().get(i), packet.emptySlots().get(i)));
        }
        if (packet.applyPreset()) {
            String recent = PlusUploadShim.consumeLastProviderSearchKey();
            if (recent != null && !recent.isBlank()) {
                search = PlusUploadShim.resolveProviderSearchKey(recent);
                if (search == null) {
                    search = recent;
                }
                if (searchField != null) {
                    searchField.setValue(search);
                }
            }
            rebuildVisible();
            boolean uploaded = tryAutoUpload();
            layoutSlots();
            return uploaded;
        }
        rebuildVisible();
        layoutSlots();
        return false;
    }

    @Override
    public void layoutSlots() {
        for (Slot slot : ownedSlots()) {
            hideSlot(slot);
        }
        ensureFields();
        layoutChrome();
        layoutScrollbar();
    }

    @Override
    public boolean inTitleBarControls(double mx, double my) {
        return super.inTitleBarControls(mx, my)
                || (searchField != null && searchField.visible && searchField.isMouseOver(mx, my));
    }

    @Override
    protected int titleRightInset() {
        return Math.max(4, chromeButtonsReserve()) + SEARCH_WIDTH + 4 + (contentChromeVisible() ? 28 : 0);
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        hovered = rowAt(mouseX, mouseY);
        int listTop = listTop();
        int listLeft = contentLeft();
        int listW = listWidth();
        int rows = visibleRowCount();
        for (int i = 0; i < rows; i++) {
            int index = scrollRows + i;
            if (index >= visibleRows.size()) {
                break;
            }
            GroupRow row = visibleRows.get(index);
            int y = listTop + i * ROW;
            boolean hover = row == hovered;
            g.fill(listLeft, y, listLeft + listW, y + ROW - 1, hover ? ROW_HOVER : (i % 2 == 0 ? ROW_FILL : ROW_ALT));
            int textX = listLeft + 2;
            if (row.pinned()) {
                g.fill(textX, y + 4, textX + 3, y + 8, PINNED_MARK);
                textX += 5;
            }
            String count = row.count() > 1 ? " x" + row.count() : "";
            String slots = String.valueOf(row.emptySlots());
            int slotW = font.width(slots);
            String name = font.plainSubstrByWidth(row.name() + count, Math.max(8, listW - (textX - listLeft) - slotW - 6));
            g.drawString(font, name, textX, y + 2, 0xFF2A2A2A, false);
            g.drawString(font, slots, listLeft + listW - slotW - 2, y + 2, 0xFF3A3A3A, false);
        }
        if (visibleRows.isEmpty()) {
            g.drawString(
                    font,
                    Component.translatable(mappingMode
                            ? "gui.mesplicedterminal.provider_picker.mapping_empty"
                            : "gui.mesplicedterminal.provider_picker.empty"),
                    listLeft + 2,
                    listTop + 2,
                    0xFF4A4A4A,
                    false);
        }
        autoButton.paint(g, mouseX, mouseY, partialTicks);
        processingButton.paint(g, mouseX, mouseY, partialTicks);
        mappingButton.paint(g, mouseX, mouseY, partialTicks);
        addMappingButton.paint(g, mouseX, mouseY, partialTicks);
        if (searchField != null && searchField.visible) {
            searchField.render(g, mouseX, mouseY, partialTicks);
        }
        if (mappingField != null && mappingField.visible) {
            mappingField.render(g, mouseX, mouseY, partialTicks);
        }
        drawTrack(g, trackDrawLeft(), listTop, Math.max(1, rows * ROW));
        scrollbar.drawForegroundLayer(g, new Rect2i(0, 0, 0, 0), new Point(mouseX, mouseY));
    }

    public ITooltip hoveredTooltip(int mouseX, int mouseY) {
        if (!visible) {
            return null;
        }
        for (CompactToggle button : List.of(autoButton, processingButton, mappingButton, addMappingButton)) {
            if (button.visible && button.isMouseOver(mouseX, mouseY)) {
                return button;
            }
        }
        return rowAt(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) {
            return false;
        }
        layoutChrome();
        if (autoButton.mouseClicked(mx, my, button)
                || processingButton.mouseClicked(mx, my, button)
                || mappingButton.mouseClicked(mx, my, button)
                || addMappingButton.mouseClicked(mx, my, button)) {
            return true;
        }
        if (searchField != null && searchField.visible && searchField.isMouseOver(mx, my) && button == 1) {
            searchField.setValue("");
            search = "";
            rebuildVisible();
            layoutScrollbar();
            return true;
        }
        if (searchField != null && searchField.visible && searchField.mouseClicked(mx, my, button)) {
            setSearchFocused(true);
            return true;
        }
        if (mappingField != null && mappingField.visible && mappingField.mouseClicked(mx, my, button)) {
            mappingField.setFocused(true);
            if (Minecraft.getInstance().screen instanceof com.lhy.mest.client.MESTScreen screen) {
                screen.setFocused(mappingField);
            }
            return true;
        }
        if (searchField != null) {
            searchField.setFocused(false);
        }
        if (mappingField != null) {
            mappingField.setFocused(false);
        }
        GroupRow row = rowAt(mx, my);
        if (row == null) {
            return my >= y + TITLE_BAR_HEIGHT;
        }
        if (mappingMode) {
            if (button == 1) {
                fillMappingEditor(row);
                return true;
            }
            if (button == 0) {
                PlusUploadShim.removeRecipeTypeMapping(row.mappingKey());
                if (row.mappingKey().equals(editingMappingKey)) {
                    editingMappingKey = null;
                }
                rebuildVisible();
            }
            return true;
        }
        if (button == 1) {
            PlusPickerPrefs.togglePinned(row.name());
            rebuildVisible();
            return true;
        }
        if (button == 0) {
            PlusUploadShim.uploadToProvider(row.id(), true, row.name());
            if (Minecraft.getInstance().screen instanceof com.lhy.mest.client.MESTScreen screen) {
                screen.dismissProviderPicker(false);
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!visible || scrollY == 0 || (!inList(mx, my) && !inScroller(mx, my))) {
            return false;
        }
        scrollbar.setCurrentScroll(scrollbar.getCurrentScroll() - (int) Math.signum(scrollY));
        scrollRows = scrollbar.getCurrentScroll();
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
        return consumed;
    }

    @Override
    public boolean scrollbarDragged(double mx, double my) {
        if (!scrollbarDragging) {
            return false;
        }
        boolean consumed = scrollbar.onMouseDrag(new Point((int) mx, (int) my), 0);
        scrollRows = scrollbar.getCurrentScroll();
        return consumed;
    }

    @Override
    public void scrollbarReleased() {
        if (scrollbarDragging) {
            scrollbar.onMouseUp(new Point(0, 0), 0);
        }
        scrollbarDragging = false;
    }

    public AETextField searchField() {
        return searchField;
    }

    public AETextField mappingField() {
        return mappingField;
    }

    public void rebindSearchField(com.lhy.mest.client.MESTScreen screen) {
        if (searchField != null) {
            screen.attachPatternSearchField(searchField);
        }
        if (mappingField != null) {
            screen.attachPatternSearchField(mappingField);
        }
    }

    public boolean isSearchFocused() {
        return searchField != null && searchField.isFocused()
                || mappingField != null && mappingField.isFocused();
    }

    public void setSearchFocused(boolean focused) {
        if (searchField == null) {
            return;
        }
        searchField.setFocused(focused);
        if (Minecraft.getInstance().screen instanceof com.lhy.mest.client.MESTScreen screen) {
            if (focused) {
                screen.setFocused(searchField);
            } else if (screen.getFocused() == searchField || screen.getFocused() == mappingField) {
                screen.setFocused(null);
                if (mappingField != null) {
                    mappingField.setFocused(false);
                }
            }
        }
    }

    public boolean searchCharTyped(char character, int modifiers) {
        if (mappingField != null && mappingField.isFocused()) {
            return mappingField.charTyped(character, modifiers);
        }
        return isSearchFocused() && searchField != null && searchField.charTyped(character, modifiers);
    }

    public boolean searchKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (mappingField != null && mappingField.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                addMappingFromFields();
                return true;
            }
            return mappingField.keyPressed(keyCode, scanCode, modifiers);
        }
        return searchField != null && searchField.isFocused()
                && searchField.keyPressed(keyCode, scanCode, modifiers);
    }

    private void fillMappingEditor(GroupRow row) {
        editingMappingKey = row.mappingKey();
        search = row.mappingKey() == null ? "" : row.mappingKey();
        if (searchField != null) {
            searchField.setValue(search);
        }
        if (mappingField != null) {
            mappingField.setValue(row.mappingValue() == null ? "" : row.mappingValue());
        }
    }

    private void addMappingFromFields() {
        String key = search == null ? "" : search.trim();
        String value = mappingField != null ? mappingField.getValue().trim() : "";
        var player = Minecraft.getInstance().player;
        if (key.isEmpty()) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("extendedae_plus.message.mapping.search_required"));
            }
            return;
        }
        if (value.isEmpty()) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("extendedae_plus.message.mapping.cn_required"));
            }
            return;
        }
        if (editingMappingKey != null && !editingMappingKey.isBlank() && !editingMappingKey.equals(key)) {
            PlusUploadShim.removeRecipeTypeMapping(editingMappingKey);
        }
        if (!PlusUploadShim.addOrUpdateRecipeTypeMapping(key, value)) {
            if (player != null) {
                player.sendSystemMessage(Component.translatable("extendedae_plus.message.mapping.add_fail"));
            }
            return;
        }
        if (player != null) {
            player.sendSystemMessage(Component.translatable("extendedae_plus.message.mapping.add_success", key, value));
        }
        boolean stayInMapping = mappingMode && editingMappingKey != null;
        editingMappingKey = null;
        search = value;
        if (searchField != null) {
            searchField.setValue(value);
        }
        if (mappingField != null) {
            mappingField.setValue("");
        }
        mappingMode = stayInMapping;
        rebuildVisible();
        layoutSlots();
    }

    private boolean tryAutoUpload() {
        if (!PlusPickerPrefs.autoUploadUniqueMatch() || mappingMode) {
            return false;
        }
        rebuildVisible();
        if (visibleRows.size() != 1 || search == null || search.isBlank()) {
            return false;
        }
        GroupRow only = visibleRows.get(0);
        PlusUploadShim.uploadToProvider(only.id(), true, only.name());
        return true;
    }

    private void rebuildVisible() {
        visibleRows.clear();
        if (mappingMode) {
            for (RecipeTypeMapping mapping : PlusUploadShim.getRecipeTypeMappings()) {
                String label = mapping.key() + " → " + mapping.value();
                if (!nameMatches(label, search)) {
                    continue;
                }
                visibleRows.add(new GroupRow(-1L, label, 0, 1, false, mapping.key(), mapping.value()));
            }
            visibleRows.sort(Comparator.comparing(GroupRow::name, ProviderSelectPanel::naturalCompare));
            return;
        }
        LinkedHashMap<String, GroupRow> groups = new LinkedHashMap<>();
        for (RawRow row : raw) {
            String name = row.name.getString();
            if (!nameMatches(name, search)) {
                continue;
            }
            boolean pinned = PlusPickerPrefs.isPinned(name);
            GroupRow existing = groups.get(name);
            if (existing == null) {
                groups.put(name, new GroupRow(row.id, name, row.emptySlots, 1, pinned, "", ""));
            } else {
                long bestId = row.emptySlots > existing.emptySlots ? row.id : existing.id;
                groups.put(name, new GroupRow(
                        bestId, name, existing.emptySlots + row.emptySlots, existing.count + 1, pinned, "", ""));
            }
        }
        visibleRows.addAll(groups.values());
        visibleRows.sort(Comparator
                .comparing(GroupRow::pinned).reversed()
                .thenComparing(GroupRow::name, ProviderSelectPanel::naturalCompare));
    }

    private void ensureFields() {
        Font font = Minecraft.getInstance().font;
        if (searchField == null) {
            searchField = new AETextField(style, font, 0, 0, SEARCH_WIDTH, SEARCH_HEIGHT);
            searchField.setBordered(false);
            searchField.setMaxLength(64);
            searchField.setResponder(value -> {
                search = value == null ? "" : value;
                rebuildVisible();
                layoutScrollbar();
            });
            searchField.setPlaceholder(GuiText.SearchPlaceholder.text());
            if (Minecraft.getInstance().screen instanceof com.lhy.mest.client.MESTScreen screen) {
                screen.attachPatternSearchField(searchField);
            }
        }
        if (mappingField == null) {
            mappingField = new AETextField(style, font, 0, 0, 48, SEARCH_HEIGHT);
            mappingField.setBordered(false);
            mappingField.setMaxLength(32);
            mappingField.setPlaceholder(Component.translatable("gui.mesplicedterminal.provider_picker.alias"));
            if (Minecraft.getInstance().screen instanceof com.lhy.mest.client.MESTScreen screen) {
                screen.attachPatternSearchField(mappingField);
            }
        }
    }

    private void layoutChrome() {
        boolean show = visible;
        boolean titleSearch = show && drawsTitleBar();
        int searchX = chromeButtonsLeft() - 4 - SEARCH_WIDTH;
        if (searchField != null) {
            searchField.setVisible(titleSearch);
            if (titleSearch) {
                searchField.resize(SEARCH_WIDTH, SEARCH_HEIGHT);
                searchField.move(new Point(searchX, y + 4));
            }
        }
        int footerY = footerTop();
        int bx = contentLeft();
        placeToggle(autoButton, bx, footerY, show);
        placeToggle(processingButton, bx + BUTTON + 1, footerY, show);
        placeToggle(mappingButton, bx + 2 * (BUTTON + 1), footerY, show);
        placeToggle(addMappingButton, bx + 3 * (BUTTON + 1), footerY, show);
        if (mappingField != null) {
            int fieldX = bx + 4 * (BUTTON + 1) + 1;
            int fieldW = Math.max(24, contentLeft() + contentWidth() - fieldX);
            mappingField.setVisible(show);
            if (show) {
                mappingField.resize(fieldW, SEARCH_HEIGHT);
                mappingField.move(new Point(fieldX, footerY + 1));
            }
        }
    }

    private static void placeToggle(CompactToggle button, int x, int y, boolean show) {
        button.visible = show;
        button.active = show;
        button.setX(x);
        button.setY(y);
    }

    private void layoutScrollbar() {
        int rows = visibleRowCount();
        int max = Math.max(0, visibleRows.size() - rows);
        scrollbar.setRange(0, max, 1);
        scrollbar.setHeight(Math.max(1, rows * ROW - 2));
        scrollbar.setPosition(new Point(trackDrawLeft() - 1, listTop()));
        scrollbar.setCurrentScroll(Math.min(scrollRows, max));
        scrollRows = scrollbar.getCurrentScroll();
    }

    private int listTop() {
        return contentTop();
    }

    private int listWidth() {
        return Math.max(1, contentWidth() - GUTTER);
    }

    private int footerTop() {
        return y + height - contentBottomPadSafe() - FOOTER;
    }

    private int contentBottomPadSafe() {
        return splicedWindow != null && y + height < splicedWindow.bottom() ? 0 : CONTENT_PADDING;
    }

    private int visibleRowCount() {
        return Math.max(1, (footerTop() - listTop()) / ROW);
    }

    private int trackLeft() {
        return contentLeft() + listWidth() + TRACK_GAP;
    }

    private int trackDrawLeft() {
        return trackLeft() + TRACK_SHIFT_X;
    }

    private boolean inList(double mx, double my) {
        int left = contentLeft();
        return mx >= left && mx < left + listWidth() && my >= listTop() && my < footerTop();
    }

    private boolean inScroller(double mx, double my) {
        int x0 = trackDrawLeft();
        return mx >= x0 && mx < x0 + TRACK_WIDTH && my >= listTop() && my < footerTop();
    }

    private boolean inScrollbar(double mx, double my) {
        return inScroller(mx, my);
    }

    private GroupRow rowAt(double mx, double my) {
        if (!inList(mx, my)) {
            return null;
        }
        int index = scrollRows + (int) ((my - listTop()) / ROW);
        if (index < 0 || index >= visibleRows.size()) {
            return null;
        }
        return visibleRows.get(index);
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
        g.fill(x + 1, y + 1, x + 1 + TRACK_INNER, y1, TRACK_FILL);
    }

    private static boolean nameMatches(String name, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String key = PlusUploadShim.resolveProviderSearchKey(query.trim());
        if (key == null || key.isBlank()) {
            key = query.trim();
        }
        return PinyinSearch.contains(name, key) || name.toLowerCase(Locale.ROOT).contains(key.toLowerCase(Locale.ROOT));
    }

    private static int naturalCompare(String a, String b) {
        Matcher m1 = NATURAL.matcher(a.toLowerCase(Locale.ROOT));
        Matcher m2 = NATURAL.matcher(b.toLowerCase(Locale.ROOT));
        while (m1.find() && m2.find()) {
            int cmp = m1.group(1).compareTo(m2.group(1));
            if (cmp != 0) {
                return cmp;
            }
            String n1 = m1.group(2);
            String n2 = m2.group(2);
            if (n1.isEmpty() && n2.isEmpty()) {
                continue;
            }
            int v1 = n1.isEmpty() ? 0 : Integer.parseInt(n1);
            int v2 = n2.isEmpty() ? 0 : Integer.parseInt(n2);
            if (v1 != v2) {
                return Integer.compare(v1, v2);
            }
        }
        return a.length() - b.length();
    }

    private record RawRow(long id, Component name, int emptySlots) {
    }

    private record GroupRow(
            long id, String name, int emptySlots, int count, boolean pinned, String mappingKey, String mappingValue)
            implements ITooltip {
        @Override
        public List<Component> getTooltipMessage() {
            if (!mappingKey.isEmpty()) {
                return List.of(
                        Component.literal(name),
                        Component.translatable("gui.mesplicedterminal.provider_picker.edit_mapping")
                                .withStyle(ChatFormatting.GRAY),
                        Component.translatable("gui.mesplicedterminal.provider_picker.remove_mapping")
                                .withStyle(ChatFormatting.DARK_GRAY));
            }
            return List.of(
                    Component.literal(name),
                    Component.translatable("gui.mesplicedterminal.provider_picker.slots", emptySlots)
                            .withStyle(ChatFormatting.GRAY),
                    Component.translatable("gui.mesplicedterminal.provider_picker.hint")
                            .withStyle(ChatFormatting.DARK_GRAY));
        }

        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(0, 0, 0, 0);
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return true;
        }
    }

    private static final class CompactToggle extends Button implements ITooltip {
        private final Icon icon;
        private final int atlasCol;
        private final java.util.function.Supplier<List<Component>> tooltip;

        private CompactToggle(OnPress onPress, Icon icon, java.util.function.Supplier<List<Component>> tooltip) {
            super(0, 0, BUTTON, BUTTON, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.icon = icon;
            this.atlasCol = -1;
            this.tooltip = tooltip;
        }

        private CompactToggle(OnPress onPress, int atlasCol, java.util.function.Supplier<List<Component>> tooltip) {
            super(0, 0, BUTTON, BUTTON, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.icon = Ae2Icons.COG;
            this.atlasCol = atlasCol;
            this.tooltip = tooltip;
        }

        private void paint(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderWidget(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            Blitter background = com.lhy.mest.client.MestButtonSprites.terminalButton(isHovered(), isFocused());
            background.dest(getX(), getY() + yOffset, BUTTON, BUTTON).blit(graphics);
            if (atlasCol >= 0) {
                com.lhy.mest.client.MestGuiIcons.blit(
                        graphics, atlasCol, 1, getX(), getY() + yOffset, BUTTON, BUTTON);
            } else {
                icon.getBlitter().dest(getX(), getY() + yOffset, BUTTON, BUTTON).blit(graphics);
            }
        }

        @Override
        public List<Component> getTooltipMessage() {
            return tooltip.get();
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
