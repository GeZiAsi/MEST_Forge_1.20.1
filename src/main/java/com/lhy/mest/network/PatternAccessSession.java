package com.lhy.mest.network;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.eventbus.api.Event;

import appeng.api.config.ShowPatternProviders;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.implementations.blockentities.PatternContainerGroup;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.helpers.patternprovider.PatternContainer;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.PlayerInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;

import com.lhy.mest.terminal.MESTMenu;

/**
 * Server-owned state for the pattern access module of one open MEST menu.
 */
public final class PatternAccessSession {
    private static final int SCAN_INTERVAL_TICKS = 5;
    private static final int MIN_RESNAPSHOT_INTERVAL_TICKS = 20;
    private static final int MAX_ACTIONS_PER_TICK = 16;
    private static final int MAX_PACKETS_PER_TICK = 16;
    private static final int MAX_PENDING_PACKETS = 2048;
    /** Safety margin below Minecraft's 1 MiB clientbound custom-payload limit. */
    private static final int MAX_CHUNK_PAYLOAD_BYTES = 256 * 1024;
    private static final int MAX_TRACKED_PROVIDERS = PatternProviderClientState.MAX_PROVIDERS;
    private static final int MAX_PROVIDER_SLOTS = PatternProviderClientState.MAX_INVENTORY_SIZE;
    private static final int MAX_TOTAL_TRACKED_SLOTS = PatternProviderClientState.MAX_TOTAL_INVENTORY_SLOTS;

