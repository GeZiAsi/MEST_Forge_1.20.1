package com.lhy.mest.client.dock;

import com.lhy.mest.client.wt.Icon;

/** Upgrade-column background pieces (AE2WTLib 15 has no scrolling upgrade panel of its own). */
record UpgradeBackground(Icon top, Icon middle, Icon bottom) {
    private static final UpgradeBackground SCROLLING = new UpgradeBackground(Icon.UPGRADE_BACKGROUND_SCROLLING_TOP,
            Icon.UPGRADE_BACKGROUND_SCROLLING_MIDDLE, Icon.UPGRADE_BACKGROUND_SCROLLING_BOTTOM);
    private static final UpgradeBackground FIXED = new UpgradeBackground(Icon.UPGRADE_BACKGROUND_TOP,
            Icon.UPGRADE_BACKGROUND_MIDDLE, Icon.UPGRADE_BACKGROUND_BOTTOM);

    static UpgradeBackground get(boolean scrolling) {
        return scrolling ? SCROLLING : FIXED;
    }
}
