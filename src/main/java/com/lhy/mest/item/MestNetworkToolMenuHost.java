package com.lhy.mest.item;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.upgrades.Upgrades;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;

import com.lhy.mest.config.MestConfig;

/**
 * Network-tool host whose inventory size comes from config instead of AE2's hardcoded 9.
 *
 * <p>The cards are stored under their own key so AE2's built-in 9-slot inventory (which the parent
 * still creates and saves under {@code "inv"}) can never overwrite them.
 */
public final class MestNetworkToolMenuHost extends NetworkToolMenuHost {
    static final String TAG_INVENTORY = "mest_inv";

    private final AppEngInternalInventory upgrades;

    public MestNetworkToolMenuHost(Player player, @Nullable Integer slot, ItemStack stack,
            @Nullable IInWorldGridNodeHost gridHost) {
        super(player, slot, stack, gridHost);
        this.upgrades = new AppEngInternalInventory(new InternalInventoryHost() {
            @Override
            public void saveChanges() {
                save();
            }

            @Override
            public void onChangeInventory(InternalInventory inv, int changedSlot) {
            }

            @Override
            public boolean isClientSide() {
                return player != null && player.level().isClientSide();
            }
        }, MestConfig.networkToolkitSlots());
        this.upgrades.setEnableClientEvents(true);
        this.upgrades.setFilter(new UpgradeCardFilter());
        if (stack.getTag() != null && stack.getTag().contains(TAG_INVENTORY)) {
            this.upgrades.readFromNBT(stack.getTag(), TAG_INVENTORY);
        }
    }

    private void save() {
        upgrades.writeToNBT(getItemStack().getOrCreateTag(), TAG_INVENTORY);
    }

    @Override
    public InternalInventory getInventory() {
        return upgrades;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return upgrades;
    }

    private static final class UpgradeCardFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.isEmpty() || Upgrades.isUpgradeCardItem(stack.getItem());
        }
    }
}
