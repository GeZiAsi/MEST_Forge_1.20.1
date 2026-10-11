package com.lhy.mest.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.platform.InputConstants;

import org.lwjgl.glfw.GLFW;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import com.lhy.mest.network.MestPackets;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.client.AEKeyRendering;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.AEKeyFilter;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;

import com.lhy.mest.client.Ae2Icons;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.me.common.RepoSlot;
import appeng.client.gui.me.common.StackSizeRenderer;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ActionButton;
import appeng.client.gui.widgets.IconButton;
import appeng.client.gui.widgets.OpenGuideButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.Tooltip;
import appeng.client.gui.widgets.ITooltip;
import appeng.client.gui.widgets.TabButton;
import appeng.core.AEConfig;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.localization.Tooltips;
import appeng.api.features.HotkeyAction;
import appeng.client.Hotkeys;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.HotkeyPacket;
import appeng.core.sync.packets.ConfigValuePacket;
import appeng.core.sync.packets.InventoryActionPacket;
import appeng.core.sync.packets.SwitchGuisPacket;
import appeng.menu.me.crafting.CraftingStatusMenu;
import appeng.helpers.InventoryAction;
import appeng.items.storage.ViewCellItem;
import appeng.menu.SlotSemantics;
import appeng.menu.me.common.GridInventoryEntry;
import appeng.menu.slot.FakeSlot;
import appeng.util.Platform;

import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.terminal.ArmorSlot;
import de.mari_023.ae2wtlib.wut.CycleTerminalButton;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import de.mari_023.ae2wtlib.wut.WUTHandler;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.client.PatternProviderClientHandler.Entry;
import com.lhy.mest.client.dock.DockManager;
import com.lhy.mest.client.dock.ExtraChrome;
import com.lhy.mest.client.dock.ExtraSlotColumn;
import com.lhy.mest.client.dock.ModulePanel;
import com.lhy.mest.client.dock.PanelSideBar;
import com.lhy.mest.client.dock.ScrollingUpgradeColumn;
import com.lhy.mest.client.dock.model.DockRect;
import com.lhy.mest.client.dock.model.FloatingRoot;
import com.lhy.mest.client.panel.CraftingPanel;
import com.lhy.mest.client.panel.CraftingTerminalPanel;
import com.lhy.mest.client.panel.InventoryPanel;
import com.lhy.mest.client.panel.MEListPanel;
import com.lhy.mest.client.panel.PatternAccessPanel;
import com.lhy.mest.client.panel.PatternCachePanel;
import com.lhy.mest.client.panel.PatternEncodingPanel;
import com.lhy.mest.client.panel.ProviderSelectPanel;
import com.lhy.mest.client.panel.NetworkToolkitPanel;
import com.lhy.mest.client.panel.TrashPanel;
import com.lhy.mest.client.panel.ToolkitPanel;
import com.lhy.mest.client.panel.WirelessSettingsPanel;
import com.lhy.mest.client.panel.HotkeysPanel;
import com.lhy.mest.api.client.MestModuleContext;
import com.lhy.mest.client.hotkey.HotkeyActions;
import com.lhy.mest.client.hotkey.PanelHotkeys;
import net.minecraftforge.fml.ModList;
import com.lhy.mest.integration.MestRecipeTransferContext;
import com.lhy.mest.compat.plus.PlusScreenSupport;
import com.lhy.mest.network.PatternProviderActionPacket;
import com.lhy.mest.network.ProviderPickerListPacket;
import com.lhy.mest.terminal.MESTMenu;
import com.lhy.mest.terminal.MestSlotSemantics;
import com.lhy.mest.terminal.ToolkitSlot;

/**
 * Client screen for the ME Spliced Terminal.
 *
 * <p>Unlike a stock AE2 terminal (fixed JSON-driven layout), this screen extends {@link AEBaseScreen}
 * directly and hosts a set of free-floating, draggable, resizable {@link ModulePanel}s managed by a
 * {@link DockManager}. The dialog is pinned to the whole screen ({@code leftPos=topPos=0}), so menu
 * slot coordinates are absolute screen coordinates and panels can position their slots anywhere.
 */
public class MESTScreen extends AEBaseScreen<MESTMenu> implements IUniversalTerminalCapable {
    private final DockManager dock = new DockManager();
    private final List<SettingToggleButton<?>> meSettingButtons = new ArrayList<>();
    private final PanelSideBar meSideBar = new PanelSideBar();
    private final PanelSideBar moreSettingsBar = new PanelSideBar();
    private ExtraChrome upgradeColumn;
    private ExtraSlotColumn viewCellColumn;
    private ToolbarIconButton viewCellsToggleBtn;
    private boolean viewCellsVisible = true;
    private ToolbarIconButton moreSettingsToggleBtn;
    private boolean moreSettingsVisible = false;
    private final List<String> moreSettingsButtonIds = new ArrayList<>();
    private int moreSettingsPressIndex = -1;
    private double moreSettingsPressY;
    private boolean moreSettingsDragging;
    private static final String MORE_LOCK = "lock_layout";
    private static final String MORE_UNDO = "undo_layout";
    private static final String MORE_EDIT = "edit_layout";
    private static final String MORE_GROUP_PREFIX = "combined_module_";
    private static final int MORE_SETTINGS_SHIFT = 3;
    private ToolbarIconButton lockLayoutBtn;
    private ToolbarIconButton undoLayoutBtn;
    private ToolbarIconButton cyclePresetBtn;
    private Button cycleTerminalBtn;
    private AETextField searchField;
    private TabButton craftingStatusBtn;
    private final List<ItemStack> currentViewCells = new ArrayList<>();
    /** True while DockManager is drawing owned slots inside their root's ordered composite pass. */
    private boolean drawingRootSlots;
    private boolean hoverFrameReady;
    private int hoverCacheX = Integer.MIN_VALUE;
    private int hoverCacheY = Integer.MIN_VALUE;
    private ModulePanel hoverTop;
    private boolean hoverFloating;
    private ExtraChrome hoverExtra;
    private boolean hoverToolbar;
    private MEListPanel meListPanel;
    private CraftingPanel craftingPanel;
    private CraftingTerminalPanel craftingTerminalPanel;
    private PatternEncodingPanel patternEncodingPanel;
    private PatternAccessPanel patternAccessPanel;
    private PatternCachePanel patternCachePanel;
    private InventoryPanel inventoryPanel;
    private ProviderSelectPanel providerSelectPanel;
    private WirelessSettingsPanel wirelessSettingsPanel;
    private TrashPanel trashPanel;
    private ToolkitPanel toolkitPanel;
    private HotkeysPanel hotkeysPanel;
    private NetworkToolkitPanel networkToolkitPanel;
    private boolean keepPendingOnRemove;
    private final MestRecipeTransferController recipeTransfer = new MestRecipeTransferController();

    public MEListPanel meListPanel() {
        return meListPanel;
    }

    public MESTScreen(MESTMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        menu.getProcessingOutputSlots()[0].setIcon(Icon.BACKGROUND_PRIMARY_OUTPUT);
        // Create the ME list panel (and thus register the client repo) in the constructor, matching
        // AE2's MEStorageScreen. Doing this in init() would be too late: the server's first full
        // inventory update can arrive before init() runs and would be dropped (null client repo).
        this.meListPanel = new MEListPanel(getMenu());
        getMenu().setGui((manager, setting) -> onMenuReceivedClientUpdate());
        recipeTransfer.beginMenu(menu);

        this.searchField = widgets.addTextField("search");
        this.meListPanel.attachSearch(searchField);

        this.craftingStatusBtn = new TabButton(Icon.CRAFT_HAMMER,
                GuiText.CraftingStatus.text(), btn -> showCraftingStatus());
        this.meListPanel.attachCraftingStatus(craftingStatusBtn);

        var cm = getMenu().getConfigManager();
        addMeSideButton(new OpenGuideButton(btn -> openHelp()));
        addMeSideButton(new SettingToggleButton<>(Settings.SORT_BY,
                cm.getSetting(Settings.SORT_BY), Platform::isSortOrderAvailable, this::toggleServerSetting));
        addMeSideButton(new SettingToggleButton<>(Settings.VIEW_MODE,
                cm.getSetting(Settings.VIEW_MODE), this::toggleServerSetting));
        if (getMenu().canConfigureTypeFilter()) {
            addMeSideButton(new SettingToggleButton<>(Settings.TYPE_FILTER,
                    cm.getSetting(Settings.TYPE_FILTER), this::toggleServerSetting));
        }
        addMeSideButton(new SettingToggleButton<>(Settings.SORT_DIRECTION,
                cm.getSetting(Settings.SORT_DIRECTION), this::toggleServerSetting));
        addMeSideButton(new ActionButton(ActionItems.TERMINAL_SETTINGS, this::showSettings));
        cyclePresetBtn = addMeSideButton(new CyclePresetButton());
        viewCellsToggleBtn = addMeSideButton(new ViewCellsToggleButton(
                () -> viewCellsVisible,
                b -> toggleViewCellsPanel()));
        moreSettingsToggleBtn = addMeSideButton(new AtlasToolbarButton(
                4, 1,
                moreSettingsMessage(),
                b -> toggleMoreSettings(),
                () -> !moreSettingsVisible));
        if (getMenu().isWUT()) {
            cycleTerminalBtn = new CycleTerminalButton(btn -> cycleTerminal());
            addMeSideButton(cycleTerminalBtn);
        }
    }

    private ToolbarIconButton addMeSideButton(ToolbarIconButton button) {
        addMeSideButton((Button) button);
        return button;
    }

    private void addMeSideButton(Button button) {
        meSideBar.add(button);
        if (button instanceof SettingToggleButton<?> settingButton) {
            meSettingButtons.add(settingButton);
        }
    }

    private void showCraftingStatus() {
        NetworkHandler.instance().sendToServer(SwitchGuisPacket.openSubMenu(CraftingStatusMenu.TYPE));
    }

    private void showSettings() {
        switchToScreen(new MestTerminalSettingsScreen(this));
    }

    public void switchToWirelessTerminalSettings() {
        if (wirelessSettingsPanel == null) {
            return;
        }
        dock.revealModule(wirelessSettingsPanel);
        wirelessSettingsPanel.reloadFromStack();
    }

    public void openTrash() {
        if (trashPanel == null) {
            return;
        }
        dock.revealModule(trashPanel);
        getMenu().openTrashMenu();
    }

    public void closeTrash() {
        getMenu().closeTrash();
        if (trashPanel != null && dock.isEffectivelyVisible(trashPanel)) {
            dock.hideModule(trashPanel);
        }
    }

    public void dismissWirelessSettings() {
        dismissWirelessSettingsPanel();
    }

    /**
     * The wireless-settings panel is a transient popover: interacting with any other panel hides it
     * rather than leaving it open behind the workspace stack. Pinning does not keep it open, and
     * being covered by the panel just clicked must not block the dismissal either.
     */
    private void dismissWirelessSettingsPanel() {
        if (wirelessSettingsPanel == null) {
            return;
        }
        if (!dock.policyFor(wirelessSettingsPanel).visible()) {
            return;
        }
        dock.hideModule(wirelessSettingsPanel);
    }

    private void dismissUnpinnedFloating(ModulePanel panel) {
        if (panel == null) {
            return;
        }
        // A popup joined to another panel is part of that window now. Hiding just this leaf
        // when the player clicks its neighbor would visually tear the combined module apart.
        if (!dock.policyFor(panel).visible() || !dock.isFloatingUnpinned(panel)
                || dock.isJoinedWindow(panel)) {
            return;
        }
        dock.hideModule(panel);
        if (panel == toolkitPanel) {
            syncToolkitOpen();
        }
    }

