package com.lhy.mestdevtest;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.GenericStack;
import appeng.parts.encoding.EncodingMode;

import com.lhy.mest.client.MESTScreen;
import com.lhy.mest.terminal.MESTMenu;

/** Screenshots of textures and layout: encoding modes, a narrow pattern-access panel, JEI buttons. */
final class DevTestUi {
    private DevTestUi() {
    }

    static void register(List<MestDevTest.Step> steps) {
        steps.add(new MestDevTest.Step("reset layout for ui", 5, null, mc -> {
            DevTestDock.resetLayout(mc);
            MestDevTest.log("pattern modes layout: {}", com.lhy.mest.client.panel.PatternModesTexture.layout());
        }));
        steps.add(new MestDevTest.Step("crafting mode items", 20, "mest_ui_crafting",
                mc -> MestDevTest.runOnServer(mc, player -> fill(player, EncodingMode.CRAFTING))));
        steps.add(new MestDevTest.Step("processing mode fluids", 20, "mest_ui_processing",
                mc -> MestDevTest.runOnServer(mc, player -> fill(player, EncodingMode.PROCESSING))));
        steps.add(new MestDevTest.Step("smithing mode", 20, "mest_ui_smithing",
                mc -> MestDevTest.runOnServer(mc, player -> fill(player, EncodingMode.SMITHING_TABLE))));
        steps.add(new MestDevTest.Step("stonecutting mode", 25, "mest_ui_stonecutting",
                mc -> MestDevTest.runOnServer(mc, player -> fill(player, EncodingMode.STONECUTTING))));
        steps.add(new MestDevTest.Step("back to crafting mode", 5, null,
                mc -> MestDevTest.runOnServer(mc, player -> {
                    if (player.containerMenu instanceof MESTMenu menu) {
                        menu.clearPatternEncoding();
                        menu.setPatternEncodingMode(EncodingMode.CRAFTING);
                    }
                })));
        // The pattern-access panel squeezed to its narrowest, with nothing matching the search.
        steps.add(new MestDevTest.Step("float pattern access", 10, null,
                mc -> DevTestDock.showFloating(mc, "pattern_access")));
        // Resizing is a layout-editor action (as in the player's screenshot).
        steps.add(new MestDevTest.Step("open layout editor", 15, null, mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                setPatternSearch(screen, "zzzz");
                mc.setScreen(new com.lhy.mest.client.MESTLayoutEditorScreen(screen, DevTestDock.dock(screen)));
            }
        }));
        steps.add(new MestDevTest.Step("narrow pattern access", 25, "mest_ui_pattern_access_narrow", mc -> {
            var screen = mc.screen;
            var terminal = screen instanceof com.lhy.mest.client.MESTLayoutEditorScreen
                    ? com.lhy.mest.client.MestClient.coveredTerminal() : null;
            if (screen == null || terminal == null) {
                MestDevTest.log("FAIL narrow pattern access: screen {} terminal {}", screen, terminal);
                return;
            }
            var panel = DevTestDock.panel(terminal, "pattern_access");
            // The resize grip sits at the outside scroller rail's corner when there is one.
            double x = panel.x + panel.width + panel.outsideHitWidth() - 2;
            double y = panel.y + panel.height - 2;
            screen.mouseClicked(x, y, 0);
            for (int i = 1; i <= 12; i++) {
                screen.mouseDragged(x - i * 20, y, 0, -20, 0);
            }
            screen.mouseReleased(x - 240, y, 0);
            MestDevTest.log("narrow pattern access: {} minWidth={}",
                    DevTestDock.describe(terminal, "pattern_access"), panel.minWidth());
        }));
        steps.add(new MestDevTest.Step("leave layout editor", 15, "mest_ui_pattern_access_narrow_terminal", mc -> {
            if (mc.screen instanceof com.lhy.mest.client.MESTLayoutEditorScreen editor) {
                editor.onClose();
            }
        }));
        steps.add(new MestDevTest.Step("clear pattern search", 5, null, mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                setPatternSearch(screen, "");
            }
        }));
        if (net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
            steps.add(new MestDevTest.Step("jei recipes for tooltip", 25, null, DevTestJei::showCraftingTableRecipes));
            steps.add(new MestDevTest.Step("hover jei bookmark", 15, "mest_ui_jei_bookmark_tooltip",
                    DevTestJei::hoverBookmark));
            steps.add(new MestDevTest.Step("close jei", 15, null, mc -> {
                if (mc.screen != null) {
                    mc.screen.onClose();
                }
            }));
        }
    }

    private static void fill(ServerPlayer player, EncodingMode mode) {
        if (!(player.containerMenu instanceof MESTMenu menu)) {
            MestDevTest.log("FAIL ui fill: menu {}", player.containerMenu);
            return;
        }
        menu.clearPatternEncoding();
        menu.setPatternEncodingMode(mode);
        var water = GenericStack.wrapInItemStack(AEFluidKey.of(Fluids.WATER), 1000);
        switch (mode) {
            case CRAFTING -> {
                var grid = menu.getPatternCraftingSlots();
                for (var slot : grid) {
                    slot.set(new ItemStack(Items.OBSIDIAN));
                }
            }
            case PROCESSING -> {
                menu.getProcessingInputSlots()[0].set(water.copy());
                menu.getProcessingInputSlots()[1].set(new ItemStack(Items.OBSIDIAN));
                menu.getProcessingInputSlots()[4].set(water.copy());
                menu.getProcessingOutputSlots()[0].set(water.copy());
                menu.getProcessingOutputSlots()[1].set(new ItemStack(Items.OBSIDIAN));
            }
            case SMITHING_TABLE -> {
                menu.getSmithingTableTemplateSlot().set(new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
                menu.getSmithingTableBaseSlot().set(new ItemStack(Items.DIAMOND_SWORD));
                menu.getSmithingTableAdditionSlot().set(new ItemStack(Items.NETHERITE_INGOT));
            }
            case STONECUTTING -> menu.getStonecuttingInputSlot().set(new ItemStack(Items.STONE));
            default -> {
            }
        }
        menu.broadcastChanges();
    }

    private static void setPatternSearch(MESTScreen screen, String text) {
        if (DevTestDock.panel(screen, "pattern_access") instanceof com.lhy.mest.client.panel.PatternAccessPanel panel
                && panel.searchField() != null) {
            panel.searchField().setValue(text);
        }
    }

    static void moveMouse(Minecraft mc, double guiX, double guiY) {
        double scale = mc.getWindow().getGuiScale();
        try {
            var xpos = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
            var ypos = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            xpos.setAccessible(true);
            ypos.setAccessible(true);
            xpos.setDouble(mc.mouseHandler, guiX * scale);
            ypos.setDouble(mc.mouseHandler, guiY * scale);
        } catch (ReflectiveOperationException e) {
            MestDevTest.LOGGER.error("MEST-DEVTEST FAIL move mouse", e);
        }
    }
}
