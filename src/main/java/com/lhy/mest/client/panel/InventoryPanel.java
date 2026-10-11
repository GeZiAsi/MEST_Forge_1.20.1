package com.lhy.mest.client.panel;

import appeng.client.gui.Icon;

import com.lhy.mest.client.Ae2Icons;
import java.util.List;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

import appeng.menu.SlotSemantics;

import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.terminal.MESTMenu;

/**
 * Floating panel that hosts the player's main inventory (9x3) and hotbar (9x1). It re-homes the menu's
 * existing player-inventory slots into its content area; resizing is fixed to the slot grid size.
 */
public class InventoryPanel extends ModulePanel {
    private static final int SLOT = 18;
    private static final int COLS = 9;

    private final List<Slot> inventorySlots;
    private final List<Slot> hotbarSlots;

    public InventoryPanel(MESTMenu menu) {
        this.inventorySlots = menu.getSlots(SlotSemantics.PLAYER_INVENTORY);
        this.hotbarSlots = menu.getSlots(SlotSemantics.PLAYER_HOTBAR);
        for (Slot s : inventorySlots) {
            registerSlot(s);
        }
        for (Slot s : hotbarSlots) {
            registerSlot(s);
        }
    }

    @Override
    public Icon icon() {
        return Ae2Icons.S_STORAGE;
    }

    @Override
    public String id() {
        return "inventory";
    }

    @Override
    public Component title() {
        return Component.translatable("container.inventory");
    }

    @Override
    public int defaultWidth() {
        return 2 * CONTENT_PADDING + COLS * SLOT;
    }

    @Override
    public int defaultHeight() {
        // 3 inventory rows + 1 hotbar row + a small gap.
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + 4 * SLOT + 4;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return TITLE_BAR_HEIGHT + 4 * SLOT + 4;
    }

    @Override
    protected int titleTextOffsetX() {
        return 1;
    }

    @Override
    public void layoutSlots() {
        if (!visible) {
            // Move owned slots off-screen so a hidden panel cannot be interacted with.
            for (Slot s : ownedSlots()) {
                hideSlot(s);
            }
            return;
        }
        int left = contentLeft() + 1;
        int top = contentTop();
        for (int i = 0; i < inventorySlots.size(); i++) {
            Slot s = inventorySlots.get(i);
            placeSlot(s, left + (i % COLS) * SLOT, top + (i / COLS) * SLOT);
        }
        int hotbarTop = top + 3 * SLOT + 4;
        for (int i = 0; i < hotbarSlots.size(); i++) {
            Slot s = hotbarSlots.get(i);
            placeSlot(s, left + (i % COLS) * SLOT, hotbarTop);
        }
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        drawSlotCells(g, inventorySlots);
        drawSlotCells(g, hotbarSlots);
        if (!inventorySlots.isEmpty()) {
            Slot first = inventorySlots.get(0);
            drawSlotGroupBorder(g, slotScreenX(first) - 1, slotScreenY(first) - 1, COLS, 3);
        }
        if (!hotbarSlots.isEmpty()) {
            Slot first = hotbarSlots.get(0);
            drawSlotGroupBorder(g, slotScreenX(first) - 1, slotScreenY(first) - 1, COLS, 1);
        }
    }

    private void drawSlotCells(GuiGraphics g, List<Slot> slots) {
        for (Slot s : slots) {
            ModulePanel.drawSlot(g, slotScreenX(s) - 1, slotScreenY(s) - 1);
        }
    }

    private static void drawSlotGroupBorder(GuiGraphics g, int px, int py, int cols, int rows) {
        int x1 = px + cols * SLOT;
        int y1 = py + rows * SLOT;
        g.hLine(px, x1 - 1, py, 0xFFF2F2F2);
        g.hLine(px, x1 - 1, y1 - 1, 0xFFF2F2F2);
        g.vLine(px, py, y1 - 1, 0xFFF2F2F2);
        g.vLine(x1 - 1, py, y1 - 1, 0xFFF2F2F2);
    }
}
