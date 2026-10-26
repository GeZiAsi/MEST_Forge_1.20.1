package com.lhy.mest.client.panel;

import org.jetbrains.annotations.Nullable;

/**
 * Where the slots sit on AE2's {@code guis/pattern_modes.png}, measured from the texture itself.
 *
 * <p>AE2 15's own texture, AE2 19's and resource packs that restyle the pattern terminal (e.g. AE2
 * "1.21 GUI" packs for 1.20.1) put the grids a pixel or two apart, so fixed offsets only line up
 * with one of them. Each mode region is scanned for its border lines instead: the first long dark
 * run is the top-left corner of the grid, a later one the result box, and the scroller tracks are
 * the short boxes left of (processing) or right of (stonecutting) the grids.
 *
 * <p>All points are relative to the region's top-left corner as {@link PatternEncodingPanel} blits
 * it, and are item positions (the 16x16 area inside a slot).
 */
public record PatternModesLayout(
        Point craftingGrid,
        Point craftingResult,
        Point processingGrid,
        Point processingOutputs,
        Track processingScroll,
        Point smithingGrid,
        Point smithingResult,
        Point stonecuttingInput,
        Point stonecuttingRecipes,
        Track stonecuttingScroll) {

    public record Point(int x, int y) {
    }

    /** A scroller handle position and its travel height. */
    public record Track(int x, int y, int height) {
    }

    /** Pixel access to the whole texture; ARGB or ABGR alike, only alpha and the colour sum count. */
    public interface Pixels {
        int pixel(int x, int y);
    }

    /** AE2 15's own texture, matching its {@code screens/terminals/encoding/*.json}. */
    public static final PatternModesLayout AE2_15 = new PatternModesLayout(
            new Point(9, 8), new Point(101, 26),
            new Point(17, 8), new Point(101, 8), new Track(8, 8, 52),
            new Point(12, 26), new Point(102, 26),
            new Point(12, 25), new Point(44, 8), new Track(110, 7, 54));

    public static final int REGION_W = 124;
    public static final int REGION_H = 66;
    public static final int CRAFTING_SRC_X = 0;
    public static final int CRAFTING_SRC_Y = 0;
    public static final int PROCESSING_SRC_X = 0;
    public static final int PROCESSING_SRC_Y = 70;
    public static final int SMITHING_SRC_X = 128;
    public static final int SMITHING_SRC_Y = 70;
    public static final int STONECUTTING_SRC_X = 0;
    public static final int STONECUTTING_SRC_Y = 141;

    private static final int SLOT = 18;
    private static final int HANDLE_WIDTH = 7;
    private static final int STONECUTTING_COLS = 4;

    /** Measures every mode; a mode whose lines cannot be found keeps AE2 15's positions. */
    public static PatternModesLayout detect(Pixels pixels) {
        var fallback = AE2_15;
        Point craftingGrid = fallback.craftingGrid;
        Point craftingResult = fallback.craftingResult;
        Point processingGrid = fallback.processingGrid;
        Point processingOutputs = fallback.processingOutputs;
        Track processingScroll = fallback.processingScroll;
        Point smithingGrid = fallback.smithingGrid;
        Point smithingResult = fallback.smithingResult;
        Point stonecuttingInput = fallback.stonecuttingInput;
        Point stonecuttingRecipes = fallback.stonecuttingRecipes;
        Track stonecuttingScroll = fallback.stonecuttingScroll;

        var crafting = new Region(pixels, CRAFTING_SRC_X, CRAFTING_SRC_Y);
        Run grid = crafting.firstRun(16, 1, REGION_W, 0);
        Run result = grid == null ? null : crafting.firstRun(17, grid.x + 3 * SLOT, REGION_W, 0);
        if (grid != null && result != null) {
            craftingGrid = new Point(grid.x + 1, grid.y + 1);
            craftingResult = crafting.boxItem(result);
        }

        var processing = new Region(pixels, PROCESSING_SRC_X, PROCESSING_SRC_Y);
        grid = processing.firstRun(16, 1, REGION_W, 0);
        if (grid != null) {
            Run outputs = processing.runOnRow(grid.y, grid.x + 3 * SLOT, REGION_W, 1);
            Run track = processing.runOnRow(grid.y, 1, grid.x, 3);
            if (outputs != null && track != null) {
                processingGrid = new Point(grid.x + 1, grid.y + 1);
                processingOutputs = new Point(outputs.x + 1, grid.y + 1);
                processingScroll = processing.track(track, fallback.processingScroll.height);
            }
        }

        var smithing = new Region(pixels, SMITHING_SRC_X, SMITHING_SRC_Y);
        grid = smithing.firstRun(16, 1, REGION_W, 0);
        result = grid == null ? null : smithing.firstRun(17, grid.x + 3 * SLOT, REGION_W, 0);
        if (grid != null && result != null) {
            smithingGrid = new Point(grid.x + 1, grid.y + 1);
            smithingResult = smithing.boxItem(result);
        }

        var stonecutting = new Region(pixels, STONECUTTING_SRC_X, STONECUTTING_SRC_Y);
        Run input = stonecutting.firstRun(16, 1, 40, 0);
        Run recipes = stonecutting.firstRun(60, 40, REGION_W, 0);
        if (input != null && recipes != null) {
            int trackX = recipes.x + 1 + STONECUTTING_COLS * 16 + 2;
            Run track = null;
            for (int y = recipes.y; y < REGION_H && track == null; y++) {
                if (stonecutting.dark(trackX, y)) {
                    track = stonecutting.runContaining(y, trackX);
                }
            }
            if (track != null) {
                stonecuttingInput = stonecutting.boxItem(input);
                stonecuttingRecipes = new Point(recipes.x + 1, recipes.y + 2);
                stonecuttingScroll = stonecutting.track(track, fallback.stonecuttingScroll.height);
            }
        }

        return new PatternModesLayout(craftingGrid, craftingResult, processingGrid, processingOutputs,
                processingScroll, smithingGrid, smithingResult, stonecuttingInput, stonecuttingRecipes,
                stonecuttingScroll);
    }

    private record Run(int x, int y, int length) {
    }

    /** One mode's 124x66 area; "dark" means the region's border colour. */
    private static final class Region {
        private final Pixels pixels;
        private final int srcX;
        private final int srcY;
        private final int borderLum;

        Region(Pixels pixels, int srcX, int srcY) {
            this.pixels = pixels;
            this.srcX = srcX;
            this.srcY = srcY;
            int min = Integer.MAX_VALUE;
            for (int y = 0; y < REGION_H; y++) {
                for (int x = 0; x < REGION_W; x++) {
                    int argb = pixel(x, y);
                    if (opaque(argb)) {
                        min = Math.min(min, lum(argb));
                    }
                }
            }
            // Borders are the darkest colour; grid separators and fills are a step lighter.
            this.borderLum = min == Integer.MAX_VALUE ? -1 : min + 12;
        }

        private int pixel(int x, int y) {
            if (x < 0 || y < 0 || x >= REGION_W || y >= REGION_H) {
                return 0;
            }
            return pixels.pixel(srcX + x, srcY + y);
        }

        private static boolean opaque(int argb) {
            return (argb >>> 24) > 10;
        }

        private static int lum(int argb) {
            return (((argb >> 16) & 0xFF) + ((argb >> 8) & 0xFF) + (argb & 0xFF)) / 3;
        }

        boolean dark(int x, int y) {
            int argb = pixel(x, y);
            return opaque(argb) && lum(argb) <= borderLum;
        }

        /** First run (top to bottom, left to right) starting in [minX, maxX) at least minLength long. */
        @Nullable
        Run firstRun(int minLength, int minX, int maxX, int fromY) {
            for (int y = fromY; y < REGION_H; y++) {
                Run run = runOnRow(y, minX, maxX, minLength);
                if (run != null) {
                    return run;
                }
            }
            return null;
        }

        @Nullable
        Run runOnRow(int y, int minX, int maxX, int minLength) {
            int x = 0;
            while (x < REGION_W) {
                if (!dark(x, y)) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < REGION_W && dark(x, y)) {
                    x++;
                }
                if (start >= minX && start < maxX && x - start >= minLength) {
                    return new Run(start, y, x - start);
                }
            }
            return null;
        }

        @Nullable
        Run runContaining(int y, int x) {
            if (!dark(x, y)) {
                return null;
            }
            int start = x;
            while (start > 0 && dark(start - 1, y)) {
                start--;
            }
            int end = x;
            while (end < REGION_W && dark(end, y)) {
                end++;
            }
            return new Run(start, y, end - start);
        }

        /** Inside width of a box whose top border is {@code run}: a dark right edge is a border. */
        private int inner(Run run) {
            return dark(run.x + run.length - 1, run.y + 1) ? run.length - 2 : run.length - 1;
        }

        /** The item position centred in a slot box. */
        Point boxItem(Run run) {
            int offset = (inner(run) - 16) / 2;
            return new Point(run.x + 1 + offset, run.y + 1 + offset);
        }

        /** A scroller handle centred in the track box, travelling down to the track's bottom. */
        Track track(Run run, int fallbackHeight) {
            int x = run.x + 1 + (inner(run) - HANDLE_WIDTH) / 2;
            int end = run.y + 1;
            while (end < REGION_H && dark(run.x, end)) {
                end++;
            }
            int height = end - run.y - 1 - (dark(run.x + 1, end - 1) ? 1 : 0);
            return new Track(x, run.y + 1, height >= 20 ? height : fallbackHeight);
        }
    }
}
