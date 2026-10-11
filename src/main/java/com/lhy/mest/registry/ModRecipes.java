package com.lhy.mest.registry;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.recipe.MestWutCombineRecipe;

public final class ModRecipes {
    private ModRecipes() {
    }

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MESplicedterminal.MODID);

    public static final RegistryObject<SimpleCraftingRecipeSerializer<MestWutCombineRecipe>> WUT_COMBINE =
            SERIALIZERS.register("wut_combine", () -> new SimpleCraftingRecipeSerializer<>(MestWutCombineRecipe::new));
}
