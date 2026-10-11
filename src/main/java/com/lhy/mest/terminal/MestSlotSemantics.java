package com.lhy.mest.terminal;

import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

/**
 * Slot groups owned by MEST modules. AE2's pattern encoding terminal reuses generic semantics such
 * as CRAFTING_GRID, but this menu also has a real crafting terminal. Keeping the pattern encoder on
 * separate semantics prevents the floating crafting panel from picking up encoder slots.
 */
public final class MestSlotSemantics {
    private MestSlotSemantics() {}

    public static final SlotSemantic PATTERN_CRAFTING_GRID =
            SlotSemantics.register("MEST_PATTERN_CRAFTING_GRID", false);
    public static final SlotSemantic PATTERN_CRAFTING_RESULT =
            SlotSemantics.register("MEST_PATTERN_CRAFTING_RESULT", false);
    public static final SlotSemantic PATTERN_PROCESSING_INPUTS =
            SlotSemantics.register("MEST_PATTERN_PROCESSING_INPUTS", false);
    public static final SlotSemantic PATTERN_PROCESSING_OUTPUTS =
            SlotSemantics.register("MEST_PATTERN_PROCESSING_OUTPUTS", false);
    public static final SlotSemantic PATTERN_STONECUTTING_INPUT =
            SlotSemantics.register("MEST_PATTERN_STONECUTTING_INPUT", false);
    public static final SlotSemantic PATTERN_SMITHING_TEMPLATE =
            SlotSemantics.register("MEST_PATTERN_SMITHING_TEMPLATE", false);
    public static final SlotSemantic PATTERN_SMITHING_BASE =
            SlotSemantics.register("MEST_PATTERN_SMITHING_BASE", false);
    public static final SlotSemantic PATTERN_SMITHING_ADDITION =
            SlotSemantics.register("MEST_PATTERN_SMITHING_ADDITION", false);
    public static final SlotSemantic PATTERN_CACHE =
            SlotSemantics.register("MEST_PATTERN_CACHE", false);
    public static final SlotSemantic TOOLKIT =
            SlotSemantics.register("MEST_TOOLKIT", false);
    /**
     * The terminal's network-tool upgrade slots. Registered with AE2's own {@code TOOLBOX} flag
     * (player side), so they behave like a vanilla network tool's toolbox.
     */
    public static final SlotSemantic NETWORK_TOOLKIT =
            SlotSemantics.register("MEST_NETWORK_TOOLKIT", true);
}
