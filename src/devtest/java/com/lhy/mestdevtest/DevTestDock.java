package com.lhy.mestdevtest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;

import com.lhy.mest.client.MESTScreen;
import com.lhy.mest.client.dock.DockManager;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.client.dock.model.DockRect;

/** Drives the terminal's dock with real screen mouse events, like a player dragging windows. */
final class DevTestDock {
    private DevTestDock() {
    }

    static DockManager dock(MESTScreen screen) {
        try {
            Field field = MESTScreen.class.getDeclaredField("dock");
            field.setAccessible(true);
            return (DockManager) field.get(screen);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static ModulePanel panel(MESTScreen screen, String id) {
        for (ModulePanel panel : dock(screen).panels()) {
            if (panel != null && id.equals(panel.id())) {
                return panel;
            }
        }
        return null;
    }

    static String describe(MESTScreen screen, String id) {
        DockManager dock = dock(screen);
        ModulePanel panel = panel(screen, id);
        if (panel == null) {
            return id + "=<none>";
        }
        return id + "@" + panel.x + "," + panel.y + " " + panel.width + "x" + panel.height
                + (dock.isEffectivelyVisible(panel) ? "" : " hidden")
                + (dock.isFloatingWindow(panel) ? " floating" : " anchored")
                + (dock.isJoinedWindow(panel) ? " joined" : "");
    }

    /** Presses at {@code from}, drags in small steps to {@code to} and releases there. */
    static void drag(MESTScreen screen, double fromX, double fromY, double toX, double toY) {
        screen.mouseClicked(fromX, fromY, 0);
        int steps = 12;
        double lastX = fromX;
        double lastY = fromY;
        for (int i = 1; i <= steps; i++) {
            double x = fromX + (toX - fromX) * i / steps;
            double y = fromY + (toY - fromY) * i / steps;
            screen.mouseDragged(x, y, 0, x - lastX, y - lastY);
            lastX = x;
            lastY = y;
        }
        screen.mouseReleased(toX, toY, 0);
    }

    /** Shows a module as its own floating window. */
    static void showFloating(Minecraft mc, String id) {
        if (!(mc.screen instanceof MESTScreen screen)) {
            return;
        }
        DockManager dock = dock(screen);
        ModulePanel panel = panel(screen, id);
        if (panel == null) {
            MestDevTest.log("FAIL splice {}: no such module", id);
            return;
        }
        if (!dock.isEffectivelyVisible(panel)) {
            invoke(screen, "runHotkey", id);
        }
        if (!dock.isFloatingWindow(panel) && dock.canToggleFloating(panel)) {
            dock.setWindowFloating(panel, true);
        }
        MestDevTest.log("splice {} before: {}", id, describe(screen, id));
    }

    /** Drags the module's title bar onto the left edge of the ME list. */
    static void spliceOntoMeList(Minecraft mc, String id) {
        if (!(mc.screen instanceof MESTScreen screen)) {
            return;
        }
        ModulePanel panel = panel(screen, id);
        ModulePanel meList = panel(screen, "me_list");
        if (panel == null || meList == null) {
            return;
        }
        // Grab the title bar where nothing (the pinned network-tool panel) covers it.
        double fromY = panel.y + 6;
        double fromX = panel.x + Math.min(30, panel.width / 3.0);
        for (int x = panel.x + 4; x < panel.x + panel.width - 24; x += 4) {
            if (dock(screen).topLeafAt(x, fromY) == panel && panel.inTitleBar(x, fromY)) {
                fromX = x;
                break;
            }
        }
        double toX = meList.x + 2;
        double toY = meList.y + meList.height / 2.0;
        drag(screen, fromX, fromY, toX, toY);
        boolean joined = dock(screen).isJoinedWindow(panel) && dock(screen).isJoinedWindow(meList)
                && !dock(screen).isFloatingWindow(panel);
        MestDevTest.log("{} splice {} onto me_list: {}", joined ? "PASS" : "FAIL", id, describe(screen, id));
    }

    /** Drags the anchored window's bottom-right resize corner by (dx, dy). */
    static void resizeAnchored(Minecraft mc, int dx, int dy) {
        if (!(mc.screen instanceof MESTScreen screen)) {
            return;
        }
        DockRect before = dock(screen).anchoredGroupBounds();
        double x = before.right() - 2;
        double y = before.bottom() - 2;
        drag(screen, x, y, x + dx, y + dy);
        DockRect after = dock(screen).anchoredGroupBounds();
        MestDevTest.log("resize anchored {} -> {}", before, after);
    }

    static void resetLayout(Minecraft mc) {
        if (mc.screen instanceof MESTScreen screen) {
            dock(screen).resetLayout();
            invoke(screen, "attachMeSideBar");
            invoke(screen, "attachExtraSlotColumns");
        }
    }

    static Object invoke(MESTScreen screen, String name, Object... args) {
        for (Method method : MESTScreen.class.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == args.length) {
                try {
                    method.setAccessible(true);
                    return method.invoke(screen, args);
                } catch (ReflectiveOperationException e) {
                    MestDevTest.LOGGER.error("MEST-DEVTEST FAIL invoke {}", name, e);
                    return null;
                }
            }
        }
        MestDevTest.log("FAIL no method {}", name);
        return null;
    }
}
