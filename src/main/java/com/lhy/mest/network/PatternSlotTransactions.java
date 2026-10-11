package com.lhy.mest.network;

import net.minecraft.world.item.ItemStack;

import appeng.api.inventories.InternalInventory;

/** Item-conserving operations shared by pattern-provider slot actions. */
final class PatternSlotTransactions {
    private static final ConservingTransfer<ItemStack> TRANSFER = new ConservingTransfer<>(
            new ConservingTransfer.StackOps<>() {
                @Override
                public ItemStack empty() {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean isEmpty(ItemStack stack) {
                    return stack.isEmpty();
                }

                @Override
                public ItemStack copy(ItemStack stack) {
                    return stack.copy();
                }

                @Override
                public int count(ItemStack stack) {
                    return stack.getCount();
                }

                @Override
                public ItemStack withCount(ItemStack stack, int count) {
                    if (count <= 0) {
                        return ItemStack.EMPTY;
                    }
                    var copy = stack.copy();
                    copy.setCount(count);
                    return copy;
                }

                @Override
                public boolean sameKind(ItemStack left, ItemStack right) {
                    return ItemStack.isSameItemSameTags(left, right);
                }

                @Override
                public boolean matches(ItemStack left, ItemStack right) {
                    return ItemStack.matches(left, right);
                }
            });

    private PatternSlotTransactions() {
    }

    static ItemStack exchange(InternalInventory patternSlot, ItemStack carried) {
        return TRANSFER.exchange(new SlotAdapter(patternSlot), carried);
    }

    static int quickMove(InternalInventory patternSlot, InternalInventory destination) {
        return TRANSFER.quickMove(new SlotAdapter(patternSlot), new DestinationAdapter(destination));
    }

    private record SlotAdapter(InternalInventory inventory) implements ConservingTransfer.Slot<ItemStack> {
        @Override
        public ItemStack get() {
            return inventory.getStackInSlot(0);
        }

        @Override
        public ItemStack extract(int amount, boolean simulate) {
            return inventory.extractItem(0, amount, simulate);
        }

        @Override
        public ItemStack insert(ItemStack stack, boolean simulate) {
            return inventory.insertItem(0, stack, simulate);
        }

        @Override
        public void set(ItemStack stack) {
            inventory.setItemDirect(0, stack);
        }
    }

    private record DestinationAdapter(InternalInventory inventory)
            implements ConservingTransfer.Destination<ItemStack> {
        @Override
        public ItemStack simulateAdd(ItemStack stack) {
            return inventory.simulateAdd(stack);
        }

        @Override
        public ItemStack add(ItemStack stack) {
            return inventory.addItems(stack);
        }
    }
}
