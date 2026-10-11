package com.lhy.mest.terminal;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import com.lhy.mest.network.ToolkitBarSyncPacket;

/**
 * Toolkit quick-bar actions.
 *
 * <p>The toolkit belongs to the player (see {@link ToolkitBarState}). Extra bars only change the
 * selected page; {@code Inventory.selected} is the slot inside that page.
 */
public final class ToolkitBarActions {
    private ToolkitBarActions() {}

    public static void setBarEnabled(ServerPlayer player, boolean enabled) {
        if (!ToolkitBarState.hasTerminal(player)) {
            return;
        }
        ToolkitBarState.setBarEnabled(player, enabled);
        if (!enabled) {
            ToolkitBarState.setSelection(
                    player, ToolkitBarState.Bar.CENTER, player.getInventory().selected);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        ToolkitBarSyncPacket.send(player);
    }

    /** Drops the selected extra-bar cell without touching vanilla hotbar slots. */
    public static void dropSelected(ServerPlayer player, boolean all) {
        if (!ToolkitBarState.isToolkitSelected(player)) {
            return;
        }
        ItemStack selected = ToolkitBarState.selectedStack(player);
        if (selected.isEmpty() || !selected.onDroppedByPlayer(player)) {
            return;
        }
        int count = all ? selected.getCount() : Math.min(1, selected.getCount());
        ItemStack dropped = selected.copyWithCount(count);
        if (dropped.isEmpty()) {
            return;
        }
        selected.shrink(dropped.getCount());
        ToolkitBarState.setSelectedStack(player, selected.isEmpty() ? ItemStack.EMPTY : selected);
        player.drop(dropped, true);
    }

    /**
     * Right-click equip from the extra bar, without vanilla {@code copyAndClear} on the live cell.
     *
     * <p>Returns {@code null} when the held item is not equipment, so the caller leaves vanilla
     * {@code use()} alone. Otherwise the result matches vanilla: pass if the slot cannot be used,
     * fail if the stacks already match or the worn piece is bound, success when the swap can run.
     * {@code apply} is false on the client so prediction cannot empty the diamond and write the
     * displaced quantum piece into the toolkit before the server agrees.
     *
     * <p>Creative keeps the hand item only when the armor slot is empty, same as vanilla. Swapping
     * onto an occupied slot still returns the worn piece; otherwise an upgraded quantum armor
     * would be deleted.
     */
    @Nullable
    public static InteractionResult equipFromSelected(Player player, boolean apply) {
        ItemStack hand = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (hand.isEmpty() || !(hand.getItem() instanceof Equipable)) {
            return null;
        }
        EquipmentSlot slot = net.minecraft.world.entity.LivingEntity.getEquipmentSlotForItem(hand);
        ItemStack worn = player.getItemBySlot(slot);
        if (ItemStack.matches(hand, worn) || preventsArmorChange(player, worn)) {
            return InteractionResult.FAIL;
        }
        if (!apply) {
            return sidedSuccess(player);
        }
        ItemStack equipped = hand.copy();
        ItemStack displaced = worn.isEmpty() ? ItemStack.EMPTY : worn.copy();
        boolean keepHand = player.isCreative() && worn.isEmpty();
        player.setItemSlot(slot, equipped);
        if (!keepHand) {
            ToolkitBarState.setSelectedStack(player, displaced);
        }
        player.awardStat(Stats.ITEM_USED.get(equipped.getItem()));
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.inventoryMenu.broadcastChanges();
            ToolkitBarSyncPacket.send(serverPlayer, true);
        }
        return sidedSuccess(player);
    }

    private static boolean preventsArmorChange(Player player, ItemStack worn) {
        return !worn.isEmpty()
                && !player.isCreative()
                && EnchantmentHelper.hasBindingCurse(worn);
    }

    /** Vanilla {@code InteractionResultHolder.sidedSuccess}: swing on the client, consume on the server. */
    private static InteractionResult sidedSuccess(Player player) {
        return player.level().isClientSide() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
    }

    public static void selectVisibleCell(ServerPlayer player, int index) {
        if (!ToolkitBarState.isBarEnabled(player) || index < 0 || index >= ToolkitBarState.VISIBLE_CELLS) {
            return;
        }
        ToolkitBarState.applyClientSelection(player, ToolkitBarState.barOf(index), ToolkitBarState.slotOf(index));
    }

    /**
     * A click on an extra-bar cell only selects it, like a vanilla hotbar slot.
     * Toolkit index 0-8 is the left bar, 9-17 the right bar.
     */
    public static void selectToolkitCell(ServerPlayer player, int toolkitIndex) {
        if (toolkitIndex < 0 || toolkitIndex >= ToolkitBarState.BAR_SLOTS * 2) {
            return;
        }
        int visible = toolkitIndex < ToolkitBarState.BAR_SLOTS
                ? toolkitIndex
                : toolkitIndex + ToolkitBarState.BAR_SLOTS;
        selectVisibleCell(player, visible);
    }
}
