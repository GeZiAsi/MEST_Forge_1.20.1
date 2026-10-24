package com.lhy.mestdevtest;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.GridHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.client.gui.me.common.RepoSlot;

import com.lhy.mest.client.MESTScreen;

/** Fluids in the ME list: filling a held bucket, and ordering a craftable fluid. */
final class DevTestFluid {
    private DevTestFluid() {
    }

    static void register(List<MestDevTest.Step> steps) {
        steps.add(new MestDevTest.Step("stock water + lava pattern", 40, null,
                mc -> MestDevTest.runOnServer(mc, DevTestFluid::stock)));
        // A held empty bucket, left-clicked on water: it comes back as a water bucket.
        steps.add(new MestDevTest.Step("carry empty bucket", 10, null,
                mc -> MestDevTest.runOnServer(mc, player -> carry(player, new ItemStack(Items.BUCKET)))));
        steps.add(new MestDevTest.Step("click water with bucket", 15, "mest_20_fluid_bucket",
                mc -> click(mc, AEFluidKey.of(Fluids.WATER), 0, false)));
        steps.add(new MestDevTest.Step("verify water bucket", 5, null,
                mc -> MestDevTest.runOnServer(mc, player -> verifyCarried(player, Items.WATER_BUCKET, "bucket"))));
        // Empty hand, left click: AE2 takes an empty bucket from the network and fills it.
        steps.add(new MestDevTest.Step("empty hand", 10, null,
                mc -> MestDevTest.runOnServer(mc, player -> carry(player, ItemStack.EMPTY))));
        steps.add(new MestDevTest.Step("click water empty-handed", 15, null,
                mc -> click(mc, AEFluidKey.of(Fluids.WATER), 0, false)));
        steps.add(new MestDevTest.Step("verify network bucket", 5, null,
                mc -> MestDevTest.runOnServer(mc, player -> verifyCarried(player, Items.WATER_BUCKET, "network bucket"))));
        // Shift-click: the filled bucket goes to the player's inventory.
        steps.add(new MestDevTest.Step("empty hand again", 10, null,
                mc -> MestDevTest.runOnServer(mc, player -> carry(player, ItemStack.EMPTY))));
        steps.add(new MestDevTest.Step("shift-click water", 15, null,
                mc -> click(mc, AEFluidKey.of(Fluids.WATER), 0, true)));
        steps.add(new MestDevTest.Step("verify shift bucket", 5, null,
                mc -> MestDevTest.runOnServer(mc, DevTestFluid::verifyShiftBucket)));
        // Lava: none stored, only craftable. A left click opens the crafting amount screen.
        steps.add(new MestDevTest.Step("empty hand for lava", 10, null,
                mc -> MestDevTest.runOnServer(mc, player -> carry(player, ItemStack.EMPTY))));
        steps.add(new MestDevTest.Step("click craftable lava", 30, "mest_21_fluid_craft",
                mc -> click(mc, AEFluidKey.of(Fluids.LAVA), 0, false)));
        steps.add(new MestDevTest.Step("verify craft amount screen", 5, null, mc -> {
            boolean ok = mc.screen instanceof appeng.client.gui.me.crafting.CraftAmountScreen;
            MestDevTest.log("{} lava click opened {}", ok ? "PASS" : "FAIL",
                    mc.screen == null ? null : mc.screen.getClass().getSimpleName());
            if (ok) {
                appeng.client.gui.implementations.AESubScreen.goBack();
            }
        }));
        steps.add(new MestDevTest.Step("back from craft amount", 30, null, mc -> MestDevTest.log(
                "{} back to terminal: {}", mc.screen instanceof MESTScreen ? "PASS" : "FAIL",
                mc.screen == null ? null : mc.screen.getClass().getSimpleName())));
        // Middle click on lava: same crafting screen.
        steps.add(new MestDevTest.Step("middle-click craftable lava", 30, null,
                mc -> click(mc, AEFluidKey.of(Fluids.LAVA), 2, false)));
        steps.add(new MestDevTest.Step("verify middle-click craft", 5, null, mc -> {
            boolean ok = mc.screen instanceof appeng.client.gui.me.crafting.CraftAmountScreen;
            MestDevTest.log("{} lava middle click opened {}", ok ? "PASS" : "FAIL",
                    mc.screen == null ? null : mc.screen.getClass().getSimpleName());
            if (ok) {
                appeng.client.gui.implementations.AESubScreen.goBack();
            }
        }));
        steps.add(new MestDevTest.Step("back again", 30, null, mc -> {
        }));
        if (net.minecraftforge.fml.ModList.get().isLoaded("gtceu")) {
            // GregTech: a liquid with its own bucket, and a craftable gas.
            AEFluidKey oil = gtFluid("oil");
            AEFluidKey oxygen = gtFluid("oxygen");
            if (oil == null || oxygen == null) {
                MestDevTest.log("FAIL gt fluids missing: oil={} oxygen={}", oil, oxygen);
                return;
            }
            steps.add(new MestDevTest.Step("stock gt oil + oxygen pattern", 40, null,
                    mc -> MestDevTest.runOnServer(mc, player -> stockGregTech(player, oil, oxygen))));
            steps.add(new MestDevTest.Step("carry bucket for oil", 10, null,
                    mc -> MestDevTest.runOnServer(mc, player -> carry(player, new ItemStack(Items.BUCKET)))));
            steps.add(new MestDevTest.Step("click oil with bucket", 15, "mest_22_gt_oil_bucket",
                    mc -> click(mc, oil, 0, false)));
            steps.add(new MestDevTest.Step("verify oil bucket", 5, null,
                    mc -> MestDevTest.runOnServer(mc, player -> verifyCarried(player,
                            oil.getFluid().getBucket(), "gt oil bucket"))));
            steps.add(new MestDevTest.Step("empty hand for oxygen", 10, null,
                    mc -> MestDevTest.runOnServer(mc, player -> carry(player, ItemStack.EMPTY))));
            steps.add(new MestDevTest.Step("click craftable oxygen", 30, "mest_23_gt_gas_craft",
                    mc -> click(mc, oxygen, 0, false)));
            steps.add(new MestDevTest.Step("verify oxygen craft screen", 5, null, mc -> {
                boolean ok = mc.screen instanceof appeng.client.gui.me.crafting.CraftAmountScreen;
                MestDevTest.log("{} gt oxygen click opened {}", ok ? "PASS" : "FAIL",
                        mc.screen == null ? null : mc.screen.getClass().getSimpleName());
                if (ok) {
                    appeng.client.gui.implementations.AESubScreen.goBack();
                }
            }));
            steps.add(new MestDevTest.Step("back from oxygen", 30, null, mc -> {
            }));
        }
    }

