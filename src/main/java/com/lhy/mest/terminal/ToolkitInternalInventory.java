package com.lhy.mest.terminal;

import com.lhy.mest.util.MestLists;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;

import com.lhy.mest.config.MestConfig;
import com.lhy.mest.network.ToolkitBarSyncPacket;

/**
 * The player's toolkit: unstackable items, remembered slot types and extra-bar settings.
 *
 * <p>Stored on the player (see {@link ToolkitDataHolder}) rather than the terminal, so losing a terminal loses nothing. HUD, the
 * extra-bar hand override and the terminal panel all read these same stack references, so in-place
 * tool changes (durability, charge) are visible everywhere and saved with the player.
 */
public final class ToolkitInternalInventory extends BaseInternalInventory {
    private final Player player;
    private final List<ItemStack> items = new ArrayList<>();
    private final List<ItemStack> memory = new ArrayList<>();
    private final InternalInventory memoryView = new MemoryView();
    private boolean barEnabled;
    private ToolkitBarState.Bar page = ToolkitBarState.Bar.CENTER;
    private boolean quickMove = true;
    /** Bumped whenever something other than the visible cells changes and the client must hear it. */
    private int settingsRevision;
    /** Bumped only for server-side page changes; client-driven ones are already shown there. */
    private int pageRevision;
    private long terminalCheckedAt = Long.MIN_VALUE;
    private boolean terminalCarried;

    public ToolkitInternalInventory(Player player) {
        this.player = player;
    }

    // ---- terminal presence -------------------------------------------------------------------

    /**
     * Whether a terminal is carried. The toolkit can only be used through a terminal, but its
     * contents never depend on one. Cached per game tick because {@code getSelected} asks often.
     */
    boolean hasTerminal() {
        long now = player.level().getGameTime();
        if (terminalCheckedAt != now) {
            terminalCheckedAt = now;
            terminalCarried = ToolkitBarState.scanTerminals(player);
        }
        return terminalCarried;
    }

    // ---- settings ----------------------------------------------------------------------------

    public boolean barEnabled() {
        return barEnabled;
    }

    void setBarEnabled(boolean enabled) {
        if (barEnabled != enabled) {
            barEnabled = enabled;
            settingsChanged();
        }
    }

    public ToolkitBarState.Bar page() {
        return page;
    }

    /** @param notifyClient false when the client asked for this page and already shows it */
    void setPage(ToolkitBarState.Bar next, boolean notifyClient) {
        if (page != next) {
            page = next;
            if (notifyClient) {
                pageRevision++;
                settingsChanged();
            }
        }
    }

    public boolean quickMove() {
        return quickMove;
    }

    void setQuickMove(boolean enabled) {
        if (quickMove != enabled) {
            quickMove = enabled;
            settingsChanged();
        }
    }

    public int settingsRevision() {
        return settingsRevision;
    }

    public int pageRevision() {
        return pageRevision;
    }

    private void settingsChanged() {
        settingsRevision++;
        sendToClient();
    }

    // ---- memory ------------------------------------------------------------------------------

    public InternalInventory memoryInventory() {
        return memoryView;
    }

    ItemStack memoryAt(int index) {
        return index >= 0 && index < size() && index < memory.size() ? memory.get(index) : ItemStack.EMPTY;
    }

    public List<ItemStack> memorySnapshot() {
        List<ItemStack> copy = new ArrayList<>(size());
        for (int i = 0; i < size(); i++) {
            copy.add(memoryAt(i).copy());
        }
        return copy;
    }

