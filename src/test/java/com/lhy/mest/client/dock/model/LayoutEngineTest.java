package com.lhy.mest.client.dock.model;

import com.lhy.mest.util.MestLists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

class LayoutEngineTest {
    @Test
    void recursivelyArrangesVisibleBranchesAndCollapsesHiddenOnes() {
        ModuleCatalog catalog = ModelTestFixtures.catalog("a", "b", "c");
        LayoutNode nested = new SplitNode(
                "split-outer",
                DockAxis.HORIZONTAL,
                0.4,
                new LeafNode("leaf-a", "a", true),
                new SplitNode(
                        "split-inner",
                        DockAxis.VERTICAL,
                        0.5,
                        new LeafNode("leaf-b", "b", true),
                        new LeafNode("leaf-c", "c", true)));
        var policies = new LinkedHashMap<String, ModuleLayoutPolicy>();
        policies.put("a", ModuleLayoutPolicy.defaults());
        policies.put("b", new ModuleLayoutPolicy(false, true, true));
        policies.put("c", ModuleLayoutPolicy.defaults());
        DockWorkspace workspace = new DockWorkspace(List.of(
                new FloatingRoot("root-main", new DockRect(0, 0, 200, 100), nested)), policies);

        LayoutProjection projection = new LayoutEngine(
                catalog,
                new LayoutStyle(DockInsets.NONE, 4)).project(workspace);

        // Both sides get their 80px preferred width; the 36px of slack is split 0.4 / 0.6.
        assertEquals(new DockRect(0, 0, 94, 100), projection.visibleLeaf("leaf-a").orElseThrow().bounds());
        assertEquals(new DockRect(98, 0, 102, 100), projection.visibleLeaf("leaf-c").orElseThrow().bounds());
        assertTrue(projection.visibleLeaf("leaf-b").isEmpty());
        assertTrue(projection.isEffectivelyVisible("split-inner"));
        assertFalse(projection.isEffectivelyVisible("leaf-b"));
        assertEquals(1, projection.dividers().size(), "collapsed split must not emit a divider");
    }

    @Test
    void preferredWidthsComeFirstAndOnlySlackFollowsTheRatio() {
        var metrics = new LinkedHashMap<String, ModuleMetrics>();
        metrics.put("wide", new ModuleMetrics(new DockSize(70, 20), new DockSize(70, 20)));
        metrics.put("narrow", new ModuleMetrics(new DockSize(20, 20), new DockSize(20, 20)));
        ModuleCatalog catalog = new ModuleCatalog(metrics);
        LayoutNode split = new SplitNode(
                "split-main",
                DockAxis.HORIZONTAL,
                0.1,
                new LeafNode("leaf-wide", "wide", true),
                new LeafNode("leaf-narrow", "narrow", true));
        DockWorkspace workspace = new DockWorkspace(List.of(
                new FloatingRoot("root-main", new DockRect(0, 0, 100, 40), split)));

        LayoutProjection projection = new LayoutEngine(
                catalog,
                new LayoutStyle(DockInsets.NONE, 4)).project(workspace);

        // A 0.1 ratio cannot squeeze "wide" below its preferred 70px; it only gets 10% of the slack.
        assertEquals(71, projection.visibleLeaf("leaf-wide").orElseThrow().bounds().width());
        assertEquals(25, projection.visibleLeaf("leaf-narrow").orElseThrow().bounds().width());
        assertEquals(new DockRect(71, 0, 4, 40), MestLists.first(projection.dividers()).bounds());
    }

    @Test
    void retainsRootAndLeafPaintOrder() {
        ModuleCatalog catalog = ModelTestFixtures.catalog("a", "b", "c");
        DockWorkspace workspace = new DockWorkspace(List.of(
                ModelTestFixtures.root("root-c", "leaf-c", "c", 0),
                ModelTestFixtures.root("root-a", "leaf-a", "a", 20),
                ModelTestFixtures.root("root-b", "leaf-b", "b", 40)));

        LayoutProjection projection = new LayoutEngine(
                catalog,
                new LayoutStyle(DockInsets.NONE, 4)).project(workspace);

        assertEquals(
                List.of("root-c", "root-a", "root-b"),
                projection.roots().stream().map(LayoutProjection.RootPlacement::rootId).toList());
        assertEquals(
                List.of("c", "a", "b"),
                projection.visibleLeavesInPaintOrder().stream()
                        .map(LayoutProjection.LeafPlacement::moduleId)
                        .toList());
    }

