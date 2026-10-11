package com.lhy.mest.client.panel;

import com.lhy.mest.client.MestGuiIcons;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;

import appeng.api.config.ActionItems;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.ITooltip;
import appeng.menu.SlotSemantics;

import de.mari_023.ae2wtlib.TextConstants;
import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import com.lhy.mest.client.wt.Icon;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.client.MESTScreen;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.terminal.MESTMenu;

/**
 * ae2wtlib wireless crafting-terminal body: armor, player preview, magnet/trash/settings
 * and the 3x3 grid. Background is the cropped 181x72 sheet; helmet slot is at (1,1).
 */
public class CraftingTerminalPanel extends ModulePanel {
    private static final int SLOT = 18;
    private static final int TEX_W = 181;
    private static final int TEX_H = 72;
    private static final int PLAYER_X = 19;
    private static final int PLAYER_Y = 2;
    private static final int PLAYER_W = 46;
    private static final int PLAYER_H = 70;
    private static final int PANEL_BUTTON = 16;
    static final int SEARCH_BUTTON = 12;
    private static final Blitter BACKGROUND = Blitter.texture(
                    new ResourceLocation(MESplicedterminal.MODID, "textures/guis/crafting_terminal.png"),
                    256, 256)
            .src(0, 0, TEX_W, TEX_H);

    private final MESTMenu menu;
    private final List<Slot> gridSlots;
    private final List<Slot> resultSlots;
    private final List<Slot> helmetSlots;
    private final List<Slot> chestSlots;
    private final List<Slot> legsSlots;
    private final List<Slot> bootsSlots;
    private final List<Slot> offhandSlots;
    private final ActionButton clearToNetwork;
    private final ActionButton clearToPlayer;
    private final SizedIconButton settingsButton;
    private final SizedIconButton magnetButton;
    private final SizedIconButton trashButton;
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private boolean hostUtilitiesOnMeList;

    public CraftingTerminalPanel(MESTMenu menu, MESTScreen screen) {
        this.menu = menu;
        this.gridSlots = menu.getSlots(SlotSemantics.CRAFTING_GRID);
        this.resultSlots = menu.getSlots(SlotSemantics.CRAFTING_RESULT);
        this.helmetSlots = menu.getSlots(AE2wtlibSlotSemantics.HELMET);
        this.chestSlots = menu.getSlots(AE2wtlibSlotSemantics.CHESTPLATE);
        this.legsSlots = menu.getSlots(AE2wtlibSlotSemantics.LEGGINGS);
        this.bootsSlots = menu.getSlots(AE2wtlibSlotSemantics.BOOTS);
        this.offhandSlots = menu.getSlots(AE2wtlibSlotSemantics.OFFHAND);
        registerAll(gridSlots);
        registerAll(resultSlots);
        registerAll(helmetSlots);
        registerAll(chestSlots);
        registerAll(legsSlots);
        registerAll(bootsSlots);
        registerAll(offhandSlots);

        clearToNetwork = new ActionButton(ActionItems.STASH, menu::clearCraftingGrid);
        clearToNetwork.setHalfSize(true);
        clearToNetwork.setDisableBackground(true);
        widgets.add(clearToNetwork);

        clearToPlayer = new ActionButton(ActionItems.STASH_TO_PLAYER_INV, menu::clearToPlayerInventory);
        clearToPlayer.setHalfSize(true);
        clearToPlayer.setDisableBackground(true);
        widgets.add(clearToPlayer);

        settingsButton = new SizedIconButton(btn -> screen.switchToWirelessTerminalSettings(), Icon.TERMINAL_SETTINGS);
        settingsButton.setMessage(TextConstants.TERMINAL_SETTINGS);
        widgets.add(settingsButton);

        magnetButton = new SizedIconButton(btn -> menu.openMagnetMenu(), Icon.MAGNET);
        magnetButton.setMessage(TextConstants.MAGNET_FILTER);
        widgets.add(magnetButton);

        trashButton = new SizedIconButton(btn -> screen.openTrash(), Icon.TRASH);
        trashButton.setMessage(TextConstants.TRASH);
        widgets.add(trashButton);
    }

    private void registerAll(List<Slot> slots) {
        for (Slot slot : slots) {
            registerSlot(slot);
        }
    }

    @Override
    public void renderIcon(GuiGraphics g, int x, int y, int w, int h, float opacity) {
        MestGuiIcons.blit(g, 2, 0, x, y, w, h, opacity);
    }

