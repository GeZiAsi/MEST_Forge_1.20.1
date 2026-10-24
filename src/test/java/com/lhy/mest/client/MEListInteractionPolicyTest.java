package com.lhy.mest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MEListInteractionPolicyTest {
    @Test
    void selectsNativeContainerActions() {
        assertEquals(MEListInteractionPolicy.ContainerAction.EMPTY_ONE,
                MEListInteractionPolicy.emptyContainerAction(false));
        assertEquals(MEListInteractionPolicy.ContainerAction.EMPTY_ALL,
                MEListInteractionPolicy.emptyContainerAction(true));
    }

    @Test
    void preservesCraftableOnlyClickSemantics() {
        assertTrue(MEListInteractionPolicy.shouldCraftOnClick(true, 64, true));
        assertTrue(MEListInteractionPolicy.shouldCraftOnClick(false, 0, true));
        assertFalse(MEListInteractionPolicy.shouldCraftOnClick(false, 64, true));
        assertFalse(MEListInteractionPolicy.shouldCraftOnClick(false, 0, false));
    }

    @Test
    void mapsShiftScrollDirectionAndNotches() {
        assertEquals(MEListInteractionPolicy.ScrollAction.INSERT_ONE,
                MEListInteractionPolicy.scrollAction(1));
        assertEquals(MEListInteractionPolicy.ScrollAction.EXTRACT_ONE,
                MEListInteractionPolicy.scrollAction(-1));
        assertEquals(3, MEListInteractionPolicy.scrollSteps(-3.75));
        assertEquals(0, MEListInteractionPolicy.scrollSteps(0.5));
        assertThrows(IllegalArgumentException.class, () -> MEListInteractionPolicy.scrollAction(0));
    }
}
