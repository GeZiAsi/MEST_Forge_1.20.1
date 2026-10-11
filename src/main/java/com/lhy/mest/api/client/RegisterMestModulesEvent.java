package com.lhy.mest.api.client;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import com.lhy.mest.module.OrderedModuleRegistry;

/**
 * Client mod-bus event, fired during client setup: register an add-on module panel. The factory's
 * panel must report the same {@code id()} it is registered under.
 */
public final class RegisterMestModulesEvent extends Event implements IModBusEvent {
    private final OrderedModuleRegistry<MestModuleFactory> registry;

    public RegisterMestModulesEvent(OrderedModuleRegistry<MestModuleFactory> registry) {
        this.registry = registry;
    }

    public void register(String moduleId, MestModuleFactory factory) {
        registry.register(moduleId, factory);
    }
}
