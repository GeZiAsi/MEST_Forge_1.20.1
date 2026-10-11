package com.lhy.mest.registry;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.terminal.MESTMenu;
import com.lhy.mest.terminal.MestMagnetMenu;

/**
 * Menu type registration. The {@link MenuType} itself is built (unregistered) inside {@link MESTMenu}
 * via {@link com.lhy.mest.terminal.MestMenuTypes}; here we bind it into the game's menu registry.
 */
public final class ModMenus {
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MESplicedterminal.MODID);

    public static final RegistryObject<MenuType<MESTMenu>> SPLICED_TERMINAL_MENU =
            MENUS.register("spliced_terminal", () -> MESTMenu.TYPE);

    public static final RegistryObject<MenuType<MestMagnetMenu>> MAGNET_MENU =
            MENUS.register("magnet", () -> MestMagnetMenu.TYPE);
}
