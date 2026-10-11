package com.lhy.mest.compat.plus;

import net.minecraft.client.gui.screens.Screen;

import com.lhy.mest.network.ProviderPickerListPacket;

/** Plus-only screen/packet helpers; inert without ExtendedAE Plus (see {@link PlusEncodingUpload}). */
public final class PlusScreenSupport {
    private PlusScreenSupport() {
    }

    public static boolean loaded() {
        return false;
    }

    public static Screen presetPicker(Screen parent, ProviderPickerListPacket packet) {
        return parent;
    }

    public static void cancelPending() {
    }

    public static boolean fillSearchFromHoveredIngredient(com.lhy.mest.client.panel.MEListPanel panel,
            int keyCode, int scanCode) {
        return false;
    }
}
