package com.lhy.mest.integration;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;

/**
 * Crafting-grid slot of every ingredient of a crafting recipe, as JEI lays it out. JEI added
 * {@code IRecipeTransferHandlerHelper#getGuiSlotIndexToIngredientMap} after 15.20, which GTL
 * ships, so this is AE2 15's own {@code UseCraftingRecipeTransfer} mapping.
 */
final class MestCraftingSlots {
    private MestCraftingSlots() {
    }

    static Map<Integer, Ingredient> guiSlotToIngredient(Recipe<?> recipe) {
        var ingredients = recipe.getIngredients();
        // JEI centres shapeless recipes smaller than 3x3 and aligns some to the bottom (slabs).
        int width;
        int height;
        if (recipe instanceof ShapedRecipe shaped) {
            width = shaped.getWidth();
            height = shaped.getHeight();
        } else if (ingredients.size() > 4) {
            width = height = 3;
        } else if (ingredients.size() > 1) {
            width = height = 2;
        } else {
            width = height = 1;
        }
        var result = new LinkedHashMap<Integer, Ingredient>(ingredients.size());
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            if (!ingredient.isEmpty()) {
                result.put(craftingIndex(i, width, height), ingredient);
            }
        }
        return result;
    }

    private static int craftingIndex(int i, int width, int height) {
        if (width == 1) {
            return height >= 2 ? i * 3 + 1 : 4;
        }
        if (height == 1) {
            return i + 3;
        }
        if (width == 2) {
            int index = i;
            if (i > 1) {
                index++;
                if (i > 3) {
                    index++;
                }
            }
            return index;
        }
        if (height == 2) {
            return i + 3;
        }
        return i;
    }
}
