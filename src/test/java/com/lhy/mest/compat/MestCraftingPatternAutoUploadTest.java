package com.lhy.mest.compat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MestCraftingPatternAutoUploadTest {
    @Test
    void fallbackCraftingGroupIdsCoverEcoAndExtendedAe() {
        assertTrue(MestCraftingPatternAutoUpload.FALLBACK_CRAFTING_GROUP_IDS.contains(
                "neoecoae:crafting_system_l4"));
        assertTrue(MestCraftingPatternAutoUpload.FALLBACK_CRAFTING_GROUP_IDS.contains(
                "neoecoae:crafting_system_l9"));
        assertTrue(MestCraftingPatternAutoUpload.FALLBACK_CRAFTING_GROUP_IDS.contains(
                "expatternprovider:assembler_matrix_pattern"));
        assertTrue(MestCraftingPatternAutoUpload.FALLBACK_CRAFTING_GROUP_IDS.contains(
                "expatternprovider:ex_molecular_assembler"));
        assertFalse(MestCraftingPatternAutoUpload.FALLBACK_CRAFTING_GROUP_IDS.contains(
                "ae2:pattern_provider"));
    }

    @Test
    void ecoGroupFilterRejectsNull() {
        assertFalse(MestCraftingPatternAutoUpload.isEcoCraftingGroup(null));
    }
}
