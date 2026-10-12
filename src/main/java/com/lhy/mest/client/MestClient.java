package com.lhy.mest.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import appeng.init.client.InitScreens;

import com.lhy.mest.network.PacketContext;
import com.lhy.mest.network.PatternProviderClientBridge;
import com.lhy.mest.network.ProviderPickerClientBridge;
import com.lhy.mest.terminal.MESTMenu;
import com.lhy.mest.terminal.MestMagnetMenu;

/**
 * Client-side setup: registers menu screens. AE2 resolves ScreenStyle JSON paths against its own
 * namespace, so we can reuse AE2's wireless terminal style directly without shipping a copy.
 */
public final class MestClient {
    private MestClient() {}

    public static void init(IEventBus modEventBus) {
        PacketContext.installClientPlayer(() -> Minecraft.getInstance().player);
        PatternProviderClientBridge.install(PatternProviderClientHandler::handle);
        ProviderPickerClientBridge.install(ProviderPickerClientHandler::handle);
        modEventBus.addListener(MestClient::onRegisterGuiOverlays);
        modEventBus.addListener(MestClient::onRegisterKeyMappings);
        modEventBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            registerScreens();
            MestClientModules.collect();
        }));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(
                net.minecraftforge.eventbus.api.EventPriority.LOWEST, MestClient::dropForeignSortButtons);
    }

    /**
     * Inventory sorters (Inventory Tweaks) add a sort button above the slot group they guess is the
     * container. The terminal's slots are spread over movable panels, so the button lands on
     * whatever panel is nearest, e.g. over the pattern-access search field. GTL's own config
     * already turns them off for AE2 terminals; do the same for this one.
     */
    private static boolean loggedSortButton;

    /** The terminal screen most recently covered by another screen (quest book, JEI, ...). */
    private static java.lang.ref.WeakReference<MESTScreen> coveredTerminal = new java.lang.ref.WeakReference<>(null);

    static void rememberCoveredTerminal(MESTScreen screen) {
        coveredTerminal = new java.lang.ref.WeakReference<>(screen);
    }

    /**
     * A covered terminal whose menu is still the player's open container, so showing that screen
     * again resumes the same terminal; {@code null} once it was really closed.
     */
    @org.jetbrains.annotations.Nullable
    public static MESTScreen coveredTerminal() {
        MESTScreen screen = coveredTerminal.get();
        var player = Minecraft.getInstance().player;
        return screen != null && player != null && player.containerMenu == screen.getMenu() ? screen : null;
    }

    private static void dropForeignSortButtons(net.minecraftforge.client.event.ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof MESTScreen) && !(event.getScreen() instanceof MestMagnetScreen)) {
            return;
        }
        for (var listener : java.util.List.copyOf(event.getListenersList())) {
            if (listener.getClass().getName().startsWith("invtweaks.")) {
                event.removeListener(listener);
                if (!loggedSortButton) {
                    loggedSortButton = true;
                    com.lhy.mest.MESplicedterminal.LOGGER.info("MEST: removed {} from the terminal screen",
                            listener.getClass().getName());
                }
            }
        }
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MestKeybindings.TOOLKIT_BAR_LEFT);
        event.register(MestKeybindings.TOOLKIT_BAR_RIGHT);
    }

    private static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAbove(
                VanillaGuiOverlay.HOTBAR.id(),
                "toolkit_bar",
                (gui, guiGraphics, partialTick, screenWidth, screenHeight) ->
                        ToolkitBarHud.render(guiGraphics, partialTick));
    }

    private static void registerScreens() {
        InitScreens.register(MESTMenu.TYPE, MESTScreen::new, "/screens/terminals/wireless_terminal.json");
        InitScreens.register(MestMagnetMenu.TYPE, MestMagnetScreen::new, "/screens/wtlib/magnet.json");
    }
}
