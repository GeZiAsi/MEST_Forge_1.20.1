package com.lhy.mest.item;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import appeng.api.networking.GridHelper;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;

/**
 * Larger network tool. {@link NetworkToolItem#findNetworkToolInv} picks it up via {@code instanceof},
 * so AE machines attach a ToolboxMenu without mixin. Extra slots live in
 * {@link MestNetworkToolMenuHost}.
 */
public class ItemMestNetworkToolkit extends NetworkToolItem {
    public ItemMestNetworkToolkit(Properties properties) {
        super(properties);
    }

    @Override
    public NetworkToolMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, BlockPos pos) {
        var gridHost = pos == null ? null : GridHelper.getNodeHost(player.level(), pos);
        return new MestNetworkToolMenuHost(player, inventorySlot, stack, gridHost);
    }

    /** AE2's tooltip preview reads its own 9-slot inventory; ours lives under another key. */
    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return Optional.empty();
    }
}