    private static AEFluidKey gtFluid(String name) {
        var fluid = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getValue(
                new net.minecraft.resources.ResourceLocation("gtceu", name));
        return fluid == null || fluid == Fluids.EMPTY ? null : AEFluidKey.of(fluid);
    }

    private static void stockGregTech(ServerPlayer player, AEFluidKey oil, AEFluidKey oxygen) {
        var host = GridHelper.getNodeHost(player.serverLevel(), MestDevTest.base);
        var node = host == null ? null : host.getGridNode(Direction.UP);
        if (node == null || node.getGrid() == null) {
            MestDevTest.log("FAIL gt stock: no grid");
            return;
        }
        var storage = node.getGrid().getStorageService().getInventory();
        for (AEKey key : List.of(oil, oxygen)) {
            storage.extract(key, Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty());
        }
        long inserted = storage.insert(oil, 16L * AEFluidKey.AMOUNT_BUCKET, Actionable.MODULATE,
                IActionSource.empty());
        if (player.serverLevel().getBlockEntity(MestDevTest.base.east()) instanceof PatternProviderBlockEntity provider) {
            provider.getLogic().getPatternInv().setItemDirect(1, PatternDetailsHelper.encodeProcessingPattern(
                    new GenericStack[] { new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1) },
                    new GenericStack[] { new GenericStack(oxygen, AEFluidKey.AMOUNT_BUCKET) }));
        }
        MestDevTest.log("gt stock: oil={} (bucket {}) oxygen bucket={}", inserted, oil.getFluid().getBucket(),
                oxygen.getFluid().getBucket());
    }

    private static void stock(ServerPlayer player) {
        var level = player.serverLevel();
        var host = GridHelper.getNodeHost(level, MestDevTest.base);
        var node = host == null ? null : host.getGridNode(Direction.UP);
        if (node == null || node.getGrid() == null) {
            MestDevTest.log("FAIL fluid stock: no grid");
            return;
        }
        var storage = node.getGrid().getStorageService().getInventory();
        // Start each run from the same amounts: no stray lava, a fixed pile of water and buckets.
        for (AEKey key : List.of(AEFluidKey.of(Fluids.LAVA), AEFluidKey.of(Fluids.WATER),
                AEItemKey.of(Items.BUCKET), AEItemKey.of(Items.WATER_BUCKET))) {
            storage.extract(key, Long.MAX_VALUE, Actionable.MODULATE, IActionSource.empty());
        }
        long water = storage.insert(AEFluidKey.of(Fluids.WATER), 64L * AEFluidKey.AMOUNT_BUCKET,
                Actionable.MODULATE, IActionSource.empty());
        long buckets = storage.insert(AEItemKey.of(Items.BUCKET), 4, Actionable.MODULATE, IActionSource.empty());
        String pattern = "no provider";
        if (level.getBlockEntity(MestDevTest.base.east()) instanceof PatternProviderBlockEntity provider) {
            var inv = provider.getLogic().getPatternInv();
            for (int i = 0; i < inv.size(); i++) {
                inv.setItemDirect(i, ItemStack.EMPTY);
            }
            inv.setItemDirect(0, PatternDetailsHelper.encodeProcessingPattern(
                    new GenericStack[] { new GenericStack(AEItemKey.of(Items.COBBLESTONE), 1) },
                    new GenericStack[] { new GenericStack(AEFluidKey.of(Fluids.LAVA), AEFluidKey.AMOUNT_BUCKET) }));
            pattern = "cobblestone -> lava";
        }
        MestDevTest.log("fluid stock: water={} buckets={} pattern={}", water, buckets, pattern);
    }

    private static void carry(ServerPlayer player, ItemStack stack) {
        player.containerMenu.setCarried(stack);
        player.containerMenu.broadcastChanges();
    }

    /** Clicks the ME list slot showing {@code key}, through the screen like the mouse would. */
    private static void click(Minecraft mc, AEKey key, int button, boolean shift) {
        if (!(mc.screen instanceof MESTScreen screen)) {
            MestDevTest.log("FAIL fluid click: screen {}", mc.screen);
            return;
        }
        var list = screen.meListPanel();
        double[] at = null;
        for (int y = list.y; y < list.y + list.height && at == null; y += 2) {
            for (int x = list.x; x < list.x + list.width; x += 2) {
                RepoSlot slot = list.repoSlotAt(x, y);
                if (slot != null && slot.getEntry() != null && key.equals(slot.getEntry().getWhat())) {
                    at = new double[] { x + 4, y + 4 };
                    break;
                }
            }
        }
        if (at == null) {
            MestDevTest.log("FAIL {} not shown in the ME list", key);
            return;
        }
        var entry = list.repoSlotAt(at[0], at[1]).getEntry();
        MestDevTest.log("clicking {} (stored={} craftable={}) button={} shift={} carried={}", key,
                entry.getStoredAmount(), entry.isCraftable(), button, shift, mc.player.containerMenu.getCarried());
        if (shift) {
            // Shift is read from the real keyboard, so deliver the shift-click as the slot click it becomes.
            try {
                var method = MESTScreen.class.getDeclaredMethod("slotClicked",
                        net.minecraft.world.inventory.Slot.class, int.class, int.class,
                        net.minecraft.world.inventory.ClickType.class);
                var slot = list.repoSlotAt(at[0], at[1]);
                method.setAccessible(true);
                method.invoke(screen, slot, slot.index, button, net.minecraft.world.inventory.ClickType.QUICK_MOVE);
            } catch (ReflectiveOperationException e) {
                MestDevTest.LOGGER.error("MEST-DEVTEST FAIL shift click", e);
            }
            return;
        }
        // Through Minecraft's mouse handler, so mods listening to screen mouse events see the click.
        try {
            double scale = mc.getWindow().getGuiScale();
            var xpos = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos");
            var ypos = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos");
            xpos.setAccessible(true);
            ypos.setAccessible(true);
            xpos.setDouble(mc.mouseHandler, at[0] * scale);
            ypos.setDouble(mc.mouseHandler, at[1] * scale);
            var onPress = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onPress",
                    long.class, int.class, int.class, int.class);
            onPress.setAccessible(true);
            long window = mc.getWindow().getWindow();
            onPress.invoke(mc.mouseHandler, window, button, org.lwjgl.glfw.GLFW.GLFW_PRESS, 0);
            onPress.invoke(mc.mouseHandler, window, button, org.lwjgl.glfw.GLFW.GLFW_RELEASE, 0);
        } catch (ReflectiveOperationException e) {
            MestDevTest.LOGGER.error("MEST-DEVTEST FAIL mouse press", e);
        }
    }

    private static void verifyCarried(ServerPlayer player, net.minecraft.world.item.Item expected, String label) {
        ItemStack carried = player.containerMenu.getCarried();
        MestDevTest.log("{} fluid {}: carried {}", carried.is(expected) ? "PASS" : "FAIL", label, carried);
    }

    private static void verifyShiftBucket(ServerPlayer player) {
        int buckets = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(Items.WATER_BUCKET)) {
                buckets += stack.getCount();
            }
        }
        MestDevTest.log("{} fluid shift-click: {} water bucket(s) in inventory, carried {}",
                buckets > 0 ? "PASS" : "FAIL", buckets, player.containerMenu.getCarried());
        player.getInventory().clearOrCountMatchingItems(s -> s.is(Items.WATER_BUCKET), -1,
                player.inventoryMenu.getCraftSlots());
    }
}
