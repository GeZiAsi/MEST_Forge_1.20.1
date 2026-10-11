package com.lhy.mest.compat.plus;

import net.minecraft.server.level.ServerPlayer;

import appeng.api.networking.IGrid;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.encoding.EncodingMode;

/**
 * ExtendedAE Plus pending-upload bridge. The 1.21.1 build drove EAEP's provider picker; the GTL
 * (Forge 1.20.1) stack does not ship EAEP, so every entry point reports "not available" and the
 * built-in pattern-access upload path is used instead.
 */
public final class PlusEncodingUpload {
    private PlusEncodingUpload() {
    }

    public static boolean available() {
        return false;
    }

    public static void captureRecipeSearchKey(Object rawRecipe) {
    }

    public static void presetSearchKeyForMode(EncodingMode mode) {
    }

    public static boolean uploadEncodedToMatrix(ServerPlayer player, RestrictedInputSlot encodedSlot, IGrid grid) {
        return false;
    }

    public static boolean beginAndOpenPicker(ServerPlayer player, RestrictedInputSlot encodedSlot) {
        return false;
    }

    public static boolean sendPickerList(ServerPlayer player) {
        return false;
    }
}
