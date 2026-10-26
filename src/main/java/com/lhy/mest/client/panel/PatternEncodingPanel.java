package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.inventory.Slot;

import appeng.api.config.ActionItems;
import appeng.client.gui.Icon;

import com.lhy.mest.client.Ae2Icons;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.TabButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.menu.slot.AppEngSlot;
import appeng.parts.encoding.EncodingMode;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.client.dock.Scrollbar;
import com.lhy.mest.terminal.MESTMenu;

/**
 * Floating pattern encoder that mirrors AE2's {@code pattern_modes.png} layout, tab icons and
 * action widgets. Extra {@link EncodingMode} values and larger processing grids from addons are
 * picked up through {@link EncodingMode#values()} and the menu's slot arrays.
 *
 * <p>Mode tabs hang off the right of a standalone (or rightmost) window, matching the original
 * chrome. When stacked with an ME/pattern-access scroller well they share that
 * {@code vertical_buttons_bg} strip. When another module sits to the right, they tuck inside
 * the leaf so they do not paint over the sibling; the blank/encoded column then shifts left
 * just enough to stay clear.
 */
public class PatternEncodingPanel extends ModulePanel {
    private static final int MODE_W = 124;
    private static final int MODE_H = 66;
    private static final int SLOT = 18;
    private static final int TAB_W = 22;
    private static final int TAB_H = 22;
    private static final int TAB_OUTSIDE_OVERLAP = 3;
    private static final int RAIL_SPRITE_WIDTH = 21;
    private static final int RAIL_OVERLAP = 2;
    private static final int RAIL_SHIFT_X = 2;
    /** Vanilla gap between the encoded-pattern slot (ends at 165) and the tab column (173). */
    private static final int TAB_GAP = 8;
    private static final int PATTERN_SLOT_X = 139;
    private static final int ENCODED_SLOT_Y = 47;
    private static final int STONECUTTING_COLS = 4;
    private static final int STONECUTTING_ROWS = 3;
    private static final int SLOT_OUTLINE = 0xFFF2F2F2;
    private static final Blitter MODES = Blitter.texture("guis/pattern_modes.png", 256, 256);
    // AE2 15's pattern_modes.png: 16x18 recipe buttons beside the stonecutting background.
    private static final Blitter STONE_SLOT = MODES.copy().src(126, 141, 16, 18);
    private static final Blitter STONE_SLOT_SELECTED = MODES.copy().src(126, 159, 16, 18);
    private static final Blitter STONE_SLOT_HOVER = MODES.copy().src(126, 177, 16, 18);

    private final MESTMenu menu;
    private final Map<EncodingMode, TabButton> modeTabs = new EnumMap<>(EncodingMode.class);
    private final ActionButton encodeBtn;
    private final PatternEncodingExtras plusChrome;
    private final ActionButton craftingClearBtn;
    private final ActionButton processingClearBtn;
    private final ActionButton processingCycleBtn;
    private final ActionButton smithingClearBtn;
    private final ToggleButton craftingSubstitutions;
    private final ToggleButton craftingFluidSubstitutions;
    private final ToggleButton smithingSubstitutions;
    private final List<AbstractWidget> widgets = new ArrayList<>();

    private int processingScroll;
    private int stonecuttingScroll;
    private final Scrollbar scrollbar = new Scrollbar();
    private StonecutterRecipe hoveredStonecuttingRecipe;

