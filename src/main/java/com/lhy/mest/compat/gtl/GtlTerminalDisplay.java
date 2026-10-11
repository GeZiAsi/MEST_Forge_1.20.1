package com.lhy.mest.compat.gtl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.client.gui.me.common.Repo;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.Tooltips;
import appeng.menu.me.common.MEStorageMenu;

import com.lhy.mest.MESplicedterminal;

/**
 * GTL's beyond-{@code long} storage display. gtlcore sends {@code Long.MAX_VALUE} in AE2's normal
 * inventory packets and the exact {@link BigInteger} amounts through its own channel, but only once
 * a screen called {@code FastCellDisplayPackets.ready}; it does that from {@code MEStorageScreen},
 * which the spliced terminal is not. This mirrors its {@code FastCellMEStorageScreenMixin} for the
 * terminal's ME list. All gtlcore access is reflective; without gtlcore this does nothing.
 */
public final class GtlTerminalDisplay {
    private static final String PACKETS = "org.gtlcore.gtlcore.integration.ae2.wireless.FastCellDisplayPackets";
    private static final String DISPLAY_MENU = "org.gtlcore.gtlcore.integration.ae2.storage.PreciseDisplayMenu";
    private static final String REPO_AMOUNTS = "org.gtlcore.gtlcore.client.ae2.PreciseRepoAmounts";
    private static final String DISPLAY_TEXT = "org.gtlcore.gtlcore.client.ae2.FastCellDisplayText";

    private static MethodHandle ready;
    private static MethodHandle displayAmounts;
    private static MethodHandle setPreciseAmounts;
    private static MethodHandle createText;
    private static MethodHandle small;
    private static MethodHandle large;
    private static MethodHandle full;
    private static boolean available;
    private static boolean resolved;

    private final Map<AEKey, Object> texts = new HashMap<>();
    @Nullable
    private Map<?, ?> lastSnapshot;
    private boolean readySent;

    /** Once per frame before the ME list draws: request the exact amounts and refresh their text. */
    public void update(MEStorageMenu menu, Repo repo) {
        if (!GtlCompat.loaded() || !resolve()) {
            return;
        }
        try {
            if (!readySent) {
                readySent = true;
                ready.invoke(menu);
            }
            @SuppressWarnings("unchecked")
            Map<AEKey, BigInteger> snapshot = (Map<AEKey, BigInteger>) displayAmounts.invoke(menu);
            if (snapshot == null || snapshot == lastSnapshot) {
                return;
            }
            lastSnapshot = snapshot;
            texts.keySet().retainAll(snapshot.keySet());
            for (var entry : snapshot.entrySet()) {
                AEKey key = entry.getKey();
                texts.put(key, createText.invoke(entry.getValue(), key.getAmountPerUnit(),
                        key instanceof AEFluidKey ? "B" : ""));
            }
            setPreciseAmounts.invoke(repo, snapshot);
        } catch (Throwable t) {
            disable(t);
        }
    }

    /** Slot label: the exact amount when AE2's long saturated, otherwise AE2's own formatting. */
    public String formatAmount(AEKey key, long amount, AmountFormat format) {
        Object text = amount == Long.MAX_VALUE ? texts.get(key) : null;
        if (text != null) {
            try {
                return (String) (format == AmountFormat.SLOT_LARGE_FONT ? large.invoke(text) : small.invoke(text));
            } catch (Throwable t) {
                disable(t);
            }
        }
        return key.formatAmount(amount, format);
    }

    /** Tooltip line for a saturated amount, or {@code null} to keep AE2's own line. */
    @Nullable
    public Component amountTooltip(ButtonToolTips label, AEKey key, long amount) {
        Object text = amount == Long.MAX_VALUE ? texts.get(key) : null;
        if (text == null) {
            return null;
        }
        try {
            return label.text(full.invoke(text)).withStyle(Tooltips.MUTED_COLOR);
        } catch (Throwable t) {
            disable(t);
            return null;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return available;
        }
        resolved = true;
        try {
            var lookup = MethodHandles.publicLookup();
            Class<?> packets = Class.forName(PACKETS);
            Class<?> displayMenu = Class.forName(DISPLAY_MENU);
            Class<?> repoAmounts = Class.forName(REPO_AMOUNTS);
            Class<?> text = Class.forName(DISPLAY_TEXT);
            ready = lookup.findStatic(packets, "ready", MethodType.methodType(void.class, MEStorageMenu.class));
            displayAmounts = lookup.findVirtual(displayMenu, "gtlcore$displayAmounts", MethodType.methodType(Map.class));
            setPreciseAmounts = lookup.findVirtual(repoAmounts, "gtlcore$setPreciseAmounts",
                    MethodType.methodType(void.class, Map.class));
            createText = lookup.findStatic(text, "create",
                    MethodType.methodType(text, BigInteger.class, int.class, String.class));
            small = lookup.findVirtual(text, "small", MethodType.methodType(String.class));
            large = lookup.findVirtual(text, "large", MethodType.methodType(String.class));
            full = lookup.findVirtual(text, "full", MethodType.methodType(String.class));
            available = true;
        } catch (ClassNotFoundException e) {
            // Older gtlcore (e.g. 1.2.2.x) has no precise terminal amounts at all.
            MESplicedterminal.LOGGER.info("MEST: this gtlcore has no precise terminal amounts ({})", e.getMessage());
            available = false;
        } catch (ReflectiveOperationException | LinkageError e) {
            MESplicedterminal.LOGGER.warn("MEST: gtlcore precise terminal amounts unavailable", e);
            available = false;
        }
        return available;
    }

    private static void disable(Throwable cause) {
        if (available) {
            MESplicedterminal.LOGGER.warn("MEST: disabling gtlcore precise terminal amounts", cause);
        }
        available = false;
    }
}
