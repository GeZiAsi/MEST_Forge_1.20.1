package com.lhy.mestdevtest;

import java.util.List;

import appeng.parts.encoding.EncodingMode;

import com.lhy.mest.client.MESTScreen;

/** The scripted checks run after the terminal opened. Extend as needed while debugging. */
final class DevTestSteps {
    private DevTestSteps() {
    }

    static void register(List<MestDevTest.Step> steps) {
        steps.add(new MestDevTest.Step("describe screen", 10, null, mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                var menu = screen.getMenu();
                MestDevTest.log("menu slots={} powered={} encodingMode={} repoEntries={}",
                        menu.slots.size(), menu.isPowered(), menu.getPatternEncodingMode(),
                        menu.getClientRepo() == null ? -1 : menu.getClientRepo().getAllEntries().size());
                var repo = screen.meListPanel().repo();
                MestDevTest.log("repo view size={} paused={} search='{}' sort={} view={} type={} first={}",
                        repo.size(), repo.isPaused(), repo.getSearchString(),
                        screen.meListPanel().getSortBy(), screen.meListPanel().getSortDisplay(),
                        screen.meListPanel().getTypeFilter(), repo.size() > 0 ? repo.get(0) : null);
            }
        }));
        steps.add(new MestDevTest.Step("processing mode", 30, "mest_02_processing", mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                screen.getMenu().setPatternEncodingMode(EncodingMode.PROCESSING);
            }
        }));
        steps.add(new MestDevTest.Step("search iron", 30, "mest_03_search", mc -> {
            if (mc.screen instanceof MESTScreen screen) {
                screen.getMenu().setPatternEncodingMode(EncodingMode.CRAFTING);
                screen.meListPanel().setSearchValue("iron");
            }
        }));
    }
}
