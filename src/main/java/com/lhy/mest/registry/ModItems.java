package com.lhy.mest.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.item.ItemMEST;
import com.lhy.mest.item.ItemMestNetworkToolkit;

/**
 * Item and creative-tab registration for the ME Spliced Terminal.
 */
public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MESplicedterminal.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MESplicedterminal.MODID);

    private static ItemMEST splicedTerminalItem;

    /**
     * Create the item only while the item registry is unfrozen (the ITEM RegisterEvent).
     * The DeferredRegister binds this same instance, and the AE2WTLib terminal definition uses it.
     */
    public static ItemMEST splicedTerminalItem() {
        if (splicedTerminalItem == null) {
            splicedTerminalItem = new ItemMEST();
        }
        return splicedTerminalItem;
    }

    public static final RegistryObject<ItemMEST> SPLICED_TERMINAL =
            ITEMS.register("spliced_terminal", ModItems::splicedTerminalItem);

    public static final RegistryObject<ItemMestNetworkToolkit> NETWORK_TOOLKIT =
            ITEMS.register("network_toolkit", () -> new ItemMestNetworkToolkit(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<CreativeModeTab> TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MESplicedterminal.MODID))
                    .icon(() -> SPLICED_TERMINAL.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(SPLICED_TERMINAL.get());
                        output.accept(NETWORK_TOOLKIT.get());
                    })
                    .build());
}
