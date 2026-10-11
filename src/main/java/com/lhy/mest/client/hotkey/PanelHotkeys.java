package com.lhy.mest.client.hotkey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.client.event.InputEvent;

import appeng.client.Hotkeys;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.HotkeyPacket;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.terminal.MestTerminal;

/**
 * Client-local panel hotkeys. Inside the terminal the screen asks {@link #match}; in the world a
 * binding that is not terminal-only sends AE2's own open-terminal hotkey packet, so the server runs
 * exactly the checks of the regular hotkey, and the requested panel is shown once the screen opens.
 */
@EventBusSubscriber(modid = MESplicedterminal.MODID, value = Dist.CLIENT)
public final class PanelHotkeys {
    private static final String FILE = "hotkeys.json";
    private static final long PENDING_TIMEOUT_MS = 5000;

    private static PanelHotkeyBindings bindings;
    @Nullable
    private static String pendingModuleId;
    private static long pendingSince;

    private PanelHotkeys() {
    }

    public static PanelHotkey get(String moduleId) {
        return bindings().get(moduleId);
    }

    public static void set(String moduleId, PanelHotkey hotkey) {
        bindings().set(moduleId, hotkey);
        save();
    }

    @Nullable
    public static String match(int key, int modifiers) {
        return bindings().match(key, modifiers, false).orElse(null);
    }

    /** The panel a world hotkey asked for, if the terminal opened soon enough after it. */
    @Nullable
    public static String takePendingOpen() {
        String moduleId = pendingModuleId;
        pendingModuleId = null;
        if (moduleId == null || Util.getMillis() - pendingSince > PENDING_TIMEOUT_MS) {
            return null;
        }
        return moduleId;
    }

    public static Component describe(PanelHotkey hotkey) {
        if (!hotkey.isBound()) {
            return Component.translatable("gui.mesplicedterminal.hotkeys.unbound");
        }
        MutableComponent text = Component.empty();
        if ((hotkey.modifiers() & PanelHotkey.CTRL) != 0) {
            text.append("Ctrl + ");
        }
        if ((hotkey.modifiers() & PanelHotkey.SHIFT) != 0) {
            text.append("Shift + ");
        }
        if ((hotkey.modifiers() & PanelHotkey.ALT) != 0) {
            text.append("Alt + ");
        }
        return text.append(InputConstants.Type.KEYSYM.getOrCreate(hotkey.key()).getDisplayName());
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null || minecraft.player == null
                || minecraft.getOverlay() != null) {
            return;
        }
        bindings().match(event.getKey(), event.getModifiers(), true).ifPresent(moduleId -> {
            var hotkey = Hotkeys.getHotkeyMapping(MestTerminal.HOTKEY_NAME);
            if (hotkey == null) {
                return;
            }
            pendingModuleId = moduleId;
            pendingSince = Util.getMillis();
            NetworkHandler.instance().sendToServer(new HotkeyPacket(hotkey));
        });
    }

    private static PanelHotkeyBindings bindings() {
        if (bindings == null) {
            bindings = load();
        }
        return bindings;
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(MESplicedterminal.MODID).resolve(FILE);
    }

    private static PanelHotkeyBindings load() {
        Path path = path();
        if (!Files.isRegularFile(path)) {
            return new PanelHotkeyBindings();
        }
        try {
            return PanelHotkeyBindings.fromJson(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            MESplicedterminal.LOGGER.warn("Failed to read panel hotkeys from {}", path, e);
            return new PanelHotkeyBindings();
        }
    }

    private static void save() {
        Path path = path();
        try {
            Files.createDirectories(path.getParent());
            Path temp = path.resolveSibling(FILE + ".tmp");
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(bindings().toJson());
            Files.writeString(temp, json, StandardCharsets.UTF_8);
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException e) {
            MESplicedterminal.LOGGER.warn("Failed to write panel hotkeys to {}", path, e);
        }
    }
}
