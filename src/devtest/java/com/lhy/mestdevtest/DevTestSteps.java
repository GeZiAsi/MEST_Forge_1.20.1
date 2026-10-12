package com.lhy.mestdevtest;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.core.definitions.AEItems;

import appeng.parts.encoding.EncodingMode;

import com.lhy.mest.client.MESTScreen;
import com.lhy.mest.client.MestClientModules;
import com.lhy.mest.terminal.MESTMenu;

/** The scripted checks run after the terminal opened. Extend as needed while debugging. */
final class DevTestSteps {
    private DevTestSteps() {
    }

    static void register(List<MestDevTest.Step> steps) {
        if (System.getProperty("mest.devtest.layout") != null) {
            registerSeededLayout(steps);
            return;
        }
        steps.add(new MestDevTest.Step("describe screen", 10, null, mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                var menu = screen.getMenu();
                MestDevTest.log("menu slots={} powered={} encodingMode={} repoEntries={}",
                        menu.slots.size(), menu.isPowered(), menu.getPatternEncodingMode(),
                        menu.getClientRepo() == null ? -1 : menu.getClientRepo().getAllEntries().size());
                var repo = screen.meListPanel().repo();
                MestDevTest.log("repo view size={} paused={} search='{}' sort={} view={} type={} first={}",
                        repo.size(), repo.isPaused(), repo.getSearchString(),
                        screen.meListPanel().getSortBy(), screen.meListPanel().getSortDisplay(),
                        screen.meListPanel().getTypeFilter(), repo.size() > 0 ? repo.get(0) : null);
            }
        }));
        steps.add(new MestDevTest.Step("processing mode", 30, "mest_02_processing", mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                screen.getMenu().setPatternEncodingMode(EncodingMode.PROCESSING);
            }
        }));
        steps.add(new MestDevTest.Step("search iron", 30, "mest_03_search", mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                screen.getMenu().setPatternEncodingMode(EncodingMode.CRAFTING);
                screen.meListPanel().setSearchValue("iron");
            }
        }));
        steps.add(new MestDevTest.Step("clear search", 5, null, mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                screen.meListPanel().setSearchValue("");
            }
        }));
        steps.add(new MestDevTest.Step("encode crafting pattern + auto-upload", 20, "mest_04_encoded_crafting",
                mc -> MestDevTest.runOnServer(mc, DevTestSteps::encodeCraftingPattern)));
        steps.add(new MestDevTest.Step("encode processing pattern", 20, "mest_05_encoded_processing",
                mc -> MestDevTest.runOnServer(mc, DevTestSteps::encodeProcessingPattern)));
        if (net.minecraftforge.fml.ModList.get().isLoaded("jei")) {
            var crafting = com.lhy.mest.integration.MestRecipeTransferContext.Target.CRAFTING;
            var encoding = com.lhy.mest.integration.MestRecipeTransferContext.Target.PATTERN_ENCODING;
            steps.add(new MestDevTest.Step("show crafting grid", 5, null, mc -> runHotkey(mc, "crafting")));
            steps.add(new MestDevTest.Step("jei transfer to crafting grid", 20, "mest_06_jei_crafting",
                    mc -> DevTestJei.transferCraftingTable(mc, crafting)));
            steps.add(new MestDevTest.Step("verify crafting grid", 5, null,
                    mc -> MestDevTest.runOnServer(mc, player -> DevTestJei.verifyTransfer(player, crafting))));
            steps.add(new MestDevTest.Step("hide crafting grid", 5, null, mc -> runHotkey(mc, "crafting")));
            steps.add(new MestDevTest.Step("jei transfer to pattern encoding", 20, "mest_07_jei_pattern",
                    mc -> DevTestJei.transferCraftingTable(mc, encoding)));
            steps.add(new MestDevTest.Step("verify pattern encoding", 5, null,
                    mc -> MestDevTest.runOnServer(mc, player -> DevTestJei.verifyTransfer(player, encoding))));
            // MEST's own buttons beside each recipe in JEI's recipe screen.
            for (String kind : List.of("CRAFT", "ENCODE")) {
                var target = kind.equals("CRAFT") ? crafting : encoding;
                steps.add(new MestDevTest.Step("clear grids for " + kind, 5, null,
                        mc -> MestDevTest.runOnServer(mc, DevTestJei::clearGrids)));
                steps.add(new MestDevTest.Step("jei recipes for " + kind, 25, "mest_12_jei_buttons",
                        DevTestJei::showCraftingTableRecipes));
                steps.add(new MestDevTest.Step("press jei " + kind, 20, null,
                        mc -> DevTestJei.pressRecipeButton(mc, kind)));
                steps.add(new MestDevTest.Step("verify jei " + kind, 5, null, mc -> {
                    MestDevTest.log("{} back in terminal after jei {}: {}",
                            mc.screen instanceof MESTScreen ? "PASS" : "FAIL", kind, mc.screen);
                    MestDevTest.runOnServer(mc, player -> DevTestJei.verifyTransfer(player, target));
                }));
            }
        }
        // An item being crafted: pinned to the first row with the assembler-light animation over it.
        steps.add(new MestDevTest.Step("pending craft of iron ingot", 25, "mest_13_pending_craft", mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                var iron = appeng.api.stacks.AEItemKey.of(Items.IRON_INGOT);
                appeng.client.gui.me.common.PinnedKeys.pinKey(iron,
                        appeng.client.gui.me.common.PinnedKeys.PinReason.CRAFTING);
                appeng.client.gui.me.common.PendingCraftingJobs.jobStatus(java.util.UUID.randomUUID(), iron, 1, 1,
                        appeng.core.sync.packets.CraftingJobStatusPacket.Status.STARTED);
                screen.meListPanel().repo().updateView();
                var repo = screen.meListPanel().repo();
                MestDevTest.log("pinned row={} first={}", repo.hasPinnedRow(),
                        repo.size() > 0 && repo.get(0) != null ? repo.get(0).getWhat() : null);
            }
        }));
        steps.add(new MestDevTest.Step("clear pending craft", 5, null, mc -> {
            appeng.client.gui.me.common.PendingCraftingJobs.clearPendingJobs();
            appeng.client.gui.me.common.PinnedKeys.clearPinnedKeys();
        }));
        steps.add(new MestDevTest.Step("open magnet settings", 30, "mest_08_magnet", mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                screen.getMenu().openMagnetMenu();
            }
        }));
        steps.add(new MestDevTest.Step("back from magnet settings", 40, null, mc -> {
            MestDevTest.log("{} magnet screen: {}", mc.screen instanceof com.lhy.mest.client.MestMagnetScreen
                    ? "PASS" : "FAIL", mc.screen == null ? null : mc.screen.getClass().getSimpleName());
            appeng.client.gui.implementations.AESubScreen.goBack();
        }));
        steps.add(new MestDevTest.Step("terminal after magnet", 1, null, mc -> MestDevTest.log(
                "{} back to terminal: {}", mc.screen instanceof MESTScreen ? "PASS" : "FAIL",
                mc.screen == null ? null : mc.screen.getClass().getSimpleName())));
        // Every module panel once: toggle it (as its hotkey does), screenshot, toggle it back.
        for (MestClientModules.Registration module : MestClientModules.all()) {
            String id = module.id();
            if (id.equals("me_list")) {
                continue;
            }
            steps.add(new MestDevTest.Step("toggle " + id, 15, "mest_mod_" + id, mc -> runHotkey(mc, id)));
            steps.add(new MestDevTest.Step("restore " + id, 5, null, mc -> runHotkey(mc, id)));
        }
        // Splicing: each module as a floating window, dragged onto the ME list's left edge.
        for (String id : List.of("crafting", "crafting_terminal", "pattern_access", "pattern_cache",
                "inventory", "wireless_settings", "toolkit", "hotkeys")) {
            steps.add(new MestDevTest.Step("reset layout for " + id, 5, null, DevTestDock::resetLayout));
            steps.add(new MestDevTest.Step("float " + id, 10, null, mc -> DevTestDock.showFloating(mc, id)));
            steps.add(new MestDevTest.Step("splice " + id, 15, "mest_splice_" + id,
                    mc -> DevTestDock.spliceOntoMeList(mc, id)));
        }
        // A smaller terminal: shrink the anchored window, then larger GUI scales.
        steps.add(new MestDevTest.Step("reset layout", 5, null, DevTestDock::resetLayout));
        steps.add(new MestDevTest.Step("shrink anchored window", 15, "mest_10_shrunk",
                mc -> DevTestDock.resizeAnchored(mc, -150, -120)));
        steps.add(new MestDevTest.Step("reset layout", 5, null, DevTestDock::resetLayout));
        for (int scale : new int[] { 3, 4 }) {
            steps.add(new MestDevTest.Step("gui scale " + scale, 20, "mest_11_gui" + scale, mc -> {
                mc.options.guiScale().set(scale);
                mc.resizeDisplay();
            }));
        }
        steps.add(new MestDevTest.Step("gui scale 2", 10, null, mc -> {
            mc.options.guiScale().set(2);
            mc.resizeDisplay();
        }));
        // Last, since it closes the terminal: the toolkit quick bars beside the vanilla hotbar.
        steps.add(new MestDevTest.Step("enable toolkit bars", 10, null,
                mc -> MestDevTest.runOnServer(mc, DevTestSteps::stockToolkitBars)));
        steps.add(new MestDevTest.Step("toolkit bars hud", 30, "mest_09_toolkit_hud", mc -> {
            mc.setScreen(null);
            MestDevTest.log("toolkit bars enabled on client={}",
                    com.lhy.mest.terminal.ToolkitBarState.isBarEnabled(mc.player));
        }));
    }

    private static void runHotkey(Minecraft mc, String id) {
        if (!(mc.screen instanceof MESTScreen screen)) {
            MestDevTest.log("FAIL no terminal screen for {}, current={}", id, mc.screen);
            return;
        }
        try {
            Method method = MESTScreen.class.getDeclaredMethod("runHotkey", String.class);
            method.setAccessible(true);
            MestDevTest.log("hotkey {} handled={}", id, method.invoke(screen, id));
        } catch (ReflectiveOperationException e) {
            MestDevTest.LOGGER.error("MEST-DEVTEST FAIL hotkey {}", id, e);
        }
    }

    private static MESTMenu serverMenu(ServerPlayer player) {
        if (player.containerMenu instanceof MESTMenu menu) {
            return menu;
        }
        MestDevTest.log("FAIL server container is {}", player.containerMenu);
        return null;
    }

    /** Four planks → crafting table, encoded without Shift so it auto-uploads to the assembler. */
    private static void encodeCraftingPattern(ServerPlayer player) {
        MESTMenu menu = serverMenu(player);
        if (menu == null) {
            return;
        }
        // The test world is reused, so drop patterns a previous run uploaded (else: duplicate).
        if (player.serverLevel().getBlockEntity(MestDevTest.base.east()) instanceof PatternProviderBlockEntity provider) {
            var inv = provider.getLogic().getPatternInv();
            for (int i = 0; i < inv.size(); i++) {
                inv.setItemDirect(i, ItemStack.EMPTY);
            }
        }
        menu.setPatternEncodingMode(EncodingMode.CRAFTING);
        menu.clearPatternEncoding();
        var grid = menu.getPatternCraftingSlots();
        for (int i : new int[] { 0, 1, 3, 4 }) {
            grid[i].set(new ItemStack(Items.OAK_PLANKS));
        }
        menu.getBlankPatternSlot().set(AEItems.BLANK_PATTERN.stack(4));
        menu.encodePattern(false);
        var encoded = menu.getEncodedPatternSlot().getItem();
        MestDevTest.log("crafting encode: encodedSlot={} blankLeft={} {}", encoded,
                menu.getBlankPatternSlot().getItem().getCount(), describePattern(player, encoded));
        if (player.serverLevel().getBlockEntity(MestDevTest.base.east()) instanceof PatternProviderBlockEntity provider) {
            var inv = provider.getLogic().getPatternInv();
            int patterns = 0;
            String uploaded = "";
            for (int i = 0; i < inv.size(); i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    patterns++;
                    uploaded = describePattern(player, inv.getStackInSlot(i));
                }
            }
            MestDevTest.log("{} crafting auto-upload: provider holds {} pattern(s) {}",
                    encoded.isEmpty() && patterns > 0 ? "PASS" : "FAIL", patterns, uploaded);
        } else {
            MestDevTest.log("FAIL pattern provider missing at {}", MestDevTest.base.east());
        }
    }

    /** One iron ingot → nine nuggets as a processing pattern, Shift so it stays in the slot. */
    private static void encodeProcessingPattern(ServerPlayer player) {
        MESTMenu menu = serverMenu(player);
        if (menu == null) {
            return;
        }
        menu.setPatternEncodingMode(EncodingMode.PROCESSING);
        menu.clearPatternEncoding();
        menu.getProcessingInputSlots()[0].set(new ItemStack(Items.IRON_INGOT));
        menu.getProcessingOutputSlots()[0].set(new ItemStack(Items.IRON_NUGGET, 9));
        menu.encodePattern(true);
        var encoded = menu.getEncodedPatternSlot().getItem();
        var details = PatternDetailsHelper.decodePattern(encoded, player.level());
        boolean ok = details != null && details.getOutputs().length == 1
                && details.getOutputs()[0].what().equals(appeng.api.stacks.AEItemKey.of(Items.IRON_NUGGET))
                && details.getOutputs()[0].amount() == 9;
        MestDevTest.log("{} processing encode: {}", ok ? "PASS" : "FAIL", describePattern(player, encoded));
    }

    private static String describePattern(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return "(empty)";
        }
        var details = PatternDetailsHelper.decodePattern(stack, player.level());
        if (details == null) {
            return "(not a pattern: " + stack + ")";
        }
        var in = new StringBuilder();
        for (var input : details.getInputs()) {
            in.append(input.getPossibleInputs()[0]).append('x').append(input.getMultiplier()).append(' ');
        }
        return "inputs=[" + in.toString().trim() + "] outputs=" + java.util.Arrays.toString(details.getOutputs());
    }

    private static void stockToolkitBars(ServerPlayer player) {
        var toolkit = com.lhy.mest.terminal.ToolkitBarState.asInventory(player);
        var tools = List.of(Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD,
                Items.SHEARS, Items.BOW, Items.FISHING_ROD, Items.FLINT_AND_STEEL, Items.SHIELD, Items.TRIDENT);
        for (int i = 0; i < tools.size() && i < toolkit.size(); i++) {
            toolkit.setItemDirect(i * 2, new ItemStack(tools.get(i)));
        }
        com.lhy.mest.terminal.ToolkitBarActions.setBarEnabled(player, true);
        MestDevTest.log("toolkit bars enabled on server={} hasTerminal={} size={}",
                com.lhy.mest.terminal.ToolkitBarState.isBarEnabled(player),
                com.lhy.mest.terminal.ToolkitBarState.hasTerminal(player), toolkit.size());
    }

    /** With a player's layout files: every preset, then the trash dragged next to the inventory. */
    private static void registerSeededLayout(List<MestDevTest.Step> steps) {
        for (int preset = 0; preset < 3; preset++) {
            int index = preset;
            steps.add(new MestDevTest.Step("preset " + preset, 20, "mest_user_preset" + preset, mc -> {
                if (mc.screen instanceof MESTScreen screen) {
                    DevTestDock.dock(screen).selectPreset(index);
                    DevTestDock.invoke(screen, "attachMeSideBar");
                    DevTestDock.invoke(screen, "attachExtraSlotColumns");
                    for (String id : List.of("me_list", "crafting_terminal", "pattern_encoding", "inventory",
                            "wireless_settings", "toolkit", "trash")) {
                        MestDevTest.log("preset {} {} anchored={}", index, DevTestDock.describe(screen, id),
                                DevTestDock.dock(screen).anchoredGroupBounds());
                    }
                }
            }));
        }
        steps.add(new MestDevTest.Step("preset 0 again", 10, null, mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                DevTestDock.dock(screen).selectPreset(0);
                screen.openTrash();
            }
        }));
        steps.add(new MestDevTest.Step("trash next to inventory", 20, "mest_user_trash_spliced", mc -> {
            if (!(mc.screen instanceof MESTScreen screen)) {
                return;
            }
            var trash = DevTestDock.panel(screen, "trash");
            var inventory = DevTestDock.panel(screen, "inventory");
            MestDevTest.log("trash before: {}", DevTestDock.describe(screen, "trash"));
            double tx = inventory.x + inventory.width - 2;
            double ty = inventory.y + inventory.height / 2.0;
            var top = DevTestDock.dock(screen).topPanelAt(tx, ty);
            MestDevTest.log("top panel at drop point {},{}: {} / network toolkit {}", tx, ty,
                    top == null ? null : top.id(), DevTestDock.describe(screen, "network_toolkit"));
            DevTestDock.drag(screen, trash.x + 30, trash.y + 6,
                    inventory.x + inventory.width - 2, inventory.y + inventory.height / 2.0);
            boolean joined = DevTestDock.dock(screen).isJoinedWindow(trash)
                    && !DevTestDock.dock(screen).isFloatingWindow(trash);
            MestDevTest.log("{} trash spliced: {} / {}", joined ? "PASS" : "FAIL",
                    DevTestDock.describe(screen, "trash"), DevTestDock.describe(screen, "inventory"));
        }));
    }
}
