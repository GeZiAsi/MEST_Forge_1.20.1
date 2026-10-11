package com.lhy.mest.api;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import com.lhy.mest.module.OrderedModuleRegistry;

/**
 * Mod-bus event, fired on both sides during common setup: register the menu slots of an add-on
 * module. The module id must equal the {@code id()} of the panel registered for it on the client.
 */
public final class RegisterMestModuleSlotsEvent extends Event implements IModBusEvent {
    private final OrderedModuleRegistry<MestModuleSlotProvider> registry;

    public RegisterMestModuleSlotsEvent(OrderedModuleRegistry<MestModuleSlotProvider> registry) {
        this.registry = registry;
    }

    public void register(String moduleId, MestModuleSlotProvider provider) {
        registry.register(moduleId, provider);
    }
}
