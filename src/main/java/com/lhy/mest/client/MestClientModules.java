package com.lhy.mest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoader;

import com.lhy.mest.api.client.MestModuleFactory;
import com.lhy.mest.api.client.RegisterMestModulesEvent;
import com.lhy.mest.client.panel.CraftingPanel;
import com.lhy.mest.client.panel.CraftingTerminalPanel;
import com.lhy.mest.client.panel.HotkeysPanel;
import com.lhy.mest.client.panel.InventoryPanel;
import com.lhy.mest.client.panel.NetworkToolkitPanel;
import com.lhy.mest.client.panel.PatternAccessPanel;
import com.lhy.mest.client.panel.PatternCachePanel;
import com.lhy.mest.client.panel.PatternEncodingPanel;
import com.lhy.mest.client.panel.ProviderSelectPanel;
import com.lhy.mest.client.panel.ToolkitPanel;
import com.lhy.mest.client.panel.TrashPanel;
import com.lhy.mest.client.panel.WirelessSettingsPanel;
import com.lhy.mest.module.BuiltinModules;
import com.lhy.mest.module.OrderedModuleRegistry;

/**
 * Every terminal module: built-ins in their fixed order, then add-ons from
 * {@link RegisterMestModulesEvent} in id order. The screen builds its panels from this list.
 */
public final class MestClientModules {
    public record Registration(String id, MestModuleFactory factory) {
    }

    private static final List<Registration> BUILTINS = List.of(
            new Registration("me_list", context -> context.screen().meListPanel()),
            new Registration("crafting", context -> new CraftingPanel(context.menu())),
            new Registration("crafting_terminal", context -> new CraftingTerminalPanel(context.menu(), context.screen())),
            new Registration("pattern_encoding", context -> new PatternEncodingPanel(context.menu())),
            new Registration("pattern_access", context -> new PatternAccessPanel(context.style())),
            new Registration("pattern_cache", context -> new PatternCachePanel(context.menu())),
            new Registration("inventory", context -> new InventoryPanel(context.menu())),
            new Registration("wireless_settings",
                    context -> new WirelessSettingsPanel(context.menu(), context.style(), context.dock())),
            new Registration("trash", context -> new TrashPanel(context.menu(), context.screen())),
            new Registration("toolkit", context -> new ToolkitPanel(context.menu())),
            new Registration("network_toolkit", context -> new NetworkToolkitPanel(context.menu())),
            new Registration(HotkeysPanel.ID, context -> new HotkeysPanel(context.style(), context.dock())),
            new Registration("provider_select", context -> com.lhy.mest.compat.plus.PlusScreenSupport.loaded()
                    ? new ProviderSelectPanel(context.style())
                    : null));

    private static final OrderedModuleRegistry<MestModuleFactory> ADDONS =
            new OrderedModuleRegistry<>(BuiltinModules.IDS);
    private static List<Registration> all = BUILTINS;

    private MestClientModules() {
    }

    /** Client setup, on the main thread. */
    static void collect() {
        if (ADDONS.isFrozen()) {
            return;
        }
        ModLoader.get().postEvent(new RegisterMestModulesEvent(ADDONS));
        ADDONS.freeze();
        var list = new ArrayList<>(BUILTINS);
        for (Map.Entry<String, MestModuleFactory> entry : ADDONS.entries()) {
            list.add(new Registration(entry.getKey(), entry.getValue()));
        }
        all = List.copyOf(list);
    }

    public static List<Registration> all() {
        return all;
    }
}