    @Override
    public String id() {
        return "crafting_terminal";
    }

    @Override
    public Component title() {
        return Component.translatable("gui.mesplicedterminal.crafting_terminal");
    }

    @Override
    public int defaultWidth() {
        return 2 * CONTENT_PADDING + TEX_W;
    }

    @Override
    public int defaultHeight() {
        return TITLE_BAR_HEIGHT + CONTENT_PADDING + TEX_H;
    }

    @Override
    public int minWidth() {
        return defaultWidth();
    }

    @Override
    public int minHeight() {
        return TITLE_BAR_HEIGHT + TEX_H;
    }

    public void setHostUtilitiesOnMeList(boolean hostOnMeList) {
        this.hostUtilitiesOnMeList = hostOnMeList;
    }

    public boolean hostUtilitiesOnMeList() {
        return hostUtilitiesOnMeList;
    }

    public void tick() {
        MagnetMode mode = menu.getMagnetMode();
        boolean showMagnet = mode != MagnetMode.INVALID && mode != MagnetMode.NO_CARD;
        magnetButton.setVisibility(showMagnet && (visible || hostUtilitiesOnMeList));
    }

    public int utilityBarWidth(int buttonSize) {
        int count = 1 + (magnetButton.visible ? 1 : 0) + 1;
        return count * (buttonSize + 1);
    }

    public void layoutUtilitiesOnSearch(int searchLeft, int searchTop, int buttonSize) {
        tick();
        int x = searchLeft;
        x -= buttonSize;
        placeSized(trashButton, true, x, searchTop, buttonSize);
        if (magnetButton.visible) {
            x -= buttonSize + 1;
            placeSized(magnetButton, true, x, searchTop, buttonSize);
        }
        x -= buttonSize + 1;
        placeSized(settingsButton, true, x, searchTop, buttonSize);
    }

