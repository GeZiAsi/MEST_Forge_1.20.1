package com.lhy.mest.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;

import appeng.api.inventories.InternalInventory;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.ToolboxMenu;
import appeng.menu.slot.RestrictedInputSlot;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.item.MestNetworkToolMenuHost;
import com.lhy.mest.terminal.MESTMenu;
import com.lhy.mest.terminal.MestNetworkToolkitAccess;

/**
 * AE machines only look for a carried {@code NetworkToolItem} and show 9 of its slots. Show every
 * slot of the larger MEST network toolkit, and when no tool is carried expose the spliced
 * terminal's built-in upgrade inventory as TOOLBOX slots.
 */
@Mixin(value = ToolboxMenu.class, remap = false)
public abstract class ToolboxMenuMixin {
    /** One-shot diagnostic: an empty machine toolbox with no terminal in reach. */
    @Unique
    private static boolean mest$warnedNoTerminal;

    @Shadow
    @Final
    private NetworkToolMenuHost inv;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void mest$attachTerminalNetworkToolkit(AEBaseMenu menu, CallbackInfo ci) {
        if (menu instanceof MESTMenu) {
            return;
        }
        AEBaseMenuAccessor access = (AEBaseMenuAccessor) menu;
        if (inv instanceof MestNetworkToolMenuHost) {
            InternalInventory upgrades = inv.getInternalInventory();
            for (int slot = 9; slot < upgrades.size(); slot++) {
                access.mest$addSlot(new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, slot),
                        SlotSemantics.TOOLBOX);
            }
            return;
        }
        if (!menu.getSlots(SlotSemantics.TOOLBOX).isEmpty()) {
            return;
        }
        Player player = menu.getPlayer();
        InternalInventory inventory = MestNetworkToolkitAccess.inventoryOf(player);
        if (inventory == null) {
            if (!mest$warnedNoTerminal && !menu.isClientSide()) {
                mest$warnedNoTerminal = true;
                MESplicedterminal.LOGGER.info(
                        "MEST: no spliced terminal in the player's inventory, so machine toolboxes stay empty");
            }
            return;
        }
        for (int slot = 0; slot < inventory.size(); slot++) {
            access.mest$addSlot(new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.UPGRADES, inventory, slot),
                    SlotSemantics.TOOLBOX);
        }
        MESplicedterminal.LOGGER.debug(
                "MEST: attached the spliced terminal's network toolkit ({} slots) to {}",
                inventory.size(), menu.getClass().getSimpleName());
    }
}
