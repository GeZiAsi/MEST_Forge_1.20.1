package com.lhy.mest.network;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import com.lhy.mest.MESplicedterminal;
import appeng.api.networking.IGrid;

import com.lhy.mest.compat.MestWtlibSupport;

/**
 * Keeps a machine menu opened from the spliced terminal valid while the player is using it
 * remotely (vanilla {@code stillValid} would close it for distance).
 *
 * <p>Re-verifies the target block and periodically checks for a linked spliced terminal.
 * Losing either revokes the remote keep-alive; normal menu validity can still allow local use.
 * This does not prove that a failed {@code stillValid} was caused only by distance, nor that the
 * carried terminal is connected to the original grid.
 */
@EventBusSubscriber(modid = MESplicedterminal.MODID)
public final class RemoteMenuAccess {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private RemoteMenuAccess() {
    }

    public static boolean open(ServerPlayer player, MenuProvider provider, ServerLevel level, BlockPos pos,
            IGrid originalGrid) {
        if (originalGrid == null) {
            return false;
        }
        var previousMenu = player.containerMenu;
        NetworkHooks.openScreen(player, provider, pos);
        return trackOpenedMenu(player, previousMenu, level, pos, originalGrid);
    }

    public static boolean trackOpenedMenu(ServerPlayer player, AbstractContainerMenu previousMenu,
            ServerLevel level, BlockPos pos, IGrid originalGrid) {
        if (player.containerMenu == previousMenu || player.containerMenu == player.inventoryMenu) {
            return false;
        }
        if (originalGrid == null) {
            return false;
        }
        track(player, level, pos, originalGrid);
        return true;
    }

    public static boolean keepsMenuValid(ServerPlayer player, AbstractContainerMenu menu) {
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || session.menu != menu || menu != player.containerMenu) {
            if (session != null) {
                SESSIONS.remove(player.getUUID());
            }
            return false;
        }
        ServerLevel level = player.server.getLevel(session.dimension);
        if (level == null || !level.isLoaded(session.pos)) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        var state = level.getBlockState(session.pos);
        if (state.isAir() || state.getBlock() != session.block) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        if (session.blockEntity != null
                && (session.blockEntity.isRemoved()
                || level.getBlockEntity(session.pos) != session.blockEntity)) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        if (!session.linkCheck.isLinked(player.serverLevel().getGameTime(),
                () -> MestWtlibSupport.hasLinkedTerminal(player, session.grid))) {
            SESSIONS.remove(player.getUUID());
            return false;
        }
        return true;
    }

    public static void clear(ServerPlayer player, AbstractContainerMenu menu) {
        Session session = SESSIONS.get(player.getUUID());
        if (session != null && session.menu == menu) {
            SESSIONS.remove(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onContainerClosed(PlayerContainerEvent.Close event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            clear(player, event.getContainer());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SESSIONS.remove(player.getUUID());
        }
    }

    private static void track(ServerPlayer player, ServerLevel level, BlockPos pos, IGrid grid) {
        SESSIONS.put(player.getUUID(), new Session(
                player.containerMenu,
                level.dimension(),
                pos.immutable(),
                level.getBlockState(pos).getBlock(),
                level.getBlockEntity(pos),
                grid));
    }

    private static final class Session {
        private final AbstractContainerMenu menu;
        private final ResourceKey<Level> dimension;
        private final BlockPos pos;
        private final Block block;
        @Nullable
        private final BlockEntity blockEntity;
        private final IGrid grid;
        private final RemoteMenuLinkCheck linkCheck = new RemoteMenuLinkCheck();

        private Session(AbstractContainerMenu menu, ResourceKey<Level> dimension, BlockPos pos, Block block,
                @Nullable BlockEntity blockEntity, IGrid grid) {
            this.menu = menu;
            this.dimension = dimension;
            this.pos = pos;
            this.block = block;
            this.blockEntity = blockEntity;
            this.grid = grid;
        }
    }
}
