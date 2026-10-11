package com.lhy.mest;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

import com.lhy.mest.config.MestConfig;
import com.lhy.mest.module.MestModuleSlots;
import com.lhy.mest.network.MestPackets;
import com.lhy.mest.registry.ModItems;
import com.lhy.mest.registry.ModMenus;
import com.lhy.mest.registry.ModRecipes;
import com.lhy.mest.terminal.MestTerminal;
import com.lhy.mest.terminal.ToolkitBarEvents;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(MESplicedterminal.MODID)
public class MESplicedterminal {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "mesplicedterminal";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public MESplicedterminal() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModItems.CREATIVE_TABS.register(modEventBus);
        ModRecipes.SERIALIZERS.register(modEventBus);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MestConfig.SPEC);

        // AE2WTLib 15 creates its own items (the WUT) during its ITEM RegisterEvent, and its
        // WUTHandler.addTerminal needs them. Our mod loads after AE2WTLib, and this listener is
        // added after our DeferredRegister, so the terminal item already exists when it runs.
        modEventBus.addListener((RegisterEvent event) -> {
            if (event.getRegistryKey().equals(ForgeRegistries.Keys.ITEMS)) {
                MestTerminal.registerTerminal();
            }
        });
        modEventBus.addListener(MestTerminal::onCommonSetup);
        modEventBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(MestModuleSlots::collect));
        modEventBus.addListener((FMLCommonSetupEvent event) -> MestPackets.register());

        ToolkitBarEvents.register();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> com.lhy.mest.client.MestClient.init(modEventBus));
    }
}
