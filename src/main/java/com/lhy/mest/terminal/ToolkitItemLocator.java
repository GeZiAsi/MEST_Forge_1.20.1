package com.lhy.mest.terminal;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import appeng.menu.locator.MenuLocator;

import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;

import com.lhy.mest.item.ItemMestNetworkToolkit;
import com.lhy.mest.item.MestNetworkToolMenuHost;

/**
 * Resolves an AE2 item menu against a toolkit cell instead of {@code Inventory.items[selected]}.
 *
 * <p>With an extra-bar cell selected, {@code MenuLocators.forHand} would otherwise point at the
 * vanilla hotbar slot. Routing those two factory methods here keeps wrenches, network tools and
 * similar menu items on the toolkit stack.
 *
 * <p>AE2 15's {@code IMenuItem.getMenuHost} wants a player-inventory slot and later re-validates
 * the stack against it, which a toolkit cell cannot satisfy. Hosts are therefore built here with no
 * slot for the item types that support it (network tools, AE2WTLib terminals).
 */
public record ToolkitItemLocator(int toolkitIndex, @Nullable BlockHitResult hit) implements MenuLocator {
    public ToolkitItemLocator(int toolkitIndex) {
        this(toolkitIndex, null);
    }

    @Nullable
    public static MenuLocator forHand(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !ToolkitBarState.isToolkitSelected(player)) {
            return null;
        }
        int index = ToolkitBarState.toolkitIndex(player);
        if (!ToolkitBarState.isValidToolkitIndex(index)
                || ToolkitBarState.stackAt(player, index).isEmpty()) {
            return null;
        }
        return new ToolkitItemLocator(index);
    }

    @Nullable
    public static MenuLocator forUse(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return null;
        }
        MenuLocator locator = forHand(player, context.getHand());
        if (!(locator instanceof ToolkitItemLocator toolkit)) {
            return null;
        }
        return new ToolkitItemLocator(toolkit.toolkitIndex(), hitResult(context));
    }

    public ItemStack locateItem(Player player) {
        return ToolkitBarState.stackAt(player, toolkitIndex);
    }

    @Nullable
    @Override
    public <T> T locate(Player player, Class<T> hostInterface) {
        ItemStack stack = locateItem(player);
        if (stack.isEmpty()) {
            return null;
        }
        Object host = null;
        if (stack.getItem() instanceof NetworkToolItem) {
            BlockPos pos = hit == null ? null : hit.getBlockPos();
            IInWorldGridNodeHost gridHost = pos == null ? null : GridHelper.getNodeHost(player.level(), pos);
            host = stack.getItem() instanceof ItemMestNetworkToolkit
                    ? new MestNetworkToolMenuHost(player, null, stack, gridHost)
                    : new NetworkToolMenuHost(player, null, stack, gridHost);
        } else if (stack.getItem() instanceof IUniversalWirelessTerminalItem terminal) {
            host = terminal.getMenuHost(player, this, stack);
        }
        return hostInterface.isInstance(host) ? hostInterface.cast(host) : null;
    }

    public void writeToPacket(FriendlyByteBuf buf) {
        buf.writeVarInt(toolkitIndex);
        buf.writeBoolean(hit != null);
        if (hit != null) {
            buf.writeBlockHitResult(hit);
        }
    }

    public static ToolkitItemLocator readFromPacket(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        BlockHitResult hit = buf.readBoolean() ? buf.readBlockHitResult() : null;
        return new ToolkitItemLocator(index, hit);
    }

    private static BlockHitResult hitResult(UseOnContext context) {
        return new BlockHitResult(
                context.getClickLocation(),
                context.getClickedFace(),
                context.getClickedPos(),
                context.isInside());
    }
}
