package com.lhy.mest.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.util.ReadableNumberConverter;

import de.mari_023.ae2wtlib.AE2wtlibTags;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;

import com.lhy.mest.client.MestRestockClient;
import com.lhy.mest.compat.MestWtlibSupport;

/** Same hotbar overlay as wtlib's RestockRender, but for the spliced terminal. */
@Mixin(Gui.class)
public class GuiRestockMixin {
    @Final
    @Shadow
    private Minecraft minecraft;

    @Inject(
            method = "renderSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;II)V"),
            cancellable = true,
            require = 1)
    private void mest$restockOverlay(
            GuiGraphics guiGraphics,
            int x,
            int y,
            float partialTick,
            Player player,
            ItemStack stack,
            int seed,
            CallbackInfo ci) {
        if (minecraft.player == null || minecraft.player.isCreative()) {
            return;
        }
        CraftingTerminalHandler wct = CraftingTerminalHandler.getCraftingTerminalHandler(minecraft.player);
        if (ItemWT.getBoolean(wct.getCraftingTerminal(), AE2wtlibTags.RESTOCK)) {
            return;
        }
        ItemStack mest = MestWtlibSupport.mestStack(minecraft.player);
        if (mest.isEmpty() || !ItemWT.getBoolean(mest, AE2wtlibTags.RESTOCK)) {
            return;
        }
        if (stack.getCount() == 1 || !MestRestockClient.isRestockAble(stack)) {
            return;
        }
        String number = ReadableNumberConverter.format(MestRestockClient.accessibleAmount(stack), 3);
        if (number.startsWith(",")) {
            number = "0" + number;
        }
        guiGraphics.renderItemDecorations(minecraft.font, stack, x, y, number);
        ci.cancel();
    }
}
