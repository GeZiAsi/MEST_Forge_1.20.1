package com.lhy.mest.terminal;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;

import de.mari_023.ae2wtlib.wut.ItemWUT;
import de.mari_023.ae2wtlib.wut.WUTHandler;

import com.lhy.mest.compat.MestWtlibSupport;
import com.lhy.mest.network.ToolkitBarSyncPacket;
import com.lhy.mest.registry.ModItems;

/**
 * Visible 27-cell cycle: left toolkit 0-8, vanilla hotbar, right toolkit 9-17.
 *
 * <p>{@code Inventory.items[0..8]} never moves. Selecting an extra bar only changes which toolkit
 * cell {@code Inventory.getSelected} reports, so vanilla hotbar interaction stays intact.
 *
 * <p>All toolkit state lives on the player ({@link ToolkitDataHolder}); a carried terminal is
 * only required to use it.
 */
public final class ToolkitBarState {
    public enum Bar {
        LEFT,
        CENTER,
        RIGHT
    }

    public static final int BAR_SLOTS = 9;
    public static final int VISIBLE_CELLS = 27;
    public static final int VISIBLE_TOOLKIT_SLOTS = BAR_SLOTS * 2;

    private ToolkitBarState() {}

    public static ToolkitInternalInventory data(Player player) {
        return ((ToolkitDataHolder) player).mest$toolkit();
    }

    public static Bar getBar(Player player) {
        return isBarEnabled(player) ? storedBar(player) : Bar.CENTER;
    }

    static Bar storedBar(Player player) {
        return data(player).page();
    }

    public static int getSlot(Player player) {
        return Math.floorMod(player.getInventory().selected, BAR_SLOTS);
    }

    public static boolean isBarEnabled(Player player) {
        ToolkitInternalInventory data = data(player);
        return data.barEnabled() && data.hasTerminal();
    }

    /** The saved toggle, regardless of whether a terminal is carried right now. */
    public static boolean isBarToggleOn(Player player) {
        return data(player).barEnabled();
    }

    public static boolean isQuickMoveEnabled(Player player) {
        return data(player).quickMove();
    }

    public static boolean hasTerminal(Player player) {
        return data(player).hasTerminal();
    }

    public static void setBarEnabled(Player player, boolean enabled) {
        data(player).setBarEnabled(enabled);
    }

    public static void setQuickMove(Player player, boolean enabled) {
        data(player).setQuickMove(enabled);
    }

    public static boolean isToolkitSelected(Player player) {
        return isBarEnabled(player) && getBar(player) != Bar.CENTER;
    }

    public static int visibleIndex(Player player) {
        return toIndex(getBar(player), getSlot(player));
    }

    public static int toIndex(Bar bar, int slot) {
        return bar.ordinal() * BAR_SLOTS + Math.floorMod(slot, BAR_SLOTS);
    }

    public static Bar barOf(int index) {
        int normalized = Math.floorMod(index, VISIBLE_CELLS);
        return Bar.values()[normalized / BAR_SLOTS];
    }

    public static int slotOf(int index) {
        return Math.floorMod(index, BAR_SLOTS);
    }

    public static int cycleIndex(int current, int delta) {
        return Math.floorMod(current - delta, VISIBLE_CELLS);
    }

    public static int toolkitIndex(Player player) {
        return toolkitIndex(getBar(player), getSlot(player));
    }

    public static int toolkitIndex(Bar bar, int slot) {
        if (bar == Bar.CENTER) {
            return -1;
        }
        int normalized = Math.floorMod(slot, BAR_SLOTS);
        return bar == Bar.LEFT ? normalized : BAR_SLOTS + normalized;
    }

    /** Server-originated or client-local selection; a server change is pushed to the client. */
    public static void setSelection(Player player, Bar bar, int slot) {
        setSelection(player, bar, slot, true);
    }

    /** Applies a selection the client already made and shows, without echoing it back. */
    public static void applyClientSelection(ServerPlayer player, Bar bar, int slot) {
        setSelection(player, bar, slot, false);
    }

    private static void setSelection(Player player, Bar bar, int slot, boolean notifyClient) {
        player.getInventory().selected = Math.floorMod(slot, BAR_SLOTS);
        Bar target = bar == Bar.CENTER || !isBarEnabled(player) ? Bar.CENTER : bar;
        data(player).setPage(target, notifyClient);
    }

    public static boolean mayStore(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getMaxStackSize() <= 1
                && !isTerminalCarrier(stack);
    }

    public static InternalInventory asInventory(Player player) {
        return data(player);
    }

