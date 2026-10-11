package com.lhy.mest.module;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModLoader;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

import com.lhy.mest.api.MestModuleSlotContext;
import com.lhy.mest.api.MestModuleSlotProvider;
import com.lhy.mest.api.RegisterMestModuleSlotsEvent;
import com.lhy.mest.terminal.MESTMenu;

/** Add-on module slots, collected once and added to every terminal menu in id order. */
public final class MestModuleSlots {
    private record Entry(String moduleId, MestModuleSlotProvider provider, SlotSemantic semantic) {
    }

    private static final OrderedModuleRegistry<MestModuleSlotProvider> REGISTRY =
            new OrderedModuleRegistry<>(BuiltinModules.IDS);
    private static List<Entry> entries = List.of();

    private MestModuleSlots() {
    }

    /** Common setup, on the main thread. */
    public static void collect() {
        if (REGISTRY.isFrozen()) {
            return;
        }
        ModLoader.get().postEvent(new RegisterMestModuleSlotsEvent(REGISTRY));
        REGISTRY.freeze();
        var built = new ArrayList<Entry>();
        for (Map.Entry<String, MestModuleSlotProvider> entry : REGISTRY.entries()) {
            built.add(new Entry(entry.getKey(), entry.getValue(),
                    SlotSemantics.register(semanticId(entry.getKey()), false)));
        }
        entries = List.copyOf(built);
    }

    static String semanticId(String moduleId) {
        return "MEST_MODULE_" + moduleId.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "_");
    }

    @Nullable
    public static SlotSemantic semantic(String moduleId) {
        for (Entry entry : entries) {
            if (entry.moduleId().equals(moduleId)) {
                return entry.semantic();
            }
        }
        return null;
    }

    public static void addTo(MESTMenu menu, ItemStack terminal, SlotAdder adder) {
        for (Entry entry : entries) {
            entry.provider().addSlots(new MestModuleSlotContext() {
                @Override
                public Player player() {
                    return menu.getPlayer();
                }

                @Override
                public ItemStack terminal() {
                    return terminal;
                }

                @Override
                public MESTMenu menu() {
                    return menu;
                }

                @Override
                public boolean isClientSide() {
                    return menu.isClientSide();
                }

                @Override
                public Slot addSlot(Slot slot) {
                    return adder.add(slot, entry.semantic());
                }
            });
        }
    }

    @FunctionalInterface
    public interface SlotAdder {
        Slot add(Slot slot, SlotSemantic semantic);
    }
}
