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