    public boolean mouseClickedUtilities(double mouseX, double mouseY, int button) {
        if (!hostUtilitiesOnMeList) {
            return false;
        }
        for (AbstractWidget widget : List.of(settingsButton, magnetButton, trashButton)) {
            if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }

    public void renderUtilities(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        settingsButton.render(g, mouseX, mouseY, partialTicks);
        magnetButton.render(g, mouseX, mouseY, partialTicks);
        trashButton.render(g, mouseX, mouseY, partialTicks);
    }

    public AbstractWidget hoveredUtility(int mouseX, int mouseY) {
        if (!hostUtilitiesOnMeList) {
            return null;
        }
        for (AbstractWidget widget : List.of(settingsButton, magnetButton, trashButton)) {
            if (widget.visible && widget.isMouseOver(mouseX, mouseY)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    public void layoutSlots() {
        if (!visible) {
            hideArmor(helmetSlots);
            hideArmor(chestSlots);
            hideArmor(legsSlots);
            hideArmor(bootsSlots);
            hideArmor(offhandSlots);
            for (AbstractWidget widget : widgets) {
                if (hostUtilitiesOnMeList
                        && (widget == settingsButton || widget == magnetButton || widget == trashButton)) {
                    continue;
                }
                widget.visible = false;
            }
            if (hostUtilitiesOnMeList) {
                tick();
            }
            return;
        }
        int left = contentLeft();
        int top = contentTop();
        placeArmor(helmetSlots, left + 1, top + 1);
        placeArmor(chestSlots, left + 1, top + 19);
        placeArmor(legsSlots, left + 1, top + 37);
        placeArmor(bootsSlots, left + 1, top + 55);
        placeArmor(offhandSlots, left + 66, top + 55);
        for (int i = 0; i < gridSlots.size(); i++) {
            placeSlot(gridSlots.get(i),
                    left + 89 + (i % 3) * SLOT,
                    top + 10 + (i / 3) * SLOT);
        }
        for (Slot slot : resultSlots) {
            placeSlot(slot, left + 158, top + 28);
        }
        updateWidgets();
    }

    private static void placeArmor(List<Slot> slots, int x, int y) {
        for (Slot slot : slots) {
            placeSlot(slot, x, y);
        }
    }

    private static void hideArmor(List<Slot> slots) {
        for (Slot slot : slots) {
            hideSlot(slot);
        }
    }

    private void updateWidgets() {
        int left = contentLeft();
        int top = contentTop();
        place(clearToNetwork, visible, left + 144, top + 9);
        place(clearToPlayer, visible, left + 154, top + 9);
        placeSized(settingsButton, visible, left + 67, top, PANEL_BUTTON);
        placeSized(trashButton, visible, left + 67, top + 36, PANEL_BUTTON);
        magnetButton.setVisibility(visible && magnetButton.visible);
        if (visible) {
            placeSized(magnetButton, magnetButton.visible, left + 67, top + 18, PANEL_BUTTON);
        }
        tick();
    }

    private static void place(AbstractWidget widget, boolean show, int x, int y) {
        widget.visible = show;
        widget.setX(x);
        widget.setY(y);
    }

    private static void placeSized(SizedIconButton widget, boolean show, int x, int y, int size) {
        widget.visible = show;
        widget.setSquareSize(size);
        widget.setX(x);
        widget.setY(y);
    }

    private static final class SizedIconButton extends Button implements ITooltip {
        private final Icon icon;

        private SizedIconButton(OnPress onPress, Icon icon) {
            super(0, 0, PANEL_BUTTON, PANEL_BUTTON, Component.empty(), onPress, Button.DEFAULT_NARRATION);
            this.icon = icon;
        }

        void setVisibility(boolean vis) {
            this.visible = vis;
            this.active = vis;
        }

        void setSquareSize(int size) {
            setWidth(size);
            setHeight(size);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            Icon bg = isHovered() ? Icon.BUTTON_BACKGROUND_HOVERED
                    : isFocused() ? Icon.BUTTON_BACKGROUND_FOCUSED
                    : Icon.BUTTON_BACKGROUND;
            bg.getBlitter().dest(getX(), getY() + yOffset, getWidth(), getHeight()).blit(guiGraphics);
            icon.getBlitter().dest(getX(), getY() + yOffset, getWidth(), getHeight()).blit(guiGraphics);
        }

        @Override
        public List<Component> getTooltipMessage() {
            return List.of(getMessage());
        }

        @Override
        public Rect2i getTooltipArea() {
            return new Rect2i(getX(), getY(), getWidth(), getHeight());
        }

        @Override
        public boolean isTooltipAreaVisible() {
            return visible;
        }
    }

    @Override
    public void renderBackgroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        updateWidgets();
        BACKGROUND.dest(contentLeft(), contentTop()).blit(g);
        renderPlayer(g, mouseX, mouseY);
        for (AbstractWidget widget : widgets) {
            widget.render(g, mouseX, mouseY, partialTicks);
        }
    }

    private void renderPlayer(GuiGraphics g, int mouseX, int mouseY) {
        Minecraft minecraft = Minecraft.getInstance();
        LivingEntity player = minecraft.player;
        if (player == null) {
            return;
        }
        int lookX = mouseX;
        int lookY = mouseY;
        if (mouseX == Integer.MIN_VALUE || mouseY == Integer.MIN_VALUE) {
            lookX = (int) (minecraft.mouseHandler.xpos()
                    * minecraft.getWindow().getGuiScaledWidth()
                    / Math.max(1, minecraft.getWindow().getScreenWidth()));
            lookY = (int) (minecraft.mouseHandler.ypos()
                    * minecraft.getWindow().getGuiScaledHeight()
                    / Math.max(1, minecraft.getWindow().getScreenHeight()));
        }
        int x1 = contentLeft() + PLAYER_X;
        int y1 = contentTop() + PLAYER_Y;
        int x2 = x1 + PLAYER_W;
        int y2 = y1 + PLAYER_H;
        int clipX1 = contentLeft();
        int clipY1 = contentTop();
        int clipX2 = clipX1 + contentWidth();
        int clipY2 = clipY1 + contentHeight();
        g.enableScissor(clipX1, clipY1, clipX2, clipY2);
        try {
            // 1.20.1 anchors the model at its feet; mirror vanilla's inventory placement in the box.
            int centerX = (x1 + x2) / 2;
            int feetY = y2 - 3;
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    g, centerX, feetY, 30, centerX - (float) lookX, feetY - 50 - (float) lookY, player);
        } finally {
            g.disableScissor();
        }
    }

    @Override
    public void renderForegroundContent(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (hosted) {
            return;
        }
        for (AbstractWidget widget : widgets) {
            if (widget.visible && widget.isMouseOver(mouseX, mouseY) && widget instanceof ITooltip tooltip
                    && !tooltip.getTooltipMessage().isEmpty()) {
                g.renderComponentTooltip(font, tooltip.getTooltipMessage(), mouseX, mouseY);
                return;
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || hosted) {
            return false;
        }
        for (AbstractWidget widget : widgets) {
            if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
        }
        return false;
    }
}
