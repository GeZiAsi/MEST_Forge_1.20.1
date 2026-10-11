package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import appeng.api.config.ActionItems;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ITooltip;
import appeng.menu.SlotSemantics;

import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.terminal.MESTMenu;

/**
 * Floating 3x3 crafting grid that reuses AE2's {@code crafting.png} artwork, arrow spacing and
 * half-size stash buttons so the module matches the stock crafting terminal.
 */
public class CraftingPanel extends ModulePanel {
    private static final int SLOT = 18;
    private static final int BG_W = 160;
    private static final int BG_H = 66;
    private static final int GRID_OFFSET_X = 18;
    private static final int GRID_OFFSET_Y = 7;
    private static final int RESULT_OFFSET_X = 126;
    private static final int RESULT_OFFSET_Y = GRID_OFFSET_Y + SLOT;
    private static final Blitter CRAFTING_BG = Blitter.texture("guis/crafting.png", 256, 256)
            .src(8, 86, BG_W, BG_H);

    private final List<Slot> gridSlots;
    private final List<Slot> resultSlots;
    private final ActionButton clearToNetwork;
    private final ActionButton clearToPlayer;
    private final List<AbstractWidget> widgets = new ArrayList<>();

    public CraftingPanel(MESTMenu menu) {
        this.gridSlots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
        this.resultSlots = menu.getSlots(SlotSemantics.CRAFTING_RESULT);
        for (Slot slot : gridSlots) {
            registerSlot(slot);
        }
        for (Slot slot : resultSlots) {
            registerSlot(slot);
        }

        clearToNetwork = new ActionButton(ActionItems.STASH, menu::clearCraftingGrid);
        clearToNetwork.setHalfSize(true);
        clearToNetwork.setDisableBackground(true);
        widgets.add(clearToNetwork);

        clearToPlayer = new ActionButton(ActionItems.STASH_TO_PLAYER_INV, menu::clearToPlayerInventory);
        clearToPlayer.setHalfSize(true);
        clearToPlayer.setDisableBackground(true);
        widgets.add(clearToPlayer);
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 1, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "crafting";
    }

    @Override
    public Component title() {
        return Component.translatable("gui.mesplicedterminal.crafting");
    }

    @Override
    public int defaultWidth() {
        return 2 * CONTENT_PADDING + BG_W;
    }

    @Override
    public int defaultHeight() {
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + BG_H;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return TITLE_BAR_HEIGHT + BG_H;
    }

    @Override
    public void layoutSlots() {
        if (!visible) {
            for (Slot slot : ownedSlots()) {
                hideSlot(slot);
            }
            for (AbstractWidget widget : widgets) {
                widget.visible = false;
            }
            return;
        }
        int left = contentLeft();
        int top = contentTop();
        for (int i = 0; i < gridSlots.size(); i++) {
            Slot slot = gridSlots.get(i);
            placeSlot(slot, left + GRID_OFFSET_X + (i % 3) * SLOT, top + GRID_OFFSET_Y + (i / 3) * SLOT);
        }
        int resultX = left + RESULT_OFFSET_X;
        int resultY = top + RESULT_OFFSET_Y;
        for (Slot slot : resultSlots) {
            placeSlot(slot, resultX, resultY);
        }
        updateWidgets();
    }

    private void updateWidgets() {
        int left = contentLeft();
        int top = contentTop();
        place(clearToNetwork, visible, left + 73, top + GRID_OFFSET_Y - 1);
        place(clearToPlayer, visible, left + 83, top + GRID_OFFSET_Y - 1);
    }

    private static void place(AbstractWidget widget, boolean show, int x, int y) {
        widget.visible = show;
        widget.setX(x);
        widget.setY(y);
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        updateWidgets();
        CRAFTING_BG.dest(contentLeft(), contentTop()).blit(g);
        for (AbstractWidget widget : widgets) {
            widget.render(g, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void renderForegroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (hosted) {
            return;
        }
        for (AbstractWidget widget : widgets) {
            if (widget.visible && widget.isMouseOver(mouseX, mouseY) && widget instanceof ITooltip tooltip
                    && !tooltip.getTooltipMessage().isEmpty()) {
                g.renderComponentTooltip(font, tooltip.getTooltipMessage(), mouseX, mouseY);
                return;
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || hosted) {
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