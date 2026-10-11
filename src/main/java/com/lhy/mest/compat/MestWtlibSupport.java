package com.lhy.mest.compat;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.me.helpers.PlayerSource;
import appeng.menu.locator.MenuLocator;
import appeng.menu.me.crafting.CraftAmountMenu;

import de.mari_023.ae2wtlib.AE2wtlibConfig;
import de.mari_023.ae2wtlib.AE2wtlibTags;
import de.mari_023.ae2wtlib.networking.ServerNetworkManager;
import de.mari_023.ae2wtlib.networking.s2c.UpdateRestockPacket;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import de.mari_023.ae2wtlib.wct.CraftingTerminalHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;
import de.mari_023.ae2wtlib.wut.WUTHandler;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.network.MestPackets;
import com.lhy.mest.network.MestRestockAmountPacket;
import com.lhy.mest.terminal.MestMagnetHost;
import com.lhy.mest.terminal.MestTerminal;
import com.lhy.mest.terminal.ToolkitBarState;
import com.lhy.mest.terminal.ToolkitHand;

/**
 * wtlib pick/restock/magnet run against the {@code crafting} terminal only. MEST is registered as
 * {@code spliced}, so those handlers miss it. This bridge uses the same public APIs against the
 * spliced terminal item.
 */
@Mod.EventBusSubscriber(modid = MESplicedterminal.MODID)
public final class MestWtlibSupport {
    private static final ThreadLocal<ItemStack> USE_ORIGINAL = new ThreadLocal<>();

    private MestWtlibSupport() {
    }

    @Nullable
    public static MenuLocator findMest(Player player) {
        if (!WUTHandler.wirelessTerminals.containsKey(MestTerminal.TERMINAL_NAME)) {
            return null;
        }
        return WUTHandler.findTerminal(player, MestTerminal.TERMINAL_NAME);
    }

    public static ItemStack mestStack(Player player) {
        MenuLocator locator = findMest(player);
        return locator == null ? ItemStack.EMPTY : WUTHandler.getItemStackFromLocator(player, locator);
    }

    private static boolean flag(ItemStack terminal, String key) {
        return ItemWT.getBoolean(terminal, key);
    }

    public static void pickBlock(ServerPlayer player, ItemStack requested) {
        if (requested.isEmpty() || craftingTerminalOwns(player, "pick")) {
            return;
        }
        ItemStack mest = mestStack(player);
        if (mest.isEmpty() || !flag(mest, AE2wtlibTags.PICK_BLOCK)) {
            return;
        }
        MEStorage storage = mestStorage(player);
        MenuLocator locator = findMest(player);
        if (storage == null || locator == null) {
            return;
        }
        PlayerSource source = new PlayerSource(player, null);
        Inventory inventory = player.getInventory();
        int targetSlot = inventory.getSuitableHotbarSlot();
        ItemStack toReplace = inventory.getItem(targetSlot);
        AEItemKey replaceKey = AEItemKey.of(toReplace);
        if (!toReplace.isEmpty() && replaceKey != null) {
            long insert = storage.insert(replaceKey, toReplace.getCount(), Actionable.SIMULATE, source);
            if (insert < toReplace.getCount()) {
                return;
            }
        }
        AEItemKey what = AEItemKey.of(requested);
        if (what == null) {
            return;
        }
        int targetAmount = requested.getMaxStackSize();
        long extracted = storage.extract(what, targetAmount, Actionable.SIMULATE, source);
        if (extracted == 0L) {
            if (!flag(mest, AE2wtlibTags.CRAFT_IF_MISSING)) {
                return;
            }
            var host = locator.locate(player, WTMenuHost.class);
            if (host == null || host.getActionableNode() == null || host.getActionableNode().getGrid() == null
                    || host.getActionableNode().getGrid().getCraftingService().getCraftingFor(what).isEmpty()) {
                return;
            }
            CraftAmountMenu.open(player, locator, what, 1);
            return;
        }
        if (!toReplace.isEmpty() && replaceKey != null) {
            long insert = storage.insert(replaceKey, toReplace.getCount(), Actionable.MODULATE, source);
            if (insert < toReplace.getCount()) {
                toReplace.setCount(toReplace.getCount() - (int) insert);
                inventory.setItem(targetSlot, toReplace);
                return;
            }
        }
        extracted = storage.extract(what, targetAmount, Actionable.MODULATE, source);
        if (extracted == 0L) {
            inventory.setItem(targetSlot, ItemStack.EMPTY);
            return;
        }
        ItemStack picked = requested.copy();
        picked.setCount((int) extracted);
        inventory.setItem(targetSlot, picked);
        ToolkitBarState.setSelection(player, ToolkitBarState.Bar.CENTER, targetSlot);
        player.connection.send(new ClientboundSetCarriedItemPacket(inventory.selected));
    }