    private static final AtomicLong NEXT_EPOCH = new AtomicLong(1);
    private static final IAEItemFilter ENCODED_PATTERN_FILTER = new IAEItemFilter() {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return true;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return !stack.isEmpty() && PatternDetailsHelper.isEncodedPattern(stack);
        }
    };

    private final MESTMenu menu;
    private final IdentityHashMap<PatternContainer, Tracker> trackers = new IdentityHashMap<>();
    private final Map<Long, Tracker> trackersById = new java.util.HashMap<>();
    private final ArrayDeque<PatternProviderListPacket> outbound = new ArrayDeque<>();
    private final PerGameTickBudget serverTickBudget = new PerGameTickBudget(1);
    private final PerGameTickBudget actionBudget = new PerGameTickBudget(MAX_ACTIONS_PER_TICK);
    private final PerGameTickBudget packetBudget = new PerGameTickBudget(MAX_PACKETS_PER_TICK);
    private final PerGameTickBudget subscribeRequestBudget = new PerGameTickBudget(1);

    private IGrid trackedGrid;
    private long epoch;
    private long nextProviderId = 1;
    private long ticks;
    private long lastSnapshotTick = -MIN_RESNAPSHOT_INTERVAL_TICKS;
    private boolean subscribed;
    private boolean snapshotRequired;
    private boolean queueInvalid;
    private ShowPatternProviders showMode = ShowPatternProviders.VISIBLE;
    // Tracked by provider object, not id: ids are reassigned from 1 on every snapshot rebuild.
    private PatternContainer lastUploadContainer;
    private int lastUploadSlot = -1;
    private ItemStack lastUploadPattern = ItemStack.EMPTY;

    public PatternAccessSession(MESTMenu menu) {
        this.menu = Objects.requireNonNull(menu);
    }

    public void setSubscribed(ServerPlayer player, boolean subscribe, byte showModeOrdinal) {
        if (menu.getPlayer() != player) {
            return;
        }
        if (!subscribe) {
            subscribed = false;
            clearServerState();
            return;
        }
        ShowPatternProviders[] values = ShowPatternProviders.values();
        ShowPatternProviders next = showModeOrdinal >= 0 && showModeOrdinal < values.length
                ? values[showModeOrdinal]
                : ShowPatternProviders.VISIBLE;
        boolean modeChanged = showMode != next;
        showMode = next;
        // Subscribe requests have their own budget so a client flooding request packets cannot
        // repeatedly force snapshot work within a single game tick.
        if (!subscribeRequestBudget.tryAcquire(player.serverLevel().getGameTime())) {
            return;
        }
        if (!subscribed) {
            subscribed = true;
            snapshotRequired = true;
        } else if (modeChanged) {
            snapshotRequired = true;
        } else {
            requestResnapshot();
        }
    }

    public void close() {
        subscribed = false;
        clearServerState();
    }

    public void serverTick() {
        if (!(menu.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        long gameTick = player.serverLevel().getGameTime();
        if (!serverTickBudget.tryAcquire(gameTick)) {
            return;
        }
        ticks++;
        if (!subscribed) {
            return;
        }

        if (!menu.canUsePatternAccess(player)) {
            resetForUnavailable();
            flush(player, gameTick);
            return;
        }

        var currentGrid = getCurrentGrid();
        if (currentGrid == null) {
            resetForUnavailable();
            flush(player, gameTick);
            return;
        }

        if (trackedGrid != currentGrid) {
            // A fresh menu subscribes immediately. Re-subscribing after an unsubscribe or disconnect is
            // throttled so alternating request packets cannot force a full snapshot every server tick.
            if (resnapshotCoolingDown()) {
                snapshotRequired = true;
            } else {
                rebuildSnapshot(currentGrid);
            }
        } else if (snapshotRequired && ticks - lastSnapshotTick >= MIN_RESNAPSHOT_INTERVAL_TICKS) {
            rebuildSnapshot(currentGrid);
        } else if (!snapshotRequired && ticks % SCAN_INTERVAL_TICKS == 0) {
            refresh(currentGrid);
        }

        flush(player, gameTick);
    }

    public void handleAction(ServerPlayer player, PatternProviderActionPacket packet) {
        if (!subscribed
                || player.containerMenu != menu
                || menu.containerId != packet.containerId()
                || !menu.canUsePatternAccess(player)) {
            return;
        }
        long gameTick = player.serverLevel().getGameTime();
        if (!actionBudget.tryAcquire(gameTick)) {
            return;
        }

        var currentGrid = getCurrentGrid();
        if (currentGrid == null) {
            return;
        }
        if (trackedGrid != currentGrid) {
            if (resnapshotCoolingDown()) {
                requestResnapshot();
                flush(player, gameTick);
                return;
            }
            rebuildSnapshot(currentGrid);
        }
        if (packet.epoch() != epoch) {
            requestResnapshot();
            flush(player, gameTick);
            return;
        }

        var tracker = trackersById.get(packet.providerId());
        if (tracker == null || !refreshTrackerMetadata(tracker, currentGrid)) {
            refresh(currentGrid);
            requestResnapshot();
            flush(player, gameTick);
            return;
        }
        refreshTrackerContents(tracker);
        if (packet.revision() != tracker.revision) {
            requestResnapshot();
            flush(player, gameTick);
            return;
        }
        if (packet.action() == PatternProviderActionPacket.Action.OPEN_PROVIDER_UI) {
            if (!openProviderUi(player, tracker)) {
                player.displayClientMessage(
                        Component.translatable("gui.mesplicedterminal.pattern_access.open_ui_failed"), true);
            }
            return;
        }
        if (packet.action() == PatternProviderActionPacket.Action.INSERT_INTO_PROVIDER) {
            insertFromPlayerSlot(player, tracker, packet.providerSlot());
            refreshTrackerContents(tracker);
            flush(player, gameTick);
            return;
        }
        if (packet.providerSlot() < 0 || packet.providerSlot() >= tracker.inventory.size()) {
            return;
        }

        var patternSlot = new FilteredInternalInventory(
                tracker.inventory.getSlotInv(packet.providerSlot()), ENCODED_PATTERN_FILTER);
        switch (packet.action()) {
            case PICKUP_OR_SET_DOWN -> exchangeWithCarried(patternSlot);
            case QUICK_MOVE_TO_PLAYER -> quickMoveToPlayer(player, patternSlot);
            case DROP -> dropFromSlot(player, patternSlot, false);
            case DROP_STACK -> dropFromSlot(player, patternSlot, true);
            case OPEN_PROVIDER_UI, INSERT_INTO_PROVIDER -> {
            }
        }

        refreshTrackerContents(tracker);
        flush(player, gameTick);
    }

    private void exchangeWithCarried(FilteredInternalInventory patternSlot) {
        var carried = menu.getCarried();
        if (!carried.isEmpty() && !PatternDetailsHelper.isEncodedPattern(carried)) {
            return;
        }
        menu.setCarried(PatternSlotTransactions.exchange(patternSlot, carried));
    }

    private boolean includeProvider(PatternContainer container, InternalInventory inventory) {
        if (showMode != ShowPatternProviders.ALL && !container.isVisibleInTerminal()) {
            return false;
        }
        return showMode != ShowPatternProviders.NOT_FULL || inventory == null || !inventoryFull(inventory);
    }

    private static boolean inventoryFull(InternalInventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return inventory.size() > 0;
    }

    private void insertFromPlayerSlot(ServerPlayer player, Tracker tracker, int menuSlotIndex) {
        if (menuSlotIndex < 0 || menuSlotIndex >= menu.slots.size()) {
            return;
        }
        var slot = menu.getSlot(menuSlotIndex);
        if (slot == null || !slot.mayPickup(player)) {
            return;
        }
        ItemStack source = slot.getItem();
        if (!PatternDetailsHelper.isEncodedPattern(source)) {
            return;
        }
        var filter = new FilteredInternalInventory(tracker.inventory, ENCODED_PATTERN_FILTER);
        ItemStack remaining = filter.addItems(source);
        slot.set(remaining);
        slot.setChanged();
    }

    private boolean openProviderUi(ServerPlayer player, Tracker tracker) {
        PatternProviderLoc loc = PatternProviderLoc.from(tracker.container);
        if (loc == null) {
            return false;
        }
        ServerLevel level = player.server.getLevel(loc.dimension());
        if (level == null || !level.isLoaded(loc.pos())) {
            return false;
        }
        return tryOpenTarget(player, tracker.container, level, loc.pos(), loc.face());
    }

    /**
     * Opens the UI of a block adjacent to the provider.
     *
     * <p>The menu-provider route is tried first because it only opens a container. Falling back to
     * {@code useWithoutItem} simulates a real right-click, which would fire every other interaction
     * the target block has (buttons, levers, machines with a use action) on a block the player may
     * be thousands of blocks away from. It is therefore limited to block entities in the player's
     * own dimension, where protection mods can veto it through {@code RightClickBlock}.
     */
    private boolean tryOpenTarget(ServerPlayer player, PatternContainer patternProvider,
            ServerLevel level, BlockPos pos, Direction face) {
        if (patternProvider instanceof PatternProviderLogicHost logicHost) {
            var targets = logicHost.getTargets();
            if (targets != null) {
                for (Direction direction : targets) {
                    if (tryTarget(player, level, pos.relative(direction), direction)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (face != null) {
            return tryTarget(player, level, pos.relative(face), face);
        }
        for (Direction direction : Direction.values()) {
            if (tryTarget(player, level, pos.relative(direction), direction)) {
                return true;
            }
        }
        return false;
    }

    private boolean tryTarget(ServerPlayer player, ServerLevel level, BlockPos targetPos,
            Direction providerToTarget) {
        if (!level.isLoaded(targetPos) || level.getBlockState(targetPos).isAir()) {
            return false;
        }
        // Spawn protection and world border, as for any other block interaction.
        if (!level.mayInteract(player, targetPos)) {
            return false;
        }
        var hit = new BlockHitResult(Vec3.atCenterOf(targetPos), providerToTarget.getOpposite(), targetPos, false);
        // The event reads the player's level, so it can only vouch for targets in that dimension.
        boolean sameLevel = level == player.level();
        if (sameLevel) {
            var event = ForgeHooks.onRightClickBlock(player, InteractionHand.MAIN_HAND, targetPos, hit);
            if (event.isCanceled() || event.getUseBlock() == Event.Result.DENY) {
                return false;
            }
        }
        if (tryOpenAt(player, level, targetPos)) {
            return true;
        }
        return sameLevel
                && level.getBlockEntity(targetPos) != null
                && tryUseTargetBlock(player, level, targetPos, hit);
    }

    private boolean tryOpenAt(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MenuProvider menuProvider) {
            return RemoteMenuAccess.open(player, menuProvider, level, pos, getCurrentGrid());
        }
        var provider = level.getBlockState(pos).getMenuProvider(level, pos);
        return provider != null && RemoteMenuAccess.open(player, provider, level, pos, getCurrentGrid());
    }

    private boolean tryUseTargetBlock(ServerPlayer player, ServerLevel level, BlockPos targetPos,
            BlockHitResult hit) {
        var previousMenu = player.containerMenu;
        level.getBlockState(targetPos).use(level, player, InteractionHand.MAIN_HAND, hit);
        return RemoteMenuAccess.trackOpenedMenu(player, previousMenu, level, targetPos, getCurrentGrid());
    }

    private static void quickMoveToPlayer(ServerPlayer player, FilteredInternalInventory patternSlot) {
        var playerInventory = new PlayerInternalInventory(player.getInventory());
        PatternSlotTransactions.quickMove(patternSlot, playerInventory);
    }

    private static void dropFromSlot(
            ServerPlayer player, FilteredInternalInventory patternSlot, boolean wholeStack) {
        ItemStack extracted = patternSlot.extractItem(0, wholeStack ? 64 : 1, false);
        if (!extracted.isEmpty()) {
            player.drop(extracted, false);
        }
    }

    private void resetForUnavailable() {
        if (trackedGrid == null && trackers.isEmpty() && !snapshotRequired) {
            return;
        }
        clearTrackingOnly();
        outbound.clear();
        queueInvalid = false;
        epoch = nextEpoch();
        queue(PatternProviderListPacket.reset(menu.containerId, epoch));
        snapshotRequired = false;
        lastSnapshotTick = ticks;
    }

    private void rebuildSnapshot(IGrid grid) {
        clearTrackingOnly();
        outbound.clear();
        queueInvalid = false;
        trackedGrid = grid;
        epoch = nextEpoch();
        nextProviderId = 1;
        queue(PatternProviderListPacket.reset(menu.containerId, epoch));

        for (var provider : collectProviders(grid)) {
            var tracker = new Tracker(nextProviderId++, provider.container(), provider.inventory(),
                    provider.group(), provider.sortOrder());
            trackers.put(provider.container(), tracker);
            trackersById.put(tracker.id, tracker);
            queueFull(tracker);
        }
        // An overflow during the rebuild already queued a RESET and asked for a new snapshot.
        snapshotRequired = queueInvalid;
        lastSnapshotTick = ticks;
    }

    private void refresh(IGrid grid) {
        var providers = collectProviders(grid);
        var seen = new IdentityHashMap<PatternContainer, Boolean>();
        for (var provider : providers) {
            seen.put(provider.container(), Boolean.TRUE);
            var tracker = trackers.get(provider.container());
            if (tracker == null) {
                tracker = new Tracker(nextProviderId++, provider.container(), provider.inventory(),
                        provider.group(), provider.sortOrder());
                trackers.put(provider.container(), tracker);
                trackersById.put(tracker.id, tracker);
                queueFull(tracker);
                continue;
            }

            if (tracker.inventory != provider.inventory()
                    || tracker.inventory.size() != provider.inventory().size()
                    || tracker.sortOrder != provider.sortOrder()
                    || !tracker.group.equals(provider.group())) {
                tracker.inventory = provider.inventory();
                tracker.group = provider.group();
                tracker.sortOrder = provider.sortOrder();
                tracker.snapshot = copyInventory(provider.inventory());
                tracker.bumpRevision();
                queueFull(tracker);
            } else {
                refreshTrackerContents(tracker);
            }
        }

        Iterator<Map.Entry<PatternContainer, Tracker>> iterator = trackers.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (!seen.containsKey(entry.getKey())) {
                var removed = entry.getValue();
                iterator.remove();
                trackersById.remove(removed.id);
                queue(PatternProviderListPacket.remove(menu.containerId, epoch, removed.id));
            }
        }
    }

    private boolean refreshTrackerMetadata(Tracker tracker, IGrid grid) {
        var container = tracker.container;
        if (container.getGrid() != grid || !includeProvider(container, container.getTerminalPatternInventory())) {
            removeTracker(tracker);
            return false;
        }

        var inventory = container.getTerminalPatternInventory();
        var group = container.getTerminalGroup();
        if (inventory == null || group == null || inventory.size() <= 0 || inventory.size() > MAX_PROVIDER_SLOTS) {
            removeTracker(tracker);
            return false;
        }
        long sortOrder = container.getTerminalSortOrder();
        if (tracker.inventory != inventory
                || tracker.inventory.size() != inventory.size()
                || tracker.sortOrder != sortOrder
                || !tracker.group.equals(group)) {
            tracker.inventory = inventory;
            tracker.group = group;
            tracker.sortOrder = sortOrder;
            tracker.snapshot = copyInventory(inventory);
            tracker.bumpRevision();
            queueFull(tracker);
        }
        return true;
    }

    private void removeTracker(Tracker tracker) {
        trackers.remove(tracker.container);
        trackersById.remove(tracker.id);
        queue(PatternProviderListPacket.remove(menu.containerId, epoch, tracker.id));
    }

    private void refreshTrackerContents(Tracker tracker) {
        if (tracker.snapshot.length != tracker.inventory.size()) {
            tracker.snapshot = copyInventory(tracker.inventory);
            tracker.bumpRevision();
            queueFull(tracker);
            return;
        }

        var changed = new Int2ObjectArrayMap<ItemStack>();
        for (int slot = 0; slot < tracker.snapshot.length; slot++) {
            var current = tracker.inventory.getStackInSlot(slot);
            if (!ItemStack.matches(current, tracker.snapshot[slot])) {
                // One defensive copy is enough: chunk() copies again before queueing the packet.
                var copy = current.copy();
                tracker.snapshot[slot] = copy;
                changed.put(slot, copy);
            }
        }
        if (!changed.isEmpty()) {
            tracker.bumpRevision();
            queueDelta(tracker, changed);
        }
    }

    private List<ProviderView> collectProviders(IGrid grid) {
        var providers = new ArrayList<ProviderView>();
        var seen = new IdentityHashMap<PatternContainer, Boolean>();

        outer:
        for (var machineClass : grid.getMachineClasses()) {
            if (!PatternContainer.class.isAssignableFrom(machineClass)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            var containerClass = (Class<? extends PatternContainer>) machineClass;
            for (var container : grid.getActiveMachines(containerClass)) {
                if (providers.size() >= MAX_TRACKED_PROVIDERS) {
                    break outer;
                }
                if (container == null || seen.put(container, Boolean.TRUE) != null
                        || container.getGrid() != grid) {
                    continue;
                }
                var inventory = container.getTerminalPatternInventory();
                if (!includeProvider(container, inventory)) {
                    continue;
                }
                var group = container.getTerminalGroup();
                if (inventory == null || group == null || inventory.size() <= 0
                        || inventory.size() > MAX_PROVIDER_SLOTS) {
                    continue;
                }
                providers.add(new ProviderView(container, inventory, group, container.getTerminalSortOrder(),
                        group.name().getString()));
            }
        }

        providers.sort(Comparator
                .comparing(ProviderView::sortKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparingLong(ProviderView::sortOrder));

        int totalSlots = 0;
        var bounded = new ArrayList<ProviderView>(providers.size());
        for (var provider : providers) {
            if (totalSlots + provider.inventory().size() > MAX_TOTAL_TRACKED_SLOTS) {
                break;
            }
            bounded.add(provider);
            totalSlots += provider.inventory().size();
        }
        return bounded;
    }

    private void queueFull(Tracker tracker) {
        var slots = new Int2ObjectArrayMap<ItemStack>();
        for (int slot = 0; slot < tracker.snapshot.length; slot++) {
            if (!tracker.snapshot[slot].isEmpty()) {
                slots.put(slot, tracker.snapshot[slot]);
            }
        }
        var chunks = chunk(slots);
        for (int i = 0; i < chunks.size(); i++) {
            queue(PatternProviderListPacket.full(menu.containerId, epoch, tracker.id, tracker.revision,
                    i, chunks.size(), tracker.inventory.size(), tracker.sortOrder, tracker.group, chunks.get(i),
                    PatternProviderLoc.from(tracker.container)));
        }
    }

    private void queueDelta(Tracker tracker, Int2ObjectMap<ItemStack> changed) {
        var chunks = chunk(changed);
        for (int i = 0; i < chunks.size(); i++) {
            queue(PatternProviderListPacket.delta(menu.containerId, epoch, tracker.id, tracker.revision,
                    i, chunks.size(), chunks.get(i)));
        }
    }

    private List<Int2ObjectMap<ItemStack>> chunk(Int2ObjectMap<ItemStack> source) {
        return PatternAccessChunkPlanner.chunk(
                source,
                PatternProviderListPacket.MAX_SLOTS_PER_PACKET,
                MAX_CHUNK_PAYLOAD_BYTES,
                this::estimateEncodedSize,
                ItemStack::copy);
    }

    /**
     * Measures the actual serialized size of one stack so chunks stay well below Minecraft's 1 MiB
     * clientbound custom-payload limit even for patterns with multi-KiB NBT.
     */
    private int estimateEncodedSize(ItemStack stack) {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buf.writeItem(stack);
            return buf.readableBytes();
        } catch (RuntimeException exception) {
            // Force this stack into a chunk of its own if it cannot be measured.
            return MAX_CHUNK_PAYLOAD_BYTES;
        } finally {
            buf.release();
        }
    }

    private void queue(PatternProviderListPacket packet) {
        if (queueInvalid) {
            return;
        }
        if (outbound.size() >= MAX_PENDING_PACKETS) {
            outbound.clear();
            epoch = nextEpoch();
            outbound.add(PatternProviderListPacket.reset(menu.containerId, epoch));
            queueInvalid = true;
            snapshotRequired = true;
            return;
        }
        outbound.add(packet);
    }

    private void flush(ServerPlayer player, long gameTick) {
        while (!outbound.isEmpty() && packetBudget.tryAcquire(gameTick)) {
            MestPackets.sendToPlayer(player, outbound.removeFirst());
        }
    }

    private void requestResnapshot() {
        snapshotRequired = true;
    }

    private boolean resnapshotCoolingDown() {
        // Applies to every full-rebuild path (including trackedGrid != null, e.g. after a grid change or
        // a forced re-subscribe) so clients cannot trigger a snapshot rebuild more than once per cooldown.
        return epoch != 0 && ticks - lastSnapshotTick < MIN_RESNAPSHOT_INTERVAL_TICKS;
    }

    public boolean uploadFromSlot(RestrictedInputSlot encodedSlot, ServerPlayer player) {
        ItemStack stack = encodedSlot.getItem();
        if (stack.isEmpty() || !PatternDetailsHelper.isEncodedPattern(stack)) {
            return false;
        }
        for (Tracker tracker : trackers.values()) {
            if (tracker.inventory == null) {
                continue;
            }
            for (int slot = 0; slot < tracker.inventory.size(); slot++) {
                if (!tracker.inventory.getStackInSlot(slot).isEmpty()) {
                    continue;
                }
                ItemStack inserting = stack.copyWithCount(1);
                ItemStack leftover = tracker.inventory.insertItem(slot, inserting, false);
                if (!leftover.isEmpty()) {
                    continue;
                }
                encodedSlot.remove(1);
                encodedSlot.setChanged();
                lastUploadContainer = tracker.container;
                lastUploadSlot = slot;
                lastUploadPattern = inserting.copy();
                return true;
            }
        }
        return false;
    }

    public void returnLastUpload(ServerPlayer player) {
        if (lastUploadContainer == null || lastUploadSlot < 0) {
            return;
        }
        Tracker tracker = trackers.get(lastUploadContainer);
        if (tracker == null || tracker.inventory == null || lastUploadSlot >= tracker.inventory.size()) {
            return;
        }
        // Only take back the pattern that was uploaded; the slot may have been emptied or reused.
        ItemStack present = tracker.inventory.extractItem(lastUploadSlot, 1, true);
        if (!ItemStack.isSameItemSameTags(present, lastUploadPattern)) {
            return;
        }
        ItemStack extracted = tracker.inventory.extractItem(lastUploadSlot, 1, false);
        if (!player.getInventory().add(extracted)) {
            player.drop(extracted, false);
        }
        lastUploadContainer = null;
        lastUploadSlot = -1;
        lastUploadPattern = ItemStack.EMPTY;
    }

    private IGrid getCurrentGrid() {
        var node = menu.getGridNode();
        return node != null && node.isActive() ? node.getGrid() : null;
    }

    private void clearServerState() {
        clearTrackingOnly();
        outbound.clear();
        queueInvalid = false;
        snapshotRequired = false;
    }

    private void clearTrackingOnly() {
        trackedGrid = null;
        trackers.clear();
        trackersById.clear();
        nextProviderId = 1;
    }

    private static ItemStack[] copyInventory(InternalInventory inventory) {
        var result = new ItemStack[inventory.size()];
        for (int slot = 0; slot < result.length; slot++) {
            result[slot] = inventory.getStackInSlot(slot).copy();
        }
        return result;
    }

    private static long nextEpoch() {
        long result = NEXT_EPOCH.getAndIncrement();
        return result > 0 ? result : NEXT_EPOCH.updateAndGet(value -> value > 0 ? value : 1);
    }

    /** {@code sortKey} caches {@code group().name().getString()} so sorting does not recompute it per comparison. */
    private record ProviderView(PatternContainer container, InternalInventory inventory,
            PatternContainerGroup group, long sortOrder, String sortKey) {
    }

    private static final class Tracker {
        private final long id;
        private final PatternContainer container;
        private InternalInventory inventory;
        private PatternContainerGroup group;
        private long sortOrder;
        private long revision = 1;
        private ItemStack[] snapshot;

        private Tracker(long id, PatternContainer container, InternalInventory inventory,
                PatternContainerGroup group, long sortOrder) {
            this.id = id;
            this.container = container;
            this.inventory = inventory;
            this.group = group;
            this.sortOrder = sortOrder;
            this.snapshot = copyInventory(inventory);
        }

        private void bumpRevision() {
            revision = revision == Long.MAX_VALUE ? 1 : revision + 1;
        }
    }
}
