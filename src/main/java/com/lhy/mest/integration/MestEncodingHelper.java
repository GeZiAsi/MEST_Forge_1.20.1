package com.lhy.mest.integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.math.LongMath;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.helpers.InventoryAction;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.slot.FakeSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.util.CraftingRecipeUtil;

import com.lhy.mest.terminal.MESTMenu;

/** AE2's recipe-encoding algorithm adapted to the combined terminal's menu type. */
public final class MestEncodingHelper {
    private static final Comparator<GridInventoryEntry> ENTRY_COMPARATOR = Comparator
            .comparing(GridInventoryEntry::isCraftable)
            .thenComparing(MestEncodingHelper::isUndamaged)
            .thenComparing(GridInventoryEntry::getStoredAmount);

    private MestEncodingHelper() {
    }

    /** How a recipe should be encoded onto a blank pattern. */
    public enum RecipeKind {
        /** Encoded as a crafting pattern (crafting/stonecutting/smithing modes). */
        CRAFTING,
        /** Encoded as a generic processing pattern. */
        PROCESSING
    }

    /** Outcome of {@link #validate}; non-{@code OK} values map to user-facing transfer errors. */
    public enum ValidationResult {
        OK,
        /** Crafting recipe does not fit the 3x3 pattern grid. */
        RECIPE_TOO_LARGE,
        /** Processing recipe without any usable inputs or outputs. */
        INCOMPATIBLE_RECIPE
    }

    public static boolean isSupportedCraftingRecipe(@Nullable Recipe<?> recipe) {
        if (recipe == null) {
            return false;
        }
        return recipe.getType() == RecipeType.CRAFTING
                || recipe.getType() == RecipeType.STONECUTTING
                || recipe.getType() == RecipeType.SMITHING;
    }

    /**
     * Unified recipe-type decision shared by the EMI and JEI bridges.
     *
     * @param categorizedAsCrafting framework-specific hint that the recipe belongs to the crafting
     *                              category even without a supported backing recipe (EMI passes its
     *                              category check, JEI passes {@code false}).
     */
    public static RecipeKind classifyRecipe(@Nullable Recipe<?> recipe, boolean categorizedAsCrafting) {
        return isSupportedCraftingRecipe(recipe) || categorizedAsCrafting
                ? RecipeKind.CRAFTING
                : RecipeKind.PROCESSING;
    }

    /**
     * Unified pre-encoding validation shared by the EMI and JEI bridges: crafting patterns must fit
     * the 3x3 grid (an unknown backing recipe is allowed), processing patterns need at least one
     * input and one output.
     */
    public static ValidationResult validate(
            RecipeKind kind,
            @Nullable Recipe<?> recipe,
            boolean hasInputs,
            boolean hasOutputs) {
        return validate(kind, recipe == null || recipe.canCraftInDimensions(3, 3), hasInputs, hasOutputs);
    }

    /** MC-free core of {@link #validate}; package-private for unit tests. */
    static ValidationResult validate(
            RecipeKind kind,
            boolean craftingRecipeFits3x3,
            boolean hasInputs,
            boolean hasOutputs) {
        if (kind == RecipeKind.CRAFTING) {
            return craftingRecipeFits3x3
                    ? ValidationResult.OK
                    : ValidationResult.RECIPE_TOO_LARGE;
        }
        return hasInputs && hasOutputs
                ? ValidationResult.OK
                : ValidationResult.INCOMPATIBLE_RECIPE;
    }

    /**
     * Unified encoding dispatch: sends the recipe to the pattern-encoding slots as either a crafting
     * or a processing pattern. Callers are expected to have run {@link #validate} first.
     *
     * <p>The crafting and processing ingredient lists are passed separately because the frameworks
     * shape them differently (e.g. JEI pads the crafting grid to 9 slots but filters empty slots for
     * processing); only the list matching {@code kind} is used.
     */
    public static void encode(
            MESTMenu menu,
            RecipeKind kind,
            @Nullable Recipe<?> recipe,
            List<List<GenericStack>> craftingIngredients,
            List<List<GenericStack>> processingInputs,
            List<GenericStack> processingOutputs,
            Predicate<ItemStack> visiblePredicate) {
        if (kind == RecipeKind.CRAFTING) {
            encodeCraftingRecipe(menu, recipe, craftingIngredients, visiblePredicate);
        } else {
            encodeProcessingRecipe(menu, processingInputs, processingOutputs);
        }
    }

    private static void encodeProcessingRecipe(
            MESTMenu menu,
            List<List<GenericStack>> genericIngredients,
            List<GenericStack> genericResults) {
        menu.setPatternEncodingMode(EncodingMode.PROCESSING);
        var priorities = getIngredientPriorities(menu);
        encodeBestMatchingStacksIntoSlots(genericIngredients, priorities, menu.getProcessingInputSlots());
        encodeBestMatchingStacksIntoSlots(
                genericResults.stream().filter(Objects::nonNull).map(List::of).toList(),
                priorities,
                menu.getProcessingOutputSlots());
    }

