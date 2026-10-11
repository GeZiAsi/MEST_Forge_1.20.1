package com.lhy.mest.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.upgrades.Upgrades;

import de.mari_023.ae2wtlib.terminal.ItemWT;

import com.lhy.mest.registry.ModMenus;

/**
 * The ME Spliced Terminal item. It plugs into the AE2WTLib terminal framework by extending
 * {@link ItemWT}, which gives us wireless grid access, power handling, quantum-bridge support,
 * hotkey locating and universal-terminal merging for free.
 */
public class ItemMEST extends ItemWT {
    public static final String DESCRIPTION_ID = "item.mesplicedterminal.spliced_terminal";

    /**
     * Fixed instead of derived from the registry name: AE2WTLib reads it while registering the
     * terminal, and vanilla would otherwise cache a description id derived too early.
     */
    @Override
    public String getDescriptionId() {
        return DESCRIPTION_ID;
    }

    @Override
    public MenuType<?> getMenuType(ItemStack stack) {
        return ModMenus.SPLICED_TERMINAL_MENU.get();
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, upgradeInventorySize(), this::onUpgradesChanged);
    }

    /** Slot count is the sum of each compatible card's max, matching the Compatible Upgrades tooltip. */
    public static int upgradeInventorySize() {
        int total = compatibleUpgradeTotal();
        return total > 0 ? total : 3;
    }

    private static int compatibleUpgradeTotal() {
        try {
            Item terminal = com.lhy.mest.registry.ModItems.SPLICED_TERMINAL.get();
            int total = 0;
            for (Item card : BuiltInRegistries.ITEM) {
                int max = Upgrades.getMaxInstallable(card, terminal);
                if (max > 0) {
                    total += max;
                }
            }
            return total;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        setAEMaxPowerMultiplier(stack, 1 + Upgrades.getEnergyCardMultiplier(upgrades));
    }
}
