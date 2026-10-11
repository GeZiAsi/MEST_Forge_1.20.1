package com.lhy.mest.compat;

import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.menu.slot.RestrictedInputSlot;

import com.lhy.mest.compat.plus.PlusEncodingUpload;

/**
 * Auto-uploads non-processing encoded patterns after encode (no Shift): ECO
 * PatternContainers first, then EAEP assembler matrix, then other crafting-assembler
 * PatternContainers (ExtendedAE assembler matrix, etc.). Duplicate checks run
 * before any insert.
 */
public final class MestCraftingPatternAutoUpload {
    static final Set<String> FALLBACK_CRAFTING_GROUP_IDS = Set.of(
            "ae2:molecular_assembler",
            "extendedae:ex_molecular_assembler",
            "extendedae:assembler_matrix_pattern",
            "extendedae_plus:assembler_matrix_pattern_plus",
            "neoecoae:crafting_system_l4",
            "neoecoae:crafting_system_l6",
            "neoecoae:crafting_system_l9",
            "ae2cs:meteorite_pattern_provider");

    private MestCraftingPatternAutoUpload() {
    }

    public static boolean tryUpload(ServerPlayer player, RestrictedInputSlot encodedSlot, IGrid grid) {
        if (player == null || encodedSlot == null || grid == null) {
            return false;
        }
        ItemStack stack = encodedSlot.getItem();
        if (stack.isEmpty() || !PatternDetailsHelper.isEncodedPattern(stack)) {
            return false;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(stack, player.level());
        if (details == null || details.supportsPushInputsToExternalInventory()) {
            return false;
        }
        if (hasDuplicate(player, grid, stack)) {
            player.displayClientMessage(
                    Component.translatable("gui.mesplicedterminal.pattern_upload.duplicate"), true);
            return false;
        }
        if (insertIntoCraftingContainer(grid, stack, MestCraftingPatternAutoUpload::isEcoCraftingGroup)) {
            clearEncoded(encodedSlot);
            return true;
        }
        if (PlusEncodingUpload.uploadEncodedToMatrix(player, encodedSlot, grid)) {
            return true;
        }
        if (insertIntoCraftingContainer(grid, stack, group -> !isEcoCraftingGroup(group))) {
            clearEncoded(encodedSlot);
            return true;
        }
        return false;
    }

    private static void clearEncoded(RestrictedInputSlot encodedSlot) {
        encodedSlot.set(ItemStack.EMPTY);
        encodedSlot.setChanged();
    }

    private static boolean hasDuplicate(ServerPlayer player, IGrid grid, ItemStack stack) {
        Level level = player.level();
        for (PatternContainer container : craftingContainers(grid)) {
            InternalInventory inventory = container.getTerminalPatternInventory();
            if (inventory != null && containsDuplicate(inventory, stack, level)) {
                return true;
            }
        }
        return false;
    }

    private static boolean insertIntoCraftingContainer(
            IGrid grid, ItemStack stack, Predicate<PatternContainerGroup> groupFilter) {
        ItemStack toInsert = stack.copy();
        for (PatternContainer container : craftingContainers(grid)) {
            if (!groupFilter.test(container.getTerminalGroup())) {
                continue;
            }
            InternalInventory inventory = container.getTerminalPatternInventory();
            if (inventory == null) {
                continue;
            }
            ItemStack remain = inventory.addItems(toInsert);
            if (remain.getCount() < toInsert.getCount()) {
                return true;
            }
        }
        return false;
    }

    static boolean isEcoCraftingGroup(PatternContainerGroup group) {
        if (group == null) {
            return false;
        }
        AEItemKey icon = group.icon();
        return icon != null && "neoecoae".equals(icon.getId().getNamespace());
    }

    private static Iterable<PatternContainer> craftingContainers(IGrid grid) {
        java.util.ArrayList<PatternContainer> result = new java.util.ArrayList<>();
        java.util.IdentityHashMap<PatternContainer, Boolean> seen = new java.util.IdentityHashMap<>();
        for (Class<?> machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Class<? extends PatternContainer> containerClass = (Class<? extends PatternContainer>) machineClass;
            for (PatternContainer container : grid.getActiveMachines(containerClass)) {
                if (container == null || seen.put(container, Boolean.TRUE) != null
                        || container.getGrid() != grid
                        || !isCraftingUploadGroup(container.getTerminalGroup())) {
                    continue;
                }
                result.add(container);
            }
        }
        return result;
    }

    static boolean isCraftingUploadGroup(PatternContainerGroup group) {
        if (group == null) {
            return false;
        }
        AEItemKey icon = group.icon();
        return icon != null && FALLBACK_CRAFTING_GROUP_IDS.contains(icon.getId().toString());
    }

    static boolean containsDuplicate(InternalInventory inventory, ItemStack candidate, Level level) {
        return containsEquivalentPattern(inventory, candidate, level);
    }

    static boolean containsEquivalentPattern(InternalInventory inventory, ItemStack candidate, Level level) {
        if (inventory == null || candidate == null || candidate.isEmpty()) {
            return false;
        }
        ItemStack strippedCandidate = stripEncodePlayer(candidate);
        IPatternDetails candidateDetails = PatternDetailsHelper.decodePattern(candidate, level);
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            if (stored.isEmpty()) {
                continue;
            }
            if (ItemStack.isSameItemSameTags(stripEncodePlayer(stored), strippedCandidate)) {
                return true;
            }
            IPatternDetails storedDetails = PatternDetailsHelper.decodePattern(stored, level);
            if (storedDetails != null && candidateDetails != null
                    && java.util.Objects.equals(storedDetails.getDefinition(), candidateDetails.getDefinition())) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack stripEncodePlayer(ItemStack stack) {
        ItemStack copy = stack.copy();
        if (copy.getTag() != null) {
            copy.getTag().remove("encodePlayer");
        }
        return copy;
    }
}
