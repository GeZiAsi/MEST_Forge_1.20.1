package com.lhy.mest.client;

import net.minecraft.network.chat.Component;

import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;

import com.lhy.mest.client.Ae2Icons;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.integration.abstraction.ItemListMod;
import appeng.menu.SlotSemantics;

import com.lhy.mest.terminal.MESTMenu;

/**
 * AE2 terminal-settings sub-screen hosted from the spliced terminal. The stock class is bound to
 * {@code MEStorageScreen}, so this copy keeps the same JSON/widgets while parenting {@link MESTScreen}.
 */
public class MestTerminalSettingsScreen extends AESubScreen<MESTMenu, MESTScreen> {
    private final AECheckbox pinAutoCraftedItemsCheckbox;
    private final AECheckbox notifyForFinishedCraftingJobsCheckbox;
    private final AECheckbox clearGridOnCloseCheckbox;
    private final AECheckbox useInternalSearchRadio;
    private final AECheckbox useExternalSearchRadio;
    private final AECheckbox rememberCheckbox;
    private final AECheckbox autoFocusCheckbox;
    private final AECheckbox syncWithExternalCheckbox;
    private final AECheckbox clearExternalCheckbox;

    public MestTerminalSettingsScreen(MESTScreen parent) {
        super(parent, "/screens/terminals/terminal_settings.json");

        addBackButton();

        Component externalSearchMod;
        boolean hasExternalSearch;
        if (ItemListMod.isEnabled()) {
            externalSearchMod = Component.literal(ItemListMod.getShortName());
            hasExternalSearch = true;
        } else {
            externalSearchMod = Component.literal("REI/EMI");
            hasExternalSearch = false;
        }

        pinAutoCraftedItemsCheckbox = widgets.addCheckbox("pinAutoCraftedItemsCheckbox",
                GuiText.TerminalSettingsPinAutoCraftedItems.text(), this::save);
        notifyForFinishedCraftingJobsCheckbox = widgets.addCheckbox("notifyForFinishedCraftingJobsCheckbox",
                GuiText.TerminalSettingsNotifyForFinishedJobs.text(), this::save);
        clearGridOnCloseCheckbox = widgets.addCheckbox("clearGridOnCloseCheckbox",
                GuiText.TerminalSettingsClearGridOnClose.text(), this::save);

        useInternalSearchRadio = widgets.addCheckbox("useInternalSearchRadio",
                GuiText.SearchSettingsUseInternalSearch.text(), this::switchToAeSearch);
        useInternalSearchRadio.setRadio(true);
        useExternalSearchRadio = widgets.addCheckbox("useExternalSearchRadio",
                GuiText.SearchSettingsUseExternalSearch.text(externalSearchMod), this::switchToExternalSearch);
        useExternalSearchRadio.setRadio(true);
        useExternalSearchRadio.active = hasExternalSearch;

        rememberCheckbox = widgets.addCheckbox("rememberCheckbox", GuiText.SearchSettingsRememberSearch.text(),
                this::save);
        autoFocusCheckbox = widgets.addCheckbox("autoFocusCheckbox", GuiText.SearchSettingsAutoFocus.text(),
                this::save);
        syncWithExternalCheckbox = widgets.addCheckbox("syncWithExternalCheckbox",
                GuiText.SearchSettingsSyncWithExternal.text(externalSearchMod), this::save);
        clearExternalCheckbox = widgets.addCheckbox("clearExternalCheckbox",
                GuiText.SearchSettingsClearExternal.text(externalSearchMod), this::save);

        updateState();
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private void switchToAeSearch() {
        useInternalSearchRadio.setSelected(true);
        useExternalSearchRadio.setSelected(false);
        save();
    }

    private void switchToExternalSearch() {
        useInternalSearchRadio.setSelected(false);
        useExternalSearchRadio.setSelected(true);
        save();
    }

    private void addBackButton() {
        var label = menu.getHost().getMainMenuIcon().getHoverName();
        TabButton button = new TabButton(Ae2Icons.BACK, label, btn -> returnToParent());
        widgets.add("back", button);
    }

    private void updateState() {
        pinAutoCraftedItemsCheckbox.setSelected(config.isPinAutoCraftedItems());
        notifyForFinishedCraftingJobsCheckbox.setSelected(config.isNotifyForFinishedCraftingJobs());
        clearGridOnCloseCheckbox.setSelected(config.isClearGridOnClose());

        useInternalSearchRadio.setSelected(!config.isUseExternalSearch());
        useExternalSearchRadio.setSelected(config.isUseExternalSearch());
        rememberCheckbox.setSelected(config.isRememberLastSearch());
        autoFocusCheckbox.setSelected(config.isAutoFocusSearch());
        syncWithExternalCheckbox.setSelected(config.isSyncWithExternalSearch());
        clearExternalCheckbox.setSelected(config.isClearExternalSearchOnOpen());

        rememberCheckbox.visible = useInternalSearchRadio.isSelected();
        autoFocusCheckbox.visible = useInternalSearchRadio.isSelected();
        syncWithExternalCheckbox.visible = useInternalSearchRadio.isSelected();
        clearExternalCheckbox.visible = useExternalSearchRadio.isSelected();
    }

    private void save() {
        config.setUseExternalSearch(useExternalSearchRadio.isSelected());
        config.setRememberLastSearch(rememberCheckbox.isSelected());
        config.setAutoFocusSearch(autoFocusCheckbox.isSelected());
        config.setSyncWithExternalSearch(syncWithExternalCheckbox.isSelected());
        config.setClearExternalSearchOnOpen(clearExternalCheckbox.isSelected());
        config.setPinAutoCraftedItems(pinAutoCraftedItemsCheckbox.isSelected());
        config.setNotifyForFinishedCraftingJobs(notifyForFinishedCraftingJobsCheckbox.isSelected());
        config.setClearGridOnClose(clearGridOnCloseCheckbox.isSelected());
        updateState();
    }
}