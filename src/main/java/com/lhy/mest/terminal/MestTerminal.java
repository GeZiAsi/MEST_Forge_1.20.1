package com.lhy.mest.terminal;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import appeng.api.features.GridLinkables;
import appeng.api.upgrades.Upgrades;
import appeng.core.localization.GuiText;
import appeng.items.materials.EnergyCardItem;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocators;

import de.mari_023.ae2wtlib.AE2wtlib;
import de.mari_023.ae2wtlib.wut.WUTHandler;

import com.lhy.mest.item.ItemMEST;
import com.lhy.mest.registry.ModItems;

/**
 * Wires the ME Spliced Terminal into the AE2WTLib terminal framework and AE2's power system.
 */
public final class MestTerminal {
    private MestTerminal() {}

    /** Internal terminal name used by AE2WTLib's registry/hotkey/universal-terminal systems. */
    public static final String TERMINAL_NAME = "spliced";
    /** AE2WTLib's default hotkey id for {@link #TERMINAL_NAME}; its key mapping is {@code key.ae2.<id>}. */
    public static final String HOTKEY_NAME = "wireless_" + TERMINAL_NAME + "_terminal";

    /**
     * Adds our terminal definition to AE2WTLib. Runs during the ITEM RegisterEvent, after AE2WTLib
     * created the universal terminal it needs and after our own item was registered.
     */
    public static void registerTerminal() {
        ItemMEST item = ModItems.splicedTerminalItem();
        WUTHandler.addTerminal(TERMINAL_NAME, item::tryOpen, MESTMenuHost::new, MESTMenu.TYPE, item,
                HOTKEY_NAME, ItemMEST.DESCRIPTION_ID);
    }

    /**
     * Register the grid-linkable handler so the terminal can be bound to a wireless access point
     * (right-click the access point with the terminal in hand). Done in common setup, after all items
     * are registered. Uses AE2's standard wireless-terminal linking handler.
     */
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            var item = ModItems.splicedTerminalItem();
            GridLinkables.register(item, WirelessTerminalItem.LINKABLE_HANDLER);
            String group = GuiText.WirelessTerminals.getTranslationKey();
            for (var card : BuiltInRegistries.ITEM) {
                if (card instanceof EnergyCardItem) {
                    Upgrades.add(card, item, 3, group);
                }
            }
            if (AE2wtlib.MAGNET_CARD != null) {
                Upgrades.add(AE2wtlib.MAGNET_CARD, item, 1, group);
            }
            if (AE2wtlib.QUANTUM_BRIDGE_CARD != null) {
                Upgrades.add(AE2wtlib.QUANTUM_BRIDGE_CARD, item, 1, group);
            }
            MenuLocators.register(
                    ToolkitItemLocator.class,
                    ToolkitItemLocator::writeToPacket,
                    ToolkitItemLocator::readFromPacket);
        });
    }
}
