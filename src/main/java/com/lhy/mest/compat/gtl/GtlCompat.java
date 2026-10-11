package com.lhy.mest.compat.gtl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;

import com.lhy.mest.MESplicedterminal;

/**
 * GTL (GregTech Leisure) behaviour that gtlcore only patches into AE2's own pattern encoding
 * terminal, reapplied to the spliced terminal's encoder. gtlcore is looked up reflectively so the
 * mod builds and runs without it; every entry point is a no-op when it is absent.
 */
public final class GtlCompat {
    public static final String MOD_ID = "gtlcore";
    private static final String VIRTUAL_ENCODING = "org.gtlcore.gtlcore.integration.ae2.pattern.VirtualIngredientEncoding";

    private static Boolean loaded;
    private static MethodHandle notConsumedKeys;
    private static MethodHandle wrapIfNotConsumed;
    private static boolean lookupFailed;

    private GtlCompat() {
    }

    public static boolean loaded() {
        if (loaded == null) {
            loaded = ModList.get() != null && ModList.get().isLoaded(MOD_ID);
        }
        return loaded;
    }

    /** GTL's universal circuit stands in for every programmed circuit, so encoders prefer it. */
    public static boolean isUniversalCircuit(@Nullable AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return false;
        }
        var id = ForgeRegistries.ITEMS.getKey(itemKey.getItem());
        return id != null && id.getPath().contains("universal_circuit");
    }

    /**
     * gtlcore's encoder choice: among the candidates, already sorted best-first, a universal
     * circuit wins; otherwise the first candidate.
     */
    public static <T> T preferUniversalCircuit(List<T> rankedBestFirst, java.util.function.Function<T, AEKey> key) {
        if (loaded()) {
            for (T candidate : rankedBestFirst) {
                if (isUniversalCircuit(key.apply(candidate))) {
                    return candidate;
                }
            }
        }
        return rankedBestFirst.isEmpty() ? null : rankedBestFirst.get(0);
    }

    /**
     * Ctrl-transfer of a GT recipe: inputs the machine does not consume (circuits, molds, lenses)
     * are encoded as gtlcore's virtual ingredients, exactly like its AE2 encoder mixin does.
     */
    public static List<List<GenericStack>> virtualiseNotConsumedInputs(Object recipe,
            List<List<GenericStack>> inputs) {
        if (!loaded() || recipe == null || !lookup()) {
            return inputs;
        }
        try {
            @SuppressWarnings("unchecked")
            Set<AEKey> notConsumed = (Set<AEKey>) notConsumedKeys.invoke(recipe);
            if (notConsumed == null || notConsumed.isEmpty()) {
                return inputs;
            }
            List<List<GenericStack>> rewritten = new ArrayList<>(inputs.size());
            for (List<GenericStack> slot : inputs) {
                List<GenericStack> options = new ArrayList<>(slot.size());
                for (GenericStack option : slot) {
                    GenericStack wrapped = (GenericStack) wrapIfNotConsumed.invoke(option, notConsumed);
                    options.add(wrapped == null ? option : wrapped);
                }
                rewritten.add(options);
            }
            return rewritten;
        } catch (Throwable t) {
            disable("virtual ingredient encoding", t);
            return inputs;
        }
    }

    private static boolean lookup() {
        if (notConsumedKeys != null) {
            return true;
        }
        if (lookupFailed) {
            return false;
        }
        try {
            Class<?> owner = Class.forName(VIRTUAL_ENCODING);
            var lookup = MethodHandles.publicLookup();
            notConsumedKeys = lookup.findStatic(owner, "notConsumedKeys",
                    MethodType.methodType(Set.class, Object.class));
            wrapIfNotConsumed = lookup.findStatic(owner, "wrapIfNotConsumed",
                    MethodType.methodType(GenericStack.class, GenericStack.class, Set.class));
            return true;
        } catch (ClassNotFoundException e) {
            lookupFailed = true;
            MESplicedterminal.LOGGER.info("MEST: this gtlcore has no virtual ingredient encoding ({})", e.getMessage());
            return false;
        } catch (ReflectiveOperationException | LinkageError e) {
            disable("virtual ingredient encoding", e);
            return false;
        }
    }

    private static void disable(String feature, Throwable cause) {
        lookupFailed = true;
        notConsumedKeys = null;
        MESplicedterminal.LOGGER.warn("MEST: gtlcore {} is unavailable, using plain AE2 behaviour", feature, cause);
    }
}
