package com.lhy.mest.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import com.lhy.mest.network.MestPackets;
import com.lhy.mest.network.PacketContext;

import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.client.gui.AESubScreen;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import com.lhy.mest.network.PatternProviderClientState;
import com.lhy.mest.network.PatternProviderListPacket;
import com.lhy.mest.network.PatternProviderLoc;
import com.lhy.mest.terminal.MESTMenu;

/** Owns all client-only state and UI routing for pattern-provider synchronization. */
public final class PatternProviderClientHandler {
    private static final ClientState STATE = new ClientState();

    private PatternProviderClientHandler() {
    }

    public static void handle(PatternProviderListPacket packet, PacketContext context) {
        if (!packet.isWellFormed()) {
            return;
        }

        var minecraft = Minecraft.getInstance();
        MESTScreen screen;
        if (minecraft.screen instanceof MESTScreen currentScreen) {
            screen = currentScreen;
        } else if (minecraft.screen instanceof AESubScreen<?, ?> subScreen
                && subScreen.getParent() instanceof MESTScreen parentScreen) {
            screen = parentScreen;
        } else {
            return;
        }
        if (minecraft.player == null
                || !(minecraft.player.containerMenu instanceof MESTMenu menu)
                || menu.containerId != packet.containerId()) {
            return;
        }

        ClientApplyResult result = STATE.apply(packet);
        if (result.requestResync()) {
            MestPackets.sendToServer(new PatternProviderListPacket.Request(menu.containerId, true, (byte) 0));
        }
        if (result.entries() != null) {
            screen.updatePatternProviders(result.entries());
        }
    }

    public static void beginSubscription(MESTMenu menu) {
        STATE.beginSession(menu);
    }

    public static void endSubscription(MESTMenu menu) {
        STATE.endSession(menu);
    }

    /** Immutable client view consumed by the pattern-access panel. */
    public record Entry(
            long epoch,
            long providerId,
            long revision,
            PatternContainerGroup group,
            int inventorySize,
            long sortOrder,
            Int2ObjectMap<ItemStack> slots,
            @Nullable PatternProviderLoc loc) {
    }

    private static final class ClientState {
        private record SortableEntry(String sortKey, Entry entry) {
        }

        private static final Comparator<SortableEntry> ENTRY_ORDER = Comparator
                .comparing(SortableEntry::sortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(sortable -> sortable.entry().sortOrder())
                .thenComparingLong(sortable -> sortable.entry().providerId());

        private final PatternProviderClientState<ItemStack, PatternContainerGroup> state =
                new PatternProviderClientState<>(new PatternProviderClientState.ValueOps<>() {
                    @Override
                    public ItemStack copy(ItemStack value) {
                        return value.copy();
                    }

                    @Override
                    public boolean isEmpty(ItemStack value) {
                        return value.isEmpty();
                    }

                    @Override
                    public boolean matches(ItemStack left, ItemStack right) {
                        return ItemStack.matches(left, right);
                    }
                });
        @Nullable
        private MESTMenu activeMenu;
        private final java.util.HashMap<Long, PatternProviderLoc> locations = new java.util.HashMap<>();

        void beginSession(MESTMenu menu) {
            boolean continueExistingEpoch = activeMenu == menu;
            activeMenu = menu;
            state.beginSession(menu.containerId, continueExistingEpoch);
            if (!continueExistingEpoch) {
                locations.clear();
            }
        }

        void endSession(MESTMenu menu) {
            if (activeMenu == menu) {
                activeMenu = null;
                state.endSession();
                locations.clear();
            }
        }

        ClientApplyResult apply(PatternProviderListPacket packet) {
            var slots = new java.util.HashMap<Integer, ItemStack>();
            for (var entry : packet.slots().int2ObjectEntrySet()) {
                slots.put(entry.getIntKey(), entry.getValue());
            }
            var update = new PatternProviderClientState.Update<>(
                    packet.containerId(),
                    packet.epoch(),
                    PatternProviderClientState.Operation.valueOf(packet.operation().name()),
                    packet.providerId(),
                    packet.revision(),
                    packet.chunkIndex(),
                    packet.chunkCount(),
                    packet.inventorySize(),
                    packet.sortOrder(),
                    packet.group(),
                    slots);
            var result = state.apply(update);
            if (packet.operation() == PatternProviderListPacket.Operation.RESET) {
                locations.clear();
            } else if (packet.operation() == PatternProviderListPacket.Operation.REMOVE) {
                locations.remove(packet.providerId());
            } else if (packet.operation() == PatternProviderListPacket.Operation.FULL && packet.loc() != null) {
                locations.put(packet.providerId(), packet.loc());
            }
            boolean requestResync = result.outcome() == PatternProviderClientState.Outcome.RESYNC_REQUIRED;
            if (result.outcome() != PatternProviderClientState.Outcome.CHANGED) {
                return new ClientApplyResult(null, requestResync);
            }

            var sortableEntries = new ArrayList<SortableEntry>(result.providers().size());
            for (var provider : result.providers()) {
                var providerSlots = new Int2ObjectArrayMap<ItemStack>();
                for (var slot : provider.slots().entrySet()) {
                    providerSlots.put(slot.getKey().intValue(), slot.getValue().copy());
                }
                var entry = new Entry(
                        provider.epoch(),
                        provider.providerId(),
                        provider.revision(),
                        provider.metadata(),
                        provider.inventorySize(),
                        provider.sortOrder(),
                        providerSlots,
                        locations.get(provider.providerId()));
                sortableEntries.add(new SortableEntry(entry.group().name().getString(), entry));
            }
            sortableEntries.sort(ENTRY_ORDER);
            var entries = new ArrayList<Entry>(sortableEntries.size());
            for (var sortable : sortableEntries) {
                entries.add(sortable.entry());
            }
            return new ClientApplyResult(List.copyOf(entries), requestResync);
        }
    }

    private record ClientApplyResult(@Nullable List<Entry> entries, boolean requestResync) {
    }
}