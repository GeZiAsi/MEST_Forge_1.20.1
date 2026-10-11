package com.lhy.mest.terminal;

import java.util.function.BiConsumer;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import appeng.api.inventories.InternalInventory;
import appeng.helpers.IPatternTerminalLogicHost;
import appeng.helpers.IPatternTerminalMenuHost;
import appeng.menu.ISubMenu;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;

import de.mari_023.ae2wtlib.wct.WCTMenuHost;

import com.lhy.mest.config.MestConfig;

/**
 * Extends {@link WCTMenuHost} so trash / magnet submenus can use ae2wtlib's public menu types
 * (they locate {@code WCTMenuHost}). Pattern cache and encoding stay MEST-specific.
 */
public class MESTMenuHost extends WCTMenuHost
        implements IPatternTerminalMenuHost, IPatternTerminalLogicHost {
    /** NBT keys on the terminal stack (the 1.21 build used data components). */
    public static final String TAG_PATTERN_CACHE = "mest_pattern_cache";
    public static final String TAG_TRASH = "mest_trash";
    public static final String TAG_NETWORK_TOOLKIT = "mest_network_toolkit";

    public static int patternCacheSize() {
        return MestConfig.patternCacheSlots();
    }

    private final StackInventory patternCache;
    private final StackInventory trash;
    private final InternalInventory toolkit;
    private final InternalInventory toolkitMemory;
    private final StackInventory networkToolkit;
    private final PatternEncodingLogic patternEncodingLogic;

    public MESTMenuHost(Player player, @Nullable Integer inventorySlot, ItemStack stack,
            BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, inventorySlot, stack, returnToMainMenu);
        this.patternCache = new StackInventory(this, TAG_PATTERN_CACHE, patternCacheSize());
        this.trash = new StackInventory(this, TAG_TRASH, MestConfig.trashSlots());
        this.toolkit = ToolkitBarState.asInventory(player);
        this.toolkitMemory = ToolkitBarState.memoryInventory(player);
        this.networkToolkit = new StackInventory(this, TAG_NETWORK_TOOLKIT, MestConfig.networkToolkitSlots());
        this.patternEncodingLogic = new PatternEncodingLogic(this);
        this.patternEncodingLogic.readFromNBT(getItemStack().getOrCreateTag());
    }

    public InternalInventory getPatternCacheInventory() {
        return patternCache;
    }

    public InternalInventory getTrashInventory() {
        return trash;
    }

    public InternalInventory getToolkitInventory() {
        return toolkit;
    }

    public InternalInventory getToolkitMemoryInventory() {
        return toolkitMemory;
    }

    public InternalInventory getNetworkToolkitInventory() {
        return networkToolkit;
    }

    public void clearTrash() {
        InternalInventory inventory = trash;
        for (int i = 0; i < inventory.size(); i++) {
            inventory.setItemDirect(i, ItemStack.EMPTY);
        }
        trash.save();
    }

    /** The player's inventory slot holding the terminal, or {@code null} (e.g. a Curios slot). */
    @Nullable
    public Integer getPlayerInventorySlot() {
        return getSlot();
    }

    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (INV_TRASH.equals(id)) {
            return trash;
        }
        return super.getSubInventory(id);
    }

    /** Sub-screens (crafting status, magnet filter) return to the spliced terminal, not the WCT. */
    @Override
    public ItemStack getMainMenuIcon() {
        return getItemStack();
    }

    /** Pressing the spliced terminal's own hotkey inside it closes it, like AE2's terminals. */
    @Override
    public String getCloseHotkey() {
        return MestTerminal.HOTKEY_NAME;
    }

    @Override
    public PatternEncodingLogic getLogic() {
        return patternEncodingLogic;
    }

    @Override
    public Level getLevel() {
        return getPlayer().level();
    }

    @Override
    public void markForSave() {
        saveChanges();
    }

    @Override
    public void saveChanges() {
        super.saveChanges();
        // WCTMenuHost's constructor already saves through here before our fields exist.
        if (patternEncodingLogic != null) {
            patternEncodingLogic.writeToNBT(getItemStack().getOrCreateTag());
        }
    }

    /** An inventory persisted under its own key on the terminal stack this host was opened for. */
    private static final class StackInventory extends AppEngInternalInventory {
        private final MESTMenuHost owner;
        private final String key;

        StackInventory(MESTMenuHost owner, String key, int size) {
            super(new Saver(), size);
            this.owner = owner;
            this.key = key;
            ((Saver) getHost()).inventory = this;
            setEnableClientEvents(true);
            CompoundTag tag = owner.getItemStack().getTag();
            if (tag != null && tag.contains(key)) {
                readFromNBT(tag, key);
            }
        }

        void save() {
            writeToNBT(owner.getItemStack().getOrCreateTag(), key);
        }
    }

    private static final class Saver implements InternalInventoryHost {
        private StackInventory inventory;

        @Override
        public void saveChanges() {
            if (inventory != null) {
                inventory.save();
            }
        }

        @Override
        public void onChangeInventory(InternalInventory inv, int slot) {
        }

        @Override
        public boolean isClientSide() {
            return inventory != null && inventory.owner.isClientSide();
        }
    }
}