    /** The spliced terminal the player is carrying, once its wireless link is confirmed live. */
    @Nullable
    private static WTMenuHost linkedHost(Player player) {
        MenuLocator locator = findMest(player);
        if (locator == null) {
            return null;
        }
        WTMenuHost host = locator.locate(player, WTMenuHost.class);
        if (host == null || !host.rangeCheck()) {
            return null;
        }
        return host;
    }

    /**
     * True while the player still carries a spliced terminal with a live grid connection. Used to
     * keep remotely opened machine menus from outliving the terminal that opened them.
     */
    public static boolean hasLinkedTerminal(Player player) {
        return hasLinkedTerminal(player, null);
    }

    /** Checks that the carried terminal is connected to the grid that opened the remote menu. */
    public static boolean hasLinkedTerminal(Player player, @Nullable IGrid expectedGrid) {
        WTMenuHost host = linkedHost(player);
        if (host == null) {
            return false;
        }
        IGridNode node = host.getActionableNode();
        IGrid grid = node == null ? null : node.getGrid();
        return node != null && node.isActive() && grid != null
                && (expectedGrid == null || grid == expectedGrid);
    }

    @Nullable
    private static MEStorage mestStorage(Player player) {
        WTMenuHost host = linkedHost(player);
        if (host == null) {
            return null;
        }
        IGridNode node = host.getActionableNode();
        if (node == null || node.getGrid() == null || node.getGrid().getStorageService() == null) {
            return null;
        }
        return node.getGrid().getStorageService().getInventory();
    }

    private static boolean craftingTerminalOwns(Player player, String component) {
        ItemStack wct = CraftingTerminalHandler.getCraftingTerminalHandler(player).getCraftingTerminal();
        if (wct.isEmpty()) {
            return false;
        }
        if ("magnet".equals(component)) {
            MagnetMode mode = MagnetHandler.getMagnetMode(wct);
            return mode.magnet() || mode.pickupToME();
        }
        if ("restock".equals(component)) {
            return flag(wct, AE2wtlibTags.RESTOCK);
        }
        return flag(wct, AE2wtlibTags.PICK_BLOCK);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (craftingTerminalOwns(player, "magnet")) {
            return;
        }
        ItemStack mest = mestStack(player);
        if (mest.isEmpty()) {
            return;
        }
        sendRestockAble(player, mest);
        MagnetMode mode = MagnetHandler.getMagnetMode(mest);
        if (!mode.magnet() || player.isShiftKeyDown()) {
            return;
        }
        MestMagnetHost filter = new MestMagnetHost(mest);
        double range = AE2wtlibConfig.INSTANCE.magnetCardRange();
        List<ItemEntity> items = player.level().getEntitiesOfClass(
                ItemEntity.class, player.getBoundingBox().inflate(range), EntitySelector.ENTITY_STILL_ALIVE);
        for (ItemEntity entity : items) {
            if (entity.getPersistentData().contains("PreventRemoteMovement")) {
                continue;
            }
            AEItemKey key = AEItemKey.of(entity.getItem());
            if (key == null || !filter.matchesPickup(key)) {
                continue;
            }
            entity.playerTouch(player);
        }
    }

