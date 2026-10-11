package com.lhy.mest.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import com.lhy.mest.network.MestPackets;

import de.mari_023.ae2wtlib.AE2wtlibTags;
import de.mari_023.ae2wtlib.terminal.ItemWT;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.compat.MestWtlibSupport;
import com.lhy.mest.compat.plus.PlusScreenSupport;
import com.lhy.mest.network.MestPickBlockPacket;
import com.lhy.mest.terminal.ToolkitBarState;

/**
 * Survival middle-click pick is handled by wtlib only for the crafting terminal item.
 * When pick-block is enabled on MEST, send our own packet using the same public pick APIs.
 */
@EventBusSubscriber(modid = MESplicedterminal.MODID, value = Dist.CLIENT)
public final class MestWtlibClientEvents {
    private MestWtlibClientEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFillSearch(ScreenEvent.KeyPressed.Pre event) {
        if (!PlusScreenSupport.loaded() || !ModList.get().isLoaded("jei")) {
            return;
        }
        if (!(event.getScreen() instanceof MESTScreen screen)) {
            return;
        }
        if (PlusScreenSupport.fillSearchFromHoveredIngredient(
                screen.meListPanel(), event.getKeyCode(), event.getScanCode())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPickBlock(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isPickBlock()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        if (player.getAbilities().instabuild || player.isSpectator()) {
            return;
        }
        ItemStack picked = stackFromHit(minecraft);
        if (picked.isEmpty()) {
            return;
        }
        ToolkitBarState.PickMatch match = ToolkitBarState.selectMatchingItem(player, picked);
        if (match == ToolkitBarState.PickMatch.TOOLKIT) {
            ToolkitBarInput.applySelection(
                    player, ToolkitBarState.getBar(player), ToolkitBarState.getSlot(player));
            event.setCanceled(true);
            return;
        }
        if (match == ToolkitBarState.PickMatch.HOTBAR) {
            ToolkitBarInput.applySelection(player, ToolkitBarState.Bar.CENTER, player.getInventory().selected);
            return;
        }
        ItemStack mest = MestWtlibSupport.mestStack(player);
        if (mest.isEmpty() || !ItemWT.getBoolean(mest, AE2wtlibTags.PICK_BLOCK)) {
            return;
        }
        if (player.getInventory().findSlotMatchingItem(picked) != -1) {
            return;
        }
        MestPackets.sendToServer(new MestPickBlockPacket(picked));
    }

    private static ItemStack stackFromHit(Minecraft minecraft) {
        HitResult hit = minecraft.hitResult;
        if (hit == null || minecraft.level == null || minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        if (hit.getType() == HitResult.Type.BLOCK && hit instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = minecraft.level.getBlockState(pos);
            return state.getCloneItemStack(blockHit, minecraft.level, pos, minecraft.player);
        }
        if (hit.getType() == HitResult.Type.ENTITY && hit instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            ItemStack stack = entity.getPickedResult(entityHit);
            return stack == null ? ItemStack.EMPTY : stack;
        }
        return ItemStack.EMPTY;
    }
}
