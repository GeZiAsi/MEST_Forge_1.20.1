package com.lhy.mest.terminal;

import com.lhy.mest.util.MestLists;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import appeng.api.storage.StorageHelper;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.MEStorage;
import appeng.helpers.InventoryAction;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AECraftingPattern;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.menu.MenuOpener;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.me.items.CraftingTermMenu;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.PatternTermSlot;
import appeng.items.contents.NetworkToolMenuHost;
import appeng.items.tools.NetworkToolItem;
import appeng.menu.slot.RestrictedInputSlot;
import appeng.parts.encoding.EncodingMode;
import appeng.parts.encoding.PatternEncodingLogic;
import appeng.util.ConfigInventory;

import de.mari_023.ae2wtlib.AE2wtlibSlotSemantics;
import de.mari_023.ae2wtlib.terminal.ArmorSlot;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import de.mari_023.ae2wtlib.wut.ItemWUT;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetHandler;
import de.mari_023.ae2wtlib.wct.magnet_card.MagnetMode;

import com.lhy.mest.MESplicedterminal;
import com.lhy.mest.compat.MestCraftingPatternAutoUpload;
import com.lhy.mest.compat.plus.PlusEncodingUpload;
import com.lhy.mest.network.PatternAccessSession;
import com.lhy.mest.network.PatternCacheActionPacket;
import com.lhy.mest.module.MestModuleSlots;

/**
 * Container for the ME Spliced Terminal. Extends AE2's {@link CraftingTermMenu} (which itself extends
 * MEStorageMenu), so it provides both the incremental ME item list and a 3x3 crafting matrix with a
 * result slot. The dockable module layout placing these into floating panels is a client-side concern.
 */
public class MESTMenu extends CraftingTermMenu {
    public static final ResourceLocation ID = new ResourceLocation(MESplicedterminal.MODID, "spliced_terminal");
    public static final MenuType<MESTMenu> TYPE = MestMenuTypes.create(ID, MESTMenu::new, MESTMenuHost.class);

    private static final int CRAFTING_GRID_WIDTH = 3;
    private static final int CRAFTING_GRID_HEIGHT = 3;
    private static final int CRAFTING_GRID_SLOTS = CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT;

    private static final String ACTION_SET_PATTERN_MODE = "mestSetPatternMode";
    private static final String ACTION_ENCODE_PATTERN = "mestEncodePattern";
    private static final String ACTION_CLEAR_PATTERN = "mestClearPattern";
    private static final String ACTION_SET_PATTERN_SUBSTITUTION = "mestSetPatternSubstitution";
    private static final String ACTION_SET_PATTERN_FLUID_SUBSTITUTION = "mestSetPatternFluidSubstitution";
    private static final String ACTION_SET_STONECUTTING_RECIPE_ID = "mestSetStonecuttingRecipeId";
    private static final String ACTION_CYCLE_PROCESSING_OUTPUT = "mestCycleProcessingOutput";
    private static final String ACTION_SCALE_ENCODING = "mestScaleEncoding";
    private static final String ACTION_UPLOAD_PATTERN = "mestUploadPattern";
    private static final String ACTION_REQUEST_PROVIDERS = "mestRequestProviders";
    private static final String ACTION_MAGNET_MENU = "magnetMenu";
    private static final String ACTION_TRASH_MENU = "trash";
    private static final String ACTION_CLOSE_TRASH = "closeTrash";
    private static final String ACTION_SET_TOOLKIT_OPEN = "mestSetToolkitOpen";
    private static final String ACTION_SET_USELESS_PATTERN = "mestSetUselessPattern";

    private final MESTMenuHost host;
    private final PatternAccessSession patternAccessSession;
    private final PatternEncodingLogic patternEncodingLogic;
    private boolean blankPatternFilled;
    private final FakeSlot[] patternCraftingSlots = new FakeSlot[CRAFTING_GRID_SLOTS];
    private final FakeSlot[] processingInputSlots = new FakeSlot[AEProcessingPattern.MAX_INPUT_SLOTS];
    private final FakeSlot[] processingOutputSlots = new FakeSlot[AEProcessingPattern.MAX_OUTPUT_SLOTS];
    private FakeSlot stonecuttingInputSlot;
    private FakeSlot smithingTableTemplateSlot;
    private FakeSlot smithingTableBaseSlot;
    private FakeSlot smithingTableAdditionSlot;
    private PatternTermSlot patternCraftOutputSlot;
    private RestrictedInputSlot blankPatternSlot;
    private RestrictedInputSlot encodedPatternSlot;
    private final ConfigInventory encodedInputsInv;
    private final ConfigInventory encodedOutputsInv;
    private final List<StonecutterRecipe> stonecuttingRecipes = new ArrayList<>();

    private CraftingRecipe currentPatternCraftingRecipe;
    private EncodingMode currentPatternMode;

    // Re-entrancy guard: updatePatternCraftingOutput() writes into patternCraftOutputSlot and reads
    // encodedInputsInv, both of which fire onSlotChange(), which calls updatePatternCraftingOutput().
    // Without this guard the constructor's initial call recurses infinitely (StackOverflowError).
    private boolean updatingPatternCraftingOutput;

    // Debounce for the recipe lookup in updatePatternCraftingOutput(): while a batch slot update is in
    // progress (full content sync, per-slot sync cascade), slot changes only mark this dirty and the
    // lookup runs once at the end of the batch instead of once per slot.
    private boolean patternCraftingOutputDirty;
    private boolean batchingSlotUpdates;
    @Nullable
    private ResourceLocation uselessPatternRecipeId;
    @Nullable
    private String uselessPatternFingerprint;
    @Nullable
    private String uselessPatternSourceId;

    @GuiSync(93)
    public EncodingMode patternEncodingMode = EncodingMode.CRAFTING;
    @GuiSync(92)
    public boolean patternSubstitute = false;
    @GuiSync(91)
    public boolean patternSubstituteFluids = true;
    @GuiSync(90)
    @Nullable
    public ResourceLocation stonecuttingRecipeId;
    @GuiSync(89)
    public boolean trashOpen;
    @GuiSync(88)
    public boolean toolkitOpen;

    public final IntSet patternSlotsSupportingFluidSubstitution = new IntArraySet();
    private boolean lastSyncedSubstitute;
    private boolean lastSyncedSubstituteFluids = true;

