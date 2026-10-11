package com.lhy.mest.compat.plus;

import java.util.List;

/**
 * Inert stand-ins for the ExtendedAE Plus upload helpers the provider picker used. The picker is
 * only created when {@link PlusScreenSupport#loaded()} is true, which never happens on the GTL
 * (Forge 1.20.1) build, so none of these are reached at runtime.
 */
public final class PlusUploadShim {
    private PlusUploadShim() {
    }

    public record RecipeTypeMapping(String key, String value) {
    }

    public static String consumeLastProviderSearchKey() {
        return null;
    }

    public static String resolveProviderSearchKey(String key) {
        return key;
    }

    public static void removeRecipeTypeMapping(String key) {
    }

    public static boolean addOrUpdateRecipeTypeMapping(String key, String value) {
        return false;
    }

    public static List<RecipeTypeMapping> getRecipeTypeMappings() {
        return List.of();
    }

    /** Would upload the pending pattern to the chosen provider; nothing is pending without EAEP. */
    public static void uploadToProvider(long providerId, boolean fromPicker, String providerName) {
    }
}
