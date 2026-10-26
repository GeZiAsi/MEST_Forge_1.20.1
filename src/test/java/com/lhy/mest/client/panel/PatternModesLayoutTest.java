package com.lhy.mest.client.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class PatternModesLayoutTest {
    private static BufferedImage ae2Texture() throws Exception {
        try (var stream = PatternModesLayout.class.getClassLoader()
                .getResourceAsStream("assets/ae2/textures/guis/pattern_modes.png")) {
            assertNotNull(stream, "AE2's pattern_modes.png on the test classpath");
            return ImageIO.read(stream);
        }
    }

    @Test
    void ae2TextureMatchesAe2SlotPositions() throws Exception {
        BufferedImage image = ae2Texture();
        assertEquals(PatternModesLayout.AE2_15, PatternModesLayout.detect(image::getRGB));
    }

    @Test
    void followsGridsMovedByAResourcePack() throws Exception {
        BufferedImage image = ae2Texture();
        int dx = -2;
        int dy = -2;
        // A pack that draws every mode two pixels up and left, as 1.21-style packs do (by one or two).
        BufferedImage moved = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int[][] regions = {
                { PatternModesLayout.CRAFTING_SRC_X, PatternModesLayout.CRAFTING_SRC_Y },
                { PatternModesLayout.PROCESSING_SRC_X, PatternModesLayout.PROCESSING_SRC_Y },
                { PatternModesLayout.SMITHING_SRC_X, PatternModesLayout.SMITHING_SRC_Y },
                { PatternModesLayout.STONECUTTING_SRC_X, PatternModesLayout.STONECUTTING_SRC_Y } };
        for (int[] region : regions) {
            int background = image.getRGB(region[0] + 2, region[1] + 3);
            for (int y = 0; y < PatternModesLayout.REGION_H; y++) {
                for (int x = 0; x < PatternModesLayout.REGION_W; x++) {
                    int sx = x - dx;
                    int sy = y - dy;
                    boolean inside = sx < PatternModesLayout.REGION_W && sy < PatternModesLayout.REGION_H;
                    moved.setRGB(region[0] + x, region[1] + y,
                            inside ? image.getRGB(region[0] + sx, region[1] + sy) : background);
                }
            }
        }
        var expected = PatternModesLayout.AE2_15;
        var detected = PatternModesLayout.detect(moved::getRGB);
        assertEquals(shift(expected.craftingGrid(), dx, dy), detected.craftingGrid());
        assertEquals(shift(expected.craftingResult(), dx, dy), detected.craftingResult());
        assertEquals(shift(expected.processingGrid(), dx, dy), detected.processingGrid());
        assertEquals(shift(expected.processingOutputs(), dx, dy), detected.processingOutputs());
        assertEquals(new PatternModesLayout.Track(expected.processingScroll().x() + dx,
                expected.processingScroll().y() + dy, expected.processingScroll().height()),
                detected.processingScroll());
        assertEquals(shift(expected.smithingGrid(), dx, dy), detected.smithingGrid());
        assertEquals(shift(expected.smithingResult(), dx, dy), detected.smithingResult());
        assertEquals(shift(expected.stonecuttingInput(), dx, dy), detected.stonecuttingInput());
        assertEquals(shift(expected.stonecuttingRecipes(), dx, dy), detected.stonecuttingRecipes());
    }

    @Test
    void blankTextureKeepsAe2Positions() {
        assertEquals(PatternModesLayout.AE2_15, PatternModesLayout.detect((x, y) -> 0));
    }

    private static PatternModesLayout.Point shift(PatternModesLayout.Point point, int dx, int dy) {
        return new PatternModesLayout.Point(point.x() + dx, point.y() + dy);
    }
}
