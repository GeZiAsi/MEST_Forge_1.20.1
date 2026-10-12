package com.lhy.mestdevtest;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import com.lhy.mest.client.MESTScreen;
import com.lhy.mest.client.dock.ModulePanel;

/** Draws a tooltip in ScreenEvent.Render.Post, the way JEI draws ingredient tooltips. */
final class DevTestOverlay {
    private static boolean registered;
    private static int x = -1;
    private static int y = -1;

    private DevTestOverlay() {
    }

    static void showTooltipOver(Minecraft mc, String panelId) {
        if (!registered) {
            registered = true;
            MinecraftForge.EVENT_BUS.addListener(DevTestOverlay::onRenderPost);
        }
        if (mc.screen instanceof MESTScreen screen) {
            ModulePanel panel = DevTestDock.panel(screen, panelId);
            if (panel != null) {
                x = panel.x + 10;
                y = panel.y + panel.height / 2;
                MestDevTest.log("post-render tooltip at {},{} over {}", x, y, DevTestDock.describe(screen, panelId));
            }
        }
    }

    static void hide() {
        x = -1;
    }

    private static void onRenderPost(ScreenEvent.Render.Post event) {
        if (x < 0 || !(event.getScreen() instanceof MESTScreen)) {
            return;
        }
        event.getGuiGraphics().renderComponentTooltip(Minecraft.getInstance().font, List.of(
                Component.literal("JEI-like tooltip drawn after the terminal"),
                Component.literal("It must cover every panel, rail and extra column under it"),
                Component.literal("-----------------------------------------------")), x, y);
    }
}
