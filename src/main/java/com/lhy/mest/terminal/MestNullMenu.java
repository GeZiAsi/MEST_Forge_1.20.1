package com.lhy.mest.terminal;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Inert owner for throw-away crafting containers used only for recipe lookups. */
final class MestNullMenu extends AbstractContainerMenu {
    MestNullMenu() {
        super(null, -1);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }
}
