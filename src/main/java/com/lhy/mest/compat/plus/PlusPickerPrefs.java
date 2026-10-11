package com.lhy.mest.compat.plus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Same {@code extendedae_plus/pinned_providers.json} Plus's picker uses, so pins and toggles
 * stay shared without mixin or reflection.
 */
public final class PlusPickerPrefs {
    private static final String RELATIVE = "extendedae_plus/pinned_providers.json";
    private static final String PINNED_KEY = "pinned";
    private static final String AUTO_UPLOAD_KEY = "auto_upload_unique_match";
    private static final String PROCESSING_KEY = "show_processing_buttons";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final Set<String> PINNED = new HashSet<>();
    private static boolean autoUploadUniqueMatch = true;
    private static boolean showProcessingButtons = true;

    static {
        load();
    }

    private PlusPickerPrefs() {
    }

    public static boolean isPinned(String name) {
        return name != null && PINNED.contains(name);
    }

    public static void togglePinned(String name) {
        if (name == null || name.isBlank()) {
            return;
        }
        if (!PINNED.add(name)) {
            PINNED.remove(name);
        }
        save();
    }

    public static boolean autoUploadUniqueMatch() {
        return autoUploadUniqueMatch;
    }

    public static void toggleAutoUploadUniqueMatch() {
        autoUploadUniqueMatch = !autoUploadUniqueMatch;
        save();
    }

    public static boolean showProcessingButtons() {
        return showProcessingButtons;
    }

    public static void toggleProcessingButtons() {
        showProcessingButtons = !showProcessingButtons;
        save();
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(RELATIVE);
    }

    private static void load() {
        Path file = path();
        if (!Files.isRegularFile(file)) {
            return;
        }
        try {
            JsonObject root = GSON.fromJson(Files.readString(file), JsonObject.class);
            if (root == null) {
                return;
            }
            PINNED.clear();
            JsonElement pinned = root.get(PINNED_KEY);
            if (pinned != null && pinned.isJsonArray()) {
                for (JsonElement entry : pinned.getAsJsonArray()) {
                    if (entry.isJsonPrimitive()) {
                        PINNED.add(entry.getAsString());
                    }
                }
            }
            if (root.has(AUTO_UPLOAD_KEY) && root.get(AUTO_UPLOAD_KEY).isJsonPrimitive()) {
                autoUploadUniqueMatch = root.get(AUTO_UPLOAD_KEY).getAsBoolean();
            }
            if (root.has(PROCESSING_KEY) && root.get(PROCESSING_KEY).isJsonPrimitive()) {
                showProcessingButtons = root.get(PROCESSING_KEY).getAsBoolean();
            }
        } catch (Exception ignored) {
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        JsonArray pinned = new JsonArray();
        for (String name : PINNED) {
            pinned.add(name);
        }
        root.add(PINNED_KEY, pinned);
        root.addProperty(AUTO_UPLOAD_KEY, autoUploadUniqueMatch);
        root.addProperty(PROCESSING_KEY, showProcessingButtons);
        Path file = path();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root));
        } catch (IOException ignored) {
        }
    }
}
