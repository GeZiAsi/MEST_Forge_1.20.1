package com.lhy.mest.terminal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.stacks.AEKey;
import appeng.util.ConfigInventory;
import appeng.util.prioritylist.IPartitionList;

/**
 * Magnet filter config stored on the MEST item, using the same NBT layout as AE2WTLib's
 * {@code MagnetHost} (which is hard-wired to the crafting terminal).
 */
public final class MestMagnetHost {
    private static final String PICKUP_CONFIG = "pickupConfig";
    private static final String INSERT_CONFIG = "insertConfig";
    private static final String PICKUP_MODE = "pickupMode";
    private static final String INSERT_MODE = "insertMode";

    public final ConfigInventory pickupConfig = ConfigInventory.configTypes(27, this::updatePickupFilter);
    public final ConfigInventory insertConfig = ConfigInventory.configTypes(27, this::updateInsertFilter);

    private IPartitionList pickupFilter;
    private IPartitionList insertFilter;
    private final ItemStack stack;

    public MestMagnetHost(ItemStack stack) {
        this.stack = stack;
        CompoundTag tag = tag();
        pickupConfig.readFromChildTag(tag, PICKUP_CONFIG);
        insertConfig.readFromChildTag(tag, INSERT_CONFIG);
        pickupFilter = createFilter(pickupConfig);
        insertFilter = createFilter(insertConfig);
    }

    private CompoundTag tag() {
        return stack.getOrCreateTag();
    }

    private IPartitionList createFilter(ConfigInventory config) {
        IPartitionList.Builder builder = IPartitionList.builder();
        builder.fuzzyMode(FuzzyMode.IGNORE_ALL);
        for (int i = 0; i < config.size(); i++) {
            builder.add(config.getKey(i));
        }
        return builder.build();
    }

    private void updatePickupFilter() {
        // ConfigInventory fires its listener while being constructed, before the stack is set.
        if (stack == null) {
            return;
        }
        pickupFilter = createFilter(pickupConfig);
        pickupConfig.writeToChildTag(tag(), PICKUP_CONFIG);
    }

    private void updateInsertFilter() {
        if (stack == null) {
            return;
        }
        insertFilter = createFilter(insertConfig);
        insertConfig.writeToChildTag(tag(), INSERT_CONFIG);
    }

    public IPartitionList getPickupFilter() {
        return pickupFilter;
    }

    public IncludeExclude getPickupMode() {
        return tag().getBoolean(PICKUP_MODE) ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
    }

    public void togglePickupMode() {
        tag().putBoolean(PICKUP_MODE, getPickupMode() != IncludeExclude.WHITELIST);
    }

    public IPartitionList getInsertFilter() {
        return insertFilter;
    }

    public IncludeExclude getInsertMode() {
        return tag().getBoolean(INSERT_MODE) ? IncludeExclude.WHITELIST : IncludeExclude.BLACKLIST;
    }

    public void toggleInsertMode() {
        tag().putBoolean(INSERT_MODE, getInsertMode() != IncludeExclude.WHITELIST);
    }

    public void copyUp() {
        pickupConfig.readFromChildTag(tag(), INSERT_CONFIG);
    }

    public void copyDown() {
        insertConfig.readFromChildTag(tag(), PICKUP_CONFIG);
    }

    public void switchInsertPickup() {
        CompoundTag tag = tag();
        pickupConfig.writeToChildTag(tag, INSERT_CONFIG);
        insertConfig.writeToChildTag(tag, PICKUP_CONFIG);
        pickupConfig.readFromChildTag(tag, PICKUP_CONFIG);
        insertConfig.readFromChildTag(tag, INSERT_CONFIG);
    }

    public boolean matchesPickup(AEKey key) {
        return matches(pickupFilter, getPickupMode(), key);
    }

    public boolean matchesInsert(AEKey key) {
        return matches(insertFilter, getInsertMode(), key);
    }

    private static boolean matches(IPartitionList filter, IncludeExclude mode, AEKey key) {
        if (key == null) {
            return false;
        }
        if (filter.isEmpty()) {
            return mode == IncludeExclude.BLACKLIST;
        }
        boolean listed = filter.isListed(key);
        return mode == IncludeExclude.WHITELIST ? listed : !listed;
    }
}