    public PatternEncodingPanel(MESTMenu menu) {
        this.menu = menu;

        for (Slot slot : menu.getPatternCraftingSlots()) {
            registerSlot(slot);
        }
        registerSlot(menu.getPatternCraftOutputSlot());
        for (Slot slot : menu.getProcessingInputSlots()) {
            registerSlot(slot);
        }
        for (Slot slot : menu.getProcessingOutputSlots()) {
            registerSlot(slot);
        }
        registerSlot(menu.getStonecuttingInputSlot());
        registerSlot(menu.getSmithingTableTemplateSlot());
        registerSlot(menu.getSmithingTableBaseSlot());
        registerSlot(menu.getSmithingTableAdditionSlot());
        registerSlot(menu.getBlankPatternSlot());
        registerSlot(menu.getEncodedPatternSlot());

        for (EncodingMode mode : EncodingMode.values()) {
            var tab = new TabButton(iconForMode(mode), tooltipForMode(mode), btn -> menu.setPatternEncodingMode(mode));
            tab.setStyle(TabButton.Style.HORIZONTAL);
            tab.setWidth(TAB_W);
            tab.setHeight(TAB_H);
            modeTabs.put(mode, tab);
            widgets.add(tab);
        }

        encodeBtn = new ActionButton(ActionItems.ENCODE,
                () -> menu.encodePattern(net.minecraft.client.gui.screens.Screen.hasShiftDown()));
        widgets.add(encodeBtn);
        plusChrome = PatternEncodingExtras.create(menu);
        if (plusChrome != null) {
            widgets.addAll(plusChrome.widgets());
        }

        craftingClearBtn = halfAction(ActionItems.CLOSE, menu::clearPatternEncoding);
        processingClearBtn = halfAction(ActionItems.CLOSE, menu::clearPatternEncoding);
        processingCycleBtn = halfAction(ActionItems.CYCLE_PROCESSING_OUTPUT, menu::cycleProcessingOutput);
        smithingClearBtn = halfAction(ActionItems.CLOSE, menu::clearPatternEncoding);

        craftingSubstitutions = substitutionToggle(menu::setPatternSubstitute);
        craftingFluidSubstitutions = fluidSubstitutionToggle();
        smithingSubstitutions = substitutionToggle(menu::setPatternSubstitute);
    }

    private ActionButton halfAction(ActionItems action, Runnable onPress) {
        var button = new ActionButton(action, onPress);
        button.setHalfSize(true);
        button.setDisableBackground(true);
        widgets.add(button);
        return button;
    }

    private ToggleButton substitutionToggle(ToggleButton.Listener listener) {
        var button = new ToggleButton(
                Ae2Icons.S_SUBSTITUTION_ENABLED,
                Ae2Icons.S_SUBSTITUTION_DISABLED,
                listener);
        button.setHalfSize(true);
        button.setDisableBackground(true);
        button.setTooltipOn(List.of(
                ButtonToolTips.SubstitutionsOn.text(),
                ButtonToolTips.SubstitutionsDescEnabled.text()));
        button.setTooltipOff(List.of(
                ButtonToolTips.SubstitutionsOff.text(),
                ButtonToolTips.SubstitutionsDescDisabled.text()));
        widgets.add(button);
        return button;
    }

