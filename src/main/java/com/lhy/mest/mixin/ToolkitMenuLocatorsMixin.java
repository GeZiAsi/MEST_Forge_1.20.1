package com.lhy.mest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;

import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;

import com.lhy.mest.terminal.ToolkitItemLocator;

@Mixin(value = MenuLocators.class, remap = false)
public abstract class ToolkitMenuLocatorsMixin {
    @Inject(method = "forHand", at = @At("HEAD"), cancellable = true)
    private static void mest$forHand(
            Player player, InteractionHand hand, CallbackInfoReturnable<MenuLocator> cir) {
        MenuLocator locator = ToolkitItemLocator.forHand(player, hand);
        if (locator != null) {
            cir.setReturnValue(locator);
        }
    }

    @Inject(method = "forItemUseContext", at = @At("HEAD"), cancellable = true)
    private static void mest$forItemUseContext(
            UseOnContext context, CallbackInfoReturnable<MenuLocator> cir) {
        MenuLocator locator = ToolkitItemLocator.forUse(context);
        if (locator != null) {
            cir.setReturnValue(locator);
        }
    }
}