    public MESTMenu(int id, Inventory ip, MESTMenuHost host) {
        super(TYPE, id, ip, host, true);
        this.host = host;
        this.patternAccessSession = new PatternAccessSession(this);
        this.patternEncodingLogic = host.getLogic();
        this.encodedInputsInv = patternEncodingLogic.getEncodedInputInv();
        this.encodedOutputsInv = patternEncodingLogic.getEncodedOutputInv();

        addSlot(new RestrictedInputSlot(
                        RestrictedInputSlot.PlacableItemType.QE_SINGULARITY,
                        host.getSubInventory(WTMenuHost.INV_SINGULARITY),
                        0),
                AE2wtlibSlotSemantics.SINGULARITY);
        addCraftingTerminalEquipmentSlots(ip);

        addPatternEncodingSlots();
        addPatternCacheSlots();
        addTrashSlots();
        addToolkitSlots();
        addToolkitNetworkToolSlots();
        addTerminalNetworkToolkitSlots();
        // Add-on modules come last so built-in slot indices never shift.
        MestModuleSlots.addTo(this, host.getItemStack(), this::addSlot);

        this.patternEncodingMode = patternEncodingLogic.getMode();
        this.patternSubstitute = patternEncodingLogic.isSubstitution();
        this.patternSubstituteFluids = patternEncodingLogic.isFluidSubstitution();
        this.stonecuttingRecipeId = patternEncodingLogic.getStonecuttingRecipeId();

        registerClientAction(ACTION_ENCODE_PATTERN, Boolean.class, this::encodePattern);
        registerClientAction(ACTION_CLEAR_PATTERN, this::clearPatternEncoding);
        registerClientAction(ACTION_SET_PATTERN_MODE, EncodingMode.class, this::setPatternEncodingMode);
        registerClientAction(ACTION_SET_PATTERN_SUBSTITUTION, Boolean.class, this::setPatternSubstitute);
        registerClientAction(ACTION_SET_PATTERN_FLUID_SUBSTITUTION, Boolean.class, this::setPatternFluidSubstitute);
        registerClientAction(ACTION_SET_STONECUTTING_RECIPE_ID, ResourceLocation.class,
                this::setStonecuttingRecipeId);
        registerClientAction(ACTION_CYCLE_PROCESSING_OUTPUT, this::cycleProcessingOutput);
        registerClientAction(ACTION_SCALE_ENCODING, Integer.class, this::scaleEncodingPattern);
        registerClientAction(ACTION_UPLOAD_PATTERN, Boolean.class, this::uploadEncodedPattern);
        registerClientAction(ACTION_REQUEST_PROVIDERS, this::requestProviderList);
        registerClientAction(ACTION_MAGNET_MENU, this::openMagnetMenu);
        registerClientAction(ACTION_TRASH_MENU, this::openTrashMenu);
        registerClientAction(ACTION_CLOSE_TRASH, this::closeTrash);
        registerClientAction(ACTION_SET_TOOLKIT_OPEN, Boolean.class, this::setToolkitOpen);
        registerClientAction(ACTION_SET_USELESS_PATTERN, String.class, this::setUselessPattern);

        updateStonecuttingRecipes();
        updatePatternCraftingOutput();
        applyPatternEncodingSlotActivation();
    }

    @Override
    public IGridNode getNetworkNode() {
        return host.getActionableNode();
    }

    public IGridNode getGridNode() {
        return host.getActionableNode();
    }

    /** AE2 15 has no link-status object; a powered, reachable grid is what "connected" meant. */
    public boolean isLinked() {
        return isPowered();
    }

    public PatternAccessSession getPatternAccessSession() {
        return patternAccessSession;
    }

    /**
     * True while this menu is the player's open, valid terminal. Covers every client packet that
     * mutates terminal-owned state but does not need a live ME grid link (pattern cache, trash,
     * encoding prefs).
     */
    public boolean canUseTerminal(ServerPlayer player) {
        return isServerSide()
                && getPlayer() == player
                && player.containerMenu == this
                && isValidMenu()
                && stillValid(player)
                && host.stillValid();
    }

    public boolean canUsePatternAccess(ServerPlayer player) {
        if (!canUseTerminal(player) || !isLinked()) {
            return false;
        }
        var node = getGridNode();
        return node != null && node.isActive() && node.getGrid() != null;
    }

    public MESTMenuHost getMestHost() {
        return host;
    }

    public boolean isWUT() {
        return host.getItemStack().getItem() instanceof ItemWUT;
    }

    public MagnetMode getMagnetMode() {
        return MagnetHandler.getMagnetMode(host.getItemStack());
    }

    public void openMagnetMenu() {
        if (isClientSide()) {
            sendClientAction(ACTION_MAGNET_MENU);
            return;
        }
        MenuOpener.open(MestMagnetMenu.TYPE, getPlayer(), getLocator());
    }

    public void openTrashMenu() {
        if (isClientSide()) {
            sendClientAction(ACTION_TRASH_MENU);
            return;
        }
        trashOpen = true;
    }