    private final class MemoryView extends BaseInternalInventory {
        @Override
        public int size() {
            return ToolkitInternalInventory.this.size();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return memoryAt(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            if (slotIndex < 0 || slotIndex >= size()) {
                return;
            }
            ensureCapacity(memory, slotIndex + 1);
            memory.set(slotIndex, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            settingsChanged();
        }
    }

    // ---- items -------------------------------------------------------------------------------

    @Override
    public int size() {
        return MestConfig.toolkitSlots();
    }

    ItemStack liveStack(int index) {
        return index >= 0 && index < size() && index < items.size() ? items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack getStackInSlot(int slotIndex) {
        return liveStack(slotIndex);
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack) {
        if (slotIndex < 0 || slotIndex >= size()) {
            return;
        }
        if (!stack.isEmpty() && !isItemValid(slotIndex, stack)) {
            return;
        }
        ensureCapacity(items, slotIndex + 1);
        // A returned armor piece must not be the same object still sitting in the equipment slot.
        // In-place edits (count, components) would then change both, which is how an equip swap
        // left quantum leggings in the hand and on the body.
        items.set(slotIndex, detachIfShared(slotIndex, stack));
        player.getInventory().setChanged();
        sendToClient();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return stack.isEmpty() || ToolkitBarState.mayStore(stack);
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    /**
     * Returns cells beyond a lowered {@code toolkitSlots} config to the player. Deferred until the
     * player is in the world, since the save data is read before the player can hold anything.
     */
    void returnOverflow() {
        if (player.level().isClientSide() || items.size() <= size()) {
            return;
        }
        for (int i = items.size() - 1; i >= size(); i--) {
            ItemStack stack = items.remove(i);
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
        }
        while (memory.size() > size()) {
            MestLists.removeLast(memory);
        }
    }

    // ---- client sync -------------------------------------------------------------------------

    private void sendToClient() {
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            ToolkitBarSyncPacket.send(serverPlayer);
        }
    }

    /** Server snapshot for the client. Visible cells always; settings only when they changed. */
    void applyServerSync(List<ItemStack> visible, @Nullable ToolkitBarSyncPacket.Settings settings) {
        int limit = Math.min(ToolkitBarState.VISIBLE_TOOLKIT_SLOTS, size());
        ensureCapacity(items, limit);
        for (int i = 0; i < limit; i++) {
            ItemStack incoming = i < visible.size() ? visible.get(i) : ItemStack.EMPTY;
            items.set(i, adopt(items.get(i), incoming));
        }
        if (settings != null) {
            barEnabled = settings.barEnabled();
            quickMove = settings.quickMove();
            if (settings.page() != null) {
                page = settings.page();
            }
            memory.clear();
            for (ItemStack remembered : settings.memory()) {
                memory.add(remembered.copy());
            }
        }
    }

    /**
     * Keep the live ItemStack identity when the item type is unchanged so {@code startUsingItem}
     * / energy tools keep seeing the same instance. WCWT replaced the snapshot on every sync and
     * cancelled bows, drills and similar held-use items.
     */
    private static ItemStack adopt(ItemStack current, ItemStack incoming) {
        if (incoming.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty() || current.getItem() != incoming.getItem()) {
            return incoming.copy();
        }
        if (ItemStack.matches(current, incoming)) {
            return current;
        }
        current.setCount(incoming.getCount());
        current.setTag(incoming.getTag() == null ? null : incoming.getTag().copy());
        return current;
    }

    // ---- persistence -------------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.put("Items", writeStacks(items));
        tag.put("Memory", writeStacks(memory));
        tag.putBoolean("Bar", barEnabled);
        tag.putInt("Page", page.ordinal());
        tag.putBoolean("QuickMove", quickMove);
        return tag;
    }

    public void load(CompoundTag tag) {
        readStacks(tag.getList("Items", Tag.TAG_COMPOUND), items);
        readStacks(tag.getList("Memory", Tag.TAG_COMPOUND), memory);
        barEnabled = tag.getBoolean("Bar");
        int pageIndex = tag.getInt("Page");
        page = pageIndex >= 0 && pageIndex < ToolkitBarState.Bar.values().length
                ? ToolkitBarState.Bar.values()[pageIndex]
                : ToolkitBarState.Bar.CENTER;
        quickMove = !tag.contains("QuickMove") || tag.getBoolean("QuickMove");
        settingsRevision++;
    }

    // Slots are stored as ints: the toolkit can exceed the 256-slot limit of container codecs.
    private static ListTag writeStacks(List<ItemStack> stacks) {
        ListTag list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putInt("Slot", i);
            list.add(stack.save(entry));
        }
        return list;
    }

    private static void readStacks(ListTag list, List<ItemStack> into) {
        into.clear();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot < 0) {
                continue;
            }
            ItemStack stack = ItemStack.of(entry);
            if (!stack.isEmpty()) {
                ensureCapacity(into, slot + 1);
                into.set(slot, stack);
            }
        }
    }

    private static void ensureCapacity(List<ItemStack> list, int size) {
        while (list.size() < size) {
            list.add(ItemStack.EMPTY);
        }
    }

    // ---- aliasing guard ----------------------------------------------------------------------

    /**
     * Copy only when {@code stack} is already stored in another slot. Held tools must keep their
     * identity so charging and drawing are not cancelled; an armor return must not alias the piece
     * still equipped.
     */
    private ItemStack detachIfShared(int slotIndex, ItemStack stack) {
        if (stack.isEmpty() || !sharesReference(slotIndex, stack)) {
            return stack;
        }
        return stack.copy();
    }

    private boolean sharesReference(int slotIndex, ItemStack stack) {
        Inventory inventory = player.getInventory();
        if (containsReference(inventory.armor, stack)
                || containsReference(inventory.offhand, stack)
                || containsReference(inventory.items, stack)) {
            return true;
        }
        for (int i = 0; i < items.size(); i++) {
            if (i != slotIndex && items.get(i) == stack) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsReference(List<ItemStack> slots, ItemStack stack) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) == stack) {
                return true;
            }
        }
        return false;
    }
}
