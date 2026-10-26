package com.lhy.mest.integration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import mezz.jei.api.gui.IRecipeLayoutDrawable;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;

import appeng.core.definitions.AEItems;
import appeng.core.localization.ItemModText;
import appeng.integration.modules.jeirei.CraftingHelper;
import appeng.integration.modules.jeirei.TransferHelper;
import appeng.menu.me.items.CraftingTermMenu;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.client.GuiSprites;
import com.lhy.mest.terminal.MESTMenu;

/**
 * Two buttons beside each recipe's bookmark button in JEI's recipe screen while the spliced
 * terminal is open: craft it in the terminal's crafting grid, or write it into pattern encoding.
 *
 * <p>JEI 15.19/15.20 (shipped by GTL) has no API for extra recipe buttons, so the recipes on the
 * page are found reflectively in JEI's recipe screen; their button areas come from the public
 * {@link IRecipeLayoutDrawable} API. Any reflection failure only turns the buttons off.
 */
final class MestJeiRecipeButtons {
    private static final int SIZE = 13;
    private static final int GAP = 2;

    private enum Kind { CRAFT, ENCODE }

    private record Button(Kind kind, Rect2i area, IRecipeLayoutDrawable<?> layout) {
        boolean contains(double x, double y) {
            return x >= area.getX() && x < area.getX() + area.getWidth()
                    && y >= area.getY() && y < area.getY() + area.getHeight();
        }
    }

    private static IRecipeTransferHandlerHelper helper;
    private static IUniversalRecipeTransferHandler<MESTMenu> encodingHandler;
    private static boolean installed;
    private static boolean broken;
    private static final List<Button> BUTTONS = new ArrayList<>();

    private MestJeiRecipeButtons() {
    }

    static void install(IRecipeTransferHandlerHelper transferHelper, IUniversalRecipeTransferHandler<MESTMenu> encoding) {
        helper = transferHelper;
        encodingHandler = encoding;
        if (!installed) {
            installed = true;
            MinecraftForge.EVENT_BUS.addListener(MestJeiRecipeButtons::onRender);
            MinecraftForge.EVENT_BUS.addListener(MestJeiRecipeButtons::onMousePressed);
            MinecraftForge.EVENT_BUS.addListener(MestJeiRecipeButtons::onScreenOpening);
        }
    }

    private static void onRender(ScreenEvent.Render.Post event) {
        BUTTONS.clear();
        Screen screen = event.getScreen();
        var player = Minecraft.getInstance().player;
        if (broken || !MestPullItemsSupport.enabled() || player == null
                || !(player.containerMenu instanceof MESTMenu menu) || !isRecipesScreen(screen)) {
            return;
        }
        for (IRecipeLayoutDrawable<?> layout : recipeLayouts(screen)) {
            Rect2i anchor = absolute(layout, layout.getRecipeBookmarkButtonArea());
            if (anchor.getWidth() <= 0) {
                anchor = absolute(layout, layout.getRecipeTransferButtonArea());
            }
            int x = anchor.getX() + anchor.getWidth() + GAP;
            int y = anchor.getY();
            if (craftingRecipe(layout) != null) {
                BUTTONS.add(new Button(Kind.CRAFT, new Rect2i(x, y, SIZE, SIZE), layout));
            }
            if (canEncode(menu, layout)) {
                BUTTONS.add(new Button(Kind.ENCODE, new Rect2i(x, y + SIZE + GAP, SIZE, SIZE), layout));
            }
        }
        GuiGraphics g = event.getGuiGraphics();
        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        // JEI has already drawn its tooltips (at z 400) by now; stay below them so a tooltip shown
        // over these buttons, e.g. the bookmark button's, keeps its text in front.
        g.pose().pushPose();
        g.pose().translate(0, 0, 100);
        Button hovered = null;
        for (Button button : BUTTONS) {
            boolean hover = button.contains(mouseX, mouseY);
            hovered = hover ? button : hovered;
            draw(g, menu, button, hover);
        }
        if (hovered != null) {
            g.renderComponentTooltip(Minecraft.getInstance().font, tooltip(menu, hovered), mouseX, mouseY);
        }
        g.pose().popPose();
    }