    @SubscribeEvent
    public static void onPickup(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (event.isCanceled() || player.level().isClientSide() || craftingTerminalOwns(player, "magnet")) {
            return;
        }
        ItemStack mest = mestStack(player);
        if (mest.isEmpty() || !MagnetHandler.getMagnetMode(mest).pickupToME()) {
            return;
        }
        ItemEntity entity = event.getItem();
        if (player.isShiftKeyDown() || entity.hasPickUpDelay()
                || player.getUUID().equals(entity.getOwner() == null ? null : entity.getOwner().getUUID())) {
            return;
        }
        MEStorage storage = mestStorage(player);
        if (storage == null) {
            return;
        }
        ItemStack stack = entity.getItem();
        AEItemKey key = AEItemKey.of(stack);
        if (key == null || !new MestMagnetHost(mest).matchesInsert(key)) {
            return;
        }
        long inserted = storage.insert(key, stack.getCount(), Actionable.MODULATE, new PlayerSource(player, null));
        if (inserted <= 0L) {
            return;
        }
        int leftover = (int) (stack.getCount() - inserted);
        stack.setCount(leftover);
        event.setCanceled(true);
        if (leftover <= 0) {
            entity.discard();
        }
    }

    @SubscribeEvent
    public static void onToss(ItemTossEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (craftingTerminalOwns(player, "restock")) {
            return;
        }
        ItemStack mest = mestStack(player);
        if (mest.isEmpty() || !flag(mest, AE2wtlibTags.RESTOCK)) {
            return;
        }
        if (ToolkitHand.isOverrideActive(player)) {
            return;
        }
        int slot = player.getInventory().selected;
        ItemStack remaining = player.getInventory().getItem(slot);
        ItemStack type = remaining.isEmpty() ? event.getEntity().getItem() : remaining;
        ItemStack now = remaining.isEmpty() ? ItemStack.EMPTY : remaining;
        restock(player, type, now, filled -> player.getInventory().setItem(slot, filled), slot);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void beforeUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        USE_ORIGINAL.set(event.getItemStack().copy());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void afterUseBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            USE_ORIGINAL.remove();
            return;
        }
        ItemStack original = USE_ORIGINAL.get();
        USE_ORIGINAL.remove();
        if (original == null) {
            return;
        }
        InteractionHand hand = event.getHand();
        int slot = hand == InteractionHand.OFF_HAND ? Inventory.SLOT_OFFHAND : player.getInventory().selected;
        restock(player, original, player.getItemInHand(hand), stack -> player.setItemInHand(hand, stack), slot);
    }

    /** Same rules as {@code AE2wtlibEvents.restock}: refill {@code now} from ME up to {@code original}'s max. */
    public static void restock(
            ServerPlayer player, ItemStack original, ItemStack now, Consumer<ItemStack> set, int slot) {
        if (player.isCreative() || original.isEmpty()) {
            return;
        }
        if (craftingTerminalOwns(player, "restock")) {
            return;
        }
        ItemStack mest = mestStack(player);
        if (mest.isEmpty() || !flag(mest, AE2wtlibTags.RESTOCK)) {
            return;
        }
        MEStorage storage = mestStorage(player);
        if (storage == null) {
            return;
        }
        int count = now.getCount();
        int toAdd = original.getMaxStackSize() - count;
        if (toAdd == 0) {
            return;
        }
        if (!now.isEmpty() && !ItemStack.isSameItemSameTags(original, now)) {
            return;
        }
        AEItemKey key = AEItemKey.of(original);
        if (key == null) {
            return;
        }
        PlayerSource source = new PlayerSource(player, null);
        long changed = toAdd > 0
                ? storage.extract(key, toAdd, Actionable.MODULATE, source)
                : -storage.insert(key, -toAdd, Actionable.MODULATE, source);
        ItemStack filled = now.isEmpty() ? original.copy() : now.copy();
        filled.setCount(count + (int) changed);
        set.accept(filled);
        ServerNetworkManager.sendToClient(player, new UpdateRestockPacket(slot, filled.getCount()));
    }

    private static void sendRestockAble(ServerPlayer player, ItemStack terminal) {
        if (player.isCreative() || !flag(terminal, AE2wtlibTags.RESTOCK)) {
            return;
        }
        WTMenuHost host = linkedHost(player);
        if (host == null || host.getActionableNode() == null || host.getActionableNode().getGrid() == null) {
            return;
        }
        KeyCounter list = host.getActionableNode().getGrid().getStorageService().getCachedInventory();
        if (list == null) {
            return;
        }
        HashMap<Item, Long> items = new HashMap<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || items.containsKey(stack.getItem())) {
                continue;
            }
            AEItemKey key = AEItemKey.of(stack);
            items.put(stack.getItem(), key == null ? 0L : list.get(key));
        }
        MestPackets.sendToPlayer(player, new MestRestockAmountPacket(items));
    }
}
