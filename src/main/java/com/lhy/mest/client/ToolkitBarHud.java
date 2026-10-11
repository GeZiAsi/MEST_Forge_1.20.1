package com.lhy.mest.client;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import com.lhy.mest.terminal.ToolkitBarState;
import com.lhy.mest.terminal.ToolkitBarState.Bar;

/**
 * Draws the toolkit quick bars beside the vanilla hotbar, using the same {@code widgets.png} hotbar art.
 *
 * <p>Left bar maps toolkit slots 0-8, right bar maps 9-17. Together with the vanilla hotbar they
 * form a 27-cell cycle: left → center → right. Extra bars always draw toolkit cells; the vanilla
 * hotbar is never replaced.
 *
 * <p>Geometry mirrors {@code Gui#renderHotbar}: the vanilla hotbar is 182px wide for nine 20px
 * cells with a 3px item inset, so the bar width must stay {@code slot * 9 + 2}. Drawing it any
 * wider stretches the cell borders away from the item positions. The 29px offhand well is only
 * reserved on the side vanilla actually draws it, which keeps both bars evenly spaced from the
 * hotbar when the offhand is empty.
 */
public final class ToolkitBarHud {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final int HOTBAR_TEXTURE_WIDTH = 182;
    private static final int SELECTION_TEXTURE_WIDTH = 24;
    /** Vanilla cell pitch and the item inset inside a cell. */
    public static final int SLOT = 20;
    public static final int ITEM_INSET = 3;
    public static final int BAR_SLOTS = ToolkitBarState.BAR_SLOTS;
    /** Smallest cell pitch we still consider usable before the bars would collide with the hotbar. */
    private static final int MIN_SLOT = 12;
    private static final int BAR_HEIGHT = 22;
    private static final int SELECTION_HEIGHT = 22;
    private static final int HOTBAR_HALF = 91;
    private static final int OFFHAND_WIDTH = 29;
    /** Gap between the hotbar frame and a toolkit bar, matching the selection sprite's overhang. */
    private static final int PADDING = 2;

    private ToolkitBarHud() {}

    public static void render(GuiGraphics g, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        if (!ToolkitBarState.isBarEnabled(player)) {
            return;
        }
        int guiWidth = g.guiWidth();
        int leftGap = offhandGap(player, true);
        int rightGap = offhandGap(player, false);
        int leftSlot = slotSize(guiWidth, leftGap);
        int rightSlot = slotSize(guiWidth, rightGap);
        int y = barY(g.guiHeight());
        Bar selectedBar = ToolkitBarState.getBar(player);
        int selectedSlot = ToolkitBarState.getSlot(player);

        drawBar(g, minecraft, player, 0, leftX(guiWidth, leftSlot, leftGap), y, leftSlot,
                selectedBar == Bar.LEFT, selectedSlot);
        drawBar(g, minecraft, player, BAR_SLOTS, rightX(guiWidth, rightSlot, rightGap), y, rightSlot,
                selectedBar == Bar.RIGHT, selectedSlot);
    }

    /**
     * Space vanilla reserves on the given side for the offhand well: 29px on the side the offhand
     * sprite is drawn, and nothing when the player has no offhand item.
     */
    public static int offhandGap(LocalPlayer player, boolean leftSide) {
        if (player == null || player.getOffhandItem().isEmpty()) {
            return 0;
        }
        // Vanilla draws the offhand well opposite the main arm.
        boolean offhandOnLeft = player.getMainArm() == HumanoidArm.RIGHT;
        return offhandOnLeft == leftSide ? OFFHAND_WIDTH : 0;
    }

    /** Cell pitch that still fits beside the hotbar, capped at the vanilla 20px. */
    public static int slotSize(int guiWidth, int gap) {
        int usable = guiWidth / 2 - HOTBAR_HALF - gap - PADDING;
        int slot = (usable - 2) / BAR_SLOTS;
        return Mth.clamp(slot, MIN_SLOT, SLOT);
    }

