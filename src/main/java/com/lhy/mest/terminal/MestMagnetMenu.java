package com.lhy.mest.terminal;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import appeng.api.config.IncludeExclude;
import appeng.api.inventories.InternalInventory;
import appeng.api.storage.ISubMenuHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.ISubMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.guisync.GuiSync;
import appeng.menu.slot.FakeSlot;
import appeng.util.ConfigInventory;
import appeng.util.ConfigMenuInventory;

import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;

import com.lhy.mest.MESplicedterminal;

/** Magnet filter submenu bound to the spliced terminal item. */
public class MestMagnetMenu extends AEBaseMenu implements ISubMenu {
    public static final ResourceLocation ID =
            new ResourceLocation(MESplicedterminal.MODID, "magnet");
    public static final MenuType<MestMagnetMenu> TYPE =
            MestMenuTypes.create(ID, MestMagnetMenu::new, MESTMenuHost.class);

    private static final String TOGGLE_PICKUP_MODE = "togglepickupmode";
    private static final String TOGGLE_INSERT_MODE = "toggleinsertmode";
    private static final String COPY_UP = "copy_up";
    private static final String COPY_DOWN = "copy_down";
    private static final String SWITCH_INSERT_PICKUP = "switch";

    private final MESTMenuHost host;
    private final MestMagnetHost magnetHost;

    @GuiSync(1)
    public IncludeExclude pickupMode = IncludeExclude.BLACKLIST;
    @GuiSync(2)
    public IncludeExclude insertMode = IncludeExclude.BLACKLIST;

    public MestMagnetMenu(int id, Inventory playerInventory, MESTMenuHost host) {
        super(TYPE, id, playerInventory, host);
        this.host = host;
        this.magnetHost = new MestMagnetHost(host.getItemStack());
        addConfigSlots(magnetHost.pickupConfig, AE2wtlibSlotSemantics.PICKUP_CONFIG);
        addConfigSlots(magnetHost.insertConfig, AE2wtlibSlotSemantics.INSERT_CONFIG);
        createPlayerInventorySlots(playerInventory);
        registerClientAction(TOGGLE_PICKUP_MODE, this::togglePickupMode);
        registerClientAction(TOGGLE_INSERT_MODE, this::toggleInsertMode);
        registerClientAction(COPY_UP, this::copyUp);
        registerClientAction(COPY_DOWN, this::copyDown);
        registerClientAction(SWITCH_INSERT_PICKUP, this::switchInsertPickup);
        pickupMode = magnetHost.getPickupMode();
        insertMode = magnetHost.getInsertMode();
    }

    private void addConfigSlots(ConfigInventory config, SlotSemantic semantic) {
        ConfigMenuInventory inv = config.createMenuWrapper();
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot((Slot) new FakeSlot((InternalInventory) inv, y * 9 + x), semantic);
            }
        }
    }

    @Override
    public ISubMenuHost getHost() {
        return host;
    }

    public MestMagnetHost getMagnetHost() {
        return magnetHost;
    }

    public IncludeExclude getPickupMode() {
        return pickupMode;
    }

    public IncludeExclude getInsertMode() {
        return insertMode;
    }

    public void togglePickupMode() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_PICKUP_MODE);
        }
        magnetHost.togglePickupMode();
        pickupMode = magnetHost.getPickupMode();
    }

    public void toggleInsertMode() {
        if (isClientSide()) {
            sendClientAction(TOGGLE_INSERT_MODE);
        }
        magnetHost.toggleInsertMode();
        insertMode = magnetHost.getInsertMode();
    }

    public void copyUp() {
        if (isClientSide()) {
            sendClientAction(COPY_UP);
        }
        magnetHost.copyUp();
    }

    public void copyDown() {
        if (isClientSide()) {
            sendClientAction(COPY_DOWN);
        }
        magnetHost.copyDown();
    }

    public void switchInsertPickup() {
        if (isClientSide()) {
            sendClientAction(SWITCH_INSERT_PICKUP);
        }
        magnetHost.switchInsertPickup();
    }

    @Override
    public void broadcastChanges() {
        pickupMode = magnetHost.getPickupMode();
        insertMode = magnetHost.getInsertMode();
        super.broadcastChanges();
    }
}
