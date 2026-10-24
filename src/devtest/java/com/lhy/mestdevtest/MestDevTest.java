package com.lhy.mestdevtest;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.LoadingErrorScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

import appeng.api.config.Actionable;
import appeng.api.networking.GridHelper;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.blockentity.storage.DriveBlockEntity;
import appeng.core.definitions.AEBlocks;
import appeng.core.definitions.AEItems;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.locator.MenuLocators;

import com.lhy.mest.client.MESTScreen;
import com.lhy.mest.item.ItemMEST;
import com.lhy.mest.registry.ModItems;

/**
 * Development-only driver (never shipped): with {@code -Dmest.devtest=true} it creates or loads a
 * flat test world, builds a small powered ME network, hands the player a linked spliced terminal,
 * opens it the normal way and saves screenshots to {@code screenshots/}. Each step is logged with
 * the {@code MEST-DEVTEST} prefix so a run can be checked from the log alone.
 */
@Mod(MestDevTest.MODID)
public class MestDevTest {
    public static final String MODID = "mestdevtest";
    static final Logger LOGGER = LogUtils.getLogger();
    // Separate worlds per mod set: loading one saved with the GTL stack without it stops at the
    // missing-registry prompt.
    private static final String WORLD = net.minecraftforge.fml.ModList.get().isLoaded("gtlcore") ? "mestdev_gtl" : "mestdev";

    private enum Stage { TITLE, WORLD, BUILD, FILL, OPEN, SHOOT, ACTIONS, DONE }

    private Stage stage = Stage.TITLE;
    private int ticks;
    static BlockPos base;
    private final List<Step> steps = new ArrayList<>();
    private int stepIndex;

    public MestDevTest() {
        if (FMLEnvironment.dist == Dist.CLIENT && Boolean.getBoolean("mest.devtest")) {
            MinecraftForge.EVENT_BUS.addListener(this::onClientTick);
            log("enabled");
            resetLayout();
            if (Boolean.getBoolean("mest.devtest.quit")) {
                startWatchdog();
            }
        }
    }

    /**
     * Panel toggles are saved, so start every run from the bundled default layout, or from a copy
     * of the layout files in {@code mest.devtest.layout} when that is set.
     */
    private static void resetLayout() {
        var dir = net.minecraftforge.fml.loading.FMLPaths.CONFIGDIR.get().resolve("mesplicedterminal");
        String seed = System.getProperty("mest.devtest.layout");
        for (String file : new String[] { "layout.json", "presets.json", "preferences.json" }) {
            try {
                java.nio.file.Files.deleteIfExists(dir.resolve(file));
                if (seed != null && java.nio.file.Files.exists(java.nio.file.Path.of(seed, file))) {
                    java.nio.file.Files.createDirectories(dir);
                    java.nio.file.Files.copy(java.nio.file.Path.of(seed, file), dir.resolve(file));
                    log("seeded {} from {}", file, seed);
                }
            } catch (java.io.IOException e) {
                log("could not reset {}: {}", file, e);
            }
        }
    }

    /**
     * A failed mod load never starts the Forge event bus, so the tick driver cannot dismiss the
     * error screen or quit; this thread does, and also bounds the whole run.
     */
    private static void startWatchdog() {
        long deadline = System.currentTimeMillis() + Long.getLong("mest.devtest.timeout", 600) * 1000L;
        Thread watchdog = new Thread(() -> {
            int errorScreenPolls = 0;
            while (true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
                Minecraft mc = Minecraft.getInstance();
                errorScreenPolls = mc != null && mc.screen instanceof LoadingErrorScreen ? errorScreenPolls + 1 : 0;
                if (errorScreenPolls >= 5) {
                    LOGGER.error("MEST-DEVTEST FAIL mod loading failed, quitting");
                    logLoadingProblems((LoadingErrorScreen) mc.screen);
                    mc.execute(mc::stop);
                    errorScreenPolls = Integer.MIN_VALUE;
                }
                if (System.currentTimeMillis() > deadline) {
                    LOGGER.error("MEST-DEVTEST FAIL timed out, halting");
                    Runtime.getRuntime().halt(2);
                }
            }
        }, "MEST devtest watchdog");
        watchdog.setDaemon(true);
        watchdog.start();
    }

