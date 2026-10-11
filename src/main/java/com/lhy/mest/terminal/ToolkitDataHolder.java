package com.lhy.mest.terminal;

/**
 * Implemented on {@code Player} by {@code ToolkitPlayerDataMixin}. Replaces the NeoForge attachment
 * the 1.21 build used: a plain field is cheap enough for {@code Inventory.getSelected} and exists
 * from the moment the player is constructed, before Forge capabilities are attached.
 */
public interface ToolkitDataHolder {
    ToolkitInternalInventory mest$toolkit();
}
