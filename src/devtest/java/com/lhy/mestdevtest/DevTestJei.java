package com.lhy.mestdevtest;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.runtime.IJeiRuntime;

import com.lhy.mest.integration.MestPullItemsSupport;
import com.lhy.mest.integration.MestRecipeTransferContext;
import com.lhy.mest.terminal.MESTMenu;

/**
 * Drives MEST's JEI transfer handlers the way JEI's "+" button does. Only loaded by JEI itself,
 * so the GTL run (which leaves JEI out) never touches it.
 */
@JeiPlugin
public class DevTestJei implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(MestDevTest.MODID, "jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    static boolean available() {
        return runtime != null;
    }

    /** Transfers the vanilla crafting-table recipe into the given target of the open terminal. */
    static void transferCraftingTable(Minecraft mc, MestRecipeTransferContext.Target target) {
        if (runtime == null || !(mc.player.containerMenu instanceof MESTMenu menu)) {
            MestDevTest.log("FAIL jei transfer: runtime={} menu={}", runtime, mc.player.containerMenu);
            return;
        }
        MestRecipeTransferContext.select(menu, target);
        // With both recipe modules shown, "+" encodes and a separate button pulls into the grid;
        // turning the pull button off routes "+" by the selected target instead.
        boolean pullButton = MestPullItemsSupport.enabled();
        MestPullItemsSupport.setEnabled(false);
        try {
            transfer(mc, menu, target);
        } finally {
            MestPullItemsSupport.setEnabled(pullButton);
        }
    }

    private static void transfer(Minecraft mc, MESTMenu menu, MestRecipeTransferContext.Target target) {
        var recipe = mc.level.getRecipeManager().byKey(new ResourceLocation("minecraft", "crafting_table"))
                .filter(r -> r instanceof CraftingRecipe)
                .map(r -> (CraftingRecipe) r)
                .orElse(null);
        var recipes = runtime.getRecipeManager();
        var category = recipes.getRecipeCategory(RecipeTypes.CRAFTING);
        var layout = recipe == null ? null : recipes.createRecipeLayoutDrawable(category, recipe,
                runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElse(null);
        var handler = runtime.getRecipeTransferManager().getRecipeTransferHandler(menu, category).orElse(null);
        if (layout == null || handler == null) {
            MestDevTest.log("FAIL jei transfer setup: recipe={} layout={} handler={}", recipe, layout, handler);
            return;
        }
        var check = handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), mc.player, false, false);
        var error = handler.transferRecipe(menu, recipe, layout.getRecipeSlotsView(), mc.player, false, true);
        MestDevTest.log("jei transfer to {} via {}: check={} result={}", target,
                handler.getClass().getSimpleName(), check == null ? "ok" : check.getType(),
                error == null ? "ok" : error.getType());
    }

    /** Server-side check a few ticks after {@link #transferCraftingTable}. */
    static void verifyTransfer(ServerPlayer player, MestRecipeTransferContext.Target target) {
        if (!(player.containerMenu instanceof MESTMenu menu)) {
            MestDevTest.log("FAIL verify jei transfer: menu={}", player.containerMenu);
            return;
        }
        int planks = 0;
        String where;
        if (target == MestRecipeTransferContext.Target.PATTERN_ENCODING) {
            for (var slot : menu.getPatternCraftingSlots()) {
                planks += slot.getItem().is(Items.OAK_PLANKS) ? 1 : 0;
            }
            where = "pattern grid (mode " + menu.getPatternEncodingMode() + ")";
        } else {
            var matrix = menu.getCraftingMatrix();
            for (int i = 0; i < matrix.size(); i++) {
                ItemStack stack = matrix.getStackInSlot(i);
                planks += stack.is(Items.OAK_PLANKS) ? 1 : 0;
            }
            where = "crafting grid";
        }
        MestDevTest.log("{} jei transfer to {}: {} plank slot(s) in {}", planks == 4 ? "PASS" : "FAIL",
                target, planks, where);
    }
}