    public void closeTrash() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLOSE_TRASH);
            return;
        }
        host.clearTrash();
        trashOpen = false;
    }

    public boolean isTrashOpen() {
        return trashOpen;
    }

    public void setToolkitOpen(boolean open) {
        if (isClientSide()) {
            sendClientAction(ACTION_SET_TOOLKIT_OPEN, open);
            toolkitOpen = open;
            return;
        }
        toolkitOpen = open;
    }

    public List<Slot> getTrashSlots() {
        return getSlots(AE2wtlibSlotSemantics.TRASH);
    }

    /** Slots an add-on module registered through {@code RegisterMestModuleSlotsEvent}. */
    public List<Slot> getModuleSlots(String moduleId) {
        SlotSemantic semantic = MestModuleSlots.semantic(moduleId);
        return semantic == null ? List.of() : getSlots(semantic);
    }

    public List<Slot> getToolkitSlots() {
        return getSlots(MestSlotSemantics.TOOLKIT);
    }

    /**
     * A carried vanilla network tool wins, matching AE machines (which keep AE2's own toolbox in
     * that case); otherwise the panel shows the terminal's built-in upgrade inventory.
     */
    public List<Slot> getNetworkToolkitSlots() {
        List<Slot> vanilla = getSlots(SlotSemantics.TOOLBOX);
        return vanilla.isEmpty() ? getSlots(MestSlotSemantics.NETWORK_TOOLKIT) : vanilla;
    }

    public ItemStack getToolkitMemoryStack(int toolkitIndex) {
        InternalInventory memory = host.getToolkitMemoryInventory();
        if (toolkitIndex < 0 || toolkitIndex >= memory.size()) {
            return ItemStack.EMPTY;
        }
        return memory.getStackInSlot(toolkitIndex);
    }

    public boolean hasToolkitMemory(int toolkitIndex) {
        return !getToolkitMemoryStack(toolkitIndex).isEmpty();
    }

    public void setToolkitMemorySlot(int toolkitIndex, boolean rememberFromSlot) {
        InternalInventory memory = host.getToolkitMemoryInventory();
        InternalInventory toolkit = host.getToolkitInventory();
        if (toolkitIndex < 0 || toolkitIndex >= memory.size() || toolkitIndex >= toolkit.size()) {
            return;
        }
        if (!rememberFromSlot) {
            memory.setItemDirect(toolkitIndex, ItemStack.EMPTY);
            broadcastChanges();
            return;
        }
        ItemStack stack = toolkit.getStackInSlot(toolkitIndex);
        if (stack.isEmpty()) {
            return;
        }
        memory.setItemDirect(toolkitIndex, stack.copyWithCount(1));
        broadcastChanges();
    }

    @Override
    protected boolean canSlotsBeHidden(SlotSemantic semantic) {
        return semantic == AE2wtlibSlotSemantics.OFFHAND
                || semantic == AE2wtlibSlotSemantics.HELMET
                || semantic == AE2wtlibSlotSemantics.CHESTPLATE
                || semantic == AE2wtlibSlotSemantics.LEGGINGS
                || semantic == AE2wtlibSlotSemantics.BOOTS;
    }

    private void addCraftingTerminalEquipmentSlots(Inventory ip) {
        addSlot(new ArmorSlot(ip, ArmorSlot.Armor.HEAD) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                Item item = stack.getItem();
                return item instanceof BlockItem blockItem && blockItem.getBlock() instanceof Equipable
                        || super.mayPlace(stack);
            }
        }, AE2wtlibSlotSemantics.HELMET);
        addSlot(new ArmorSlot(ip, ArmorSlot.Armor.CHEST), AE2wtlibSlotSemantics.CHESTPLATE);
        addSlot(new ArmorSlot(ip, ArmorSlot.Armor.LEGS), AE2wtlibSlotSemantics.LEGGINGS);
        addSlot(new ArmorSlot(ip, ArmorSlot.Armor.FEET), AE2wtlibSlotSemantics.BOOTS);
        if (Integer.valueOf(Inventory.SLOT_OFFHAND).equals(host.getSlot())) {
            addSlot(new ArmorSlot.DisabledOffhandSlot(ip), AE2wtlibSlotSemantics.OFFHAND);
        } else {
            addSlot(new ArmorSlot(ip, ArmorSlot.Armor.OFFHAND), AE2wtlibSlotSemantics.OFFHAND);
        }
    }

    public PatternEncodingLogic getPatternEncodingLogic() {
        return patternEncodingLogic;
    }

    public EncodingMode getPatternEncodingMode() {
        return patternEncodingMode;
    }

    public void setPatternEncodingMode(EncodingMode mode) {
        if (isClientSide()) {
            patternEncodingMode = mode;
            applyPatternEncodingSlotActivation();
            sendClientAction(ACTION_SET_PATTERN_MODE, mode);
            return;
        }
        if (this.patternEncodingMode != mode || patternEncodingLogic.getMode() != mode) {
            patternEncodingLogic.setMode(mode);
            this.patternEncodingMode = mode;
            if (mode == EncodingMode.STONECUTTING) {
                updateStonecuttingRecipes();
            }
            updatePatternCraftingOutput();
            checkFluidSubstitutionSupport();
            applyPatternEncodingSlotActivation();
            broadcastChanges();
        }
    }

    public boolean isPatternSubstitute() {
        return patternSubstitute;
    }

    public void setPatternSubstitute(boolean substitute) {
        if (isClientSide()) {
            patternSubstitute = substitute;
            sendClientAction(ACTION_SET_PATTERN_SUBSTITUTION, substitute);
            return;
        }
        patternEncodingLogic.setSubstitution(substitute);
        patternSubstitute = substitute;
        broadcastChanges();
    }

    public boolean isPatternFluidSubstitute() {
        return patternSubstituteFluids;
    }

    public void setPatternFluidSubstitute(boolean substitute) {
        if (isClientSide()) {
            patternSubstituteFluids = substitute;
            checkFluidSubstitutionSupport();
            sendClientAction(ACTION_SET_PATTERN_FLUID_SUBSTITUTION, substitute);
            return;
        }
        patternEncodingLogic.setFluidSubstitution(substitute);
        patternSubstituteFluids = substitute;
        checkFluidSubstitutionSupport();
        broadcastChanges();
    }

    public void encodePattern() {
        encodePattern(false);
    }

    public void encodePattern(boolean shiftDown) {
        if (isClientSide()) {
            sendClientAction(ACTION_ENCODE_PATTERN, shiftDown);
            return;
        }

        ItemStack encodedPattern = createEncodedPattern();
        if (encodedPattern != null) {
            var encodeOutput = this.encodedPatternSlot.getItem();

            if (!encodeOutput.isEmpty()
                    && !PatternDetailsHelper.isEncodedPattern(encodeOutput)
                    && !AEItems.BLANK_PATTERN.isSameAs(encodeOutput)) {
                return;
            } else if (encodeOutput.isEmpty()) {
                var blankPattern = this.blankPatternSlot.getItem();
                if (!AEItems.BLANK_PATTERN.isSameAs(blankPattern)) {
                    return;
                }

                blankPattern.shrink(1);
                if (blankPattern.getCount() <= 0) {
                    this.blankPatternSlot.set(ItemStack.EMPTY);
                }
            }

            this.encodedPatternSlot.set(encodedPattern);
            if (!shiftDown
                    && (patternEncodingMode == EncodingMode.CRAFTING
                    || patternEncodingMode == EncodingMode.SMITHING_TABLE
                    || patternEncodingMode == EncodingMode.STONECUTTING)
                    && getPlayer() instanceof ServerPlayer player) {
                var node = getGridNode();
                if (node != null) {
                    MestCraftingPatternAutoUpload.tryUpload(player, encodedPatternSlot, node.getGrid());
                }
            }
        } else {
            clearEncodedPatternOnly();
        }
    }

    public void selectUselessPattern(ResourceLocation recipeId, String fingerprint, String sourceId) {
        String selection = recipeId + "\u0000" + fingerprint + "\u0000" + sourceId;
        if (isClientSide()) {
            setUselessPattern(selection);
            sendClientAction(ACTION_SET_USELESS_PATTERN, selection);
        }
    }

    private void setUselessPattern(String selection) {
        String[] parts = selection.split("\\u0000", -1);
        if (parts.length != 3) {
            return;
        }
        ResourceLocation recipeId = ResourceLocation.tryParse(parts[0]);
        if (recipeId == null || parts[1].isEmpty() || parts[2].isEmpty()) {
            return;
        }
        uselessPatternRecipeId = recipeId;
        uselessPatternFingerprint = parts[1];
        uselessPatternSourceId = parts[2];
    }

    @Nullable
    public ResourceLocation getUselessPatternRecipeId() {
        return uselessPatternRecipeId;
    }

    @Nullable
    public String getUselessPatternFingerprint() {
        return uselessPatternFingerprint;
    }

    @Nullable
    public String getUselessPatternSourceId() {
        return uselessPatternSourceId;
    }

    public void clearUselessPatternSelection() {
        uselessPatternRecipeId = null;
        uselessPatternFingerprint = null;
        uselessPatternSourceId = null;
    }

    public void clearPatternEncoding() {
        if (isClientSide()) {
            sendClientAction(ACTION_CLEAR_PATTERN);
            return;
        }

        encodedInputsInv.clear();
        encodedOutputsInv.clear();
        updatePatternCraftingOutput();
        broadcastChanges();
    }

    public void cycleProcessingOutput() {
        if (isClientSide()) {
            sendClientAction(ACTION_CYCLE_PROCESSING_OUTPUT);
            return;
        }
        if (patternEncodingMode != EncodingMode.PROCESSING) {
            return;
        }

        var newOutputs = new ItemStack[getProcessingOutputSlots().length];
        for (int i = 0; i < processingOutputSlots.length; i++) {
            newOutputs[i] = ItemStack.EMPTY;
            if (!processingOutputSlots[i].getItem().isEmpty()) {
                for (int j = 1; j < processingOutputSlots.length; j++) {
                    var nextItem = processingOutputSlots[(i + j) % processingOutputSlots.length].getItem();
                    if (!nextItem.isEmpty()) {
                        // Copy so no live slot stack reference is shared and later mutated through set().
                        newOutputs[i] = nextItem.copy();
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < newOutputs.length; i++) {
            processingOutputSlots[i].set(newOutputs[i]);
        }
        broadcastChanges();
    }

    public boolean canCycleProcessingOutputs() {
        return patternEncodingMode == EncodingMode.PROCESSING
                && Arrays.stream(processingOutputSlots).filter(s -> !s.getItem().isEmpty()).count() > 1;
    }

    /**
     * EAEP-style processing encoder scale. {@code code}: 2/3/5 multiply, -2/-3/-5 divide,
     * 0 restore ratio, 1 rotate outputs.
     */
    public void scaleEncodingPattern(int code) {
        if (isClientSide()) {
            sendClientAction(ACTION_SCALE_ENCODING, code);
            return;
        }
        if (patternEncodingMode != EncodingMode.PROCESSING) {
            return;
        }
        if (code == 1) {
            cycleProcessingOutput();
            return;
        }
        if (code == 0) {
            long gcd = PatternEncodingAmounts.sharedGcd(
                    PatternEncodingAmounts.copyInv(encodedInputsInv), PatternEncodingAmounts.copyInv(encodedOutputsInv));
            if (gcd > 1L) {
                writeInv(encodedInputsInv, PatternEncodingAmounts.divideStacks(
                        PatternEncodingAmounts.copyInv(encodedInputsInv), gcd));
                writeInv(encodedOutputsInv, PatternEncodingAmounts.divideStacks(
                        PatternEncodingAmounts.copyInv(encodedOutputsInv), gcd));
            }
            broadcastChanges();
            return;
        }
        boolean divide = code < 0;
        int scale = Math.abs(code);
        var input = PatternEncodingAmounts.copyInvArray(encodedInputsInv);
        var output = PatternEncodingAmounts.copyInvArray(encodedOutputsInv);
        if (!PatternEncodingAmounts.canScale(input, scale, divide)
                || !PatternEncodingAmounts.canScale(output, scale, divide)) {
            return;
        }
        writeInv(encodedInputsInv, Arrays.asList(PatternEncodingAmounts.scaleStacks(input, scale, divide)));
        writeInv(encodedOutputsInv, Arrays.asList(PatternEncodingAmounts.scaleStacks(output, scale, divide)));
        broadcastChanges();
    }

    public void requestProviderList() {
        if (isClientSide()) {
            sendClientAction(ACTION_REQUEST_PROVIDERS);
            return;
        }
        if (PlusEncodingUpload.available() && getPlayer() instanceof ServerPlayer player) {
            PlusEncodingUpload.sendPickerList(player);
        }
    }

    public void uploadEncodedPattern(boolean returnLast) {
        if (isClientSide()) {
            sendClientAction(ACTION_UPLOAD_PATTERN, returnLast);
            return;
        }
        if (!(getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        if (returnLast) {
            patternAccessSession.returnLastUpload(player);
            return;
        }
        if (PlusEncodingUpload.available() && PlusEncodingUpload.beginAndOpenPicker(player, encodedPatternSlot)) {
            return;
        }
        if (!patternAccessSession.uploadFromSlot(encodedPatternSlot, player)) {
            player.displayClientMessage(
                    Component.translatable("gui.mesplicedterminal.pattern_upload.no_provider"), true);
        }
    }

    private void tryFillBlankPattern() {
        if (blankPatternFilled || blankPatternSlot == null || !isLinked()) {
            return;
        }
        ItemStack current = blankPatternSlot.getItem();
        if (!current.isEmpty() && !AEItems.BLANK_PATTERN.isSameAs(current)) {
            return;
        }
        int space = Math.min(
                blankPatternSlot.getMaxStackSize() - current.getCount(),
                AEItems.BLANK_PATTERN.stack(1).getMaxStackSize());
        if (space <= 0) {
            blankPatternFilled = true;
            return;
        }
        var node = getGridNode();
        if (node == null || node.getGrid() == null) {
            return;
        }
        var storage = node.getGrid().getStorageService().getInventory();
        var power = node.getGrid().getEnergyService();
        if (storage == null || power == null) {
            return;
        }
        AEItemKey blankKey = AEItemKey.of(AEItems.BLANK_PATTERN.asItem());
        long extracted = appeng.api.storage.StorageHelper.poweredExtraction(
                power, storage, blankKey, space, getActionSource());
        if (extracted <= 0L) {
            return;
        }
        int toInsert = (int) Math.min(extracted, space);
        if (current.isEmpty()) {
            blankPatternSlot.set(AEItems.BLANK_PATTERN.stack(toInsert));
        } else {
            current.grow(toInsert);
            blankPatternSlot.set(current);
        }
        long leftover = extracted - toInsert;
        if (leftover > 0L) {
            appeng.api.storage.StorageHelper.poweredInsert(
                    power, storage, blankKey, leftover, getActionSource());
        }
        blankPatternFilled = true;
    }

    private static void writeInv(ConfigInventory inventory, List<GenericStack> stacks) {
        inventory.beginBatch();
        try {
            for (int i = 0; i < inventory.size(); i++) {
                inventory.setStack(i, i < stacks.size() ? stacks.get(i) : null);
            }
        } finally {
            inventory.endBatch();
        }
    }

    @Contract("null -> false")
    public boolean canModifyAmountForPatternSlot(@Nullable Slot slot) {
        return isProcessingPatternSlot(slot) && slot.hasItem();
    }

    @Contract("null -> false")
    public boolean isProcessingPatternSlot(@Nullable Slot slot) {
        if (slot == null || patternEncodingMode != EncodingMode.PROCESSING) {
            return false;
        }

        for (var processingOutputSlot : processingOutputSlots) {
            if (processingOutputSlot == slot) {
                return true;
            }
        }
        for (var processingInputSlot : processingInputSlots) {
            if (processingInputSlot == slot) {
                return true;
            }
        }
        return false;
    }

    @Contract("null -> false")
    public boolean isPatternEncodingInputSlot(@Nullable Slot slot) {
        if (slot == null) {
            return false;
        }
        return switch (patternEncodingMode) {
            case CRAFTING -> containsIdentity(patternCraftingSlots, slot);
            case PROCESSING -> containsIdentity(processingInputSlots, slot);
            case SMITHING_TABLE -> slot == smithingTableTemplateSlot
                    || slot == smithingTableBaseSlot
                    || slot == smithingTableAdditionSlot;
            case STONECUTTING -> slot == stonecuttingInputSlot;
        };
    }

    private static boolean containsIdentity(Slot[] slots, Slot target) {
        for (var slot : slots) {
            if (slot == target) {
                return true;
            }
        }
        return false;
    }

    public FakeSlot[] getPatternCraftingSlots() {
        return patternCraftingSlots;
    }

    public PatternTermSlot getPatternCraftOutputSlot() {
        return patternCraftOutputSlot;
    }

    public FakeSlot[] getProcessingInputSlots() {
        return processingInputSlots;
    }

    public FakeSlot[] getProcessingOutputSlots() {
        return processingOutputSlots;
    }

    public FakeSlot getStonecuttingInputSlot() {
        return stonecuttingInputSlot;
    }

    public FakeSlot getSmithingTableTemplateSlot() {
        return smithingTableTemplateSlot;
    }

    public FakeSlot getSmithingTableBaseSlot() {
        return smithingTableBaseSlot;
    }

    public FakeSlot getSmithingTableAdditionSlot() {
        return smithingTableAdditionSlot;
    }

    public RestrictedInputSlot getBlankPatternSlot() {
        return blankPatternSlot;
    }

    public RestrictedInputSlot getEncodedPatternSlot() {
        return encodedPatternSlot;
    }

    public List<StonecutterRecipe> getStonecuttingRecipes() {
        return stonecuttingRecipes;
    }

    @Nullable
    public ResourceLocation getStonecuttingRecipeId() {
        return stonecuttingRecipeId;
    }

    public void setStonecuttingRecipeId(ResourceLocation id) {
        if (isClientSide()) {
            stonecuttingRecipeId = id;
            sendClientAction(ACTION_SET_STONECUTTING_RECIPE_ID, id);
            return;
        }

        updateStonecuttingRecipes();
        ResourceLocation validatedId = stonecuttingRecipes.stream()
                .anyMatch(recipe -> recipe.getId().equals(id)) ? id : null;
        patternEncodingLogic.setStonecuttingRecipeId(validatedId);
        stonecuttingRecipeId = validatedId;
        updatePatternCraftingOutput();
        broadcastChanges();
    }

    private void addPatternEncodingSlots() {
        var encodedInputs = encodedInputsInv.createMenuWrapper();
        var encodedOutputs = encodedOutputsInv.createMenuWrapper();

        for (int i = 0; i < patternCraftingSlots.length; i++) {
            var slot = new FakeSlot(encodedInputs, i);
            slot.setHideAmount(true);
            this.addSlot(this.patternCraftingSlots[i] = slot, MestSlotSemantics.PATTERN_CRAFTING_GRID);
        }
        this.addSlot(this.patternCraftOutputSlot = new PatternTermSlot(getPlayerInventory().player,
                getActionSource(), powerSource, host.getInventory(), encodedInputs, this),
                MestSlotSemantics.PATTERN_CRAFTING_RESULT);
        this.patternCraftOutputSlot.setIcon(null);

        for (int i = 0; i < processingInputSlots.length; i++) {
            this.addSlot(this.processingInputSlots[i] = new FakeSlot(encodedInputs, i),
                    MestSlotSemantics.PATTERN_PROCESSING_INPUTS);
        }
        for (int i = 0; i < processingOutputSlots.length; i++) {
            this.addSlot(this.processingOutputSlots[i] = new FakeSlot(encodedOutputs, i),
                    MestSlotSemantics.PATTERN_PROCESSING_OUTPUTS);
        }

        this.addSlot(this.stonecuttingInputSlot = new FakeSlot(encodedInputs, 0),
                MestSlotSemantics.PATTERN_STONECUTTING_INPUT);
        this.stonecuttingInputSlot.setHideAmount(true);

        this.addSlot(this.smithingTableTemplateSlot = new FakeSlot(encodedInputs, 0),
                MestSlotSemantics.PATTERN_SMITHING_TEMPLATE);
        this.smithingTableTemplateSlot.setHideAmount(true);
        this.addSlot(this.smithingTableBaseSlot = new FakeSlot(encodedInputs, 1),
                MestSlotSemantics.PATTERN_SMITHING_BASE);
        this.smithingTableBaseSlot.setHideAmount(true);
        this.addSlot(this.smithingTableAdditionSlot = new FakeSlot(encodedInputs, 2),
                MestSlotSemantics.PATTERN_SMITHING_ADDITION);
        this.smithingTableAdditionSlot.setHideAmount(true);

        this.addSlot(this.blankPatternSlot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.BLANK_PATTERN,
                patternEncodingLogic.getBlankPatternInv(), 0), SlotSemantics.BLANK_PATTERN);
        this.addSlot(
                this.encodedPatternSlot = new RestrictedInputSlot(RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN,
                        patternEncodingLogic.getEncodedPatternInv(), 0),
                SlotSemantics.ENCODED_PATTERN);
        this.encodedPatternSlot.setStackLimit(1);
    }

    private void addPatternCacheSlots() {
        var inventory = host.getPatternCacheInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            var cacheSlot = new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.ENCODED_PATTERN, inventory, slot);
            cacheSlot.setIcon(appeng.client.gui.Icon.BACKGROUND_ENCODED_PATTERN);
            addSlot(cacheSlot, MestSlotSemantics.PATTERN_CACHE);
        }
    }

    public List<Slot> getPatternCacheSlots() {
        return getSlots(MestSlotSemantics.PATTERN_CACHE);
    }

    private void addTrashSlots() {
        InternalInventory inventory = host.getTrashInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            addSlot(new AppEngSlot(inventory, slot), AE2wtlibSlotSemantics.TRASH);
        }
    }

    private void addToolkitSlots() {
        InternalInventory inventory = host.getToolkitInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            addSlot(new ToolkitSlot(inventory, slot, this), MestSlotSemantics.TOOLKIT);
        }
    }

    /**
     * Vanilla {@link ToolboxMenu} only searches the player inventory. If the network tool is in
     * the toolkit, attach the same TOOLBOX slots through {@link ToolkitItemLocator}.
     */
    private void addToolkitNetworkToolSlots() {
        if (getToolbox().isPresent()) {
            return;
        }
        InternalInventory toolkit = host.getToolkitInventory();
        for (int index = 0; index < toolkit.size(); index++) {
            ItemStack stack = toolkit.getStackInSlot(index);
            if (!(stack.getItem() instanceof NetworkToolItem)) {
                continue;
            }
            NetworkToolMenuHost toolHost = new ToolkitItemLocator(index)
                    .locate(getPlayer(), NetworkToolMenuHost.class);
            if (toolHost == null) {
                continue;
            }
            InternalInventory upgrades = toolHost.getInventory();
            for (int slot = 0; slot < upgrades.size(); slot++) {
                addSlot(new ToolkitToolboxSlot(upgrades, slot, toolHost, getPlayer(), index), SlotSemantics.TOOLBOX);
            }
            return;
        }
    }

    /**
     * AE2's {@code ToolboxMenu.tick} closes the menu once the tool is gone; these slots live in our
     * menu instead, so they freeze rather than touch whatever stack now sits at that index.
     */
    private static final class ToolkitToolboxSlot extends RestrictedInputSlot {
        private final NetworkToolMenuHost toolHost;
        private final Player owner;
        private final int toolkitIndex;

        ToolkitToolboxSlot(InternalInventory upgrades, int slot, NetworkToolMenuHost toolHost, Player owner,
                int toolkitIndex) {
            super(RestrictedInputSlot.PlacableItemType.UPGRADES, upgrades, slot);
            this.toolHost = toolHost;
            this.owner = owner;
            this.toolkitIndex = toolkitIndex;
        }

        /** The tool this host was built for still sits in its toolkit cell. */
        private boolean toolPresent() {
            return ToolkitBarState.stackAt(owner, toolkitIndex) == toolHost.getItemStack();
        }

        @Override
        public ItemStack getItem() {
            return toolPresent() ? super.getItem() : ItemStack.EMPTY;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return toolPresent() && super.mayPlace(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return toolPresent() && super.mayPickup(player);
        }

        @Override
        public void set(ItemStack stack) {
            if (toolPresent()) {
                super.set(stack);
            }
        }

        @Override
        public ItemStack remove(int amount) {
            return toolPresent() ? super.remove(amount) : ItemStack.EMPTY;
        }
    }

    private void addTerminalNetworkToolkitSlots() {
        InternalInventory inventory = host.getNetworkToolkitInventory();
        for (int slot = 0; slot < inventory.size(); slot++) {
            addSlot(new RestrictedInputSlot(
                    RestrictedInputSlot.PlacableItemType.UPGRADES, inventory, slot),
                    MestSlotSemantics.NETWORK_TOOLKIT);
        }
    }

    private ItemStack updatePatternCraftingOutput() {
        // During super-construction (CraftingTermMenu.<init> calls updateCurrentRecipeAndOutput, which
        // fires onSlotChange before our fields are initialised) encodedInputsInv is still null, and
        // patternCraftOutputSlot may not exist yet. Bail out until addPatternEncodingSlots() has run.
        if (encodedInputsInv == null || patternCraftOutputSlot == null) {
            return ItemStack.EMPTY;
        }
        if (updatingPatternCraftingOutput) {
            // Already recomputing — setDisplayedCraftingOutput()/getKey() below re-enters onSlotChange().
            return patternCraftOutputSlot.getItem();
        }
        updatingPatternCraftingOutput = true;
        try {
            return updatePatternCraftingOutput0();
        } finally {
            updatingPatternCraftingOutput = false;
        }
    }

    private ItemStack updatePatternCraftingOutput0() {
        var level = this.getPlayerInventory().player.level();

        var items = net.minecraft.core.NonNullList.withSize(CRAFTING_GRID_WIDTH * CRAFTING_GRID_HEIGHT,
                ItemStack.EMPTY);
        boolean invalidIngredients = false;
        for (int x = 0; x < items.size(); x++) {
            var stack = getEncodedCraftingIngredient(x);
            if (stack != null) {
                items.set(x, stack);
            } else {
                invalidIngredients = true;
            }
        }

        var input = new TransientCraftingContainer(new MestNullMenu(), CRAFTING_GRID_WIDTH, CRAFTING_GRID_HEIGHT);
        for (int i = 0; i < items.size(); i++) {
            input.setItem(i, items.get(i));
        }

        if (this.currentPatternCraftingRecipe == null
                || !this.currentPatternCraftingRecipe.matches(input, level)) {
            if (invalidIngredients) {
                this.currentPatternCraftingRecipe = null;
            } else {
                this.currentPatternCraftingRecipe = level.getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, level)
                        .orElse(null);
            }
            this.currentPatternMode = this.patternEncodingMode;
        }
        checkFluidSubstitutionSupport();

        final ItemStack result;
        if (this.currentPatternCraftingRecipe == null) {
            result = ItemStack.EMPTY;
        } else {
            result = this.currentPatternCraftingRecipe.assemble(input, level.registryAccess());
        }

        this.patternCraftOutputSlot.setDisplayedCraftingOutput(result);
        return result;
    }

    private void checkFluidSubstitutionSupport() {
        this.patternSlotsSupportingFluidSubstitution.clear();

        if (this.patternEncodingMode != EncodingMode.CRAFTING || this.currentPatternCraftingRecipe == null) {
            return;
        }

        var encodedPattern = createEncodedPattern();
        if (encodedPattern != null) {
            var decodedPattern = PatternDetailsHelper.decodePattern(encodedPattern,
                    this.getPlayerInventory().player.level());
            if (decodedPattern instanceof AECraftingPattern craftingPattern) {
                for (int i = 0; i < craftingPattern.getSparseInputs().length; i++) {
                    if (craftingPattern.getValidFluid(i) != null) {
                        patternSlotsSupportingFluidSubstitution.add(i);
                    }
                }
            }
        }
    }

    @Nullable
    private ItemStack createEncodedPattern() {
        return switch (this.patternEncodingMode) {
            case CRAFTING -> encodeCraftingPattern();
            case PROCESSING -> encodeProcessingPattern();
            case SMITHING_TABLE -> encodeSmithingTablePattern();
            case STONECUTTING -> encodeStonecuttingPattern();
        };
    }

    @Nullable
    private ItemStack encodeCraftingPattern() {
        var ingredients = new ItemStack[CRAFTING_GRID_SLOTS];
        boolean valid = false;
        for (int x = 0; x < ingredients.length; x++) {
            ingredients[x] = getEncodedCraftingIngredient(x);
            if (ingredients[x] == null) {
                return null;
            } else if (!ingredients[x].isEmpty()) {
                valid = true;
            }
        }
        if (!valid) {
            return null;
        }

        var result = updatePatternCraftingOutput();
        if (result.isEmpty() || currentPatternCraftingRecipe == null) {
            return null;
        }

        return PatternDetailsHelper.encodeCraftingPattern(this.currentPatternCraftingRecipe, ingredients, result,
                isPatternSubstitute(), isPatternFluidSubstitute());
    }

    @Nullable
    private ItemStack encodeProcessingPattern() {
        var inputs = new GenericStack[encodedInputsInv.size()];
        boolean valid = false;
        for (int slot = 0; slot < encodedInputsInv.size(); slot++) {
            inputs[slot] = encodedInputsInv.getStack(slot);
            if (inputs[slot] != null) {
                valid = true;
            }
        }
        if (!valid) {
            return null;
        }

        var outputs = new GenericStack[encodedOutputsInv.size()];
        for (int slot = 0; slot < encodedOutputsInv.size(); slot++) {
            outputs[slot] = encodedOutputsInv.getStack(slot);
        }
        if (outputs[0] == null) {
            return null;
        }

        return PatternDetailsHelper.encodeProcessingPattern(inputs, outputs);
    }

    @Nullable
    private ItemStack encodeSmithingTablePattern() {
        if (!(encodedInputsInv.getKey(0) instanceof AEItemKey template)
                || !(encodedInputsInv.getKey(1) instanceof AEItemKey base)
                || !(encodedInputsInv.getKey(2) instanceof AEItemKey addition)) {
            return null;
        }

        var input = new SimpleContainer(template.toStack(), base.toStack(), addition.toStack());
        var level = getPlayer().level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.SMITHING, input, level)
                .orElse(null);
        if (recipe == null) {
            return null;
        }

        var output = AEItemKey.of(recipe.assemble(input, level.registryAccess()));
        return PatternDetailsHelper.encodeSmithingTablePattern(recipe, template, base, addition, output,
                patternEncodingLogic.isSubstitution());
    }

    @Nullable
    private ItemStack encodeStonecuttingPattern() {
        if (stonecuttingRecipeId == null) {
            return null;
        }
        if (!(encodedInputsInv.getKey(0) instanceof AEItemKey input)) {
            return null;
        }

        var recipeInput = new SimpleContainer(input.toStack());
        var level = getPlayer().level();
        var recipe = level.getRecipeManager()
                .getRecipeFor(RecipeType.STONECUTTING, recipeInput, level, stonecuttingRecipeId)
                .map(com.mojang.datafixers.util.Pair::getSecond)
                .orElse(null);
        if (recipe == null) {
            return null;
        }

        var output = AEItemKey.of(recipe.getResultItem(level.registryAccess()));
        return PatternDetailsHelper.encodeStonecuttingPattern(recipe, input, output,
                patternEncodingLogic.isSubstitution());
    }

    @Nullable
    private ItemStack getEncodedCraftingIngredient(int slot) {
        var what = encodedInputsInv.getKey(slot);
        if (what == null) {
            return ItemStack.EMPTY;
        } else if (what instanceof AEItemKey itemKey) {
            return itemKey.toStack(1);
        } else {
            return null;
        }
    }

    private void clearEncodedPatternOnly() {
        var encodedPattern = this.encodedPatternSlot.getItem();
        if (PatternDetailsHelper.isEncodedPattern(encodedPattern)) {
            this.encodedPatternSlot.set(AEItems.BLANK_PATTERN.stack(encodedPattern.getCount()));
        }
    }

    private void updateStonecuttingRecipes() {
        stonecuttingRecipes.clear();
        if (encodedInputsInv == null) {
            return;
        }
        if (encodedInputsInv.getKey(0) instanceof AEItemKey itemKey) {
            var level = getPlayer().level();
            var recipeInput = new SimpleContainer(itemKey.toStack());
            stonecuttingRecipes.addAll(
                    level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, recipeInput, level));
        }

        if (stonecuttingRecipeId != null
                && stonecuttingRecipes.stream().noneMatch(r -> r.getId().equals(stonecuttingRecipeId))) {
            if (isServerSide()) {
                patternEncodingLogic.setStonecuttingRecipeId(null);
            }
            stonecuttingRecipeId = null;
        }
    }

    public void applyPatternEncodingSlotActivation() {
        boolean crafting = patternEncodingMode == EncodingMode.CRAFTING;
        boolean processing = patternEncodingMode == EncodingMode.PROCESSING;
        boolean smithing = patternEncodingMode == EncodingMode.SMITHING_TABLE;
        boolean stonecutting = patternEncodingMode == EncodingMode.STONECUTTING;

        for (var slot : patternCraftingSlots) {
            slot.setActive(crafting);
        }
        patternCraftOutputSlot.setActive(crafting);
        for (var slot : processingInputSlots) {
            slot.setActive(processing);
        }
        for (var slot : processingOutputSlots) {
            slot.setActive(processing);
        }
        smithingTableTemplateSlot.setActive(smithing);
        smithingTableBaseSlot.setActive(smithing);
        smithingTableAdditionSlot.setActive(smithing);
        stonecuttingInputSlot.setActive(stonecutting);
    }

    @Override
    public void setItem(int slotID, int stateId, ItemStack stack) {
        boolean wasBatching = batchingSlotUpdates;
        batchingSlotUpdates = true;
        try {
            super.setItem(slotID, stateId, stack);
        } finally {
            batchingSlotUpdates = wasBatching;
        }
        markPatternCraftingOutputDirty();
    }

    @Override
    public void initializeContents(int stateId, List<ItemStack> items, ItemStack carried) {
        boolean wasBatching = batchingSlotUpdates;
        batchingSlotUpdates = true;
        try {
            super.initializeContents(stateId, items, carried);
        } finally {
            batchingSlotUpdates = wasBatching;
        }
        markPatternCraftingOutputDirty();
    }

    private void markPatternCraftingOutputDirty() {
        patternCraftingOutputDirty = true;
        if (!batchingSlotUpdates) {
            flushPatternCraftingOutput();
        }
    }

    private void flushPatternCraftingOutput() {
        if (patternCraftingOutputDirty) {
            patternCraftingOutputDirty = false;
            updatePatternCraftingOutput();
        }
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.patternEncodingMode = patternEncodingLogic.getMode();
            this.patternSubstitute = patternEncodingLogic.isSubstitution();
            this.patternSubstituteFluids = patternEncodingLogic.isFluidSubstitution();
            this.stonecuttingRecipeId = patternEncodingLogic.getStonecuttingRecipeId();
        }
        flushPatternCraftingOutput();
        super.broadcastChanges();
        // AEBaseMenu construction can invoke this override before our fields are initialized.
        if (patternAccessSession != null) {
            patternAccessSession.serverTick();
        }
        if (isServerSide()) {
            tryFillBlankPattern();
        }
    }

    @Override
    public void removed(Player player) {
        patternAccessSession.close();
        super.removed(player);
    }

    @Override
    public void onServerDataSync() {
        super.onServerDataSync();

        // Client-side: only refresh UI-derived state. Never write the synced mode back into
        // patternEncodingLogic here - the server owns the logic's item data, and writing to it from the
        // client can drift from (and later fight with) the authoritative server state.
        // AE2 15 does not report which fields changed, so compare against what was seen last.
        if (currentPatternMode != patternEncodingMode) {
            currentPatternMode = patternEncodingMode;
            updatePatternCraftingOutput();
            updateStonecuttingRecipes();
        }
        if (lastSyncedSubstitute != patternSubstitute || lastSyncedSubstituteFluids != patternSubstituteFluids) {
            lastSyncedSubstitute = patternSubstitute;
            lastSyncedSubstituteFluids = patternSubstituteFluids;
            checkFluidSubstitutionSupport();
        }
        applyPatternEncodingSlotActivation();
    }

    @Override
    public void onSlotChange(Slot slot) {
        super.onSlotChange(slot);

        if (slot == this.encodedPatternSlot && isServerSide()) {
            this.broadcastChanges();
        }
        if (slot == this.stonecuttingInputSlot) {
            updateStonecuttingRecipes();
        }
        markPatternCraftingOutputDirty();
    }

    @Override
    protected ItemStack transferStackToMenu(ItemStack input) {
        if (prefersToolkitQuickMove()) {
            input = insertIntoToolkit(input, true);
            if (input.isEmpty()) {
                return ItemStack.EMPTY;
            }
            input = insertIntoToolkit(input, false);
            if (input.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        if (trashOpen) {
            for (Slot trashSlot : getTrashSlots()) {
                if (trashSlot.mayPlace(input)) {
                    input = trashSlot.safeInsert(input);
                    if (input.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        if (blankPatternSlot.mayPlace(input)) {
            input = blankPatternSlot.safeInsert(input);
            if (input.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        if (encodedPatternSlot.mayPlace(input)) {
            input = encodedPatternSlot.safeInsert(input);
            if (input.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        if (PatternDetailsHelper.isEncodedPattern(input)) {
            for (Slot cacheSlot : getPatternCacheSlots()) {
                if (cacheSlot.mayPlace(input)) {
                    input = cacheSlot.safeInsert(input);
                    if (input.isEmpty()) {
                        return ItemStack.EMPTY;
                    }
                }
            }
        }

        return super.transferStackToMenu(input);
    }

    @Override
    protected void handleNetworkInteraction(ServerPlayer player, AEKey clickedKey, InventoryAction action) {
        if (prefersToolkitQuickMove() && clickedKey instanceof AEItemKey itemKey
                && itemKey.toStack().getMaxStackSize() <= 1) {
            if (action == InventoryAction.SHIFT_CLICK) {
                insertFromNetworkToToolkit(itemKey);
                return;
            }
            if (action == InventoryAction.MOVE_REGION) {
                int limit = getToolkitSlots().size();
                for (int i = 0; i < limit && insertFromNetworkToToolkit(itemKey); i++) {
                }
                return;
            }
        }
        if (trashOpen && clickedKey instanceof AEItemKey itemKey) {
            if (action == InventoryAction.SHIFT_CLICK) {
                insertFromNetworkToTrash(itemKey);
                return;
            }
            if (action == InventoryAction.MOVE_REGION) {
                int limit = getTrashSlots().size();
                for (int i = 0; i < limit && insertFromNetworkToTrash(itemKey); i++) {
                }
                return;
            }
        }
        super.handleNetworkInteraction(player, clickedKey, action);
    }

    private boolean prefersToolkitQuickMove() {
        return toolkitOpen && ToolkitBarState.isQuickMoveEnabled(getPlayer());
    }

    private ItemStack insertIntoToolkit(ItemStack input, boolean rememberedEmptyOnly) {
        for (Slot toolkitSlot : getToolkitSlots()) {
            if (input.isEmpty()) {
                break;
            }
            if (rememberedEmptyOnly) {
                if (!(toolkitSlot instanceof ToolkitSlot slot)
                        || toolkitSlot.hasItem()
                        || !hasToolkitMemory(slot.toolkitIndex())) {
                    continue;
                }
            }
            if (toolkitSlot.mayPlace(input)) {
                input = toolkitSlot.safeInsert(input);
            }
        }
        return input;
    }

    private boolean insertFromNetworkToToolkit(AEItemKey itemKey) {
        return moveFromNetwork(itemKey, 1, stack -> insertIntoToolkit(insertIntoToolkit(stack, true), false));
    }

    private boolean insertFromNetworkToTrash(AEItemKey itemKey) {
        return moveFromNetwork(itemKey, itemKey.getMaxStackSize(), stack -> {
            for (Slot trashSlot : getTrashSlots()) {
                if (stack.isEmpty()) {
                    break;
                }
                if (trashSlot.mayPlace(stack)) {
                    stack = trashSlot.safeInsert(stack);
                }
            }
            return stack;
        });
    }

    /**
     * Extracts first (with power) and only then places the items, so the amount handed out can
     * never exceed what the network actually gave up. Whatever does not fit goes back.
     */
    private boolean moveFromNetwork(AEItemKey itemKey, int amount, UnaryOperator<ItemStack> placer) {
        IGridNode node = getGridNode();
        if (node == null || node.getGrid() == null) {
            return false;
        }
        MEStorage storage = node.getGrid().getStorageService().getInventory();
        var energy = node.getGrid().getEnergyService();
        long extracted = StorageHelper.poweredExtraction(energy, storage, itemKey, amount, getActionSource());
        if (extracted <= 0L) {
            return false;
        }
        ItemStack remainder = placer.apply(itemKey.toStack((int) extracted));
        long placed = extracted - remainder.getCount();
        if (!remainder.isEmpty()) {
            long returned = StorageHelper.poweredInsert(
                    energy, storage, itemKey, remainder.getCount(), getActionSource());
            remainder.shrink((int) returned);
            if (!remainder.isEmpty()) {
                getPlayer().getInventory().placeItemBackInInventory(remainder);
            }
        }
        return placed > 0L;
    }

    public void handlePatternCacheAction(PatternCacheActionPacket.Action action, boolean value) {
        switch (action) {
            case SWAP -> rotateProcessingOutputs();
            case TIMES_2 -> modifyProcessingPatterns(2, false);
            case TIMES_3 -> modifyProcessingPatterns(3, false);
            case TIMES_5 -> modifyProcessingPatterns(5, false);
            case EQUALS_1 -> restoreProcessingRatios();
            case DIVIDE_2 -> modifyProcessingPatterns(2, true);
            case DIVIDE_3 -> modifyProcessingPatterns(3, true);
            case DIVIDE_5 -> modifyProcessingPatterns(5, true);
            case TIMES_8 -> modifyProcessingPatterns(8, false);
            case DIVIDE_8 -> modifyProcessingPatterns(8, true);
            case TIMES_16 -> modifyProcessingPatterns(16, false);
            case DIVIDE_16 -> modifyProcessingPatterns(16, true);
            case TIMES_32 -> modifyProcessingPatterns(32, false);
            case DIVIDE_32 -> modifyProcessingPatterns(32, true);
            case ITEM_SUBSTITUTION -> changeCraftingSubstitution(true, value);
            case FLUID_SUBSTITUTION -> changeCraftingSubstitution(false, value);
        }
    }

    private void modifyProcessingPatterns(int scale, boolean divide) {
        if (scale <= 0) {
            return;
        }
        for (Slot slot : getPatternCacheSlots()) {
            ItemStack stack = slot.getItem();
            var details = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
            if (!(details instanceof AEProcessingPattern process)) {
                continue;
            }
            var input = process.getSparseInputs();
            var output = process.getSparseOutputs();
            if (!PatternEncodingAmounts.canScale(input, scale, divide)
                    || !PatternEncodingAmounts.canScale(output, scale, divide)) {
                continue;
            }
            slot.set(PatternDetailsHelper.encodeProcessingPattern(
                    PatternEncodingAmounts.scaleStacks(input, scale, divide),
                    PatternEncodingAmounts.scaleStacks(output, scale, divide)));
        }
        broadcastChanges();
    }

    private void restoreProcessingRatios() {
        for (Slot slot : getPatternCacheSlots()) {
            ItemStack stack = slot.getItem();
            var details = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
            if (!(details instanceof AEProcessingPattern process)) {
                continue;
            }
            var inputs = Arrays.asList(process.getSparseInputs());
            var outputs = Arrays.asList(process.getSparseOutputs());
            long gcd = PatternEncodingAmounts.sharedGcd(inputs, outputs);
            if (gcd <= 1L) {
                continue;
            }
            slot.set(PatternDetailsHelper.encodeProcessingPattern(
                    PatternEncodingAmounts.divideStacks(inputs, gcd).toArray(GenericStack[]::new),
                    PatternEncodingAmounts.divideStacks(outputs, gcd).toArray(GenericStack[]::new)));
        }
        broadcastChanges();
    }

    private void rotateProcessingOutputs() {
        for (Slot slot : getPatternCacheSlots()) {
            ItemStack stack = slot.getItem();
            var details = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
            if (!(details instanceof AEProcessingPattern process)) {
                continue;
            }
            slot.set(PatternDetailsHelper.encodeProcessingPattern(
                    process.getSparseInputs(),
                    PatternEncodingAmounts.rotateOutputs(process.getSparseOutputs())));
        }
        broadcastChanges();
    }

    private void changeCraftingSubstitution(boolean itemSubstitution, boolean value) {
        for (Slot slot : getPatternCacheSlots()) {
            ItemStack stack = slot.getItem();
            var details = PatternDetailsHelper.decodePattern(stack, getPlayer().level());
            if (!(details instanceof AECraftingPattern craft)) {
                continue;
            }
            var recipe = craftingRecipeFor(craft);
            if (recipe == null) {
                continue;
            }
            try {
                slot.set(PatternDetailsHelper.encodeCraftingPattern(
                        recipe,
                        itemize(craft.getSparseInputs()),
                        itemize(craft.getOutputs().length == 0 ? null : craft.getOutputs()[0]),
                        itemSubstitution ? value : craft.canSubstitute,
                        itemSubstitution ? craft.canSubstituteFluids : value));
            } catch (RuntimeException ignored) {
            }
        }
        broadcastChanges();
    }

    @Nullable
    private CraftingRecipe craftingRecipeFor(AECraftingPattern craft) {
        var items = itemize(craft.getSparseInputs());
        if (items.length != 9) {
            return null;
        }
        var container = new TransientCraftingContainer(new MestNullMenu(), 3, 3);
        for (int i = 0; i < items.length; i++) {
            container.setItem(i, items[i]);
        }
        return getPlayer().level().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, container, getPlayer().level())
                .orElse(null);
    }

    private static ItemStack[] itemize(GenericStack[] stacks) {
        var items = new ItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            items[i] = itemize(stacks[i]);
        }
        return items;
    }

    private static ItemStack itemize(@Nullable GenericStack stack) {
        if (stack != null && stack.what() instanceof AEItemKey itemKey) {
            return itemKey.toStack((int) stack.amount());
        }
        return ItemStack.EMPTY;
    }
}
