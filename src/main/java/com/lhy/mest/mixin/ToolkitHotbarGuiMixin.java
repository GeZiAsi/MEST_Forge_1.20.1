package com.lhy.mest.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import com.lhy.mest.terminal.ToolkitBarState;

@Mixin(Gui.class)
public class ToolkitHotbarGuiMixin {
    @Final
    @Shadow
    private Minecraft minecraft;

    /** The second widgets blit in {@code renderHotbar} is the vanilla selection frame. */
    @WrapWithCondition(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 1))
    private boolean mest$skipVanillaHotbarSelection(
            GuiGraphics graphics, ResourceLocation texture, int x, int y, int u, int v, int width, int height) {
        Player player = minecraft.player;
        return player == null || !ToolkitBarState.isToolkitSelected(player);
    }
}