    private void syncToolkitOpen() {
        if (toolkitPanel == null) {
            return;
        }
        boolean open = dock.isEffectivelyVisible(toolkitPanel);
        if (getMenu().toolkitOpen != open) {
            getMenu().setToolkitOpen(open);
        }
    }

    private void syncUtilityHost() {
        if (craftingTerminalPanel == null || meListPanel == null) {
            return;
        }
        boolean onMeList = !dock.isEffectivelyVisible(craftingTerminalPanel)
                && dock.isEffectivelyVisible(meListPanel);
        craftingTerminalPanel.setHostUtilitiesOnMeList(onMeList);
    }

    private boolean isCraftingModuleVisible() {
        return craftingPanel != null && dock.isEffectivelyVisible(craftingPanel)
                || craftingTerminalPanel != null && dock.isEffectivelyVisible(craftingTerminalPanel);
    }

    private Component viewCellsMessage() {
        return viewCellsMessage(viewCellsVisible);
    }

    private static Component viewCellsMessage(boolean visible) {
        return Component.translatable(visible
                ? "gui.mesplicedterminal.view_cells.visible"
                : "gui.mesplicedterminal.view_cells.hidden");
    }

    private void toggleViewCellsPanel() {
        viewCellsVisible = !viewCellsVisible;
        dock.setViewCellsVisible(viewCellsVisible);
        if (viewCellsToggleBtn != null) {
            viewCellsToggleBtn.setMessage(viewCellsMessage());
        }
        attachExtraSlotColumns();
    }

    private Component moreSettingsMessage() {
        return Component.translatable(moreSettingsVisible
                ? "gui.mesplicedterminal.more_settings.hide"
                : "gui.mesplicedterminal.more_settings.show");
    }

    private void toggleMoreSettings() {
        moreSettingsVisible = !moreSettingsVisible;
        dock.setMoreSettingsVisible(moreSettingsVisible);
        if (moreSettingsToggleBtn != null) {
            moreSettingsToggleBtn.setMessage(moreSettingsMessage());
        }
        attachMeSideBar();
    }

    private <SE extends Enum<SE>> void toggleServerSetting(SettingToggleButton<SE> btn, boolean backwards) {
        SE next = btn.getNextValue(backwards);
        NetworkHandler.instance().sendToServer(new ConfigValuePacket(btn.getSetting(), next));
        btn.set(next);
        if (meListPanel != null) {
            meListPanel.repo().updateView();
        }
    }

    public void onMenuReceivedClientUpdate() {
        for (SettingToggleButton<?> button : meSettingButtons) {
            syncSettingButton(button);
        }
        if (meListPanel != null) {
            meListPanel.repo().updateView();
        }
    }

    private <SE extends Enum<SE>> void syncSettingButton(SettingToggleButton<SE> button) {
        button.set(getMenu().getConfigManager().getSetting(button.getSetting()));
    }

    protected boolean shouldAddToolbar() {
        // We do not use AE2's fixed vertical toolbar; it is pinned to leftPos/topPos (0,0)
        // and would hang off the screen. The same buttons auto-attach to the spliced group.
        return false;
    }

    @Override
    protected <P extends AEBaseScreen<MESTMenu>> void onReturnFromSubScreen(
            appeng.client.gui.AESubScreen<MESTMenu, P> subScreen) {
        if (subScreen instanceof MestTerminalSettingsScreen && meListPanel != null) {
            meListPanel.tick(hasShiftDown());
        }
    }

    @Override
    protected void init() {
        // Keep the base dialog rect negligible (0x0 at 0,0) so JEI/EMI don't treat the whole screen as
        // occluded by the GUI (they assume [leftPos,topPos,imageWidth,imageHeight] is always occupied).
        // Slot coordinates are absolute screen coords set directly by panels, so they're unaffected.
        this.imageWidth = 0;
        this.imageHeight = 0;
        layoutToolboxSlots();

        boolean firstOpen = dock.isEmpty();
        if (firstOpen) {
            List<ModulePanel> panels = createModulePanels();
            dock.init(panels, this.width, this.height);
            viewCellsVisible = dock.viewCellsVisible();
            moreSettingsVisible = dock.moreSettingsVisible();
            if (viewCellsToggleBtn != null) {
                viewCellsToggleBtn.setMessage(viewCellsMessage());
            }
            if (moreSettingsToggleBtn != null) {
                moreSettingsToggleBtn.setMessage(moreSettingsMessage());
            }
            patternAccessPanel.applyRememberedButtons();
            if (providerSelectPanel != null) {
                getMenu().requestProviderList();
            }
        } else {
            dock.updateViewport(this.width, this.height);
        }

        super.init();

        this.leftPos = 0;
        this.topPos = 0;

        if (searchField != null) {
            removeWidget(searchField);
        }

        // The screen is registered with AE2's stock wireless_terminal style, which inherits text labels
        // (player inventory title, dialog title, item-count) anchored to a fixed GUI layout that doesn't
        // apply to our full-screen floating-panel UI. Hide them all so they don't bleed through.
        setTextHidden("player_inventory_title", true);
        setTextHidden("dialog_title", true);
        setTextHidden("entriesShown", true);

        syncUtilityHost();
        dock.layoutAll();
        dock.applyPendingCenter();
        String pendingHotkey = PanelHotkeys.takePendingOpen();
        if (pendingHotkey != null) {
            ModulePanel requested = panelById(pendingHotkey);
            if (requested == null) {
                runHotkey(pendingHotkey);
            } else if (!dock.policyFor(requested).visible()) {
                toggleModuleFromHotkey(requested);
            }
        }
        if (trashPanel != null && dock.isEffectivelyVisible(trashPanel) && !getMenu().isTrashOpen()) {
            getMenu().openTrashMenu();
        }
        dock.relayoutAllSlots();
        if (patternAccessPanel != null) {
            patternAccessPanel.rebindSearchField(this);
        }
        if (providerSelectPanel != null) {
            providerSelectPanel.rebindSearchField(this);
        }
        if (upgradeColumn == null && (Object) getMenu().getHost() instanceof IUpgradeableObject upgradeable) {
            List<Slot> upgradeSlots = new ArrayList<>();
            upgradeSlots.addAll(getMenu().getSlots(AE2wtlibSlotSemantics.SINGULARITY));
            upgradeSlots.addAll(getMenu().getSlots(SlotSemantics.UPGRADE));
            upgradeColumn = new ScrollingUpgradeColumn(upgradeSlots, upgradeable);
        }
        if (viewCellColumn == null) {
            viewCellColumn = new ExtraSlotColumn(
                    getMenu().getSlots(SlotSemantics.VIEW_CELL),
                    () -> List.of(GuiText.TerminalViewCellsTooltip.text()));
        }
        attachMeSideBar();
        attachExtraSlotColumns();
        syncToolkitOpen();
        syncExternalModGuiMetrics();
        refreshRecipeTransferAvailability();
        if (firstOpen && meListPanel != null && searchField != null
                && AEConfig.instance().isAutoFocusSearch()
                && !AEConfig.instance().isUseExternalSearch()) {
            meListPanel.setSearchFocused(true);
        }
    }

    private Component layoutLockMessage() {
        return Component.translatable(dock.isLayoutLocked()
                ? "gui.mesplicedterminal.unlock_layout"
                : "gui.mesplicedterminal.lock_layout");
    }

    private void refreshLayoutActions() {
        if (lockLayoutBtn != null) {
            lockLayoutBtn.setIcon(dock.isLayoutLocked() ? Icon.LOCKED : Icon.UNLOCKED);
            lockLayoutBtn.setMessage(layoutLockMessage());
        }
        if (undoLayoutBtn != null) {
            undoLayoutBtn.active = dock.canUndoLayout();
        }
        if (cyclePresetBtn != null) {
            cyclePresetBtn.setMessage(cyclePresetMessage());
        }
    }

    private Component cyclePresetMessage() {
        return Component.translatable(
                "gui.mesplicedterminal.layout_preset.cycle",
                dock.presetName(dock.activePreset()));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        refreshLayoutActions();
        syncUtilityHost();
        if (meListPanel != null && dock.isEffectivelyVisible(meListPanel)) {
            meListPanel.tick(hasShiftDown());
            updateViewCellFilter();
        }
        if (patternEncodingPanel != null && dock.isEffectivelyVisible(patternEncodingPanel)) {
            patternEncodingPanel.tick();
        }
        if (craftingTerminalPanel != null && (dock.isEffectivelyVisible(craftingTerminalPanel)
                || craftingTerminalPanel.hostUtilitiesOnMeList())) {
            craftingTerminalPanel.tick();
        }
        if (wirelessSettingsPanel != null && dock.isEffectivelyVisible(wirelessSettingsPanel)) {
            wirelessSettingsPanel.reloadFromStack();
        }
        syncToolkitOpen();
        if (patternAccessPanel != null && dock.isEffectivelyVisible(patternAccessPanel)) {
            patternAccessPanel.tick();
        }
        if (patternCachePanel != null && dock.isEffectivelyVisible(patternCachePanel)) {
            patternCachePanel.layoutSlots();
        }
        attachMeSideBar();
        attachExtraSlotColumns();
        syncExternalModGuiMetrics();
        refreshRecipeTransferAvailability();
    }

    private void refreshRecipeTransferAvailability() {
        recipeTransfer.synchronize(
                getMenu(),
                patternEncodingPanel != null && dock.isEffectivelyVisible(patternEncodingPanel),
                isCraftingModuleVisible());
    }

    private void attachMeSideBar() {
        DockRect group = dock.anchoredGroupBounds();
        if (group == null) {
            meSideBar.hide();
            moreSettingsBar.hide();
            return;
        }
        rebuildMoreSettingsModules();
        meSideBar.layoutAgainst(group.x(), group.y(), true);
        for (Button button : moreSettingsBar.buttons()) {
            button.visible = true;
        }
        int moreLeft = (meSideBar.isVisible() ? meSideBar.bounds().getX() : group.x()) - MORE_SETTINGS_SHIFT;
        moreSettingsBar.layoutAgainst(moreLeft, group.y(), moreSettingsVisible);
        ensureCyclePickerOnScreen();
    }

    private void ensureCyclePickerOnScreen() {
        if (!(cycleTerminalBtn instanceof ITooltip tooltip) || !cycleTerminalBtn.visible) {
            return;
        }
        Rect2i area = tooltip.getTooltipArea();
        if (area.getX() >= 0) {
            return;
        }
        cycleTerminalBtn.setX(cycleTerminalBtn.getX() - area.getX());
    }

    private void rebuildMoreSettingsModules() {
        if (moreSettingsDragging) {
            return;
        }
        List<String> available = new ArrayList<>();
        available.add(MORE_LOCK);
        available.add(MORE_UNDO);
        available.add(MORE_EDIT);
        List<String> resolved = new ArrayList<>();
        List<String> groups = new ArrayList<>();
        // Put joined-window controls first, even if an older saved order lists every panel ahead of
        // them; otherwise they can end up below the visible screen at the bottom of this rail.
        for (int group = 1; group <= FloatingRoot.MAX_GROUPS; group++) {
            if (dock.hasGroup(group)) {
                String id = MORE_GROUP_PREFIX + group;
                available.add(id);
                groups.add(id);
            }
        }
        for (String id : dock.moreSettingsOrder()) {
            if (groups.contains(id) && !resolved.contains(id)) {
                resolved.add(id);
            }
        }
        for (String id : groups) {
            if (!resolved.contains(id)) {
                resolved.add(id);
            }
        }
        for (ModulePanel panel : dock.panels()) {
            if (panel != null && dock.groupOf(panel) == 0 && dock.policyFor(panel).showTerminalButton()) {
                available.add(panel.id());
            }
        }
        for (String id : dock.moreSettingsOrder()) {
            if (available.contains(id) && !resolved.contains(id)) {
                resolved.add(id);
            }
        }
        for (String id : available) {
            if (!resolved.contains(id)) {
                resolved.add(id);
            }
        }
        if (resolved.equals(moreSettingsButtonIds) && !moreSettingsBar.buttons().isEmpty()) {
            return;
        }
        moreSettingsButtonIds.clear();
        moreSettingsButtonIds.addAll(resolved);
        moreSettingsBar.clearFrom(0);
        for (String id : resolved) {
            moreSettingsBar.add(createMoreSettingsButton(id));
        }
    }

