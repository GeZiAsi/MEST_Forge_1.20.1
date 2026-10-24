package com.lhy.mest.client;

/** Pure interaction decisions shared by the docked ME list and its unit tests. */
public final class MEListInteractionPolicy {
    public enum ContainerAction {
        EMPTY_ONE,
        EMPTY_ALL
    }

    public enum ScrollAction {
        INSERT_ONE,
        EXTRACT_ONE
    }

    private MEListInteractionPolicy() {
    }

    public static ContainerAction emptyContainerAction(boolean quickMove) {
        return quickMove ? ContainerAction.EMPTY_ALL : ContainerAction.EMPTY_ONE;
    }

    public static boolean shouldCraftOnClick(boolean viewOnlyCraftable, long storedAmount, boolean craftable) {
        if (viewOnlyCraftable) {
            return true;
        }
        return storedAmount == 0 && craftable;
    }

    public static ScrollAction scrollAction(double scrollY) {
        if (scrollY == 0) {
            throw new IllegalArgumentException("scrollY must be non-zero");
        }
        return scrollY > 0 ? ScrollAction.INSERT_ONE : ScrollAction.EXTRACT_ONE;
    }

    public static int scrollSteps(double scrollY) {
        return (int) Math.abs(scrollY);
    }
}