    /** Matches the vanilla sprite's aspect: nine {@code slot}-wide cells plus the 1px end borders. */
    public static int barWidth(int slot) {
        return slot * BAR_SLOTS + 2;
    }

    public static int leftX(int guiWidth, int slot, int gap) {
        int x = guiWidth / 2 - HOTBAR_HALF - gap - PADDING - barWidth(slot);
        return Math.max(0, x);
    }

    public static int rightX(int guiWidth, int slot, int gap) {
        int x = guiWidth / 2 + HOTBAR_HALF + gap + PADDING;
        return Math.min(Math.max(0, guiWidth - barWidth(slot)), x);
    }

    public static int barY(int guiHeight) {
        return guiHeight - BAR_HEIGHT;
    }

    /**
     * Toolkit inventory index for a click, or -1. Left bar yields 0-8, right bar yields 9-17.
     *
     * <p>Pure geometry: the caller supplies the offhand gaps so this stays independent of the
     * client singleton and can be regression-tested.
     */
    public static int slotAt(int guiWidth, int guiHeight, int leftGap, int rightGap,
            double mouseX, double mouseY) {
        int leftSlot = slotSize(guiWidth, leftGap);
        int rightSlot = slotSize(guiWidth, rightGap);
        int y = barY(guiHeight);
        int left = hitBar(leftX(guiWidth, leftSlot, leftGap), y, leftSlot, mouseX, mouseY, 0);
        return left >= 0
                ? left
                : hitBar(rightX(guiWidth, rightSlot, rightGap), y, rightSlot, mouseX, mouseY, BAR_SLOTS);
    }

    private static int hitBar(int barX, int barY, int slot, double mouseX, double mouseY, int firstSlot) {
        int width = barWidth(slot);
        if (mouseY < barY || mouseY >= barY + BAR_HEIGHT
                || mouseX < barX || mouseX >= barX + width) {
            return -1;
        }
        int offset = (int) ((mouseX - barX - ITEM_INSET) / slot);
        return offset < 0 || offset >= BAR_SLOTS ? -1 : firstSlot + offset;
    }

    private static void drawBar(
            GuiGraphics g,
            Minecraft minecraft,
            Player player,
            int firstSlot,
            int x,
            int y,
            int slot,
            boolean selected,
            int selectedSlot) {
        drawBarFrame(g, x, y, slot, selected, selectedSlot);
        for (int i = 0; i < BAR_SLOTS; i++) {
            ItemStack stack = ToolkitBarState.stackAt(player, firstSlot + i);
            int itemX = x + ITEM_INSET + i * slot;
            int itemY = y + ITEM_INSET;
            if (stack.isEmpty()) {
                ItemStack memory = ToolkitBarState.memoryAt(player, firstSlot + i);
                if (memory.isEmpty()) {
                    continue;
                }
                g.setColor(1.0F, 1.0F, 1.0F, 0.38F);
                g.renderItem(memory, itemX, itemY);
                g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
                continue;
            }
            g.renderItem(stack, itemX, itemY);
            g.renderItemDecorations(minecraft.font, stack, itemX, itemY);
        }
    }

    private static void drawBarFrame(GuiGraphics g, int x, int y, int slot, boolean selected, int selectedSlot) {
        RenderSystem.enableBlend();
        g.pose().pushPose();
        try {
            g.pose().translate(0.0F, 0.0F, -90.0F);
            g.blit(WIDGETS, x, y, barWidth(slot), BAR_HEIGHT,
                    0, 0, HOTBAR_TEXTURE_WIDTH, BAR_HEIGHT, 256, 256);
            if (selected) {
                g.blit(WIDGETS, x - 1 + selectedSlot * slot, y - 1, slot + 4, SELECTION_HEIGHT,
                        0, 22, SELECTION_TEXTURE_WIDTH, SELECTION_HEIGHT, 256, 256);
            }
        } finally {
            g.pose().popPose();
            RenderSystem.disableBlend();
        }
    }
}
