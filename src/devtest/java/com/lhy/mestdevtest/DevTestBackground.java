package com.lhy.mestdevtest;

import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

import net.minecraft.client.Minecraft;

/**
 * Keeps the test client out of the way of whoever is using the computer: the window never keeps
 * the keyboard focus, and the game never grabs (and so never moves) the mouse cursor.
 */
final class DevTestBackground {
    private static final int WS_EX_NOACTIVATE = 0x08000000;

    private DevTestBackground() {
    }

    /** Once, as soon as the game loop runs: hand the focus back and stop the window taking it again. */
    static void install(Minecraft mc) {
        // Unfocused, the game would pause itself (and the integrated server with it).
        mc.options.pauseOnLostFocus = false;
        mc.setWindowActive(false);
        if (!Platform.isWindows()) {
            return;
        }
        try {
            var hwnd = new HWND(new Pointer(
                    org.lwjgl.glfw.GLFWNativeWin32.glfwGetWin32Window(mc.getWindow().getWindow())));
            var user32 = User32.INSTANCE;
            int style = user32.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
            user32.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, style | WS_EX_NOACTIVATE);
            if (hwnd.equals(user32.GetForegroundWindow())) {
                HWND previous = nextVisibleWindow(user32, hwnd);
                if (previous != null) {
                    user32.SetForegroundWindow(previous);
                }
            }
            MestDevTest.log("running in the background (window no longer takes focus)");
        } catch (Throwable t) {
            MestDevTest.LOGGER.warn("MEST-DEVTEST could not keep the window in the background", t);
        }
    }

    /** Every tick: the game only grabs the mouse while it believes its window is active. */
    static void tick(Minecraft mc) {
        if (mc.isWindowActive()) {
            mc.setWindowActive(false);
        }
    }

    /** The window below ours in z-order that a person was using: visible, titled, top level. */
    private static HWND nextVisibleWindow(User32 user32, HWND own) {
        HWND candidate = user32.GetWindow(own, new DWORD(WinUser.GW_HWNDNEXT));
        while (candidate != null) {
            if (user32.IsWindowVisible(candidate) && user32.GetWindowTextLength(candidate) > 0) {
                return candidate;
            }
            candidate = user32.GetWindow(candidate, new DWORD(WinUser.GW_HWNDNEXT));
        }
        return null;
    }
}