    private Button createMoreSettingsButton(String id) {
        return switch (id) {
            case MORE_LOCK -> {
                lockLayoutBtn = new ToolbarIconButton(
                        dock.isLayoutLocked() ? Icon.LOCKED : Icon.UNLOCKED,
                        layoutLockMessage(),
                        b -> dock.toggleLayoutLocked());
                yield lockLayoutBtn;
            }
            case MORE_UNDO -> {
                undoLayoutBtn = new ToolbarIconButton(
                        Ae2Icons.BACK,
                        Component.translatable("gui.mesplicedterminal.undo_layout"),
                        b -> dock.undoLayout());
                undoLayoutBtn.active = dock.canUndoLayout();
                yield undoLayoutBtn;
            }
            case MORE_EDIT -> new AtlasToolbarButton(
                    5, 1,
                    Component.translatable("gui.mesplicedterminal.edit_layout"),
                    b -> Minecraft.getInstance().setScreen(new MESTLayoutEditorScreen(this, dock)));
            default -> {
                if (id.startsWith(MORE_GROUP_PREFIX)) {
                    yield new CombinedModuleButton(Integer.parseInt(id.substring(MORE_GROUP_PREFIX.length())));
                }
                ModulePanel panel = panelById(id);
                yield panel == null
                        ? new ToolbarIconButton(Ae2Icons.COG, Component.literal(id), b -> { })
                        : new ModuleToggleButton(panel);
            }
        };
    }

    /**
     * Builds every registered module, built-ins first, and keeps typed handles on the built-ins
     * this screen coordinates directly.
     */
    private List<ModulePanel> createModulePanels() {
        List<ModulePanel> panels = new ArrayList<>();
        for (MestClientModules.Registration registration : MestClientModules.all()) {
            ModulePanel panel = registration.factory().create(moduleContext(registration.id()));
            if (panel == null) {
                continue;
            }
            if (!registration.id().equals(panel.id())) {
                throw new IllegalStateException("MEST module registered as " + registration.id()
                        + " created a panel with id " + panel.id());
            }
            panels.add(panel);
            if (panel instanceof CraftingPanel p) {
                craftingPanel = p;
            } else if (panel instanceof CraftingTerminalPanel p) {
                craftingTerminalPanel = p;
            } else if (panel instanceof PatternEncodingPanel p) {
                patternEncodingPanel = p;
            } else if (panel instanceof PatternAccessPanel p) {
                patternAccessPanel = p;
            } else if (panel instanceof PatternCachePanel p) {
                patternCachePanel = p;
            } else if (panel instanceof InventoryPanel p) {
                inventoryPanel = p;
            } else if (panel instanceof WirelessSettingsPanel p) {
                wirelessSettingsPanel = p;
            } else if (panel instanceof TrashPanel p) {
                trashPanel = p;
            } else if (panel instanceof ToolkitPanel p) {
                toolkitPanel = p;
            } else if (panel instanceof NetworkToolkitPanel p) {
                networkToolkitPanel = p;
            } else if (panel instanceof HotkeysPanel p) {
                hotkeysPanel = p;
            } else if (panel instanceof ProviderSelectPanel p) {
                providerSelectPanel = p;
            }
        }
        if (craftingTerminalPanel != null) {
            meListPanel.setUtilitySource(craftingTerminalPanel);
        }
        return panels;
    }

    private MestModuleContext moduleContext(String moduleId) {
        MESTScreen screen = this;
        return new MestModuleContext() {
            @Override
            public String moduleId() {
                return moduleId;
            }

            @Override
            public MESTMenu menu() {
                return getMenu();
            }

            @Override
            public MESTScreen screen() {
                return screen;
            }

            @Override
            public ScreenStyle style() {
                return style;
            }

            @Override
            public DockManager dock() {
                return dock;
            }

            @Override
            public List<Slot> slots() {
                return getMenu().getModuleSlots(moduleId);
            }
        };
    }

    private ModulePanel panelById(String id) {
        for (ModulePanel panel : dock.panels()) {
            if (panel != null && id.equals(panel.id())) {
                return panel;
            }
        }
        return null;
    }

    private void attachExtraSlotColumns() {
        DockRect group = dock.anchoredGroupBounds();
        if (group == null) {
            if (upgradeColumn != null) {
                upgradeColumn.hide();
            }
            if (viewCellColumn != null) {
                viewCellColumn.hide();
            }
            layoutToolboxSlots();
            return;
        }        int attachX = extraColumnAttachX(group);
        if (rightEdgeIsPatternCache(group)) {
            attachX += 2;
        } else {
            attachX -= 2;
        }
        int attachY = group.y();
        if (upgradeColumn instanceof ScrollingUpgradeColumn scrolling) {
            scrolling.setMaxRows(Math.max(2, (group.height() / 2 - 10) / 18));
        }
        if (upgradeColumn != null && upgradeColumn.hasSlots()) {
            upgradeColumn.layoutAgainst(attachX, attachY, true);
            attachX = upgradeColumn.nextColumnX();
        } else if (upgradeColumn != null) {
            upgradeColumn.hide();
        }
        if (viewCellColumn != null && viewCellColumn.hasSlots() && viewCellsVisible) {
            viewCellColumn.layoutAgainst(attachX, attachY, true);
        } else if (viewCellColumn != null) {
            viewCellColumn.hide();
        }
        layoutToolboxSlots();
    }

    /**
     * The network-tool module owns whatever {@link MESTMenu#getNetworkToolkitSlots()} reports — the
     * carried vanilla tool's slots when there is one, the terminal's built-in inventory otherwise.
     * The set the module does not show is parked, so neither ends up drawn at the screen origin.
     */
    private void layoutToolboxSlots() {
        boolean moduleVisible = networkToolkitPanel != null && dock.isEffectivelyVisible(networkToolkitPanel);
        for (Slot slot : getMenu().getSlots(SlotSemantics.TOOLBOX)) {
            if (!moduleVisible || !networkToolkitPanel.ownsSlot(slot)) {
                parkSlot(slot);
            }
        }
        for (Slot slot : getMenu().getSlots(MestSlotSemantics.NETWORK_TOOLKIT)) {
            if (!moduleVisible || !networkToolkitPanel.ownsSlot(slot)) {
                parkSlot(slot);
            }
        }
    }

    private static void parkSlot(Slot slot) {
        slot.x = -9999;
        slot.y = -9999;
    }

    /**
     * Sit extra chrome to the right of hanging ME/pattern-access rails, same as the wireless
     * terminal well. Do not tuck into a right-edge panel that keeps an in-panel scroller.
     */
    private int extraColumnAttachX(DockRect group) {
        int hanging = 0;
        int rightEdgeInset = 0;
        for (ModulePanel panel : dock.panels()) {
            if (!dock.isEffectivelyVisible(panel) || dock.isFloatingWindow(panel)) {
                continue;
            }
            if (panel.x >= group.right() || panel.x + panel.width <= group.x()
                    || panel.y >= group.bottom() || panel.y + panel.height <= group.y()) {
                continue;
            }
            if (panel.x + panel.width < group.right() - 2) {
                continue;
            }
            hanging = Math.max(hanging, panel.outsideHitWidth());
            rightEdgeInset = Math.max(rightEdgeInset, panel.contentRightInset);
        }
        if (hanging > 0) {
            return group.right() + hanging;
        }
        if (rightEdgeInset > 0) {
            return group.right() + 2;
        }
        return group.right() - 4;
    }

    private boolean rightEdgeIsPatternCache(DockRect group) {
        for (ModulePanel panel : dock.panels()) {
            if (!dock.isEffectivelyVisible(panel) || dock.isFloatingWindow(panel)) {
                continue;
            }
            if (!"pattern_cache".equals(panel.id())) {
                continue;
            }
            if (panel.x + panel.width >= group.right() - 2
                    && panel.y < group.bottom()
                    && panel.y + panel.height > group.y()) {
                return true;
            }
        }
        return false;
    }

    private void updateViewCellFilter() {
        var viewCells = getMenu().getViewCells();
        if (!currentViewCells.equals(viewCells)) {
            currentViewCells.clear();
            currentViewCells.addAll(viewCells);
            meListPanel.repo().setPartitionList(ViewCellItem.createFilter(AEKeyFilter.none(), viewCells));
        }
    }