    static void log(String message, Object... args) {
        LOGGER.info("MEST-DEVTEST " + message, args);
    }

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ticks++;
        try {
            switch (stage) {
                case TITLE -> {
                    if (mc.screen instanceof LoadingErrorScreen errorScreen) {
                        logLoadingProblems(errorScreen);
                        mc.setScreen(new TitleScreen(true));
                    }
                    // A fresh game directory shows the accessibility onboarding screen first.
                    if (mc.screen instanceof AccessibilityOnboardingScreen) {
                        mc.options.onboardAccessibility = false;
                        mc.options.save();
                        mc.setScreen(new TitleScreen(true));
                        log("skipped accessibility onboarding");
                    }
                    if (ticks % 200 == 0) {
                        log("waiting for title screen, current={}", mc.screen);
                    }
                    if (mc.screen instanceof TitleScreen title && ticks > 40) {
                        openWorld(mc, title);
                        stage = Stage.WORLD;
                        ticks = 0;
                    }
                }
                case WORLD -> {
                    if (mc.player != null && mc.getSingleplayerServer() != null && mc.level != null && ticks > 60) {
                        stage = Stage.BUILD;
                        ticks = 0;
                    }
                }
                case BUILD -> {
                    runOnServer(mc, this::buildNetwork);
                    stage = Stage.FILL;
                    ticks = 0;
                }
                case FILL -> {
                    if (ticks == 30) {
                        runOnServer(mc, this::insertCells);
                    }
                    if (ticks > 120) {
                        runOnServer(mc, this::fillNetworkAndGiveTerminal);
                        stage = Stage.OPEN;
                        ticks = 0;
                    }
                }
                case OPEN -> {
                    if (ticks == 40) {
                        runOnServer(mc, this::openTerminal);
                    }
                    if (mc.screen instanceof MESTScreen) {
                        log("terminal screen opened: {}", mc.screen.getClass().getName());
                        stage = Stage.SHOOT;
                        ticks = 0;
                    } else if (ticks > 200) {
                        log("FAIL terminal screen did not open, screen={}", mc.screen);
                        screenshot(mc, "mest_fail_open");
                        stage = Stage.DONE;
                    }
                }
                case SHOOT -> {
                    if (ticks == 1) {
                        mc.options.guiScale().set(Integer.getInteger("mest.devtest.guiScale", 2));
                        mc.resizeDisplay();
                        mc.getToasts().clear();
                    }
                    if (ticks == 60) {
                        screenshot(mc, "mest_01_open");
                        DevTestSteps.register(steps);
                        stage = Stage.ACTIONS;
                        ticks = 0;
                    }
                }
                case ACTIONS -> {
                    if (stepIndex >= steps.size()) {
                        stage = Stage.DONE;
                        ticks = 0;
                        break;
                    }
                    Step step = steps.get(stepIndex);
                    if (ticks == 1) {
                        log("step {} start: {}", stepIndex, step.name());
                        step.action().accept(mc);
                    }
                    if (ticks >= step.waitTicks()) {
                        if (step.screenshot() != null) {
                            screenshot(mc, step.screenshot());
                        }
                        stepIndex++;
                        ticks = 0;
                    }
                }
                case DONE -> {
                    if (ticks == 20) {
                        log("finished");
                        if (Boolean.getBoolean("mest.devtest.quit")) {
                            mc.stop();
                        }
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.error("MEST-DEVTEST FAIL in stage {}", stage, t);
            screenshot(mc, "mest_fail_exception");
            stage = Stage.DONE;
            ticks = 0;
        }
    }

    private static void logLoadingProblems(LoadingErrorScreen screen) {
        for (String field : new String[] { "modLoadErrors", "modLoadWarnings" }) {
            try {
                var f = LoadingErrorScreen.class.getDeclaredField(field);
                f.setAccessible(true);
                for (Object entry : (List<?>) f.get(screen)) {
                    String text = entry instanceof net.minecraftforge.fml.ModLoadingWarning warning
                            ? warning.formatToString()
                            : entry instanceof net.minecraftforge.fml.ModLoadingException exception
                                    ? exception.formatToString()
                                    : String.valueOf(entry);
                    log("loading {}: {}", field, text);
                }
            } catch (ReflectiveOperationException e) {
                log("could not read {}: {}", field, e);
            }
        }
    }

    private static void openWorld(Minecraft mc, TitleScreen title) {
        if (mc.getLevelSource().levelExists(WORLD)) {
            log("loading existing world {}", WORLD);
            mc.createWorldOpenFlows().loadLevel(title, WORLD);
            return;
        }
        log("creating world {}", WORLD);
        GameRules rules = new GameRules();
        rules.getRule(GameRules.RULE_DAYLIGHT).set(false, null);
        rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false, null);
        var settings = new LevelSettings(WORLD, GameType.CREATIVE, false, Difficulty.PEACEFUL, true, rules,
                WorldDataConfiguration.DEFAULT);
        mc.createWorldOpenFlows().createFreshLevel(WORLD, settings, new WorldOptions(1L, false, false),
                registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                        .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());
    }

    static void runOnServer(Minecraft mc, Consumer<ServerPlayer> work) {
        var server = mc.getSingleplayerServer();
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                log("FAIL no server player");
                return;
            }
            try {
                work.accept(player);
            } catch (Throwable t) {
                LOGGER.error("MEST-DEVTEST FAIL on server", t);
            }
        });
    }

    private void buildNetwork(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        base = player.blockPosition().offset(3, 0, 0);
        level.setBlockAndUpdate(base, AEBlocks.CONTROLLER.block().defaultBlockState());
        level.setBlockAndUpdate(base.north(), AEBlocks.CREATIVE_ENERGY_CELL.block().defaultBlockState());
        // The access point only connects through its back, so face it away from the controller.
        level.setBlockAndUpdate(base.above(), AEBlocks.WIRELESS_ACCESS_POINT.block().defaultBlockState()
                .setValue(BlockStateProperties.FACING, Direction.UP));
        level.setBlockAndUpdate(base.below(), AEBlocks.DRIVE.block().defaultBlockState());
        level.setBlockAndUpdate(base.east(), AEBlocks.PATTERN_PROVIDER.block().defaultBlockState());
        // A provider next to an assembler is a crafting container, the target of auto-upload.
        level.setBlockAndUpdate(base.east().east(), AEBlocks.MOLECULAR_ASSEMBLER.block().defaultBlockState());
        log("network built at {}", base);
    }

    /** Cells go in once the drive's grid node exists, otherwise the drive never mounts them. */
    private void insertCells(ServerPlayer player) {
        if (player.serverLevel().getBlockEntity(base.below()) instanceof DriveBlockEntity drive) {
            drive.getInternalInventory().setItemDirect(0, AEItems.ITEM_CELL_64K.stack());
            drive.getInternalInventory().setItemDirect(1, AEItems.ITEM_CELL_64K.stack());
            drive.getInternalInventory().setItemDirect(2, AEItems.FLUID_CELL_64K.stack());
            log("cells inserted");
        } else {
            log("FAIL drive block entity missing");
        }
    }

    private void fillNetworkAndGiveTerminal(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        var host = GridHelper.getNodeHost(level, base);
        var node = host == null ? null : host.getGridNode(Direction.UP);
        if (node == null || node.getGrid() == null) {
            log("FAIL controller has no grid node");
        } else {
            var storage = node.getGrid().getStorageService().getInventory();
            long total = 0;
            for (var item : List.of(Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.REDSTONE,
                    Items.COBBLESTONE, Items.OAK_LOG, Items.OAK_PLANKS, Items.STICK, Items.GLASS,
                    Items.COPPER_INGOT, Items.COAL, Items.QUARTZ, Items.EMERALD, Items.LAPIS_LAZULI)) {
                total += storage.insert(AEItemKey.of(item), 10_000, Actionable.MODULATE, IActionSource.empty());
            }
            storage.insert(AEItemKey.of(AEItems.BLANK_PATTERN.asItem()), 64, Actionable.MODULATE,
                    IActionSource.empty());
            log("inserted {} items, grid powered={}", total, node.isActive());
        }
        ItemStack terminal = new ItemStack(ModItems.SPLICED_TERMINAL.get());
        WirelessTerminalItem.LINKABLE_HANDLER.link(terminal, GlobalPos.of(level.dimension(), base.above()));
        ItemMEST item = (ItemMEST) terminal.getItem();
        item.injectAEPower(terminal, item.getAEMaxPower(terminal), Actionable.MODULATE);
        player.getInventory().setItem(0, terminal);
        player.getInventory().selected = 0;
        var wap = GridHelper.getNodeHost(level, base.above());
        var wapNode = wap == null ? null : wap.getGridNode(Direction.DOWN);
        log("terminal given and linked; wap node={} active={}", wapNode, wapNode != null && wapNode.isActive());
    }

    private void openTerminal(ServerPlayer player) {
        ItemStack stack = player.getInventory().getItem(0);
        if (!(stack.getItem() instanceof ItemMEST item)) {
            log("FAIL slot 0 holds {}", stack);
            return;
        }
        var grid = item.getLinkedGrid(stack, player.level(), null);
        log("linked grid={} linkedPos={}", grid, item.getLinkedPosition(stack));
        boolean opened = item.tryOpen(player, MenuLocators.forInventorySlot(0), stack, false);
        log("tryOpen returned {}", opened);
    }

    static void screenshot(Minecraft mc, String name) {
        Screenshot.grab(new File(mc.gameDirectory.getAbsolutePath()), name + ".png", mc.getMainRenderTarget(),
                message -> log("screenshot {}: {}", name, message.getString()));
    }

    record Step(String name, int waitTicks, String screenshot, Consumer<Minecraft> action) {
    }
}
