package com.lhy.mest.terminal;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.network.NetworkHooks;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;

/**
 * AE2 15's {@code MenuTypeBuilder.build} forces the {@code ae2:} namespace and queues the type into
 * AE2's own registration. This mirrors its open/decode logic for menus registered under our own id.
 */
public final class MestMenuTypes {
    private MestMenuTypes() {
    }

    @FunctionalInterface
    public interface Factory<M extends AEBaseMenu, H> {
        M create(int containerId, Inventory inventory, H host);
    }

    public static <M extends AEBaseMenu, H> MenuType<M> create(ResourceLocation id, Factory<M, H> factory,
            Class<H> hostClass) {
        MenuType<M> type = IForgeMenuType.create((containerId, inventory, buf) -> {
            MenuLocator locator = MenuLocators.readFromPacket(buf);
            H host = locator.locate(inventory.player, hostClass);
            if (host == null) {
                var connection = Minecraft.getInstance().getConnection();
                if (connection != null) {
                    connection.send(new ServerboundContainerClosePacket(containerId));
                }
                throw new IllegalStateException("Couldn't find menu host at " + locator + " for " + id
                        + " on client. Closing menu.");
            }
            M menu = factory.create(containerId, inventory, host);
            menu.setReturnedFromSubScreen(buf.readBoolean());
            return menu;
        });
        MenuOpener.addOpener(type, (player, locator, fromSubMenu) -> open(player, locator, fromSubMenu,
                factory, hostClass));
        return type;
    }

    private static <M extends AEBaseMenu, H> boolean open(Player player, MenuLocator locator, boolean fromSubMenu,
            Factory<M, H> factory, Class<H> hostClass) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        H host = locator.locate(player, hostClass);
        if (host == null) {
            return false;
        }
        Component title = host instanceof ItemMenuHost itemHost
                ? itemHost.getItemStack().getHoverName()
                : Component.empty();
        var provider = new SimpleMenuProvider((containerId, inventory, p) -> {
            M menu = factory.create(containerId, inventory, host);
            menu.setLocator(locator);
            return menu;
        }, title);
        NetworkHooks.openScreen(serverPlayer, provider, buf -> {
            MenuLocators.writeToPacket(buf, locator);
            buf.writeBoolean(fromSubMenu);
        });
        return true;
    }
}
