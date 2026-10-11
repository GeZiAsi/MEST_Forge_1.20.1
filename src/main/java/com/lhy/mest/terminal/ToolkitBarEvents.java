package com.lhy.mest.terminal;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import com.lhy.mest.network.ToolkitBarSyncPacket;

public final class ToolkitBarEvents {
    private ToolkitBarEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(ToolkitBarEvents.class);
    }

    /**
     * Equip from the extra bar is not a vanilla hotbar swap. Cancel {@code use()} on both sides so
     * the client cannot predict it, and apply the copy-based swap only on the server.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        if (!ToolkitHand.isOverrideActive(player)) {
            return;
        }
        InteractionResult result = ToolkitBarActions.equipFromSelected(player, !player.level().isClientSide());
        if (result == null) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(result);
    }

    /** The toolkit belongs to the player and survives death and the return from the End. */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        Player original = event.getOriginal();
        Player replacement = event.getEntity();
        ToolkitBarState.data(replacement).load(ToolkitBarState.data(original).save());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ToolkitBarSyncPacket.sendFull(player);
        }
    }

    /** Respawn and dimension travel give the client a fresh player with an empty toolkit copy. */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ToolkitBarSyncPacket.sendFull(player);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ToolkitBarSyncPacket.sendFull(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.player instanceof ServerPlayer player) {
            if (ToolkitBarState.storedBar(player) != ToolkitBarState.Bar.CENTER
                    && !ToolkitBarState.isBarEnabled(player)) {
                ToolkitBarState.setSelection(
                        player, ToolkitBarState.Bar.CENTER, player.getInventory().selected);
            }
            ToolkitBarState.tick(player);
        }
    }
}