    @Test
    void splicedWindowFillsTheRootAsARectangleAndGivesLeftoverToTheExpandingLeaf() {
        var metrics = new LinkedHashMap<String, ModuleMetrics>();
        metrics.put("fixed", new ModuleMetrics(new DockSize(50, 40), new DockSize(80, 60)));
        metrics.put("flex", new ModuleMetrics(new DockSize(40, 30), new DockSize(90, 80), true));
        ModuleCatalog catalog = new ModuleCatalog(metrics);
        LayoutNode split = new SplitNode(
                "split-main",
                DockAxis.VERTICAL,
                0.5,
                new LeafNode("leaf-fixed", "fixed", true),
                new LeafNode("leaf-flex", "flex", true));
        DockWorkspace workspace = new DockWorkspace(List.of(
                new FloatingRoot("root-main", new DockRect(0, 0, 200, 200), split)));

        LayoutProjection projection = new LayoutEngine(catalog, new LayoutStyle(DockInsets.NONE, 0))
                .project(workspace);

        DockRect fixed = projection.visibleLeaf("leaf-fixed").orElseThrow().bounds();
        DockRect flex = projection.visibleLeaf("leaf-flex").orElseThrow().bounds();
        assertEquals(new DockRect(0, 0, 200, 60), fixed);
        assertEquals(new DockRect(0, 60, 200, 140), flex);
        assertEquals(fixed.right(), flex.right());
        assertEquals(fixed.bottom(), flex.y());
        assertEquals(200, flex.bottom());
        assertEquals(1, projection.dividers().size());
    }

    @Test
    void splicedWindowKeepsANestedRowRectangularInsteadOfLShaped() {
        var metrics = new LinkedHashMap<String, ModuleMetrics>();
        metrics.put("crafting", new ModuleMetrics(new DockSize(100, 60), new DockSize(200, 80)));
        metrics.put("inventory", new ModuleMetrics(new DockSize(80, 60), new DockSize(100, 100)));
        metrics.put("access", new ModuleMetrics(new DockSize(60, 60), new DockSize(150, 100)));
        metrics.put("encoding", new ModuleMetrics(new DockSize(80, 60), new DockSize(120, 80)));
        ModuleCatalog catalog = new ModuleCatalog(metrics);
        LayoutNode left = new SplitNode(
                "split-left",
                DockAxis.VERTICAL,
                0.5,
                new LeafNode("leaf-crafting", "crafting", true),
                new SplitNode(
                        "split-lower",
                        DockAxis.HORIZONTAL,
                        0.5,
                        new LeafNode("leaf-inventory", "inventory", true),
                        new LeafNode("leaf-access", "access", true)));
        LayoutNode rootNode = new SplitNode(
                "split-root",
                DockAxis.HORIZONTAL,
                0.5,
                left,
                new LeafNode("leaf-encoding", "encoding", true));
        DockWorkspace workspace = new DockWorkspace(List.of(
                new FloatingRoot("root-main", new DockRect(0, 0, 450, 180), rootNode)));

        LayoutProjection projection = new LayoutEngine(catalog, new LayoutStyle(DockInsets.NONE, 0))
                .project(workspace);

        DockRect crafting = projection.visibleLeaf("leaf-crafting").orElseThrow().bounds();
        DockRect inventory = projection.visibleLeaf("leaf-inventory").orElseThrow().bounds();
        DockRect access = projection.visibleLeaf("leaf-access").orElseThrow().bounds();
        DockRect encoding = projection.visibleLeaf("leaf-encoding").orElseThrow().bounds();
        assertEquals(crafting.right(), encoding.x());
        assertEquals(inventory.right(), access.x());
        assertEquals(crafting.bottom(), inventory.y());
        assertEquals(inventory.y(), access.y());
        assertEquals(inventory.bottom(), access.bottom());
        assertEquals(encoding.bottom(), inventory.bottom());
        assertEquals(0, crafting.x());
        assertEquals(0, crafting.y());
        assertEquals(450, encoding.right());
        assertEquals(180, encoding.bottom());
    }
}
