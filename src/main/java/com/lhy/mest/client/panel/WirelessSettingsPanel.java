package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import com.lhy.mest.client.dock.model.ModuleDefaults;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import com.lhy.mest.network.MestPackets;

import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;

import de.mari_023.ae2wtlib.AE2wtlibTags;
import de.mari_023.ae2wtlib.TextConstants;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

import com.lhy.mest.client.dock.DockManager;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.network.MestTerminalSettingsPacket;
import com.lhy.mest.network.ToolkitBarTogglePacket;
import com.lhy.mest.network.ToolkitQuickMovePacket;
import com.lhy.mest.terminal.MESTMenu;
import com.lhy.mest.terminal.ToolkitBarState;

/**
 * Wireless-terminal settings as a dock module. Saves through {@link MestTerminalSettingsPacket}
 * (only the settings flags) and {@link MagnetHandler}.
 */
public class WirelessSettingsPanel extends ModulePanel {
    private static final int ROW = 16;
    private static final int HEADER = 12;
    private static final int CHECK_H = 14;
    private static final int MIN_W = 176;

    private final MESTMenu menu;
    private final ScreenStyle style;
    private final DockManager dock;
    private final AECheckbox pickBlock;
    private final AECheckbox craftIfMissing;
    private final AECheckbox restock;
    private final AECheckbox pullItems;
    private final AECheckbox magnet;
    private final AECheckbox pickupToME;
    private final AECheckbox toolkitBar;
    private final AECheckbox toolkitQuickMove;
    private final List<AbstractWidget> widgets = new ArrayList<>();

    public WirelessSettingsPanel(MESTMenu menu, ScreenStyle style, DockManager dock) {
        this.menu = menu;
        this.style = style;
        this.dock = dock;
        pickBlock = checkbox(TextConstants.PICK_BLOCK);
        craftIfMissing = checkbox(TextConstants.CRAFT_IF_MISSING);
        restock = checkbox(TextConstants.RESTOCK);
        pullItems = checkbox(Component.translatable("gui.mesplicedterminal.wireless_settings.pull_items"));
        magnet = checkbox(TextConstants.MAGNET);
        pickupToME = checkbox(TextConstants.PICKUP_TO_ME);
        toolkitBar = checkbox(
                Component.translatable("gui.mesplicedterminal.wireless_settings.toolkit_bar"));
        toolkitQuickMove = checkbox(
                Component.translatable("gui.mesplicedterminal.wireless_settings.toolkit_quick_move"));
        pickBlock.setChangeListener(this::onPickBlockChanged);
        craftIfMissing.setChangeListener(this::save);
        restock.setChangeListener(this::save);
        pullItems.setChangeListener(this::savePullItems);
        magnet.setChangeListener(this::save);
        pickupToME.setChangeListener(this::save);
        toolkitBar.setChangeListener(this::saveToolkitBar);
        toolkitQuickMove.setChangeListener(this::saveToolkitQuickMove);
        widgets.add(pickBlock);
        widgets.add(craftIfMissing);
        widgets.add(restock);
        widgets.add(magnet);
        widgets.add(pickupToME);
        widgets.add(pullItems);
        widgets.add(toolkitBar);
        widgets.add(toolkitQuickMove);
        reloadFromStack();
    }

    private AECheckbox checkbox(Component label) {
        return new AECheckbox(0, 0, MIN_W - 2 * CONTENT_PADDING, CHECK_H, style, label);
    }

    @Override
    public ModuleDefaults defaults() {
        return new ModuleDefaults(false, true, false, false);
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 5, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "wireless_settings";
    }

    @Override
    public Component title() {
        return TextConstants.TERMINAL_SETTINGS;
    }

    @Override
    public int defaultWidth() {
        return MIN_W;
    }

    @Override
    public int defaultHeight() {
        return minHeight();
    }

    @Override
    public int minWidth() {
        return MIN_W;
    }

    @Override
    public int minHeight() {
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + neededContentHeight();
    }

    @Override
    public boolean expandsVertically() {
        return true;
    }

    private static int neededContentHeight() {
        return ROW * 3 + HEADER + ROW * 2 + HEADER + ROW * 3 + 4;
    }

