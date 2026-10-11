package com.lhy.mest.client;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import appeng.api.config.IncludeExclude;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;

import de.mari_023.ae2wtlib.TextConstants;
import com.lhy.mest.client.wt.Icon;
import com.lhy.mest.client.wt.IconButton;

import com.lhy.mest.terminal.MestMagnetMenu;

/** Same magnet.json chrome and buttons as wtlib, bound to {@link MestMagnetMenu}. */
public class MestMagnetScreen extends AEBaseScreen<MestMagnetMenu> {
    public MestMagnetScreen(MestMagnetMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        AESubScreen.addBackButton(menu, "back", widgets);
        widgets.add("pickup_mode", new IconButton(button -> menu.togglePickupMode(), Icon.YES) {
            @Override
            protected Icon getIcon() {
                return icon(menu.getPickupMode());
            }

            @Override
            public Component getMessage() {
                return TextConstants.getPickupMode(menu.getPickupMode());
            }
        });
        widgets.add("insert_mode", new IconButton(button -> menu.toggleInsertMode(), Icon.YES) {
            @Override
            protected Icon getIcon() {
                return icon(menu.getInsertMode());
            }

            @Override
            public Component getMessage() {
                return TextConstants.getInsertMode(menu.getInsertMode());
            }
        });
        widgets.add("copy_up", new IconButton(button -> menu.copyUp(), Icon.UP)
                .withTooltip(TextConstants.COPY_PICKUP));
        widgets.add("copy_down", new IconButton(button -> menu.copyDown(), Icon.DOWN)
                .withTooltip(TextConstants.COPY_INSERT));
        widgets.add("switch", new IconButton(button -> menu.switchInsertPickup(), Icon.SWITCH)
                .withTooltip(TextConstants.SWITCH));
    }

    private static Icon icon(IncludeExclude mode) {
        return mode == IncludeExclude.WHITELIST ? Icon.YES : Icon.NO;
    }

    protected boolean shouldAddToolbar() {
        return false;
    }
}
