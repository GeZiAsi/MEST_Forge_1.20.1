package com.lhy.mest.module;

import com.lhy.mest.util.MestLists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


import org.junit.jupiter.api.Test;

class OrderedModuleRegistryTest {

    @Test
    void iteratesInIdOrderRegardlessOfRegistrationOrder() {
        var first = new OrderedModuleRegistry<String>(BuiltinModules.IDS);
        first.register("zeta:panel", "z");
        first.register("alpha:panel", "a");
        var second = new OrderedModuleRegistry<String>(BuiltinModules.IDS);
        second.register("alpha:panel", "a");
        second.register("zeta:panel", "z");

        assertEquals(first.entries(), second.entries());
        assertEquals("alpha:panel", MestLists.first(first.entries()).getKey());
    }

    @Test
    void rejectsBuiltinsReservedActionsBadIdsAndDuplicates() {
        var registry = new OrderedModuleRegistry<String>(BuiltinModules.IDS);
        registry.register("mymod:panel", "x");

        assertThrows(IllegalArgumentException.class, () -> registry.register("trash", "x"));
        assertThrows(IllegalArgumentException.class, () -> registry.register("action:cycle_preset", "x"));
        assertThrows(IllegalArgumentException.class, () -> registry.register("MyMod:Panel", "x"));
        assertThrows(IllegalArgumentException.class, () -> registry.register("", "x"));
        assertThrows(IllegalArgumentException.class, () -> registry.register("mymod:panel", "y"));
    }

    @Test
    void closesAfterTheEvent() {
        var registry = new OrderedModuleRegistry<String>(BuiltinModules.IDS);
        registry.freeze();

        assertThrows(IllegalStateException.class, () -> registry.register("mymod:panel", "x"));
    }

    @Test
    void semanticIdsAreUpperCaseAndSafe() {
        assertEquals("MEST_MODULE_MYMOD_BIG_PANEL", MestModuleSlots.semanticId("mymod:big-panel"));
    }
}
