package com.lhy.mest.client;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.settings.KeyConflictContext;

import com.lhy.mest.MESplicedterminal;

public final class MestKeybindings {
    public static final String CATEGORY = "key.mesplicedterminal.category";

    public static final KeyMapping TOOLKIT_BAR_LEFT = create("toolkit_bar_left", GLFW.GLFW_KEY_LEFT);
    public static final KeyMapping TOOLKIT_BAR_RIGHT = create("toolkit_bar_right", GLFW.GLFW_KEY_RIGHT);

    private MestKeybindings() {}

    private static KeyMapping create(String name, int defaultKey) {
        return new KeyMapping(
                "key." + MESplicedterminal.MODID + "." + name,
                KeyConflictContext.IN_GAME,
                InputConstants.Type.KEYSYM,
                defaultKey,
                CATEGORY);
    }
}
