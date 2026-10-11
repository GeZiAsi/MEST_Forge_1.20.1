package com.lhy.mest.client;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.InputEvent;
import com.lhy.mest.network.MestPackets;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.network.ToolkitBarDropPacket;
import com.lhy.mest.network.ToolkitBarSelectPacket;
import com.lhy.mest.terminal.ToolkitBarState;
import com.lhy.mest.terminal.ToolkitBarState.Bar;

/**
 * HUD input for the 27-cell toolkit cycle: left 0-8, vanilla hotbar 9-17, right 18-26.
 *
 * <p>Scroll and left/right keys move the page around that ring. Number keys stay vanilla so they
 * pick a slot inside the current page. Shift+scroll is left alone so held tools can cycle modes.
 * Clicking an extra-bar cell selects it.
 *
 * <p>Selecting an extra bar makes {@code Inventory.getSelected} report that toolkit cell. Vanilla
 * attack, mining and use stay on the vanilla input path; the real hotbar contents never move.
 */
@EventBusSubscriber(modid = MESplicedterminal.MODID, value = Dist.CLIENT)
public final class ToolkitBarInput {
    private ToolkitBarInput() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!hudActive(minecraft, player)) {
            if (player != null && ToolkitBarState.isToolkitSelected(player)
                    && !ToolkitBarState.isBarEnabled(player)) {
                applySelection(player, Bar.CENTER, player.getInventory().selected);
            }
            return;
        }
        while (MestKeybindings.TOOLKIT_BAR_LEFT.consumeClick()) {
            cycleBar(player, -1);
        }
        while (MestKeybindings.TOOLKIT_BAR_RIGHT.consumeClick()) {
            cycleBar(player, 1);
        }
        if (ToolkitBarState.isToolkitSelected(player)) {
            while (minecraft.options.keyDrop.consumeClick()) {
                MestPackets.sendToServer(
                        new ToolkitBarDropPacket(net.minecraft.client.gui.screens.Screen.hasControlDown()));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (event.isCanceled()
                || player != null && player.isShiftKeyDown()
                || !hudActive(minecraft, player)
                || player.isSpectator()) {
            return;
        }
        int delta = (int) Math.signum(event.getScrollDelta());
        if (delta == 0) {
            return;
        }
        applyIndex(player, ToolkitBarState.cycleIndex(ToolkitBarState.visibleIndex(player), delta));
        event.setCanceled(true);
    }

    /**
     * Left-click on a quick-bar cell selects it instead of attacking. Clicks elsewhere keep the
     * vanilla attack path so the resolved toolkit item is used normally.
     */
    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!hudActive(minecraft, player) || event.getAction() != GLFW.GLFW_PRESS
                || event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        int slot = slotUnderCursor(minecraft);
        if (slot < 0) {
            return;
        }
        applySelection(player, slot < ToolkitBarState.BAR_SLOTS ? Bar.LEFT : Bar.RIGHT,
                slot % ToolkitBarState.BAR_SLOTS);
        event.setCanceled(true);
    }

    private static int slotUnderCursor(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        double scaleX = (double) minecraft.getWindow().getGuiScaledWidth()
                / Math.max(1, minecraft.getWindow().getScreenWidth());
        double scaleY = (double) minecraft.getWindow().getGuiScaledHeight()
                / Math.max(1, minecraft.getWindow().getScreenHeight());
        int guiWidth = minecraft.getWindow().getGuiScaledWidth();
        return ToolkitBarHud.slotAt(
                guiWidth,
                minecraft.getWindow().getGuiScaledHeight(),
                ToolkitBarHud.offhandGap(player, true),
                ToolkitBarHud.offhandGap(player, false),
                minecraft.mouseHandler.xpos() * scaleX,
                minecraft.mouseHandler.ypos() * scaleY);
    }

    private static void cycleBar(LocalPlayer player, int direction) {
        Bar current = ToolkitBarState.getBar(player);
        int index = current.ordinal();
        Bar next = Bar.values()[Math.floorMod(index + direction, Bar.values().length)];
        applySelection(player, next, ToolkitBarState.getSlot(player));
    }

    private static void applyIndex(LocalPlayer player, int index) {
        applySelection(player, ToolkitBarState.barOf(index), ToolkitBarState.slotOf(index));
    }

    public static void applySelection(LocalPlayer player, Bar bar, int slot) {
        ToolkitBarState.setSelection(player, bar, slot);
        MestPackets.sendToServer(new ToolkitBarSelectPacket(ToolkitBarState.toIndex(bar, slot)));
    }

    private static boolean hudActive(Minecraft minecraft, LocalPlayer player) {
        return player != null
                && !player.isSpectator()
                && minecraft.screen == null
                && !minecraft.options.hideGui
                && ToolkitBarState.isBarEnabled(player);
    }
}
