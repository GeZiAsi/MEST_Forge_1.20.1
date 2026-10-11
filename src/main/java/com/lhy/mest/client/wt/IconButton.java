package com.lhy.mest.client.wt;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;

import appeng.client.gui.widgets.ITooltip;

/** AE2WTLib 1.21's icon button, drawn with {@link Icon}. */
public class IconButton extends Button implements ITooltip {
    private final Icon icon;
    private final Icon bg;
    private final Icon bgHovered;
    private final Icon bgFocused;
    @Nullable
    private List<Component> tooltip;

    public IconButton(OnPress onPress, Icon icon) {
        this(onPress, icon, Icon.BUTTON_BACKGROUND, Icon.BUTTON_BACKGROUND_HOVERED, Icon.BUTTON_BACKGROUND_FOCUSED);
    }

    public IconButton(OnPress onPress, Icon icon, Icon bg, Icon bgHovered, Icon bgFocused) {
        super(0, 0, 16, 16, Component.empty(), onPress, Button.DEFAULT_NARRATION);
        this.icon = icon;
        this.bg = bg;
        this.bgHovered = bgHovered;
        this.bgFocused = bgFocused;
    }

    public static IconButton withAE2Background(OnPress onPress, Icon icon) {
        return new IconButton(onPress, icon, Icon.TOOLBAR_BUTTON_BACKGROUND, Icon.TOOLBAR_BUTTON_BACKGROUND_HOVERED,
                Icon.TOOLBAR_BUTTON_BACKGROUND_FOCUSED);
    }

    public void setVisibility(boolean vis) {
        visible = vis;
        active = vis;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
        if (!visible) {
            return;
        }
        var yOffset = isHovered() ? 1 : 0;
        var background = getBG();
        var bgSizeOffset = background.width() > 16 ? 1 : 0;
        background.getBlitter()
                .dest(getX() - 1, getY() + yOffset, background.width(), background.height())

                .blit(guiGraphics);
        getIcon().getBlitter().dest(getX() - 1 + bgSizeOffset, getY() + bgSizeOffset + yOffset)
                .blit(guiGraphics);
    }

    protected Icon getIcon() {
        return icon;
    }

    private Icon getBG() {
        return isHovered() ? bgHovered : isFocused() ? bgFocused : bg;
    }

    @Override
    public List<Component> getTooltipMessage() {
        if (tooltip == null) {
            return Collections.singletonList(getMessage());
        }
        return tooltip;
    }

    @Override
    public Rect2i getTooltipArea() {
        return new Rect2i(getX(), getY(), getBG().width(), getBG().height());
    }

    @Override
    public boolean isTooltipAreaVisible() {
        return visible;
    }

    @Contract("_ -> this")
    public IconButton withTooltip(Component message) {
        super.setMessage(message);
        return this;
    }

    @Contract("_ -> this")
    public IconButton withTooltip(List<Component> tooltip) {
        this.tooltip = tooltip;
        return this;
    }
}
