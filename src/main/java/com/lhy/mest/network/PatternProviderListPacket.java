package com.lhy.mest.network;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import appeng.api.implementations.blockentities.PatternContainerGroup;

import com.lhy.mest.terminal.MESTMenu;

/**
 * Incremental, menu-scoped synchronization for the pattern access panel.
 */
public record PatternProviderListPacket(
        int containerId,
        long epoch,
        Operation operation,
        long providerId,
        long revision,
        int chunkIndex,
        int chunkCount,
        int inventorySize,
        long sortOrder,
        @Nullable PatternContainerGroup group,
        Int2ObjectMap<ItemStack> slots,
        @Nullable PatternProviderLoc loc) {

    /** Keeps every payload well below Minecraft's clientbound custom-payload limit. */
    static final int MAX_SLOTS_PER_PACKET = PatternProviderClientState.MAX_SLOTS_PER_CHUNK;

    public enum Operation {
        RESET,
        FULL,
        DELTA,
        REMOVE
    }

    public static PatternProviderListPacket reset(int containerId, long epoch) {
        return new PatternProviderListPacket(containerId, epoch, Operation.RESET,
                0, 0, 0, 1, 0, 0, null, new Int2ObjectArrayMap<>(), null);
    }

    public static PatternProviderListPacket full(int containerId, long epoch, long providerId, long revision,
            int chunkIndex, int chunkCount, int inventorySize, long sortOrder, PatternContainerGroup group,
            Int2ObjectMap<ItemStack> slots, @Nullable PatternProviderLoc loc) {
        return new PatternProviderListPacket(containerId, epoch, Operation.FULL,
                providerId, revision, chunkIndex, chunkCount, inventorySize, sortOrder, group, slots,
                chunkIndex == 0 ? loc : null);
    }

    public static PatternProviderListPacket delta(int containerId, long epoch, long providerId, long revision,
            int chunkIndex, int chunkCount, Int2ObjectMap<ItemStack> slots) {
        return new PatternProviderListPacket(containerId, epoch, Operation.DELTA,
                providerId, revision, chunkIndex, chunkCount, 0, 0, null, slots, null);
    }

    public static PatternProviderListPacket remove(int containerId, long epoch, long providerId) {
        return new PatternProviderListPacket(containerId, epoch, Operation.REMOVE,
                providerId, 0, 0, 1, 0, 0, null, new Int2ObjectArrayMap<>(), null);
    }

    public static PatternProviderListPacket read(FriendlyByteBuf buf) {
        int containerId = -1;
        try {
            containerId = buf.readVarInt();
            long epoch = buf.readVarLong();
            Operation operation = buf.readEnum(Operation.class);
            PatternProviderListPacket packet;
            if (operation == Operation.RESET) {
                packet = reset(containerId, epoch);
            } else {
                long providerId = buf.readVarLong();
                if (operation == Operation.REMOVE) {
                    packet = remove(containerId, epoch, providerId);
                } else {
                    long revision = buf.readVarLong();
                    int chunkIndex = buf.readVarInt();
                    int chunkCount = buf.readVarInt();
                    int inventorySize = 0;
                    long sortOrder = 0;
                    PatternContainerGroup group = null;
                    PatternProviderLoc loc = null;
                    if (operation == Operation.FULL) {
                        inventorySize = buf.readVarInt();
                        sortOrder = buf.readVarLong();
                        group = PatternContainerGroup.readFromPacket(buf);
                        loc = readLoc(buf);
                    }
                    var slots = readSlots(buf);
                    packet = new PatternProviderListPacket(containerId, epoch, operation, providerId, revision,
                            chunkIndex, chunkCount, inventorySize, sortOrder, group, slots, loc);
                }
            }
            if (buf.isReadable()) {
                throw new IllegalArgumentException("Trailing pattern provider list data");
            }
            return packet;
        } catch (RuntimeException exception) {
            discardRemaining(buf);
            return invalid(containerId);
        }
    }

    public void write(FriendlyByteBuf buf) {
        if (!isWellFormed()) {
            throw new IllegalStateException("Cannot encode a malformed pattern provider list update");
        }
        buf.writeVarInt(containerId);
        buf.writeVarLong(epoch);
        buf.writeEnum(operation);
        if (operation == Operation.RESET) {
            return;
        }

        buf.writeVarLong(providerId);
        if (operation == Operation.REMOVE) {
            return;
        }

        buf.writeVarLong(revision);
        buf.writeVarInt(chunkIndex);
        buf.writeVarInt(chunkCount);
        if (operation == Operation.FULL) {
            buf.writeVarInt(inventorySize);
            buf.writeVarLong(sortOrder);
            if (group == null) {
                throw new IllegalStateException("Full pattern provider update is missing its group");
            }
            group.writeToPacket(buf);
            writeLoc(buf, loc);
        }
        writeSlots(buf, slots);
    }

    private static void writeLoc(FriendlyByteBuf buf, @Nullable PatternProviderLoc loc) {
        if (loc == null) {
            buf.writeBoolean(false);
            return;
        }
        buf.writeBoolean(true);
        buf.writeBlockPos(loc.pos());
        buf.writeResourceLocation(loc.dimensionId());
        buf.writeVarInt(loc.face() == null ? -1 : loc.face().ordinal());
    }

    @Nullable
    private static PatternProviderLoc readLoc(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) {
            return null;
        }
        BlockPos pos = buf.readBlockPos();
        ResourceLocation dimId = buf.readResourceLocation();
        int faceOrd = buf.readVarInt();
        Direction face = faceOrd >= 0 && faceOrd < Direction.values().length ? Direction.values()[faceOrd] : null;
        return new PatternProviderLoc(pos, face, ResourceKey.create(Registries.DIMENSION, dimId));
    }

    public boolean isWellFormed() {
        if (containerId < 0 || epoch <= 0 || operation == null || slots == null) {
            return false;
        }
        if (operation == Operation.RESET) {
            return providerId == 0 && revision == 0 && chunkIndex == 0 && chunkCount == 1
                    && inventorySize == 0 && sortOrder == 0 && group == null && slots.isEmpty() && loc == null;
        }
        if (operation == Operation.REMOVE) {
            return providerId > 0 && revision == 0 && chunkIndex == 0 && chunkCount == 1
                    && inventorySize == 0 && sortOrder == 0 && group == null && slots.isEmpty() && loc == null;
        }
        if (providerId <= 0 || revision <= 0
                || chunkCount <= 0 || chunkCount > PatternProviderClientState.MAX_CHUNKS_PER_UPDATE
                || chunkIndex < 0 || chunkIndex >= chunkCount
                || slots.size() > MAX_SLOTS_PER_PACKET) {
            return false;
        }
        if (operation == Operation.FULL) {
            if (inventorySize <= 0 || inventorySize > PatternProviderClientState.MAX_INVENTORY_SIZE
                    || group == null || !isPlausibleFullChunkCount(chunkCount, inventorySize)) {
                return false;
            }
        } else if (inventorySize != 0 || sortOrder != 0 || group != null || loc != null) {
            return false;
        }
        for (var entry : slots.int2ObjectEntrySet()) {
            int slot = entry.getIntKey();
            if (slot < 0 || slot >= PatternProviderClientState.MAX_INVENTORY_SIZE
                    || entry.getValue() == null
                    || operation == Operation.FULL && slot >= inventorySize) {
                return false;
            }
        }
        return true;
    }

    static boolean isPlausibleFullChunkCount(int chunkCount, int inventorySize) {
        if (inventorySize <= 0 || inventorySize > PatternProviderClientState.MAX_INVENTORY_SIZE) {
            return false;
        }
        // Byte-based chunk splitting can put as little as one slot into each chunk.
        return chunkCount >= 1 && chunkCount <= inventorySize;
    }

    private static PatternProviderListPacket invalid(int containerId) {
        return new PatternProviderListPacket(containerId, 0, Operation.RESET,
                0, 0, 0, 1, 0, 0, null, new Int2ObjectArrayMap<>(), null);
    }

    private static void discardRemaining(FriendlyByteBuf buf) {
        try {
            buf.skipBytes(buf.readableBytes());
        } catch (RuntimeException ignored) {
            // Decoding must still return a rejected sentinel if the buffer itself is already invalid.
        }
    }

    public record Request(int containerId, boolean subscribe, byte showMode) {
        public static Request read(FriendlyByteBuf buf) {
            try {
                int containerId = buf.readVarInt();
                boolean subscribe = buf.readBoolean();
                byte showMode = buf.readByte();
                if (buf.isReadable()) {
                    throw new IllegalArgumentException("Trailing pattern provider request data");
                }
                return new Request(containerId, subscribe, showMode);
            } catch (RuntimeException exception) {
                discardRemaining(buf);
                return new Request(-1, false, (byte) 0);
            }
        }

        public void write(FriendlyByteBuf buf) {
            if (!isWellFormed()) {
                throw new IllegalStateException("Cannot encode a malformed pattern provider request");
            }
            buf.writeVarInt(containerId);
            buf.writeBoolean(subscribe);
            buf.writeByte(showMode);
        }

        boolean isWellFormed() {
            return containerId >= 0 && showMode >= 0 && showMode <= 2;
        }

        public static void handle(Request packet, PacketContext context) {
            if (!packet.isWellFormed()) {
                return;
            }
            context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player
                        && player.containerMenu instanceof MESTMenu menu
                        && menu.containerId == packet.containerId()
                        && (!packet.subscribe() || menu.canUsePatternAccess(player))) {
                    menu.getPatternAccessSession().setSubscribed(player, packet.subscribe(), packet.showMode());
                }
            });
        }
    }

    private static void writeSlots(FriendlyByteBuf buf, Int2ObjectMap<ItemStack> slots) {
        if (slots.size() > MAX_SLOTS_PER_PACKET) {
            throw new IllegalStateException("Too many pattern provider slots in one packet");
        }
        buf.writeVarInt(slots.size());
        for (var entry : slots.int2ObjectEntrySet()) {
            buf.writeShort(entry.getIntKey());
            buf.writeItem(entry.getValue());
        }
    }

    private static Int2ObjectMap<ItemStack> readSlots(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        if (size < 0 || size > MAX_SLOTS_PER_PACKET) {
            throw new IllegalArgumentException("Pattern provider slot count out of range: " + size);
        }
        Int2ObjectMap<ItemStack> slots = new Int2ObjectArrayMap<>(size);
        for (int i = 0; i < size; i++) {
            int slot = buf.readShort();
            slots.put(slot, buf.readItem());
        }
        return slots;
    }
}