    private static void encodeCraftingRecipe(
            MESTMenu menu,
            @Nullable Recipe<?> recipe,
            List<List<GenericStack>> genericIngredients,
            Predicate<ItemStack> visiblePredicate) {
        if (recipe != null && recipe.getType() == RecipeType.STONECUTTING) {
            menu.setPatternEncodingMode(EncodingMode.STONECUTTING);
            menu.setStonecuttingRecipeId(recipe.getId());
        } else if (recipe != null && recipe.getType() == RecipeType.SMITHING) {
            menu.setPatternEncodingMode(EncodingMode.SMITHING_TABLE);
        } else {
            menu.setPatternEncodingMode(EncodingMode.CRAFTING);
        }

        var priorities = getIngredientPriorities(menu);
        var encodedInputs = NonNullList.withSize(menu.getPatternCraftingSlots().length, ItemStack.EMPTY);

        if (recipe != null) {
            var ingredients3x3 = CraftingRecipeUtil.ensure3by3CraftingMatrix(recipe);
            for (int slot = 0; slot < ingredients3x3.size() && slot < encodedInputs.size(); slot++) {
                var ingredient = ingredients3x3.get(slot);
                if (ingredient.isEmpty()) {
                    continue;
                }
                var bestNetworkIngredient = priorities.entrySet().stream()
                        .filter(entry -> entry.getKey() instanceof AEItemKey itemKey && itemKey.matches(ingredient))
                        .max(Map.Entry.comparingByValue())
                        .map(entry -> entry.getKey() instanceof AEItemKey itemKey ? itemKey.toStack() : null);
                var bestIngredient = bestNetworkIngredient.orElseGet(() -> {
                    var candidates = ingredient.getItems();
                    for (var candidate : candidates) {
                        if (visiblePredicate.test(candidate)) {
                            return candidate;
                        }
                    }
                    return candidates.length == 0 ? ItemStack.EMPTY : candidates[0];
                });
                encodedInputs.set(slot, bestIngredient);
            }
        } else {
            for (int slot = 0; slot < genericIngredients.size() && slot < encodedInputs.size(); slot++) {
                var candidates = genericIngredients.get(slot);
                if (candidates == null || candidates.isEmpty()) {
                    continue;
                }
                var bestIngredient = findBestIngredient(priorities, candidates).what();
                if (bestIngredient instanceof AEItemKey itemKey) {
                    encodedInputs.set(slot, itemKey.toStack());
                } else {
                    encodedInputs.set(slot, GenericStack.wrapInItemStack(bestIngredient, 1));
                }
            }
        }

        for (int i = 0; i < encodedInputs.size(); i++) {
            setFilter(menu.getPatternCraftingSlots()[i], encodedInputs.get(i));
        }
        for (var outputSlot : menu.getProcessingOutputSlots()) {
            setFilter(outputSlot, ItemStack.EMPTY);
        }
    }

    private static void encodeBestMatchingStacksIntoSlots(
            List<List<GenericStack>> possibleInputsBySlot,
            Map<AEKey, Integer> priorities,
            FakeSlot[] slots) {
        var encodedInputs = new ArrayList<GenericStack>();
        for (var candidates : possibleInputsBySlot) {
            if (candidates != null && !candidates.isEmpty()) {
                addOrMerge(encodedInputs, findBestIngredient(priorities, candidates));
            }
        }

        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = i < encodedInputs.size()
                    ? GenericStack.wrapInItemStack(encodedInputs.get(i))
                    : ItemStack.EMPTY;
            setFilter(slots[i], stack);
        }
    }

    private static void setFilter(FakeSlot slot, ItemStack stack) {
        NetworkHandler.instance().sendToServer(new InventoryActionPacket(InventoryAction.SET_FILTER, slot.index, stack));
    }

    private static GenericStack findBestIngredient(
            Map<AEKey, Integer> priorities,
            List<GenericStack> possibleIngredients) {
        return possibleIngredients.stream()
                .filter(Objects::nonNull)
                .map(stack -> Pair.of(stack, priorities.getOrDefault(stack.what(), Integer.MIN_VALUE)))
                .max(Comparator.comparingInt(Pair::getRight))
                .map(Pair::getLeft)
                .orElseThrow(() -> new IllegalArgumentException("Recipe slot has no encodable ingredients"));
    }

    private static void addOrMerge(List<GenericStack> stacks, GenericStack newStack) {
        for (int i = 0; i < stacks.size(); i++) {
            var existing = stacks.get(i);
            if (Objects.equals(existing.what(), newStack.what())) {
                long newAmount = LongMath.saturatedAdd(existing.amount(), newStack.amount());
                stacks.set(i, new GenericStack(newStack.what(), newAmount));
                long overflow = newStack.amount() - (newAmount - existing.amount());
                if (overflow > 0) {
                    stacks.add(new GenericStack(newStack.what(), overflow));
                }
                return;
            }
        }
        stacks.add(newStack);
    }

    private static Map<AEKey, Integer> getIngredientPriorities(MESTMenu menu) {
        var result = new HashMap<AEKey, Integer>();
        var repo = menu.getClientRepo();
        if (repo != null) {
            var orderedEntries = repo.getAllEntries().stream()
                    .sorted(ENTRY_COMPARATOR)
                    .map(GridInventoryEntry::getWhat)
                    .toList();
            for (int i = 0; i < orderedEntries.size(); i++) {
                result.put(orderedEntries.get(i), i);
            }
        }
        for (var item : menu.getPlayerInventory().items) {
            var key = AEItemKey.of(item);
            if (key != null) {
                result.putIfAbsent(key, -1);
            }
        }
        return result;
    }

    private static boolean isUndamaged(GridInventoryEntry entry) {
        return !(entry.getWhat() instanceof AEItemKey itemKey) || !itemKey.isDamaged();
    }
}
