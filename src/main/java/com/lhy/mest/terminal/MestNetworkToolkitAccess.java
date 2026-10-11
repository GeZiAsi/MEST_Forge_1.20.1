package com.lhy.mest.terminal;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.AppEngSlot;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;

import com.lhy.mest.compat.MestWtlibSupport;
import com.lhy.mest.config.MestConfig;
import com.lhy.mest.item.ItemMEST;

/** Live network-toolkit inventory stored on the spliced terminal the player is carrying. */
public final class MestNetworkToolkitAccess {
    private MestNetworkToolkitAccess() {
    }

    /**
     * Marks the terminal's own toolbox inventory. Screens use this to tell the terminal's slots
     * apart from a carried vanilla network tool's, which keeps AE2's own panel.
     */
    public static final class TerminalToolboxInventory extends AppEngInternalInventory {
        private TerminalToolboxInventory(InternalInventoryHost host, int size) {
            super(host, size);
        }
    }

    /** True when this toolbox slot belongs to the spliced terminal rather than a vanilla tool. */
    public static boolean isTerminalToolbox(Slot slot) {
        return slot instanceof AppEngSlot appEngSlot
                && appEngSlot.getInventory() instanceof TerminalToolboxInventory;
    }

    /**
     * The spliced terminal in the player's inventory (Curios first through the wtlib locator, then a
     * plain inventory scan). Returns {@code null} when the player carries no terminal.
     */
    @Nullable
    public static InternalInventory inventoryOf(Player player) {
        ItemStack stack = MestWtlibSupport.mestStack(player);
        if (!(stack.getItem() instanceof ItemMEST)) {
            stack = ItemStack.EMPTY;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                ItemStack candidate = player.getInventory().getItem(slot);
                if (candidate.getItem() instanceof ItemMEST) {
                    stack = candidate;
                    break;
                }
            }
        }
        if (stack.isEmpty()) {
            return null;
        }
        TerminalHost host = new TerminalHost(player, stack);
        TerminalToolboxInventory inventory = new TerminalToolboxInventory(host, MestConfig.networkToolkitSlots());
        host.inventory = inventory;
        inventory.setEnableClientEvents(true);
        if (stack.getTag() != null && stack.getTag().contains(MESTMenuHost.TAG_NETWORK_TOOLKIT)) {
            inventory.readFromNBT(stack.getTag(), MESTMenuHost.TAG_NETWORK_TOOLKIT);
        }
        return inventory;
    }

    private static final class TerminalHost implements InternalInventoryHost {
        private final Player player;
        private final ItemStack stack;
        private AppEngInternalInventory inventory;

        private TerminalHost(Player player, ItemStack stack) {
            this.player = player;
            this.stack = stack;
        }

        @Override
        public void saveChanges() {
            if (inventory != null) {
                inventory.writeToNBT(stack.getOrCreateTag(), MESTMenuHost.TAG_NETWORK_TOOLKIT);
            }
        }

        @Override
        public void onChangeInventory(InternalInventory inv, int slot) {
        }

        @Override
        public boolean isClientSide() {
            return player.level().isClientSide();
        }
    }
}