    private static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 0 || !isRecipesScreen(event.getScreen())) {
            return;
        }
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof MESTMenu menu)) {
            return;
        }
        for (Button button : List.copyOf(BUTTONS)) {
            if (!button.contains(event.getMouseX(), event.getMouseY())) {
                continue;
            }
            event.setCanceled(true);
            if (press(menu, button)) {
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
                // Back to the terminal, like JEI's own transfer button.
                requestReturnToTerminal();
                event.getScreen().onClose();
            }
            return;
        }
    }

    /**
     * After a transfer into the terminal, the recipe screen should hand back to the terminal. JEI
     * closes to the screen it was opened from, which is not the terminal when the recipe was looked
     * up from somewhere else while the terminal stayed open (e.g. a quest book opened over it).
     */
    static void requestReturnToTerminal() {
        returnToTerminal = true;
    }

    private static boolean returnToTerminal;

    private static void onScreenOpening(ScreenEvent.Opening event) {
        if (!returnToTerminal) {
            return;
        }
        returnToTerminal = false;
        // Only the recipe screen closing after the transfer; any later screen opens as asked.
        if (!isRecipesScreen(event.getCurrentScreen())
                || event.getNewScreen() instanceof com.lhy.mest.client.MESTScreen) {
            return;
        }
        var terminal = com.lhy.mest.client.MestClient.coveredTerminal();
        if (terminal != null) {
            event.setNewScreen(terminal);
        }
    }

    private static boolean press(MESTMenu menu, Button button) {
        var player = Minecraft.getInstance().player;
        if (button.kind() == Kind.CRAFT) {
            CraftingRecipe recipe = craftingRecipe(button.layout());
            if (recipe == null || allMissing(menu, recipe)) {
                return false;
            }
            MestRecipeTransferContext.select(menu, MestRecipeTransferContext.Target.CRAFTING);
            CraftingHelper.performTransfer(menu, recipe, Screen.hasControlDown());
            return true;
        }
        MestRecipeTransferContext.select(menu, MestRecipeTransferContext.Target.PATTERN_ENCODING);
        IRecipeTransferError error = transferEncoding(menu, button.layout(), true);
        return error == null;
    }

    private static <R> IRecipeTransferError transferEncoding(MESTMenu menu, IRecipeLayoutDrawable<R> layout,
            boolean doTransfer) {
        var previous = MestRecipeTransferContext.targetFor(menu);
        MestRecipeTransferContext.select(menu, MestRecipeTransferContext.Target.PATTERN_ENCODING);
        try {
            return encodingHandler.transferRecipe(menu, layout.getRecipe(), layout.getRecipeSlotsView(),
                    Minecraft.getInstance().player, false, doTransfer);
        } finally {
            if (!doTransfer && previous != null) {
                MestRecipeTransferContext.select(menu, previous);
            }
        }
    }

    private static boolean canEncode(MESTMenu menu, IRecipeLayoutDrawable<?> layout) {
        if (encodingHandler == null) {
            return false;
        }
        IRecipeTransferError error = transferEncoding(menu, layout, false);
        return error == null || error.getType() == IRecipeTransferError.Type.COSMETIC;
    }

    private static void draw(GuiGraphics g, MESTMenu menu, Button button, boolean hover) {
        Rect2i a = button.area();
        GuiSprites.button(g, a.getX(), a.getY(), a.getWidth(), a.getHeight(), hover);
        if (button.kind() == Kind.CRAFT) {
            CraftingRecipe recipe = craftingRecipe(button.layout());
            int tint = recipe == null ? 0 : highlight(menu, recipe);
            if (tint != 0) {
                g.fill(a.getX() + 1, a.getY() + 1, a.getX() + a.getWidth() - 1, a.getY() + a.getHeight() - 1, tint);
            }
        }
        ItemStack icon = button.kind() == Kind.CRAFT
                ? new ItemStack(Items.CRAFTING_TABLE)
                : AEItems.BLANK_PATTERN.stack();
        g.pose().pushPose();
        g.pose().translate(a.getX() + 1.5F, a.getY() + 1.5F, 0);
        g.pose().scale(10 / 16F, 10 / 16F, 1);
        g.renderItem(icon, 0, 0);
        g.pose().popPose();
    }

    private static List<Component> tooltip(MESTMenu menu, Button button) {
        List<Component> lines = new ArrayList<>();
        if (button.kind() == Kind.ENCODE) {
            lines.add(Component.translatable("gui.mesplicedterminal.jei.encode_pattern"));
            return lines;
        }
        lines.add(Component.translatable("gui.mesplicedterminal.jei.craft_in_terminal"));
        lines.add(Component.translatable("gui.mesplicedterminal.jei.craft_in_terminal.ctrl"));
        CraftingRecipe recipe = craftingRecipe(button.layout());
        if (recipe == null || helper == null) {
            return lines;
        }
        var missing = missing(menu, recipe);
        if (allMissing(menu, recipe)) {
            lines.add(ItemModText.NO_ITEMS.text().withStyle(ChatFormatting.RED));
        } else if (missing.totalSize() > 0) {
            lines.addAll(TransferHelper.createCraftingTooltip(missing, Screen.hasControlDown()));
        }
        return lines;
    }

    private static int highlight(MESTMenu menu, CraftingRecipe recipe) {
        if (helper == null) {
            return 0;
        }
        var missing = missing(menu, recipe);
        if (missing.totalSize() == 0) {
            return 0;
        }
        return missing.anyMissing() ? 0x80FFA500 : 0x804545FF;
    }

    private static CraftingTermMenu.MissingIngredientSlots missing(MESTMenu menu, CraftingRecipe recipe) {
        Map<Integer, Ingredient> ingredients = MestCraftingSlots.guiSlotToIngredient(recipe);
        return menu.findMissingIngredients(ingredients);
    }

    private static boolean allMissing(MESTMenu menu, CraftingRecipe recipe) {
        if (helper == null) {
            return false;
        }
        Map<Integer, Ingredient> ingredients = MestCraftingSlots.guiSlotToIngredient(recipe);
        return !ingredients.isEmpty()
                && menu.findMissingIngredients(ingredients).missingSlots().size() == ingredients.size();
    }

    @Nullable
    private static CraftingRecipe craftingRecipe(IRecipeLayoutDrawable<?> layout) {
        return layout.getRecipe() instanceof CraftingRecipe crafting
                && crafting.getType() == net.minecraft.world.item.crafting.RecipeType.CRAFTING
                && crafting.canCraftInDimensions(3, 3)
                ? crafting
                : null;
    }

    /** Button areas are relative to the recipe; the recipe rect is in screen space. */
    private static Rect2i absolute(IRecipeLayoutDrawable<?> layout, Rect2i area) {
        Rect2i rect = layout.getRect();
        return new Rect2i(rect.getX() + area.getX(), rect.getY() + area.getY(), area.getWidth(), area.getHeight());
    }

    // --- JEI internals: the recipe layouts on the current page ---------------------------------

    private static final String RECIPES_GUI = "mezz.jei.gui.recipes.RecipesGui";
    private static Field layoutsField;
    private static Field listField;

    private static boolean isRecipesScreen(Screen screen) {
        return screen != null && screen.getClass().getName().equals(RECIPES_GUI);
    }

    private static List<IRecipeLayoutDrawable<?>> recipeLayouts(Screen screen) {
        List<IRecipeLayoutDrawable<?>> result = new ArrayList<>();
        try {
            if (layoutsField == null) {
                layoutsField = fieldOfType(screen.getClass(), "mezz.jei.gui.recipes.RecipeGuiLayouts");
                listField = layoutsField.getType().getDeclaredField("recipeLayoutsWithButtons");
                listField.setAccessible(true);
            }
            Object layouts = layoutsField.get(screen);
            for (Object entry : (List<?>) listField.get(layouts)) {
                IRecipeLayoutDrawable<?> layout = layoutOf(entry);
                if (layout != null) {
                    result.add(layout);
                }
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            broken = true;
            MESplicedterminal.LOGGER.warn("MEST: JEI recipe buttons disabled, unknown JEI recipe screen layout", e);
        }
        return result;
    }

    @Nullable
    private static IRecipeLayoutDrawable<?> layoutOf(Object entry) throws ReflectiveOperationException {
        // JEI 15.20: record accessor recipeLayout(); JEI 15.4x: getRecipeLayout().
        for (String name : new String[] { "recipeLayout", "getRecipeLayout" }) {
            try {
                Method method = entry.getClass().getMethod(name);
                method.setAccessible(true);
                return method.invoke(entry) instanceof IRecipeLayoutDrawable<?> layout ? layout : null;
            } catch (NoSuchMethodException ignored) {
                // try the other name
            }
        }
        return null;
    }

    private static Field fieldOfType(Class<?> owner, String typeName) throws NoSuchFieldException {
        for (Class<?> c = owner; c != null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getType().getName().equals(typeName)) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        throw new NoSuchFieldException(typeName + " in " + owner.getName());
    }
}