    private ToggleButton fluidSubstitutionToggle() {
        var button = new ToggleButton(
                Ae2Icons.S_FLUID_SUBSTITUTION_ENABLED,
                Ae2Icons.S_FLUID_SUBSTITUTION_DISABLED,
                menu::setPatternFluidSubstitute);
        button.setHalfSize(true);
        button.setDisableBackground(true);
        button.setTooltipOn(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescEnabled.text()));
        button.setTooltipOff(List.of(
                ButtonToolTips.FluidSubstitutions.text(),
                ButtonToolTips.FluidSubstitutionsDescDisabled.text()));
        widgets.add(button);
        return button;
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 3, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "pattern_encoding";
    }

    @Override
    public Component title() {
        return Component.translatable("gui.mesplicedterminal.pattern_encoding");
    }

    @Override
    public int defaultWidth() {
        return 2 * CONTENT_PADDING + MODE_W + 14 + SLOT;
    }

    @Override
    public int defaultHeight() {
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + MODE_H;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return TITLE_BAR_HEIGHT + MODE_H;
    }

    @Override
    public int outsideHitWidth() {
        return tabsOutside() ? TAB_W - TAB_OUTSIDE_OVERLAP : 0;
    }

    @Override
    public int preferredContentRightInset() {
        return visible && !tabsOutside() ? TAB_W : 0;
    }

    @Override
    public boolean hasJoinableOutsideRail() {
        return tabsOutside();
    }

    public void tick() {
        // Slot activity can change without x/y/mode changing (server sync of encoding mode,
        // first client activation). Always re-place so ghost slots do not stay at -9999.
        layoutSlots();
        updateWidgets();
    }

    @Override
    public void layoutSlots() {
        hideOwnedSlots();
        if (!visible) {
            for (AbstractWidget widget : widgets) {
                widget.visible = false;
            }
            return;
        }

        int bgX = contentLeft();
        int bgY = contentTop();
        int patternX = patternSlotScreenX();
        setSlot(menu.getBlankPatternSlot(), patternX, bgY);
        setSlot(menu.getEncodedPatternSlot(), patternX, bgY + ENCODED_SLOT_Y);

        EncodingMode mode = menu.getPatternEncodingMode();
        if (mode == EncodingMode.CRAFTING) {
            layoutCraftingSlots(bgX, bgY);
        } else if (mode == EncodingMode.PROCESSING) {
            layoutProcessingSlots(bgX, bgY);
        } else if (mode == EncodingMode.SMITHING_TABLE) {
            layoutSmithingSlots(bgX, bgY);
        } else if (mode == EncodingMode.STONECUTTING) {
            var input = layout().stonecuttingInput();
            setSlot(menu.getStonecuttingInputSlot(), bgX + input.x(), bgY + input.y());
        }
    }

    private void layoutCraftingSlots(int bgX, int bgY) {
        var grid = layout().craftingGrid();
        var slots = menu.getPatternCraftingSlots();
        for (int i = 0; i < slots.length; i++) {
            setSlot(slots[i], bgX + grid.x() + (i % 3) * SLOT, bgY + grid.y() + (i / 3) * SLOT);
        }
        var result = layout().craftingResult();
        setSlot(menu.getPatternCraftOutputSlot(), bgX + result.x(), bgY + result.y());
    }

    private void layoutProcessingSlots(int bgX, int bgY) {
        int maxScroll = maxProcessingScroll();
        if (processingScroll > maxScroll) {
            processingScroll = maxScroll;
        }
        var grid = layout().processingGrid();
        var inputs = menu.getProcessingInputSlots();
        for (int i = 0; i < inputs.length; i++) {
            int row = i / 3 - processingScroll;
            if (row >= 0 && row < 3) {
                setSlot(inputs[i], bgX + grid.x() + (i % 3) * SLOT, bgY + grid.y() + row * SLOT);
            }
        }
        var column = layout().processingOutputs();
        var outputs = menu.getProcessingOutputSlots();
        for (int i = 0; i < outputs.length; i++) {
            int row = i - processingScroll;
            if (row >= 0 && row < 3) {
                setSlot(outputs[i], bgX + column.x(), bgY + column.y() + row * SLOT);
            }
        }
    }

    private void layoutSmithingSlots(int bgX, int bgY) {
        var grid = layout().smithingGrid();
        setSlot(menu.getSmithingTableTemplateSlot(), bgX + grid.x(), bgY + grid.y());
        setSlot(menu.getSmithingTableBaseSlot(), bgX + grid.x() + SLOT, bgY + grid.y());
        setSlot(menu.getSmithingTableAdditionSlot(), bgX + grid.x() + 2 * SLOT, bgY + grid.y());
    }

    private void hideOwnedSlots() {
        for (Slot slot : ownedSlots()) {
            hideSlot(slot);
        }
    }

    private void setSlot(Slot slot, int slotX, int slotY) {
        if (slot instanceof AppEngSlot appEngSlot && !appEngSlot.isActive()) {
            return;
        }
        placeSlot(slot, slotX, slotY);
    }

    private void updateWidgets() {
        int bgX = contentLeft();
        int bgY = contentTop();
        EncodingMode current = menu.getPatternEncodingMode();
        int tabIndex = 0;
        for (EncodingMode mode : EncodingMode.values()) {
            TabButton tab = modeTabs.get(mode);
            tab.visible = visible;
            tab.setSelected(current == mode);
            tab.setX(tabX());
            tab.setY(y + tabIndex * (TAB_H - 1));
            tabIndex++;
        }

        encodeBtn.visible = visible;
        encodeBtn.setX(patternSlotScreenX());
        encodeBtn.setY(bgY + 20);

        boolean crafting = visible && current == EncodingMode.CRAFTING;
        boolean processing = visible && current == EncodingMode.PROCESSING;
        boolean smithing = visible && current == EncodingMode.SMITHING_TABLE;
        if (plusChrome != null) {
            plusChrome.layout(bgX, bgY, encodeBtn.getX(), encodeBtn.getY(), visible, processing);
        }

        placeHalf(craftingClearBtn, crafting, bgX + 62, bgY + 6);
        placeHalf(craftingSubstitutions, crafting, bgX + 72, bgY + 6);
        placeHalf(craftingFluidSubstitutions, crafting, bgX + 82, bgY + 6);
        craftingSubstitutions.setState(menu.isPatternSubstitute());
        craftingFluidSubstitutions.setState(menu.isPatternFluidSubstitute());

        placeHalf(processingClearBtn, processing, bgX + 71, bgY + 6);
        boolean showCycle = processing && menu.canCycleProcessingOutputs()
                && (plusChrome == null || !plusChrome.scaleButtonsVisible());
        placeHalf(processingCycleBtn, showCycle, bgX + 90, bgY + 6);

        placeHalf(smithingClearBtn, smithing, bgX + 6, bgY + 14);
        placeHalf(smithingSubstitutions, smithing, bgX + 16, bgY + 14);
        smithingSubstitutions.setState(menu.isPatternSubstitute());
    }

    private static void placeHalf(AbstractWidget widget, boolean show, int x, int y) {
        widget.visible = show;
        widget.setX(x);
        widget.setY(y);
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        updateWidgets();
        drawTabRail(g);
        int bgX = contentLeft();
        int bgY = contentTop();
        modeBackground(menu.getPatternEncodingMode()).dest(bgX, bgY).blit(g);
        highlightFluidSubstitutionSlots(g, mouseX, mouseY);

        if (menu.getPatternEncodingMode() == EncodingMode.STONECUTTING) {
            renderStonecuttingRecipes(g, font, mouseX, mouseY, bgX, bgY);
        } else if (menu.getPatternEncodingMode() == EncodingMode.SMITHING_TABLE) {
            renderSmithingResult(g, font, bgX, bgY);
        } else if (menu.getPatternEncodingMode() == EncodingMode.PROCESSING && maxProcessingScroll() > 0) {
            scrollbar.setScroll(processingScroll);
            var track = layout().processingScroll();
            scrollbar.render(g, bgX + track.x(), bgY + track.y(), 7, track.height(), maxProcessingScroll());
        }

        int patternX = patternSlotScreenX();
        drawTerminalPatternSlot(g, patternX, bgY);
        drawTerminalPatternSlot(g, patternX, bgY + ENCODED_SLOT_Y);

        for (AbstractWidget widget : widgets) {
            widget.render(g, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void renderForegroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (hoveredStonecuttingRecipe != null) {
            g.renderComponentTooltip(font, List.of(hoveredStonecuttingRecipe
                    .getResultItem(menu.getPlayer().level().registryAccess())
                    .getHoverName()), mouseX, mouseY);
        }
    }

    private void highlightFluidSubstitutionSlots(GuiGraphics g, int mouseX, int mouseY) {
        if (menu.getPatternEncodingMode() != EncodingMode.CRAFTING
                || !menu.isPatternFluidSubstitute()
                || !craftingFluidSubstitutions.visible
                || !craftingFluidSubstitutions.isMouseOver(mouseX, mouseY)) {
            return;
        }
        var slots = menu.getPatternCraftingSlots();
        for (int index : menu.patternSlotsSupportingFluidSubstitution) {
            if (index < 0 || index >= slots.length) {
                continue;
            }
            Slot slot = slots[index];
            if (slot.x > -1000 && slot.y > -1000) {
                g.fill(slotScreenX(slot), slotScreenY(slot), slotScreenX(slot) + 16, slotScreenY(slot) + 16, 0xFF7AC25F);
            }
        }
    }

    private void renderSmithingResult(GuiGraphics g, Font font, int bgX, int bgY) {
        ItemStack result = getSmithingResult();
        if (!result.isEmpty()) {
            var at = layout().smithingResult();
            g.renderItem(result, bgX + at.x(), bgY + at.y());
            g.renderItemDecorations(font, result, bgX + at.x(), bgY + at.y());
        }
    }

    private void renderStonecuttingRecipes(GuiGraphics g, Font font, int mouseX, int mouseY, int bgX, int bgY) {
        hoveredStonecuttingRecipe = null;
        var recipes = menu.getStonecuttingRecipes();
        var selected = menu.getStonecuttingRecipeId();
        stonecuttingScroll = Math.min(stonecuttingScroll, maxStonecuttingScroll());
        int start = stonecuttingScroll * STONECUTTING_COLS;
        int end = Math.min(recipes.size(), start + STONECUTTING_COLS * STONECUTTING_ROWS);
        for (int i = start; i < end; i++) {
            var recipe = recipes.get(i);
            int visibleIndex = i - start;
            int slotX = stonecuttingSlotX(bgX, visibleIndex);
            int slotY = stonecuttingSlotY(bgY, visibleIndex);
            boolean isSelected = selected != null && selected.equals(recipe.getId());
            boolean hover = contains(mouseX, mouseY, slotX, slotY, 16, 18);
            Blitter slotBg = isSelected ? STONE_SLOT_SELECTED : hover ? STONE_SLOT_HOVER : STONE_SLOT;
            slotBg.dest(slotX, slotY - 1).blit(g);
            ItemStack result = recipe.getResultItem(menu.getPlayer().level().registryAccess());
            g.renderItem(result, slotX, slotY);
            g.renderItemDecorations(font, result, slotX, slotY);
            if (hover) {
                hoveredStonecuttingRecipe = recipe;
            }
        }
        if (maxStonecuttingScroll() > 0) {
            scrollbar.setScroll(stonecuttingScroll);
            var track = layout().stonecuttingScroll();
            scrollbar.render(g, bgX + track.x(), bgY + track.y(), 7, track.height(), maxStonecuttingScroll());
        }
    }

    /**
     * AE2's pattern terminal bakes these recesses into {@code pattern.png}. Generated panel chrome
     * has none, so draw {@link Icon#SLOT_BACKGROUND} with a white rim inset 1px into the 18x18 well.
     */
    private static void drawTerminalPatternSlot(GuiGraphics g, int itemX, int itemY) {
        int px = itemX - 1;
        int py = itemY - 1;
        ModulePanel.drawSlot(g, px, py);
        int x0 = px + 1;
        int y0 = py + 1;
        int x1 = px + SLOT - 2;
        int y1 = py + SLOT - 2;
        g.hLine(x0, x1, y0, SLOT_OUTLINE);
        g.hLine(x0, x1, y1, SLOT_OUTLINE);
        g.vLine(x0, y0, y1, SLOT_OUTLINE);
        g.vLine(x1, y0, y1, SLOT_OUTLINE);
    }

    public ITooltip hoveredTooltip(int mouseX, int mouseY) {
        if (!visible) {
            return null;
        }
        for (AbstractWidget widget : widgets) {
            if (widget.visible && widget.isMouseOver(mouseX, mouseY) && widget instanceof ITooltip tooltip
                    && tooltip.isTooltipAreaVisible() && !tooltip.getTooltipMessage().isEmpty()) {
                return tooltip;
            }
        }
        return null;
    }

    public boolean mouseClicked(double mx, double my, int button) {
        if (!visible) {
            return false;
        }
        for (AbstractWidget widget : widgets) {
            if (widget.visible && widget.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        if (!contains(mx, my)) {
            return false;
        }
        if (button == 0 && menu.getPatternEncodingMode() == EncodingMode.STONECUTTING) {
            var recipe = stonecuttingRecipeAt(mx, my);
            if (recipe != null) {
                menu.setStonecuttingRecipeId(recipe.getId());
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.UI_STONECUTTER_SELECT_RECIPE, 1.0F));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (!visible || !contains(mx, my) || scrollY == 0) {
            return false;
        }
        if (menu.getPatternEncodingMode() == EncodingMode.PROCESSING && maxProcessingScroll() > 0) {
            processingScroll = Math.max(0, Math.min(maxProcessingScroll(),
                    processingScroll - (int) Math.signum(scrollY)));
            scrollbar.setScroll(processingScroll);
            layoutSlots();
            return true;
        }
        if (menu.getPatternEncodingMode() == EncodingMode.STONECUTTING && maxStonecuttingScroll() > 0) {
            stonecuttingScroll = Math.max(0,
                    Math.min(maxStonecuttingScroll(), stonecuttingScroll - (int) Math.signum(scrollY)));
            scrollbar.setScroll(stonecuttingScroll);
            return true;
        }
        return false;
    }

    @Override
    public boolean scrollbarPressed(double mx, double my) {
        if (!visible) {
            return false;
        }
        int bgX = contentLeft();
        int bgY = contentTop();
        if (menu.getPatternEncodingMode() == EncodingMode.PROCESSING && maxProcessingScroll() > 0) {
            var track = layout().processingScroll();
            if (!contains(mx, my, bgX + track.x(), bgY + track.y(), 7, track.height())) {
                return false;
            }
            scrollbar.setScroll(processingScroll);
            boolean consumed = scrollbar.mousePressed(mx, my, bgX + track.x(), bgY + track.y(), 7, track.height(),
                    3, maxProcessingScroll());
            if (consumed) {
                processingScroll = scrollbar.scroll();
                layoutSlots();
            }
            return consumed;
        }
        if (menu.getPatternEncodingMode() == EncodingMode.STONECUTTING && maxStonecuttingScroll() > 0) {
            var track = layout().stonecuttingScroll();
            if (!contains(mx, my, bgX + track.x(), bgY + track.y(), 7, track.height())) {
                return false;
            }
            scrollbar.setScroll(stonecuttingScroll);
            boolean consumed = scrollbar.mousePressed(mx, my, bgX + track.x(), bgY + track.y(), 7, track.height(),
                    STONECUTTING_ROWS, maxStonecuttingScroll());
            if (consumed) {
                stonecuttingScroll = scrollbar.scroll();
            }
            return consumed;
        }
        return false;
    }

    @Override
    public boolean scrollbarDragged(double mx, double my) {
        if (!scrollbar.isDragging()) {
            return false;
        }
        int bgY = contentTop();
        if (menu.getPatternEncodingMode() == EncodingMode.PROCESSING) {
            var track = layout().processingScroll();
            scrollbar.mouseDragged(my, bgY + track.y(), track.height(), maxProcessingScroll());
            processingScroll = scrollbar.scroll();
            layoutSlots();
            return true;
        }
        if (menu.getPatternEncodingMode() == EncodingMode.STONECUTTING) {
            var track = layout().stonecuttingScroll();
            scrollbar.mouseDragged(my, bgY + track.y(), track.height(), maxStonecuttingScroll());
            stonecuttingScroll = scrollbar.scroll();
            return true;
        }
        return false;
    }

    @Override
    public void scrollbarReleased() {
        scrollbar.mouseReleased();
    }

    @Override
    public boolean scrollbarDragging() {
        return scrollbar.isDragging();
    }

    private StonecutterRecipe stonecuttingRecipeAt(double mx, double my) {
        int bgX = contentLeft();
        int bgY = contentTop();
        var recipes = menu.getStonecuttingRecipes();
        int start = stonecuttingScroll * STONECUTTING_COLS;
        int end = Math.min(recipes.size(), start + STONECUTTING_COLS * STONECUTTING_ROWS);
        for (int i = start; i < end; i++) {
            int visibleIndex = i - start;
            int slotX = stonecuttingSlotX(bgX, visibleIndex);
            int slotY = stonecuttingSlotY(bgY, visibleIndex);
            if (contains(mx, my, slotX, slotY, 16, 18)) {
                return recipes.get(i);
            }
        }
        return null;
    }

    /** AE2 15's recipe grid: 16x18 buttons, the first item at the measured recipes point. */
    private static int stonecuttingSlotX(int bgX, int visibleIndex) {
        return bgX + layout().stonecuttingRecipes().x() + (visibleIndex % STONECUTTING_COLS) * 16;
    }

    private static int stonecuttingSlotY(int bgY, int visibleIndex) {
        return bgY + layout().stonecuttingRecipes().y() + (visibleIndex / STONECUTTING_COLS) * 18;
    }

    private static PatternModesLayout layout() {
        return PatternModesTexture.layout();
    }

    private int maxProcessingScroll() {
        return Math.max(0, menu.getProcessingInputSlots().length / 3 - 3);
    }

    private int maxStonecuttingScroll() {
        int rows = (menu.getStonecuttingRecipes().size() + STONECUTTING_COLS - 1) / STONECUTTING_COLS;
        return Math.max(0, rows - STONECUTTING_ROWS);
    }

    private ItemStack getSmithingResult() {
        var input = new SimpleContainer(
                menu.getSmithingTableTemplateSlot().getItem(),
                menu.getSmithingTableBaseSlot().getItem(),
                menu.getSmithingTableAdditionSlot().getItem());
        var level = menu.getPlayer().level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMITHING, input, level)
                .orElse(null);
        if (recipe == null) {
            return ItemStack.EMPTY;
        }
        return recipe.assemble(input, level.registryAccess());
    }

    /** AE2 15's encoding terminal tabs show the workstation item instead of AE2 19's tab icons. */
    private static ItemStack iconForMode(EncodingMode mode) {
        if (mode == EncodingMode.PROCESSING) {
            return new ItemStack(Items.FURNACE);
        }
        if (mode == EncodingMode.SMITHING_TABLE) {
            return new ItemStack(Items.SMITHING_TABLE);
        }
        if (mode == EncodingMode.STONECUTTING) {
            return new ItemStack(Items.STONECUTTER);
        }
        return new ItemStack(Items.CRAFTING_TABLE);
    }

    private static Component tooltipForMode(EncodingMode mode) {
        if (mode == EncodingMode.CRAFTING) {
            return GuiText.CraftingPattern.text();
        }
        if (mode == EncodingMode.PROCESSING) {
            return GuiText.ProcessingPattern.text();
        }
        if (mode == EncodingMode.SMITHING_TABLE) {
            return GuiText.SmithingTablePattern.text();
        }
        if (mode == EncodingMode.STONECUTTING) {
            return GuiText.StonecuttingPattern.text();
        }
        return Component.literal(mode.name());
    }

    private boolean tabsOutside() {
        return visible && rightmostInWindow;
    }

    /**
     * Same 9-slice well as {@link MEListPanel}'s outside scroller. Drawn only when this leaf is
     * the top of a joined rail cluster so the strip is one piece behind the tabs and any
     * scroller stacked below.
     */
    private void drawTabRail(GuiGraphics g) {
        if (!tabsOutside() || !drawOutsideRail || joinedRailH <= height) {
            return;
        }
        int railLeft = x + width - RAIL_OVERLAP + RAIL_SHIFT_X;
        com.lhy.mest.client.GuiSprites.verticalButtonsBackground(g, railLeft - 2, joinedRailY, RAIL_SPRITE_WIDTH, joinedRailH);
        g.flush();
    }

    private int tabX() {
        return tabsOutside() ? x + width - TAB_OUTSIDE_OVERLAP : x + width - TAB_W;
    }

    /**
     * Preferred vanilla encode-column X, shifted left when mode tabs occupy the right gutter so
     * they cannot cover the blank/encoded slots.
     */
    private int patternSlotScreenX() {
        int preferred = contentLeft() + PATTERN_SLOT_X;
        if (tabsOutside()) {
            return preferred;
        }
        int withGap = tabX() - TAB_GAP - SLOT;
        if (withGap >= contentLeft() + MODE_W) {
            return Math.min(preferred, withGap);
        }
        return Math.min(preferred, tabX() - SLOT);
    }

    private static Blitter modeBackground(EncodingMode mode) {
        if (mode == EncodingMode.PROCESSING) {
            return MODES.copy().src(PatternModesLayout.PROCESSING_SRC_X, PatternModesLayout.PROCESSING_SRC_Y,
                    MODE_W, MODE_H);
        }
        if (mode == EncodingMode.SMITHING_TABLE) {
            return MODES.copy().src(PatternModesLayout.SMITHING_SRC_X, PatternModesLayout.SMITHING_SRC_Y,
                    MODE_W, MODE_H);
        }
        if (mode == EncodingMode.STONECUTTING) {
            return MODES.copy().src(PatternModesLayout.STONECUTTING_SRC_X, PatternModesLayout.STONECUTTING_SRC_Y,
                    MODE_W, MODE_H);
        }
        return MODES.copy().src(PatternModesLayout.CRAFTING_SRC_X, PatternModesLayout.CRAFTING_SRC_Y,
                MODE_W, MODE_H);
    }

    private static boolean contains(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
