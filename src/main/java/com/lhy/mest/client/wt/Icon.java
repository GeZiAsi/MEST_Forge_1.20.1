package com.lhy.mest.client.wt;

import net.minecraft.resources.ResourceLocation;

import appeng.client.gui.style.Blitter;

import com.lhy.mest.MESplicedterminal;

/**
 * The AE2WTLib 1.21 icon set. AE2WTLib 15 ships a smaller one, so the sheet is bundled as
 * {@code textures/guis/wtlib_icons.png} (AE2WTLib, MIT, by mari_023).
 */
public record Icon(int x, int y, int width, int height, Texture texture) {
    public static final Texture TEXTURE = new Texture(
            new ResourceLocation(MESplicedterminal.MODID, "textures/guis/wtlib_icons.png"), 128, 128);
    public static final Texture AE2TEXTURE = new Texture(appeng.client.gui.Icon.TEXTURE,
            appeng.client.gui.Icon.TEXTURE_WIDTH, appeng.client.gui.Icon.TEXTURE_HEIGHT);

    public static final Icon BUTTON_BACKGROUND = new Icon(63, 0, 16, 17);
    public static final Icon BUTTON_BACKGROUND_HOVERED = new Icon(95, 1, 16, 16);
    public static final Icon BUTTON_BACKGROUND_FOCUSED = new Icon(79, 0, 16, 17);

    // AE2 15's states.png has a different layout from AE2 19's; these point at its equivalents.
    public static final Icon TOOLBAR_BUTTON_BACKGROUND = new Icon(240, 240, 16, 16, AE2TEXTURE);
    public static final Icon TOOLBAR_BUTTON_BACKGROUND_HOVERED = TOOLBAR_BUTTON_BACKGROUND;
    public static final Icon TOOLBAR_BUTTON_BACKGROUND_FOCUSED = TOOLBAR_BUTTON_BACKGROUND;

    public static final Icon TERMINAL_SETTINGS = new Icon(32, 64, 16, 16, AE2TEXTURE);
    public static final Icon MAGNET = new Icon(0, 0);
    public static final Icon MAGNET_FILTER = new Icon(0, 16);
    public static final Icon TRASH = new Icon(0, 32);

    public static final Icon PATTERN_ACCESS = new Icon(16, 0);
    public static final Icon PATTERN_ENCODING = new Icon(16, 16);
    public static final Icon CRAFTING = new Icon(16, 32);

    public static final Icon NO = new Icon(32, 0);
    public static final Icon YES = new Icon(32, 16);
    public static final Icon UP = new Icon(32, 32);
    public static final Icon DOWN = new Icon(32, 48);
    public static final Icon SWITCH = new Icon(32, 64);

    public static final Icon UPGRADE_BACKGROUND_TOP = new Icon(77, 62, 23, 23);
    public static final Icon UPGRADE_BACKGROUND_MIDDLE = new Icon(77, 85, 23, 18);
    public static final Icon UPGRADE_BACKGROUND_BOTTOM = new Icon(77, 103, 23, 25);
    public static final Icon UPGRADE_BACKGROUND_SCROLLING_TOP = new Icon(48, 62, 29, 23);
    public static final Icon UPGRADE_BACKGROUND_SCROLLING_MIDDLE = new Icon(48, 85, 29, 18);
    public static final Icon UPGRADE_BACKGROUND_SCROLLING_BOTTOM = new Icon(48, 103, 29, 25);

    private Icon(int x, int y) {
        this(x, y, 16, 16);
    }

    private Icon(int x, int y, int width, int height) {
        this(x, y, width, height, TEXTURE);
    }

    public Blitter getBlitter() {
        return Blitter.texture(texture.location(), texture.width(), texture.height())
                .src(x, y, width, height);
    }

    public record Texture(ResourceLocation location, int width, int height) {
    }
}