    @Override
    public void drawBG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        syncResizeCursor(mouseX, mouseY);
        // Composite every root atomically (background -> owned slots -> next root). AE key renderers
        // mix immediate item rendering with batched fluids/add-on types, so a later global slot pass
        // cannot preserve window order reliably even when all calls receive a pose-stack z offset.
        drawingRootSlots = true;
        try {
            dock.renderBackground(
                    g, this.font, mouseX, mouseY, partialTicks, this::drawRootSlot, this::renderAnchoredChrome);
        } finally {
            drawingRootSlots = false;
        }
    }

    private void drawRootSlot(GuiGraphics g, Slot slot) {
        renderSlotContents(g, slot);
    }

    @Override
    public void drawFG(GuiGraphics g, int offsetX, int offsetY, int mouseX, int mouseY) {
        // Foreground is for panel controls and labels only. Panel-owned slots are intentionally not
        // rendered here, otherwise every window's slots would be drawn after every window's chrome.
        dock.renderForeground(g, this.font, mouseX, mouseY, 0);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTicks) {
        hoverFrameReady = false;
        super.render(g, mouseX, mouseY, partialTicks);
        g.pose().pushPose();
        try {
            g.pose().translate(0.0F, 0.0F, 4500.0F);
            renderAttachedChromeTooltips(g, mouseX, mouseY);
        } finally {
            g.pose().popPose();
        }
        ItemStack carried = getMenu().getCarried();
        if (!carried.isEmpty()) {
            g.pose().pushPose();
            try {
                // Vanilla draws the carried stack at the default pose after slots, so extra chrome
                // (z≈layer*400) covers it. Redraw above that chrome but below the GUI far plane —
                // z=4500 clips the 3D item model and the icon looks cut off.
                g.pose().translate(0.0F, 0.0F, dock.cursorItemZ());
                g.renderItem(carried, mouseX - 8, mouseY - 8);
                g.renderItemDecorations(this.font, carried, mouseX - 8, mouseY - 8);
            } finally {
                g.pose().popPose();
            }
        }
        syncResizeCursor(mouseX, mouseY);
    }

    private void renderAttachedChromeTooltips(GuiGraphics g, int mouseX, int mouseY) {
        if (cycleTerminalBtn instanceof ITooltip cycleTooltip
                && cycleTerminalBtn.visible
                && cycleTooltip.isTooltipAreaVisible()
                && cyclePickerHit(mouseX, mouseY)
                && !cycleTooltip.getTooltipMessage().isEmpty()) {
            drawAeWidgetTooltip(g, mouseX, mouseY, cycleTooltip);
            return;
        }
        ModulePanel top = dock.topPanelAt(mouseX, mouseY);
        if (top != null && dock.isFloatingWindow(top)) {
            return;
        }
        ExtraChrome extra = extraColumnAt(mouseX, mouseY);
        if (extra != null) {
            List<Component> lines = extra.chromeTooltipAt(mouseX, mouseY);
            if (!lines.isEmpty()) {
                drawTooltipWithHeader(g, mouseX, mouseY, lines);
                return;
            }
        }
        ITooltip moreSettings = moreSettingsBar.hoveredTooltip(mouseX, mouseY);
        if (moreSettings != null && moreSettings.isTooltipAreaVisible()
                && !moreSettings.getTooltipMessage().isEmpty()) {
            drawAeWidgetTooltip(g, mouseX, mouseY, moreSettings);
            return;
        }
        ITooltip sidebar = meSideBar.hoveredTooltip(mouseX, mouseY);
        if (sidebar != null && sidebar.isTooltipAreaVisible() && !sidebar.getTooltipMessage().isEmpty()) {
            drawAeWidgetTooltip(g, mouseX, mouseY, sidebar);
            return;
        }
        if (meListPanel != null) {
            ITooltip craftingStatus = meListPanel.hoveredCraftingStatusTooltip(mouseX, mouseY);
            if (craftingStatus != null && !craftingStatus.getTooltipMessage().isEmpty()) {
                drawAeWidgetTooltip(g, mouseX, mouseY, craftingStatus);
                return;
            }
        }
        if (patternEncodingPanel != null) {
            ITooltip encoding = patternEncodingPanel.hoveredTooltip(mouseX, mouseY);
            if (encoding != null && encoding.isTooltipAreaVisible() && !encoding.getTooltipMessage().isEmpty()) {
                drawAeWidgetTooltip(g, mouseX, mouseY, encoding);
            }
        }
    }

    /** Same path as {@code AEBaseScreen.renderTooltips}: split {@code title\\nbody} then header/body colors. */
    private void drawAeWidgetTooltip(GuiGraphics g, int mouseX, int mouseY, ITooltip tooltip) {
        drawTooltipWithHeader(g, mouseX, mouseY, new Tooltip(tooltip.getTooltipMessage()).getContent());
    }

    /**
     * Left toolbar and extra slot columns belong to the anchored terminal group, so they paint
     * with that group and stay under floating windows.
     */
    private void renderAnchoredChrome(GuiGraphics g, Font font, int mouseX, int mouseY, float partialTicks) {
        if (meSideBar.isVisible()) {
            meSideBar.renderBackground(g);
            for (Button button : meSideBar.buttons()) {
                button.render(g, mouseX, mouseY, partialTicks);
            }
        }
        if (moreSettingsBar.isVisible()) {
            g.pose().pushPose();
            try {
                g.pose().translate(0.0F, 0.0F, 80.0F);
                moreSettingsBar.renderBackground(g);
                for (Button button : moreSettingsBar.buttons()) {
                    button.render(g, mouseX, mouseY, partialTicks);
                }
            } finally {
                g.pose().popPose();
            }
        }
        if (cycleTerminalBtn != null && cycleTerminalBtn.visible) {
            g.pose().pushPose();
            try {
                g.pose().translate(0.0F, 0.0F, 120.0F);
                cycleTerminalBtn.render(g, mouseX, mouseY, partialTicks);
            } finally {
                g.pose().popPose();
            }
        }
        if (upgradeColumn != null && upgradeColumn.isVisible()) {
            upgradeColumn.renderBackground(g);
            g.flush();
            g.pose().pushPose();
            try {
                g.pose().translate(0.0F, 0.0F, DockManager.SLOT_CONTENT_Z);
                upgradeColumn.renderSlots(g, this::drawRootSlot);
                g.flush();
            } finally {
                g.pose().popPose();
            }
        }
        if (viewCellColumn != null && viewCellColumn.isVisible()) {
            viewCellColumn.renderBackground(g);
            g.flush();
            g.pose().pushPose();
            try {
                g.pose().translate(0.0F, 0.0F, DockManager.SLOT_CONTENT_Z);
                viewCellColumn.renderSlots(g, this::drawRootSlot);
                g.flush();
            } finally {
                g.pose().popPose();
            }
        }
    }

    /**
     * ClientSort (and similar injectors) place buttons at {@code leftPos + imageWidth}.
     * Slots are already in screen space, so left/top stay 0 and imageWidth is the
     * player-inventory's right edge.
     */
    private void syncExternalModGuiMetrics() {
        leftPos = 0;
        topPos = 0;
        if (inventoryPanel != null && dock.isEffectivelyVisible(inventoryPanel)
                && inventoryPanel.width > 0 && inventoryPanel.height > 0) {
            imageWidth = Math.max(1, inventoryPanel.x + inventoryPanel.width);
            imageHeight = Math.max(1, inventoryPanel.y + inventoryPanel.height);
            return;
        }
        imageWidth = 1;
        imageHeight = 1;
    }

    /**
     * Tight core rectangle (ME list, else first visible panel) so JEI/EMI treat the
     * terminal as a small GUI and stair-step around the rest via exclusion zones.
     */
    public Rect2i recipeViewerBounds() {
        ModulePanel core = null;
        if (meListPanel != null && dock.isEffectivelyVisible(meListPanel)
                && meListPanel.width > 0 && meListPanel.height > 0) {
            core = meListPanel;
        } else {
            for (ModulePanel panel : dock.panels()) {
                if (panel != null && dock.isEffectivelyVisible(panel)
                        && !dock.isFloatingWindow(panel)
                        && panel.width > 0 && panel.height > 0) {
                    core = panel;
                    break;
                }
            }
        }
        if (core != null) {
            return new Rect2i(Math.max(0, core.x), Math.max(0, core.y),
                    Math.max(1, core.width), Math.max(1, core.height));
        }
        return new Rect2i(0, 0, 1, 1);
    }

    /**
     * Full union of the spliced terminal plus attached chrome. Kept for callers that
     * need the occupied envelope rather than the JEI core rectangle.
     */
    public Rect2i recipeViewerEnvelope() {
        DockRect group = dock.anchoredGroupBounds();
        if (group == null) {
            for (DockRect bounds : dock.exclusionBounds()) {
                group = group == null ? bounds : group.union(bounds);
            }
        }
        if (group == null) {
            for (ModulePanel panel : dock.panels()) {
                if (panel == null || !panel.visible || panel.width <= 0 || panel.height <= 0) {
                    continue;
                }
                DockRect part = new DockRect(panel.x, panel.y, panel.width, panel.height);
                group = group == null ? part : group.union(part);
            }
        }
        if (group == null) {
            return new Rect2i(0, 0, 0, 0);
        }
        int left = group.x();
        int top = group.y();
        int right = group.right();
        int bottom = group.bottom();
        if (moreSettingsBar.isVisible()) {
            Rect2i rail = moreSettingsBar.bounds();
            left = Math.min(left, rail.getX() - 2);
            top = Math.min(top, rail.getY() - 1);
            bottom = Math.max(bottom, rail.getY() + rail.getHeight() + 3);
        }
        if (meSideBar.isVisible()) {
            Rect2i rail = meSideBar.bounds();
            left = Math.min(left, rail.getX() - 2);
            top = Math.min(top, rail.getY() - 1);
            bottom = Math.max(bottom, rail.getY() + rail.getHeight() + 3);
        }
        if (meListPanel != null && dock.isEffectivelyVisible(meListPanel)) {
            right = Math.max(right, meListPanel.x + meListPanel.width + meListPanel.outsideHitWidth());
            top = Math.min(top, meListPanel.y - meListPanel.outsideHitTop());
        }
        if (patternEncodingPanel != null && dock.isEffectivelyVisible(patternEncodingPanel)) {
            right = Math.max(right, patternEncodingPanel.x + patternEncodingPanel.width
                    + patternEncodingPanel.outsideHitWidth());
        }
        if (patternAccessPanel != null && dock.isEffectivelyVisible(patternAccessPanel)) {
            right = Math.max(right, patternAccessPanel.x + patternAccessPanel.width
                    + patternAccessPanel.outsideHitWidth());
        }
        if (upgradeColumn != null && upgradeColumn.isVisible()) {
            Rect2i extra = upgradeColumn.bounds();
            right = Math.max(right, extra.getX() + extra.getWidth());
            bottom = Math.max(bottom, extra.getY() + extra.getHeight());
        }
        if (viewCellColumn != null && viewCellColumn.isVisible()) {
            Rect2i extra = viewCellColumn.bounds();
            right = Math.max(right, extra.getX() + extra.getWidth());
            bottom = Math.max(bottom, extra.getY() + extra.getHeight());
        }
        return new Rect2i(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    /**
     * EMI/JEI ghost-ingredient drop onto the topmost floating (or anchored) panel. AE2's generic
     * handler walks every {@link FakeSlot} and ignores z-order, so a covered encoding grid would
     * steal drops from the raised window — or, with slots parked at -9999, accept nothing.
     */
    public boolean dropRecipeViewerStack(GenericStack stack, int x, int y) {
        ModulePanel top = dock.topLeafAt(x, y);
        if (top == null || stack == null) {
            return false;
        }
        ItemStack item = wrapFilterAsItem(stack);
        for (Slot slot : getMenu().slots) {
            if (!slot.isActive() || !(slot instanceof FakeSlot fake) || !top.ownsSlot(slot)) {
                continue;
            }
            if (slot.x <= -1000 || slot.y <= -1000) {
                continue;
            }
            if (x < slot.x || x >= slot.x + 16 || y < slot.y || y >= slot.y + 16) {
                continue;
            }
            if (fake.canSetFilterTo(item)) {
                fake.setFilterTo(item);
                return true;
            }
        }
        return false;
    }

    /** Highlight z-order-correct ghost-slot drop targets while EMI is dragging an ingredient. */
    public void renderRecipeViewerDropTargets(GuiGraphics graphics, Set<GenericStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return;
        }
        for (Slot slot : getMenu().slots) {
            if (!slot.isActive() || !(slot instanceof FakeSlot fake)
                    || slot.x <= -1000 || slot.y <= -1000) {
                continue;
            }
            ModulePanel top = dock.topLeafAt(slot.x + 8, slot.y + 8);
            if (top == null || !top.ownsSlot(slot)) {
                continue;
            }
            boolean any = false;
            for (GenericStack stack : stacks) {
                if (stack != null && fake.canSetFilterTo(wrapFilterAsItem(stack))) {
                    any = true;
                    break;
                }
            }
            if (any) {
                graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x8822FF55);
            }
        }
    }

    private static ItemStack wrapFilterAsItem(GenericStack stack) {
        AEKey key = stack.what();
        if (key instanceof AEItemKey itemKey) {
            return itemKey.toStack((int) Math.max(1L, Math.min(Integer.MAX_VALUE, stack.amount())));
        }
        return GenericStack.wrapInItemStack(key, Math.max(1L, stack.amount()));
    }

    @Override
    public List<Rect2i> getExclusionZones() {
        List<Rect2i> zones = new ArrayList<>();
        for (ModulePanel panel : dock.panels()) {
            if (panel == null || !dock.isEffectivelyVisible(panel)
                    || panel.width <= 0 || panel.height <= 0) {
                continue;
            }
            zones.add(new Rect2i(panel.x, panel.y, panel.width, panel.height));
        }
        if (moreSettingsBar.isVisible()) {
            Rect2i rail = moreSettingsBar.bounds();
            zones.add(new Rect2i(rail.getX() - 2, rail.getY() - 1, rail.getWidth() + 1, rail.getHeight() + 4));
        }
        if (meSideBar.isVisible()) {
            Rect2i rail = meSideBar.bounds();
            zones.add(new Rect2i(rail.getX() - 2, rail.getY() - 1, rail.getWidth() + 1, rail.getHeight() + 4));
        }
        if (meListPanel != null && dock.isEffectivelyVisible(meListPanel)) {
            if (meListPanel.outsideHitWidth() > 0) {
                zones.add(new Rect2i(
                        meListPanel.x + meListPanel.width,
                        meListPanel.y - 1,
                        meListPanel.outsideHitWidth(),
                        meListPanel.height + 1));
            }
            if (meListPanel.outsideHitTop() > 0) {
                zones.add(new Rect2i(
                        meListPanel.x + meListPanel.width + meListPanel.outsideHitWidth() - 24,
                        meListPanel.y - meListPanel.outsideHitTop(),
                        20,
                        meListPanel.outsideHitTop() + 20));
            }
        }
        if (patternEncodingPanel != null && dock.isEffectivelyVisible(patternEncodingPanel)
                && patternEncodingPanel.outsideHitWidth() > 0) {
            zones.add(new Rect2i(
                    patternEncodingPanel.x + patternEncodingPanel.width,
                    patternEncodingPanel.y,
                    patternEncodingPanel.outsideHitWidth(),
                    patternEncodingPanel.height));
        }
        if (patternAccessPanel != null && dock.isEffectivelyVisible(patternAccessPanel)
                && patternAccessPanel.outsideHitWidth() > 0) {
            zones.add(new Rect2i(
                    patternAccessPanel.x + patternAccessPanel.width,
                    patternAccessPanel.y - 1,
                    patternAccessPanel.outsideHitWidth(),
                    patternAccessPanel.height + 1));
        }
        addExtraColumnExclusion(zones, upgradeColumn);
        addExtraColumnExclusion(zones, viewCellColumn);
        return zones;
    }

    private static void addExtraColumnExclusion(List<Rect2i> zones, ExtraChrome column) {
        if (column != null && column.isVisible()) {
            Rect2i extra = column.bounds();
            zones.add(new Rect2i(extra.getX(), extra.getY(), extra.getWidth(), extra.getHeight()));
        }
    }

    /** Shared AE2 icon mapping for a module panel; also used by the layout editor sidebar. */
    public static void blitPanelIcon(GuiGraphics graphics, ModulePanel panel, int x, int y) {
        blitPanelIcon(graphics, panel, x, y, 16, 16);
    }

    public static void blitPanelIcon(GuiGraphics graphics, ModulePanel panel, int x, int y, int w, int h) {
        blitPanelIcon(graphics, panel, x, y, w, h, 1.0F);
    }

    public static void blitPanelIcon(
            GuiGraphics graphics, ModulePanel panel, int x, int y, int w, int h, float opacity) {
        panel.renderIcon(graphics, x, y, w, h, opacity);
    }

    /**
     * Preset cycle uses row 2, icons 10–12 of {@code layout_preset_icons.png} (16×16).
     */
    private final class CyclePresetButton extends ToolbarIconButton {
        private static final ResourceLocation ICONS = new ResourceLocation(
                MESplicedterminal.MODID, "textures/guis/layout_preset_icons.png");
        private static final int ICON_U0 = 144;
        private static final int ICON_V = 16;
        private static final int ICON_SIZE = 16;

        CyclePresetButton() {
            super(Icon.SCHEDULING_ROUND_ROBIN, cyclePresetMessage(), b -> dock.cyclePreset());
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            Icon bgIcon = isHovered()
                    ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_HOVER
                    : isFocused() ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            bgIcon.getBlitter()
                    .dest(getX() - 1, getY() + yOffset, 18, 20)

                    .blit(guiGraphics);
            int iconU = ICON_U0 + dock.activePreset() * ICON_SIZE;
            Blitter.texture(ICONS, 256, 256)
                    .src(iconU, ICON_V, ICON_SIZE, ICON_SIZE)
                    .dest(getX(), getY() + 1 + yOffset)

                    .blit(guiGraphics);
        }
    }

    private static final class ViewCellsToggleButton extends ToolbarIconButton {
        private static final ResourceLocation STATES = new ResourceLocation(
                MESplicedterminal.MODID, "textures/guis/pattern_cache_states.png");
        private static final int ICON_U_VISIBLE = 144;
        private static final int ICON_U_HIDDEN = 160;
        private static final int ICON_V = 32;
        private static final int ICON_SIZE = 16;

        private final BooleanSupplier viewCellsVisible;

        ViewCellsToggleButton(BooleanSupplier viewCellsVisible, OnPress onPress) {
            super(Icon.BACKGROUND_VIEW_CELL, Component.empty(), onPress);
            this.viewCellsVisible = viewCellsVisible;
            setMessage(viewCellsMessage(viewCellsVisible.getAsBoolean()));
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            Icon bgIcon = isHovered()
                    ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_HOVER
                    : isFocused() ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            bgIcon.getBlitter()
                    .dest(getX() - 1, getY() + yOffset, 18, 20)

                    .blit(guiGraphics);
            int iconU = viewCellsVisible.getAsBoolean() ? ICON_U_VISIBLE : ICON_U_HIDDEN;
            Blitter.texture(STATES, 256, 256)
                    .src(iconU, ICON_V, ICON_SIZE, ICON_SIZE)
                    .dest(getX(), getY() + 1 + yOffset)

                    .blit(guiGraphics);
        }
    }

    private static class ToolbarIconButton extends IconButton {
        private Icon icon;

        ToolbarIconButton(Icon icon, Component tooltip, OnPress onPress) {
            super(onPress);
            this.icon = icon;
            setMessage(tooltip);
        }

        void setIcon(Icon icon) {
            this.icon = icon;
        }

        @Override
        protected Icon getIcon() {
            return icon;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            if (!isDisableBackground()) {
                Icon bgIcon = isHovered()
                        ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_HOVER
                        : isFocused() ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
                bgIcon.getBlitter()
                        .dest(getX() - 1, getY() + yOffset, 18, 20)

                        .blit(guiGraphics);
            }
            int ix = getX() + (16 - icon.width) / 2;
            int iy = getY() + 1 + yOffset + (16 - icon.height) / 2;
            var blitter = icon.getBlitter();
            if (!active) {
                blitter.opacity(0.5f);
            }
            blitter.dest(ix, iy).blit(guiGraphics);
        }
    }

    private static class AtlasToolbarButton extends ToolbarIconButton {
        private final int col;
        private final int row;
        private final java.util.function.BooleanSupplier dimmed;

        AtlasToolbarButton(int col, int row, Component tooltip, OnPress onPress) {
            this(col, row, tooltip, onPress, () -> false);
        }

        AtlasToolbarButton(
                int col, int row, Component tooltip, OnPress onPress, java.util.function.BooleanSupplier dimmed) {
            super(Ae2Icons.COG, tooltip, onPress);
            this.col = col;
            this.row = row;
            this.dimmed = dimmed;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            Icon bgIcon = isHovered()
                    ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_HOVER
                    : isFocused() ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            bgIcon.getBlitter()
                    .dest(getX() - 1, getY() + yOffset, 18, 20)

                    .blit(guiGraphics);
            MestGuiIcons.blit(
                    guiGraphics, col, row, getX(), getY() + 1 + yOffset, 16, 16, dimmed.getAsBoolean() ? 0.5F : 1.0F);
        }
    }

    private final class ModuleToggleButton extends ToolbarIconButton {
        private final ModulePanel panel;

        ModuleToggleButton(ModulePanel panel) {
            super(panel.icon(), moduleToggleMessage(panel, dock.isEffectivelyVisible(panel)),
                    button -> toggleModule(panel));
            this.panel = panel;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            boolean shown = dock.isEffectivelyVisible(panel);
            setMessage(moduleToggleMessage(panel, shown));
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            Icon bgIcon = isHovered()
                    ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_HOVER
                    : isFocused() ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            bgIcon.getBlitter()
                    .dest(getX() - 1, getY() + yOffset, 18, 20)

                    .blit(guiGraphics);
            int ix = getX();
            int iy = getY() + 1 + yOffset;
            blitPanelIcon(guiGraphics, panel, ix, iy, 16, 16, shown ? 1.0F : 0.5F);
        }
    }

    /** Toggles the bound panel, or runs a layout action such as a preset switch. */
    private boolean runHotkey(String id) {
        ModulePanel panel = panelById(id);
        if (panel != null) {
            toggleModuleFromHotkey(panel);
            return true;
        }
        if (HotkeyActions.CYCLE_PRESET.equals(id)) {
            dock.cyclePreset();
            return true;
        }
        int combined = HotkeyActions.groupIndex(id);
        if (combined > 0) {
            if (dock.hasGroup(combined)) {
                toggleCombinedModule(combined);
            }
            return true;
        }
        int preset = HotkeyActions.presetIndex(id);
        if (preset >= 0 && preset < dock.presetCount()) {
            dock.selectPreset(preset);
            return true;
        }
        return false;
    }

    /** Panel hotkeys follow the side-bar buttons, plus the trash and settings panels' own open paths. */
    private void toggleModuleFromHotkey(ModulePanel panel) {
        boolean shown = dock.policyFor(panel).visible();
        if (panel == trashPanel) {
            if (shown) {
                closeTrash();
            } else {
                openTrash();
            }
            dock.save();
            attachMeSideBar();
            attachExtraSlotColumns();
            return;
        }
        toggleModule(panel);
        if (!shown && panel == wirelessSettingsPanel) {
            wirelessSettingsPanel.reloadFromStack();
        }
    }

    /** Shows or hides a combined module (a floating spliced window) as one unit. */
    private final class CombinedModuleButton extends ToolbarIconButton {
        private final int group;

        CombinedModuleButton(int group) {
            super(Ae2Icons.COG, combinedModuleMessage(group), button -> toggleCombinedModule(group));
            this.group = group;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partial) {
            boolean shown = dock.isGroupShown(group);
            setMessage(combinedModuleMessage(group));
            if (!visible) {
                return;
            }
            int yOffset = isHovered() ? 1 : 0;
            Icon bgIcon = isHovered()
                    ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_HOVER
                    : isFocused() ? Ae2Icons.TOOLBAR_BUTTON_BACKGROUND_FOCUS : Icon.TOOLBAR_BUTTON_BACKGROUND;
            bgIcon.getBlitter()
                    .dest(getX() - 1, getY() + yOffset, 18, 20)

                    .blit(guiGraphics);
            MestGuiIcons.blitCombined(guiGraphics, group, getX(), getY() + 1 + yOffset, 16, 16, shown ? 1.0F : 0.5F);
        }
    }

    private Component combinedModuleMessage(int group) {
        return Component.translatable(dock.isGroupShown(group)
                        ? "gui.mesplicedterminal.module_toggle.hide"
                        : "gui.mesplicedterminal.module_toggle.show")
                .append(": ")
                .append(Component.translatable("gui.mesplicedterminal.combined_module", group));
    }

    private void toggleCombinedModule(int group) {
        dock.toggleGroup(group);
        attachMeSideBar();
        attachExtraSlotColumns();
        syncToolkitOpen();
    }

    private void toggleModule(ModulePanel panel) {
        var policy = dock.policyFor(panel);
        if (policy.visible()) {
            // Hiding: no z-order change needed.
            dock.setModulePolicy(panel, policy.withVisible(false));
        } else {
            // Showing: make visible AND raise to the top layer. A hidden panel that was
            // stacked at the back of the root list would otherwise reappear behind the panels
            // that cover it, so the user could never click it.
            dock.revealModule(panel);
        }
        dock.save();
        attachMeSideBar();
        attachExtraSlotColumns();
        syncToolkitOpen();
    }

    private static Component moduleToggleMessage(ModulePanel panel, boolean visible) {
        return Component.translatable(visible
                        ? "gui.mesplicedterminal.module_toggle.hide"
                        : "gui.mesplicedterminal.module_toggle.show")
                .append(": ")
                .append(panel.title());
    }

    /**
     * Panel and attached-column slots are drawn in the owning root's ordered background composite.
     * The later vanilla/AE2 flat slot loop must skip them or it would redraw lower-window contents
     * above raised windows. Slots not owned by terminal chrome retain the base rendering path.
     */
    @Override
    public void renderSlot(GuiGraphics g, Slot s) {
        if (s.x <= -1000 || s.y <= -1000) {
            return;
        }
        if (!drawingRootSlots && (dock.ownsSlot(s) || extraColumnOwns(s))) {
            return;
        }
        renderSlotContents(g, s);
    }

    private void renderSlotContents(GuiGraphics g, Slot s) {
        if (s instanceof RepoSlot repoSlot) {
            if (meListPanel != null) {
                meListPanel.renderRepoSlot(g, this.font, repoSlot);
            }
            return;
        }
        if (toolkitPanel != null && toolkitPanel.ownsSlot(s) && toolkitPanel.hideLiveItem(s)) {
            toolkitPanel.renderMemoryGhost(g, s);
            return;
        }
        super.renderSlot(g, s);
        if (toolkitPanel != null && toolkitPanel.ownsSlot(s)) {
            toolkitPanel.renderMemoryGhost(g, s);
        }
        if (shouldShowCraftableIndicatorForSlot(s)) {
            g.pose().pushPose();
            try {
                g.pose().translate(0.0F, 0.0F, 100.0F);
                StackSizeRenderer.renderSizeLabel(g, font, s.x - 11, s.y - 11, "+", false);
            } finally {
                g.pose().popPose();
            }
        }
    }

    /**
     * Gate the vanilla hover highlight + hoveredSlot + findSlot (click hit-test) by panel z-order: a slot is
     * only "hovered" if it belongs to the topmost panel under the cursor. This single override kills cross-panel
     * highlight bleed and makes clicked/hovered slots respect z-order. It also makes JEI/EMI's
     * {@code getStackUnderMouse} (which uses hoveredSlot) z-order-correct.
     */
    @Override
    protected boolean isHovering(Slot slot, double mx, double my) {
        if (slot.x <= -1000 || slot.y <= -1000) {
            return false;
        }
        ensureHoverFrame(mx, my);
        if (hoverTop == null || !hoverFloating) {
            if (hoverExtra != null) {
                return hoverExtra.ownsSlot(slot) && super.isHovering(slot, mx, my);
            }
            if (hoverToolbar) {
                return false;
            }
        }
        if (hoverTop != null) {
            // Own-slot identity, not the projection cache: a floating panel can be visible and
            // hit-tested before panelForSlot() has a fresh mapping, which made encoding/crafting
            // ghost slots unclickable (no hover, no EMI/JEI drop, no place).
            return hoverTop.ownsSlot(slot) && super.isHovering(slot, mx, my);
        }
        return super.isHovering(slot, mx, my);
    }

    private Slot panelSlotAt(ModulePanel panel, double mouseX, double mouseY) {
        if (panel == null || dock.topLeafAt(mouseX, mouseY) != panel) {
            return null;
        }
        for (Slot slot : getMenu().slots) {
            if (panel.ownsSlot(slot) && slot.isActive()
                    && mouseX >= ModulePanel.slotScreenX(slot) && mouseX < ModulePanel.slotScreenX(slot) + 16
                    && mouseY >= ModulePanel.slotScreenY(slot) && mouseY < ModulePanel.slotScreenY(slot) + 16) {
                return slot;
            }
        }
        return null;
    }

    private boolean shouldShowCraftableIndicatorForSlot(Slot slot) {
        if (!getMenu().isPatternEncodingInputSlot(slot) || meListPanel == null) {
            return false;
        }
        GenericStack contents = GenericStack.fromItemStack(slot.getItem());
        return contents != null && meListPanel.repo().isCraftable(contents.what());
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        // The global toolbar is not owned by any floating root. Do not focus an arbitrary panel
        // underneath it when the toolbar happens to overlap the workspace.
        if (isMouseOverToolbarWidget(mx, my)) {
            if (meListPanel != null) {
                meListPanel.setSearchFocused(false);
            }
            if (button == 0 && scrollbarPressedAt(mx, my)) {
                return true;
            }
            if (mouseClickedSideBar(mx, my, button)) {
                return true;
            }
            if (mouseClickedExtraChrome(mx, my, button)) {
                return true;
            }
            boolean handled = super.mouseClicked(mx, my, button);
            return handled || extraColumnAt(mx, my) != null
                    || meSideBar.isMouseOver(mx, my)
                    || moreSettingsBar.isMouseOver(mx, my);
        }

        // Raise the pointed root before dispatching to panel-owned controls. A finally block commits
        // the raise only if dock chrome did not take ownership and merge it into a drag/resize.
        dock.beginPanelInteraction(mx, my);
        try {
            return mouseClickedInWorkspace(mx, my, button);
        } finally {
            dock.commitPanelInteraction();
        }
    }

    private boolean mouseClickedInWorkspace(double mx, double my, int button) {
        ModulePanel target = dock.topLeafAt(mx, my);
        if (target != hotkeysPanel && hotkeysPanel != null) {
            hotkeysPanel.cancelCapture();
        }

        if (target != providerSelectPanel && !dock.isJoinedWindow(providerSelectPanel)) {
            dismissProviderPicker(true);
        }
        if (target != wirelessSettingsPanel && !dock.isJoinedWindow(wirelessSettingsPanel)) {
            dismissWirelessSettingsPanel();
        }
        if (target != toolkitPanel) {
            dismissUnpinnedFloating(toolkitPanel);
        }

        if (target == craftingPanel || target == craftingTerminalPanel) {
            recipeTransfer.select(
                    getMenu(), MestRecipeTransferContext.Target.CRAFTING);
        } else if (target == patternEncodingPanel) {
            recipeTransfer.select(
                    getMenu(), MestRecipeTransferContext.Target.PATTERN_ENCODING);
        }

        if (button == 0 && scrollbarPressedAt(mx, my)) {
            return true;
        }
        if (target != null && !target.inResizeHandle(mx, my) && target.mouseClicked(mx, my, button)) {
            return true;
        }
        if (target != meListPanel && meListPanel != null) {
            meListPanel.setSearchFocused(false);
        }

        // AE2 handles the pick-item mouse binding specially so craftable entries open the crafting
        // amount screen even in survival mode, where vanilla's creative clone handling would not run.
        if (target == meListPanel && Minecraft.getInstance().options.keyPickItem.matchesMouse(button)) {
            Slot slot = meListPanel.repoSlotAt(mx, my);
            if (slot instanceof RepoSlot repoSlot && repoSlot.isCraftable()) {
                handleGridInventoryEntryMouseClick(repoSlot.getEntry(), button, ClickType.CLONE);
                return true;
            }
        }
        if (target == patternEncodingPanel
                && Minecraft.getInstance().options.keyPickItem.matchesMouse(button)) {
            Slot slot = panelSlotAt(patternEncodingPanel, mx, my);
            if (slot != null
                    && patternEncodingPanel.ownsSlot(slot)
                    && getMenu().canModifyAmountForPatternSlot(slot)) {
                GenericStack currentStack = GenericStack.fromItemStack(slot.getItem());
                if (currentStack != null) {
                    switchToScreen(new MestSetProcessingPatternAmountScreen(
                            this,
                            currentStack,
                            newStack -> NetworkHandler.instance().sendToServer(new InventoryActionPacket(
                                    InventoryAction.SET_FILTER,
                                    slot.index,
                                    GenericStack.wrapInItemStack(newStack)))));
                    return true;
                }
            }
        }

        if (dock.mouseClicked(mx, my, button)) {
            return true;
        }

        boolean handled = super.mouseClicked(mx, my, button);
        // Empty content in the topmost panel still blocks panels and widgets below it.
        return handled || dock.topPanelAt(mx, my) != null;
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int guiLeft, int guiTop, int button) {
        if (dock.topPanelAt(mx, my) != null || isMouseOverToolbarWidget(mx, my)) {
            return false;
        }
        return super.hasClickedOutside(mx, my, guiLeft, guiTop, button);
    }

    private void ensureHoverFrame(double mx, double my) {
        int ix = (int) mx;
        int iy = (int) my;
        if (hoverFrameReady && ix == hoverCacheX && iy == hoverCacheY) {
            return;
        }
        hoverCacheX = ix;
        hoverCacheY = iy;
        hoverTop = dock.topPanelAt(mx, my);
        hoverFloating = dock.isFloatingWindow(hoverTop);
        hoverExtra = extraColumnAt(mx, my);
        hoverToolbar = (hoverTop == null || !hoverFloating)
                && (meSideBar.isMouseOver(mx, my)
                        || moreSettingsBar.isMouseOver(mx, my)
                        || hoverExtra != null);
        hoverFrameReady = true;
    }

    private boolean isMouseOverToolbarWidget(double mx, double my) {
        ensureHoverFrame(mx, my);
        return hoverToolbar;
    }

    private boolean mouseClickedSideBar(double mx, double my, int button) {
        if (button == 0 && moreSettingsBar.isVisible()) {
            List<Button> buttons = moreSettingsBar.buttons();
            for (int index = 0; index < buttons.size(); index++) {
                Button widget = buttons.get(index);
                if (widget.visible && widget.isMouseOver(mx, my)) {
                    moreSettingsPressIndex = index;
                    moreSettingsPressY = my;
                    moreSettingsDragging = false;
                    return true;
                }
            }
        }
        if (cycleTerminalBtn != null && cycleTerminalBtn.visible && cyclePickerHit(mx, my)) {
            if (cycleTerminalBtn.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        for (Button widget : meSideBar.buttons()) {
            if (widget == cycleTerminalBtn || !widget.visible || !widget.isMouseOver(mx, my)) {
                continue;
            }
            if (widget.mouseClicked(mx, my, button)) {
                return true;
            }
        }
        return false;
    }

    private boolean cyclePickerHit(double mx, double my) {
        if (cycleTerminalBtn == null || !cycleTerminalBtn.visible) {
            return false;
        }
        if (cycleTerminalBtn.isMouseOver(mx, my)) {
            return true;
        }
        return cycleTerminalBtn instanceof ITooltip tooltip
                && tooltip.getTooltipArea().contains((int) mx, (int) my);
    }

    private void reorderMoreSettings(double mouseY) {
        List<Button> buttons = moreSettingsBar.buttons();
        if (moreSettingsPressIndex < 0 || moreSettingsPressIndex >= buttons.size()) {
            return;
        }
        int target = moreSettingsPressIndex;
        for (int index = 0; index < buttons.size(); index++) {
            Button widget = buttons.get(index);
            if (!widget.visible) {
                continue;
            }
            int mid = widget.getY() + widget.getHeight() / 2;
            if (mouseY < mid) {
                target = index;
                break;
            }
            target = index;
        }
        if (target == moreSettingsPressIndex) {
            return;
        }
        Button moved = buttons.remove(moreSettingsPressIndex);
        String id = moreSettingsButtonIds.remove(moreSettingsPressIndex);
        buttons.add(target, moved);
        moreSettingsButtonIds.add(target, id);
        moreSettingsPressIndex = target;
        attachMeSideBar();
    }

    private boolean mouseReleasedSideBar(double mx, double my, int button) {
        boolean handled = false;
        for (Button widget : moreSettingsBar.buttons()) {
            handled |= widget.mouseReleased(mx, my, button);
        }
        for (Button widget : meSideBar.buttons()) {
            handled |= widget.mouseReleased(mx, my, button);
        }
        return handled;
    }

    private boolean scrollbarPressedAt(double mx, double my) {
        ModulePanel target = dock.topLeafAt(mx, my);
        if (target != null && target.scrollbarPressed(mx, my)) {
            return true;
        }
        for (ModulePanel panel : dock.panels()) {
            if (panel != target && dock.isPointReachable(panel, mx, my) && panel.scrollbarPressed(mx, my)) {
                return true;
            }
        }
        return false;
    }

    private ExtraChrome extraColumnAt(double mx, double my) {
        List<ExtraChrome> columns = extraColumns();
        for (int i = columns.size() - 1; i >= 0; i--) {
            ExtraChrome column = columns.get(i);
            if (column.contains(mx, my)) {
                return column;
            }
        }
        return null;
    }

    private boolean extraColumnOwns(Slot slot) {
        for (ExtraChrome column : extraColumns()) {
            if (column.ownsSlot(slot)) {
                return true;
            }
        }
        return false;
    }

    private boolean mouseClickedExtraChrome(double mx, double my, int button) {
        ExtraChrome column = extraColumnAt(mx, my);
        return column != null && column.mouseClicked(mx, my, button);
    }

    private List<ExtraChrome> extraColumns() {
        List<ExtraChrome> columns = new ArrayList<>(2);
        if (upgradeColumn != null) {
            columns.add(upgradeColumn);
        }
        if (viewCellColumn != null) {
            columns.add(viewCellColumn);
        }
        return columns;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dragX, double dragY) {
        if (button == 0 && moreSettingsPressIndex >= 0) {
            if (!moreSettingsDragging && Math.abs(my - moreSettingsPressY) > 4) {
                moreSettingsDragging = true;
            }
            if (moreSettingsDragging) {
                reorderMoreSettings(my);
                return true;
            }
        }
        for (ExtraChrome column : extraColumns()) {
            if (column.mouseDragged(mx, my)) {
                return true;
            }
        }
        for (ModulePanel panel : dock.panels()) {
            if (panel.scrollbarDragged(mx, my)) {
                return true;
            }
        }
        if (dock.mouseDragged(mx, my, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0 && moreSettingsPressIndex >= 0) {
            boolean dragged = moreSettingsDragging;
            int index = moreSettingsPressIndex;
            moreSettingsPressIndex = -1;
            moreSettingsDragging = false;
            if (dragged) {
                dock.setMoreSettingsOrder(List.copyOf(moreSettingsButtonIds));
                attachMeSideBar();
                return true;
            }
            List<Button> buttons = moreSettingsBar.buttons();
            if (index >= 0 && index < buttons.size()) {
                Button widget = buttons.get(index);
                widget.mouseClicked(mx, my, button);
                widget.mouseReleased(mx, my, button);
            }
            return true;
        }
        boolean scrollbarDrag = false;
        for (ModulePanel panel : dock.panels()) {
            scrollbarDrag |= panel.scrollbarDragging();
            panel.scrollbarReleased();
        }
        boolean extraDrag = false;
        for (ExtraChrome column : extraColumns()) {
            extraDrag |= column.mouseReleased(mx, my, button);
        }
        mouseReleasedSideBar(mx, my, button);
        if (scrollbarDrag || extraDrag) {
            return true;
        }
        boolean handled = dock.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
        syncResizeCursor(mx, my);
        return handled;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollY) {
        ExtraChrome extra = extraColumnAt(mx, my);
        if (extra != null && extra.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
        // These three also scroll from their edge scrollers just outside the leaf, but only when no
        // other window covers the pointer; a raised panel's wheel must not reach the one beneath.
        if (patternAccessPanel != null && dock.isPointReachable(patternAccessPanel, mx, my)
                && patternAccessPanel.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
        if (patternCachePanel != null && dock.isPointReachable(patternCachePanel, mx, my)
                && patternCachePanel.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
        if (providerSelectPanel != null && dock.isPointReachable(providerSelectPanel, mx, my)
                && providerSelectPanel.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
        ModulePanel target = dock.topLeafAt(mx, my);
        // Shift+scroll over an ME entry rolls single items in/out of the network instead of
        // scrolling the grid; this screen-level interaction runs before the panel's own scroll.
        if (target == meListPanel && meListPanel.inGrid(mx, my) && scrollY != 0
                && hasShiftDown() && meListPanel.repoSlotAt(mx, my) != null) {
            RepoSlot repoSlot = meListPanel.repoSlotAt(mx, my);
            GridInventoryEntry entry = repoSlot.getEntry();
            long serial = entry != null ? entry.getSerial() : -1;
            InventoryAction action = toInventoryAction(MEListInteractionPolicy.scrollAction(scrollY));
            for (int i = 0; i < MEListInteractionPolicy.scrollSteps(scrollY); i++) {
                getMenu().handleInteraction(serial, action);
            }
            return true;
        }
        if (target != null && target != patternAccessPanel && target != patternCachePanel
                && target != providerSelectPanel
                && target.mouseScrolled(mx, my, scrollY)) {
            return true;
        }
        return dock.topPanelAt(mx, my) != null || super.mouseScrolled(mx, my, scrollY);
    }

    public void updatePatternProviders(List<Entry> entries) {
        if (patternAccessPanel != null) {
            patternAccessPanel.setProviders(entries);
        }
    }

    public void applyProviderPickerList(ProviderPickerListPacket packet) {
        if (providerSelectPanel == null) {
            return;
        }
        boolean floating = dock.isFloatingWindow(providerSelectPanel);
        if (packet.applyPreset() && !floating) {
            keepPendingOnRemove = true;
            minecraft.setScreen(PlusScreenSupport.presetPicker(this, packet));
            return;
        }
        boolean autoUploaded = providerSelectPanel.applyList(packet);
        if (packet.applyPreset() && floating && !autoUploaded) {
            dock.revealModule(providerSelectPanel);
        } else if (autoUploaded) {
            dismissProviderPicker(false);
        }
    }

    public void dismissProviderPicker(boolean cancelPending) {
        if (providerSelectPanel == null) {
            return;
        }
        if (!dock.isFloatingUnpinned(providerSelectPanel)) {
            return;
        }
        if (!dock.isEffectivelyVisible(providerSelectPanel)) {
            return;
        }
        dock.hideModule(providerSelectPanel);
        if (cancelPending && PlusScreenSupport.loaded()) {
            PlusScreenSupport.cancelPending();
        }
    }

    /**
     * Route clicks on virtual ME slots to the network interaction handler instead of the vanilla
     * slot-click path (RepoSlots are read-only client slots backed by the ME network).
     */
    @Override
    protected void slotClicked(Slot slot, int slotIdx, int mouseButton, ClickType clickType) {
        if (slot instanceof RepoSlot repoSlot) {
            handleGridInventoryEntryMouseClick(repoSlot.getEntry(), mouseButton, clickType);
            return;
        }
        if (clickType == ClickType.QUICK_MOVE
                && patternAccessReceivesShiftClick()
                && slot != null
                && PatternDetailsHelper.isEncodedPattern(slot.getItem())) {
            Entry target = patternAccessPanel.firstDisplayedProvider();
            if (target != null) {
                MestPackets.sendToServer(new PatternProviderActionPacket(
                        getMenu().containerId,
                        target.epoch(),
                        target.providerId(),
                        target.revision(),
                        slot.index,
                        PatternProviderActionPacket.Action.INSERT_INTO_PROVIDER));
                return;
            }
        }
        super.slotClicked(slot, slotIdx, mouseButton, clickType);
    }

    private boolean patternAccessReceivesShiftClick() {
        return patternAccessPanel != null && patternAccessPanel.visible
                && (meListPanel == null || !meListPanel.visible);
    }

    private void handleGridInventoryEntryMouseClick(GridInventoryEntry entry, int mouseButton, ClickType clickType) {
        var menu = getMenu();

        // Preserve AE2's container strategy path for fluids and other keys backed by a portable
        // container. It takes precedence over ordinary item extraction/insertion.
        if (mouseButton == 0 && entry != null && ContainerItemStrategies.isKeySupported(entry.getWhat())) {
            InventoryAction action = toInventoryAction(MEListInteractionPolicy.fillContainerAction(
                    clickType == ClickType.QUICK_MOVE, menu.getCarried().isEmpty()));
            menu.handleInteraction(entry.getSerial(), action);
            return;
        }

        if (mouseButton == 1 && !menu.getCarried().isEmpty()) {
            var emptyingAction = ContainerItemStrategies.getEmptyingAction(menu.getCarried());
            if (emptyingAction != null && menu.isKeyVisible(emptyingAction.what())) {
                menu.handleInteraction(-1,
                        toInventoryAction(MEListInteractionPolicy.emptyContainerAction(
                                clickType == ClickType.QUICK_MOVE)));
                return;
            }
        }

        if (entry == null) {
            // Putting a held item down into the network (click on empty virtual slot).
            if (clickType == ClickType.PICKUP && !menu.getCarried().isEmpty()) {
                menu.handleInteraction(-1, mouseButton == 1
                        ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                        : InventoryAction.PICKUP_OR_SET_DOWN);
            }
            return;
        }

        long serial = entry.getSerial();

        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_SPACE)) {
            menu.handleInteraction(serial, InventoryAction.MOVE_REGION);
            return;
        }

        InventoryAction action = null;
        switch (clickType) {
            case PICKUP -> {
                action = mouseButton == 1
                        ? InventoryAction.SPLIT_OR_PLACE_SINGLE
                        : InventoryAction.PICKUP_OR_SET_DOWN;
                if (action == InventoryAction.PICKUP_OR_SET_DOWN
                        && MEListInteractionPolicy.shouldCraftOnClick(
                                meListPanel.isViewOnlyCraftable(), entry.getStoredAmount(), entry.isCraftable())
                        && menu.getCarried().isEmpty()) {
                    menu.handleInteraction(serial, InventoryAction.AUTO_CRAFT);
                    return;
                }
            }
            case QUICK_MOVE -> action = mouseButton == 1
                    ? InventoryAction.PICKUP_SINGLE
                    : InventoryAction.SHIFT_CLICK;
            case CLONE -> {
                if (entry.isCraftable()) {
                    menu.handleInteraction(serial, InventoryAction.AUTO_CRAFT);
                    return;
                } else if (menu.getPlayer().getAbilities().instabuild) {
                    action = InventoryAction.CREATIVE_DUPLICATE;
                }
            }
            default -> {
            }
        }
        if (action != null) {
            menu.handleInteraction(serial, action);
        }
    }

    private static InventoryAction toInventoryAction(MEListInteractionPolicy.ContainerAction action) {
        return switch (action) {
            case FILL_ONE -> InventoryAction.FILL_ITEM;
            // AE2 15 only moves one container-load per click.
            case FILL_ALL, FILL_ALL_TO_PLAYER -> InventoryAction.FILL_ITEM;
            case EMPTY_ONE, EMPTY_ALL -> InventoryAction.EMPTY_ITEM;
        };
    }

    private static InventoryAction toInventoryAction(MEListInteractionPolicy.ScrollAction action) {
        return switch (action) {
            case INSERT_ONE -> InventoryAction.ROLL_DOWN;
            case EXTRACT_ONE -> InventoryAction.ROLL_UP;
        };
    }

    @Override
    protected EmptyingAction getEmptyingAction(Slot slot, ItemStack carried) {
        if (getMenu().isProcessingPatternSlot(slot)) {
            EmptyingAction action = ContainerItemStrategies.getEmptyingAction(carried);
            if (action != null) {
                return action;
            }
        }
        return super.getEmptyingAction(slot, carried);
    }

    @Override
    protected List<Component> getTooltipFromContainerItem(ItemStack stack) {
        if (toolkitPanel != null && toolkitPanel.isLockEditing() && hoveredSlot instanceof ToolkitSlot) {
            return List.of();
        }
        List<Component> lines = super.getTooltipFromContainerItem(stack);
        if (hoveredSlot != null && shouldShowCraftableIndicatorForSlot(hoveredSlot)) {
            lines = new ArrayList<>(lines);
            lines.add(ButtonToolTips.Craftable.text().withStyle(ChatFormatting.DARK_GRAY));
        }
        return lines;
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int x, int y) {
        g.pose().pushPose();
        try {
            g.pose().translate(0.0F, 0.0F, 4500.0F);
            renderTooltipAtOverlay(g, x, y);
        } finally {
            g.pose().popPose();
        }
    }

    private void renderTooltipAtOverlay(GuiGraphics g, int x, int y) {
        ModulePanel target = dock.topLeafAt(x, y);
        if (toolkitPanel != null && toolkitPanel.isLockEditing()
                && (target == toolkitPanel || hoveredSlot instanceof ToolkitSlot)) {
            ITooltip lockTip = toolkitPanel.hoveredTooltip(x, y);
            if (lockTip != null && lockTip.isTooltipAreaVisible() && !lockTip.getTooltipMessage().isEmpty()) {
                drawAeWidgetTooltip(g, x, y, lockTip);
            }
            return;
        }
        if (target == hotkeysPanel && hotkeysPanel != null) {
            ITooltip hotkeyTip = hotkeysPanel.hoveredTooltip(x, y);
            if (hotkeyTip != null) {
                drawAeWidgetTooltip(g, x, y, hotkeyTip);
                return;
            }
        }
        if (target == toolkitPanel && toolkitPanel != null) {
            ITooltip lockTip = toolkitPanel.hoveredTooltip(x, y);
            if (lockTip != null && lockTip.isTooltipAreaVisible() && !lockTip.getTooltipMessage().isEmpty()) {
                drawAeWidgetTooltip(g, x, y, lockTip);
                return;
            }
        }
        if (target == providerSelectPanel && providerSelectPanel != null) {
            ITooltip picker = providerSelectPanel.hoveredTooltip(x, y);
            if (picker != null && picker.isTooltipAreaVisible() && !picker.getTooltipMessage().isEmpty()) {
                drawAeWidgetTooltip(g, x, y, picker);
                return;
            }
        }
        if (target == patternEncodingPanel && patternEncodingPanel != null) {
            ITooltip encoding = patternEncodingPanel.hoveredTooltip(x, y);
            if (encoding != null && encoding.isTooltipAreaVisible() && !encoding.getTooltipMessage().isEmpty()) {
                drawAeWidgetTooltip(g, x, y, encoding);
                return;
            }
        }
        if (target == patternEncodingPanel && getMenu().getCarried().isEmpty()) {
            Slot slot = panelSlotAt(patternEncodingPanel, x, y);
            if (slot != null
                    && patternEncodingPanel.ownsSlot(slot)
                    && getMenu().canModifyAmountForPatternSlot(slot)) {
                GenericStack stack = GenericStack.fromItemStack(slot.getItem());
                if (stack != null) {
                    var lines = new ArrayList<>(getTooltipFromContainerItem(slot.getItem()));
                    lines.add(Tooltips.getAmountTooltip(ButtonToolTips.Amount, stack));
                    lines.add(Tooltips.getSetAmountTooltip());
                    drawTooltip(g, x, y, lines);
                    return;
                }
            }
        }

        if (target == meListPanel && !getMenu().getCarried().isEmpty()
                && meListPanel.repoSlotAt(x, y) instanceof RepoSlot) {
            ItemStack carried = getMenu().getCarried();
            EmptyingAction action = ContainerItemStrategies.getEmptyingAction(carried);
            if (action != null && getMenu().isKeyVisible(action.what())) {
                drawTooltip(g, x, y,
                        Tooltips.getEmptyingTooltip(ButtonToolTips.StoreAction, carried, action));
                return;
            }
        }

        // ME entry tooltip only when the ME list panel is the topmost panel at the cursor — otherwise a
        // panel stacked above the ME list would still surface the underlying ME item's tooltip (bug 5).
        if (meListPanel != null && dock.isEffectivelyVisible(meListPanel) && getMenu().getCarried().isEmpty()
                && target == meListPanel) {
            GridInventoryEntry entry = meListPanel.entryAt(x, y);
            if (entry != null) {
                List<Component> lines = AEKeyRendering.getTooltip(entry.getWhat());
                if (Tooltips.shouldShowAmountTooltip(entry.getWhat(), entry.getStoredAmount())) {
                    var exact = meListPanel.gtlDisplay().amountTooltip(
                            ButtonToolTips.StoredAmount, entry.getWhat(), entry.getStoredAmount());
                    lines.add(exact != null ? exact : Tooltips.getAmountTooltip(
                            ButtonToolTips.StoredAmount, entry.getWhat(), entry.getStoredAmount()));
                }
                if (entry.getRequestableAmount() > 0) {
                    String amount = entry.getWhat().formatAmount(entry.getRequestableAmount(), AmountFormat.FULL);
                    lines.add(ButtonToolTips.RequestableAmount.text(amount));
                }
                if (entry.isCraftable()
                        && !(meListPanel.isViewOnlyCraftable() || entry.getStoredAmount() <= 0)) {
                    lines.add(ButtonToolTips.Craftable.text().withStyle(ChatFormatting.DARK_GRAY));
                }
                if (Minecraft.getInstance().options.advancedItemTooltips) {
                    lines.add(ButtonToolTips.Serial.text(entry.getSerial()).withStyle(ChatFormatting.DARK_GRAY));
                }
                if (entry.getWhat() instanceof AEItemKey itemKey) {
                    var stack = itemKey.getReadOnlyStack();
                    g.renderTooltip(font, lines, stack.getTooltipImage(), stack, x, y);
                } else {
                    g.renderComponentTooltip(font, lines, x, y);
                }
                return;
            }
        }
        super.renderTooltip(g, x, y);
    }

    public void attachPatternSearchField(AETextField field) {
        if (field != null && !children().contains(field)) {
            addWidget(field);
        }
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (meListPanel != null && meListPanel.searchCharTyped(character, modifiers)) {
            return true;
        }
        if (patternAccessPanel != null && getFocused() == patternAccessPanel.searchField()) {
            return super.charTyped(character, modifiers);
        }
        if (patternAccessPanel != null && patternAccessPanel.searchCharTyped(character, modifiers)) {
            return true;
        }
        if (providerSelectPanel != null && getFocused() == providerSelectPanel.searchField()) {
            return super.charTyped(character, modifiers);
        }
        if (providerSelectPanel != null && getFocused() == providerSelectPanel.mappingField()) {
            return super.charTyped(character, modifiers);
        }
        if (providerSelectPanel != null && providerSelectPanel.searchCharTyped(character, modifiers)) {
            return true;
        }
        return super.charTyped(character, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hotkeysPanel != null && hotkeysPanel.isCapturing()) {
            return hotkeysPanel.captureKey(keyCode, modifiers);
        }
        boolean meSearch = meListPanel != null && meListPanel.isSearchFocused();
        boolean patSearch = patternAccessPanel != null
                && (getFocused() == patternAccessPanel.searchField() || patternAccessPanel.isSearchFocused());
        boolean pickerSearch = providerSelectPanel != null
                && (getFocused() == providerSelectPanel.searchField()
                || getFocused() == providerSelectPanel.mappingField()
                || providerSelectPanel.isSearchFocused());
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && (meSearch || patSearch || pickerSearch)) {
            if (meSearch) {
                meListPanel.setSearchFocused(false);
            }
            if (patSearch) {
                patternAccessPanel.setSearchFocused(false);
            }
            if (pickerSearch) {
                providerSelectPanel.setSearchFocused(false);
            }
            setFocused(null);
        } else if (meSearch) {
            if (meListPanel.searchKeyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                meListPanel.setSearchFocused(false);
                return true;
            }
            return true;
        } else if (patSearch) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                patternAccessPanel.setSearchFocused(false);
                setFocused(null);
                return true;
            }
            if (getFocused() == patternAccessPanel.searchField()) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            if (patternAccessPanel.searchKeyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        } else if (pickerSearch) {
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                if (providerSelectPanel.searchKeyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
                providerSelectPanel.setSearchFocused(false);
                setFocused(null);
                return true;
            }
            if (getFocused() == providerSelectPanel.searchField()
                    || getFocused() == providerSelectPanel.mappingField()) {
                return super.keyPressed(keyCode, scanCode, modifiers);
            }
            if (providerSelectPanel.searchKeyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            return true;
        }
        if (PlusScreenSupport.loaded()
                && ModList.get().isLoaded("jei")
                && PlusScreenSupport.fillSearchFromHoveredIngredient(meListPanel, keyCode, scanCode)) {
            return true;
        }
        if (patternAccessPanel != null
                && minecraft.options.keyDrop.matches(keyCode, scanCode)
                && patternAccessPanel.dropHovered(
                        minecraft.mouseHandler.xpos()
                                * this.width / Math.max(1, minecraft.getWindow().getScreenWidth()),
                        minecraft.mouseHandler.ypos()
                                * this.height / Math.max(1, minecraft.getWindow().getScreenHeight()),
                        hasControlDown())) {
            return true;
        }
        String hotkey = PanelHotkeys.match(keyCode, modifiers);
        if (hotkey != null && runHotkey(hotkey)) {
            return true;
        }
        boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
        if (!handled) {
            return checkForTerminalKeys(keyCode, scanCode);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        super.mouseMoved(mx, my);
        if (patternAccessPanel != null) {
            patternAccessPanel.shiftHoverExtract(mx, my);
        }
        syncResizeCursor(mx, my);
    }

    private void syncResizeCursor(double mx, double my) {
        CursorHelper.apply(dock.pointerCursor(mx, my));
    }

    public WTMenuHost getHost() {
        return (WTMenuHost) getMenu().getHost();
    }

    /** AE2WTLib 1.21's terminal-hotkey dispatch; AE2WTLib 15 leaves it to each screen. */
    private boolean checkForTerminalKeys(int keyCode, int scanCode) {
        var closingHotkey = Hotkeys.getHotkeyMapping(getHost().getCloseHotkey());
        if (closingHotkey != null && closingHotkey.mapping().matches(keyCode, scanCode)) {
            getHost().getPlayer().closeContainer();
            return true;
        }
        for (String terminal : WUTHandler.terminalNames) {
            String hotkeyId = "crafting".equals(terminal)
                    ? HotkeyAction.WIRELESS_TERMINAL
                    : "wireless_" + terminal + "_terminal";
            var hotkey = Hotkeys.getHotkeyMapping(hotkeyId);
            if (hotkey != null && hotkey.mapping().matches(keyCode, scanCode)) {
                NetworkHandler.instance().sendToServer(new HotkeyPacket(hotkey));
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isHandlingRightClick() {
        // AEBaseScreen temporarily sets this flag while it forwards a right click to a button as a
        // left click. SettingToggleButton reads it to decide whether to cycle backwards.
        return super.isHandlingRightClick();
    }

    public void saveUiPreferences() {
        dock.saveUiPreferences();
    }

    @Override
    public void storeState() {
        dock.save();
        dock.saveUiPreferences();
        if (meListPanel != null) {
            // Mirrors AE2's MEStorageScreen.storeState(): carry the search to the next screen.
            meListPanel.storeSearch();
        }
    }

    void closePatternAccessSubscription() {
        if (patternAccessPanel != null) {
            patternAccessPanel.closeSubscription();
        }
    }

    private void clearRecipeTransferContext() {
        recipeTransfer.clear(getMenu());
    }

    @Override
    public void onClose() {
        if (AEConfig.instance().isClearGridOnClose()) {
            getMenu().clearCraftingGrid();
            getMenu().clearPatternEncoding();
        }
        clearRecipeTransferContext();
        CursorHelper.resetCursor();
        super.onClose();
    }

    @Override
    public void removed() {
        if (meListPanel != null) {
            // AE2 MEStorageScreen.removed() calls storeState() so the next constructor
            // (craft-amount return, terminal reopen) sees the search that was on screen,
            // not the value from the previous close.
            meListPanel.storeSearch();
            meListPanel.markFinishedCraftingPinsPrunable();
        }
        MestPinnedKeysCap.resetToVanilla();
        closePatternAccessSubscription();
        if (PlusScreenSupport.loaded() && !keepPendingOnRemove) {
            PlusScreenSupport.cancelPending();
        }
        keepPendingOnRemove = false;
        dock.finishPendingGesture();
        dock.save();
        dock.saveUiPreferences();
        CursorHelper.resetCursor();
        super.removed();
    }
}
