package com.lhy.mest.recipe;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import de.mari_023.ae2wtlib.AE2wtlib;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import de.mari_023.ae2wtlib.wut.WTDefinition;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import de.mari_023.ae2wtlib.wut.recipe.Common;

import com.lhy.mest.item.ItemMEST;
import com.lhy.mest.registry.ModItems;
import com.lhy.mest.registry.ModRecipes;
import com.lhy.mest.terminal.MestTerminal;

/**
 * Shapeless merge of the spliced terminal with any other AE2WTLib wireless terminal into a WUT.
 * Mirrors {@code ae2wtlib:combine} but matches every registered terminal definition at runtime.
 */
public final class MestWutCombineRecipe extends CustomRecipe {
    public MestWutCombineRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer input, Level level) {
        return findInputs(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registries) {
        Inputs inputs = findInputs(input);
        if (inputs == null) {
            return ItemStack.EMPTY;
        }
        ItemStack wut = new ItemStack(AE2wtlib.UNIVERSAL_TERMINAL);
        wut = Common.mergeTerminal(wut, inputs.mest().copy(), MestTerminal.TERMINAL_NAME);
        return Common.mergeTerminal(wut, inputs.other().copy(), inputs.otherName());
    }

    @Nullable
    private static Inputs findInputs(CraftingContainer input) {
        ItemStack mest = ItemStack.EMPTY;
        ItemStack other = ItemStack.EMPTY;
        String otherName = null;
        int count = 0;
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            count++;
            if (stack.getItem() instanceof ItemWUT) {
                return null;
            }
            if (stack.getItem() instanceof ItemMEST) {
                if (!mest.isEmpty()) {
                    return null;
                }
                mest = stack;
                continue;
            }
            String name = WUTHandler.getCurrentTerminal(stack);
            if (name.isEmpty() || !other.isEmpty()) {
                return null;
            }
            other = stack;
            otherName = name;
        }
        if (count != 2 || mest.isEmpty() || other.isEmpty()) {
            return null;
        }
        return new Inputs(mest, other, otherName);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registries) {
        return new ItemStack(AE2wtlib.UNIVERSAL_TERMINAL);
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(ModItems.SPLICED_TERMINAL.get()));
        ItemStack[] others = WUTHandler.wirelessTerminals.values().stream()
                .map(WTDefinition::item)
                .filter(item -> !(item instanceof ItemMEST))
                .filter(item -> item instanceof net.minecraft.world.level.ItemLike)
                .map(item -> new ItemStack((net.minecraft.world.level.ItemLike) item))
                .toArray(ItemStack[]::new);
        if (others.length > 0) {
            ingredients.add(Ingredient.of(others));
        }
        return ingredients;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.WUT_COMBINE.get();
    }

    private record Inputs(ItemStack mest, ItemStack other, String otherName) {
    }
}
