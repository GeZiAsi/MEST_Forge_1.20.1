package com.lhy.mest.client;

import java.util.function.Consumer;

import com.google.common.primitives.Longs;

import appeng.api.stacks.GenericStack;
import appeng.client.gui.AESubScreen;
import appeng.client.gui.Icon;

import com.lhy.mest.client.Ae2Icons;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.me.common.ClientDisplaySlot;
import appeng.client.gui.widgets.NumberEntryWidget;
import appeng.client.gui.widgets.TabButton;
import appeng.core.localization.GuiText;
import appeng.menu.SlotSemantics;

import com.lhy.mest.terminal.MESTMenu;

/** AE2-native amount editor adapted to the combined terminal menu. */
public final class MestSetProcessingPatternAmountScreen
        extends AESubScreen<MESTMenu, MESTScreen> {
    private final NumberEntryWidget amount;
    private final GenericStack currentStack;
    private final Consumer<GenericStack> setter;

    public MestSetProcessingPatternAmountScreen(
            MESTScreen parent,
            GenericStack currentStack,
            Consumer<GenericStack> setter) {
        super(parent, "/screens/set_processing_pattern_amount.json");
        this.currentStack = currentStack;
        this.setter = setter;

        widgets.addButton("save", GuiText.Set.text(), this::confirm);

        var icon = getMenu().getHost().getMainMenuIcon();
        widgets.add("back", new TabButton(Ae2Icons.BACK, icon.getHoverName(), button -> returnToParent()));

        this.amount = widgets.addNumberEntryWidget("amountToStock", NumberEntryType.of(currentStack.what()));
        this.amount.setLongValue(currentStack.amount());
        this.amount.setMaxValue(getMaxAmount());
        this.amount.setTextFieldStyle(style.getWidget("amountToStockInput"));
        this.amount.setMinValue(0);
        this.amount.setHideValidationIcon(true);
        this.amount.setOnConfirm(this::confirm);

        addClientSideSlot(new ClientDisplaySlot(currentStack), SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    protected void init() {
        super.init();
        setSlotsHidden(SlotSemantics.TOOLBOX, true);
    }

    private void confirm() {
        amount.getLongValue().ifPresent(newAmount -> {
            newAmount = Longs.constrainToRange(newAmount, 0, getMaxAmount());
            setter.accept(newAmount <= 0 ? null : new GenericStack(currentStack.what(), newAmount));
            returnToParent();
        });
    }

    private long getMaxAmount() {
        return 999999L * currentStack.what().getAmountPerUnit();
    }

    @Override
    public void removed() {
        getParent().closePatternAccessSubscription();
        super.removed();
    }
}
