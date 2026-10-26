package com.lhy.mest.client.panel;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import appeng.core.AppEng;

import com.lhy.mest.MESplicedterminal;

/**
 * The slot layout of the {@code pattern_modes.png} the resource packs currently provide, measured
 * once per resource reload.
 */
public final class PatternModesTexture {
    private static volatile PatternModesLayout layout;

    private PatternModesTexture() {
    }

    /** Clears the measured layout whenever resource packs change. */
    public static final ResourceManagerReloadListener RELOAD_LISTENER = manager -> layout = null;

    public static PatternModesLayout layout() {
        PatternModesLayout current = layout;
        if (current == null) {
            current = measure();
            layout = current;
        }
        return current;
    }

    private static PatternModesLayout measure() {
        var id = AppEng.makeId("textures/guis/pattern_modes.png");
        try {
            var resource = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);
            if (resource == null) {
                return PatternModesLayout.AE2_15;
            }
            try (var stream = resource.open(); NativeImage image = NativeImage.read(stream)) {
                // The measured positions are in pixels of AE2's 256x256 sheet; a scaled-up pack is not.
                if (image.getWidth() != 256 || image.getHeight() != 256) {
                    MESplicedterminal.LOGGER.info("MEST: {} is {}x{}, using AE2's slot positions", id,
                            image.getWidth(), image.getHeight());
                    return PatternModesLayout.AE2_15;
                }
                var measured = PatternModesLayout.detect(image::getPixelRGBA);
                if (!measured.equals(PatternModesLayout.AE2_15)) {
                    MESplicedterminal.LOGGER.info("MEST: pattern encoding slots follow {} from pack '{}': {}", id,
                            resource.sourcePackId(), measured);
                }
                return measured;
            }
        } catch (Exception e) {
            MESplicedterminal.LOGGER.warn("MEST: could not measure {}, using AE2's slot positions", id, e);
            return PatternModesLayout.AE2_15;
        }
    }
}