    public void reloadFromStack() {
        ItemStack stack = menu.getMestHost().getItemStack();
        CompoundTag tag = stack.getOrCreateTag();
        pickBlock.setSelected(tag.getBoolean(AE2wtlibTags.PICK_BLOCK));
        craftIfMissing.setSelected(tag.getBoolean(AE2wtlibTags.CRAFT_IF_MISSING));
        craftIfMissing.active = pickBlock.isSelected();
        restock.setSelected(tag.getBoolean(AE2wtlibTags.RESTOCK));
        pullItems.setSelected(dock.pullItemsRecipeButton());
        MagnetMode mode = MagnetMode.fromByte(tag.getByte(AE2wtlibTags.MAGNET_SETTINGS));
        magnet.setSelected(mode.magnet());
        pickupToME.setSelected(mode.pickupToME());
        boolean hasCard = MagnetHandler.getMagnetMode(stack) != MagnetMode.NO_CARD
                && MagnetHandler.getMagnetMode(stack) != MagnetMode.INVALID;
        magnet.active = hasCard;
        pickupToME.active = hasCard;
        toolkitBar.setSelected(ToolkitBarState.isBarToggleOn(menu.getPlayer()));
        toolkitQuickMove.setSelected(ToolkitBarState.isQuickMoveEnabled(menu.getPlayer()));
    }

    private void onPickBlockChanged() {
        craftIfMissing.active = pickBlock.isSelected();
        save();
    }

    private void savePullItems() {
        dock.setPullItemsRecipeButton(pullItems.isSelected());
    }

    private void saveToolkitBar() {
        ToolkitBarState.setBarEnabled(menu.getPlayer(), toolkitBar.isSelected());
        MestPackets.sendToServer(new ToolkitBarTogglePacket(toolkitBar.isSelected()));
    }

    private void saveToolkitQuickMove() {
        ToolkitBarState.setQuickMove(menu.getPlayer(), toolkitQuickMove.isSelected());
        MestPackets.sendToServer(new ToolkitQuickMovePacket(toolkitQuickMove.isSelected()));
    }

    private void save() {
        ItemStack stack = menu.getMestHost().getItemStack();
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(AE2wtlibTags.PICK_BLOCK, pickBlock.isSelected());
        tag.putBoolean(AE2wtlibTags.CRAFT_IF_MISSING, craftIfMissing.isSelected());
        tag.putBoolean(AE2wtlibTags.RESTOCK, restock.isSelected());
        MagnetMode mode = MagnetMode.fromByte(tag.getByte(AE2wtlibTags.MAGNET_SETTINGS));
        MagnetMode next = mode.set(magnet.isSelected(), pickupToME.isSelected());
        MagnetHandler.saveMagnetMode(stack, next);
        MestPackets.sendToServer(new MestTerminalSettingsPacket(
                pickBlock.isSelected(),
                craftIfMissing.isSelected(),
                restock.isSelected(),
                next.getId()));
    }

    @Override
    public void layoutSlots() {
        boolean show = visible;
        int left = contentLeft();
        int top = contentTop();
        int width = Math.max(80, contentWidth());
        int y = top;
        place(pickBlock, show, left, y, width);
        y += ROW;
        place(craftIfMissing, show, left, y, width);
        y += ROW;
        place(restock, show, left, y, width);
        y += ROW + HEADER;
        place(magnet, show, left, y, width);
        y += ROW;
        place(pickupToME, show, left, y, width);
        y += ROW + HEADER;
        place(pullItems, show, left, y, width);
        y += ROW;
        place(toolkitBar, show, left, y, width);
        y += ROW;
        place(toolkitQuickMove, show, left, y, width);
    }

    private static void place(AECheckbox box, boolean show, int x, int y, int width) {
        box.visible = show;
        box.setX(x);
        box.setY(y);
        box.setWidth(width);
        box.setHeight(CHECK_H);
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        layoutSlots();
        int color = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB();
        int left = contentLeft();
        int top = contentTop();
        g.drawString(font, Component.translatable("gui.mesplicedterminal.wireless_settings.magnet"),
                left, top + ROW * 3 + 1, color, false);
        g.drawString(font, Component.translatable("gui.mesplicedterminal.wireless_settings.mest"),
                left, top + ROW * 3 + HEADER + ROW * 2 + 1, color, false);
        for (AbstractWidget widget : widgets) {
            widget.render(g, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) {
            return false;
        }
        for (AbstractWidget widget : widgets) {
            if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }
}
