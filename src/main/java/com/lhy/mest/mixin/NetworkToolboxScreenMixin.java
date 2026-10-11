package com.lhy.mest.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.inventory.Slot;

import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.WidgetContainer;
import appeng.core.localization.GuiText;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.client.MESTScreen;
import com.lhy.mest.client.dock.ToolboxChrome;
import com.lhy.mest.terminal.MestNetworkToolkitAccess;

/**
 * AE2's toolbox chrome is a fixed 3×3 panel that is only added to some screens, and it stacks any
 * extra upgrade slots on top of itself. Every AE screen with TOOLBOX slots gets MEST's own compact
 * panel instead: 3×3 visible rows plus a scrollbar when the network toolkit holds more than nine
 * slots. Carrying a vanilla network tool keeps AE2's own panel.
 */
@Mixin(AEBaseScreen.class)
public abstract class NetworkToolboxScreenMixin {
    @Shadow(remap = false)
    @Final
    protected WidgetContainer widgets;

    @Unique
    private ToolboxChrome mest$toolbox;

    /** The vanilla {@code ToolboxPanel} we displaced, restored when a vanilla tool takes over. */
    @Unique
    private ICompositeWidget mest$replaced;

    /** GUI-relative position AE2 gave the first TOOLBOX slot, captured right after it placed them. */
    @Unique
    private Point mest$slotAnchor;

    @Inject(method = "init", at = @At("RETURN"))
    private void mest$installToolbox(CallbackInfo ci) {
        // AE2 re-populates widgets during init and re-anchors any "toolbox" composite to the vanilla
        // widget position, so the panel position has to be re-asserted here.
        mest$refreshToolbox((AEBaseScreen<?>) (Object) this, false);
    }

    @Inject(method = "repositionSlots", at = @At("RETURN"), remap = false)
    private void mest$repositionToolboxSlots(SlotSemantic semantic, CallbackInfo ci) {
        if (semantic != SlotSemantics.TOOLBOX) {
            return;
        }
        // AE2 just placed the slots, so their coordinates are the anchor to follow.
        mest$refreshToolbox((AEBaseScreen<?>) (Object) this, true);
        if (mest$toolbox != null) {
            mest$toolbox.placeSlots();
        }
    }

    @Unique
    private void mest$refreshToolbox(AEBaseScreen<?> screen, boolean captureAnchor) {
        if (screen instanceof MESTScreen) {
            return;
        }
        AEBaseMenu menu = screen.getMenu();
        List<Slot> slots = menu.getSlots(SlotSemantics.TOOLBOX);
        if (slots.isEmpty() || !MestNetworkToolkitAccess.isTerminalToolbox(slots.get(0))
                || !ToolboxChrome.hasVisibleSlots(slots)) {
            mest$removeToolbox();
            return;
        }
        if (captureAnchor) {
            mest$slotAnchor = new Point(slots.get(0).x, slots.get(0).y);
        }
        if (mest$slotAnchor == null) {
            return;
        }
        Point position = new Point(
                mest$slotAnchor.getX() - ToolboxChrome.PAD - 1 - ToolboxChrome.shiftX(),
                mest$slotAnchor.getY() - ToolboxChrome.PAD - 1);
        if (mest$toolbox != null && mest$toolbox.ownsSlot(slots.get(0))) {
            mest$toolbox.setPosition(position);
            return;
        }
        mest$toolbox = new ToolboxChrome(slots, GuiText.NetworkTool.text());
        mest$toolbox.setPosition(position);
        mest$replaced = ((WidgetContainerAccessor) widgets).mest$compositeWidgets().put("toolbox", mest$toolbox);
        MESplicedterminal.LOGGER.info(
                "MEST: network toolbox panel at {},{} anchored on slot {},{} ({} slots, skin shift {})",
                position.getX(), position.getY(), mest$slotAnchor.getX(), mest$slotAnchor.getY(),
                slots.size(), ToolboxChrome.shiftX());
    }

    @Unique
    private void mest$removeToolbox() {
        if (mest$toolbox == null) {
            return;
        }
        var composites = ((WidgetContainerAccessor) widgets).mest$compositeWidgets();
        if (composites.get("toolbox") == mest$toolbox) {
            if (mest$replaced != null) {
                composites.put("toolbox", mest$replaced);
            } else {
                composites.remove("toolbox");
            }
        }
        mest$toolbox.hide();
        mest$toolbox = null;
        mest$replaced = null;
    }
}