    public static InternalInventory memoryInventory(Player player) {
        return data(player).memoryInventory();
    }

    public static ItemStack stackAt(Player player, int index) {
        return data(player).liveStack(index);
    }

    public static ItemStack memoryAt(Player player, int index) {
        return data(player).memoryAt(index);
    }

    /** Live toolkit cell for the current extra-bar selection, or empty. */
    public static ItemStack selectedStack(Player player) {
        int index = toolkitIndex(player);
        return isValidToolkitIndex(index) ? stackAt(player, index) : ItemStack.EMPTY;
    }

    /**
     * Selects the extra-bar or vanilla hotbar cell that already holds {@code stack}.
     *
     * @return {@code TOOLKIT} if an extra-bar cell matched, {@code HOTBAR} if a vanilla hotbar cell
     *     matched, otherwise {@code NONE}
     */
    public static PickMatch selectMatchingItem(Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return PickMatch.NONE;
        }
        if (isToolkitSelected(player)
                && ItemStack.isSameItemSameTags(selectedStack(player), stack)) {
            return PickMatch.TOOLKIT;
        }
        for (int index = 0; index < VISIBLE_TOOLKIT_SLOTS; index++) {
            if (ItemStack.isSameItemSameTags(stackAt(player, index), stack)) {
                setSelection(player, index < BAR_SLOTS ? Bar.LEFT : Bar.RIGHT, index % BAR_SLOTS);
                return PickMatch.TOOLKIT;
            }
        }
        Inventory inventory = player.getInventory();
        int slot = inventory.findSlotMatchingItem(stack);
        if (slot != -1) {
            setSelection(player, Bar.CENTER, Inventory.isHotbarSlot(slot) ? slot : inventory.selected);
            return PickMatch.HOTBAR;
        }
        return PickMatch.NONE;
    }

    public enum PickMatch {
        NONE,
        TOOLKIT,
        HOTBAR
    }

    public static void setSelectedStack(Player player, ItemStack stack) {
        int index = toolkitIndex(player);
        if (!isValidToolkitIndex(index)) {
            return;
        }
        if (!stack.isEmpty() && !mayStore(stack)) {
            // Vanilla just wrote a stack the bar cannot hold, for example the armor it displaced
            // when equipping something else. Empty the cell first so the old item cannot linger as
            // a second copy next to the equipped one, then hand the stack back to the player
            // instead of dropping it on the floor of this method.
            data(player).setItemDirect(index, ItemStack.EMPTY);
            ItemStack remainder = stack.copy();
            if (!player.getInventory().add(remainder)) {
                player.drop(remainder, false);
            }
            return;
        }
        data(player).setItemDirect(index, stack);
    }

    /**
     * Puts a picked-up unstackable item into the first empty remembered slot of the same type.
     * Shrinks {@code stack} in place when accepted.
     *
     * @return {@code true} if the stack was fully moved into a memory slot
     */
    public static boolean tryInsertIntoMemory(Player player, ItemStack stack) {
        if (!mayStore(stack)) {
            return false;
        }
        ToolkitInternalInventory toolkit = data(player);
        if (!toolkit.hasTerminal()) {
            return false;
        }
        for (int i = 0; i < toolkit.size(); i++) {
            ItemStack remembered = toolkit.memoryAt(i);
            if (remembered.isEmpty() || !remembered.is(stack.getItem()) || !toolkit.liveStack(i).isEmpty()) {
                continue;
            }
            toolkit.setItemDirect(i, stack.copy());
            stack.setCount(0);
            return true;
        }
        return false;
    }

    /** Server tick: return overflow cells and push changed cells to the client. */
    public static void tick(ServerPlayer player) {
        data(player).returnOverflow();
        ToolkitBarSyncPacket.send(player);
    }

    public static void applyClientSync(Player player, List<ItemStack> visible, ToolkitBarSyncPacket.Settings settings) {
        data(player).applyServerSync(visible, settings);
    }

    public static boolean isValidToolkitIndex(int index) {
        return index >= 0 && index < VISIBLE_TOOLKIT_SLOTS;
    }

    /** @return whether any terminal is carried */
    static boolean scanTerminals(Player player) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (isTerminalCarrier(inventory.getItem(i))) {
                return true;
            }
        }
        return isTerminalCarrier(MestWtlibSupport.mestStack(player));
    }

    public static boolean isTerminalCarrier(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(ModItems.SPLICED_TERMINAL.get())
                        || stack.getItem() instanceof ItemWUT
                                && WUTHandler.hasTerminal(stack, MestTerminal.TERMINAL_NAME));
    }
}
