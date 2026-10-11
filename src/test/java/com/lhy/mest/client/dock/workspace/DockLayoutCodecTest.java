package com.lhy.mest.client.dock.workspace;

import com.lhy.mest.util.MestLists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lhy.mest.client.dock.model.DockRect;
import com.lhy.mest.client.dock.model.LeafNode;
import com.lhy.mest.client.dock.model.ModuleLayoutPolicy;
import com.lhy.mest.client.dock.model.SplitNode;

class DockLayoutCodecTest {
    @Test
    void roundTripsCurrentVersionWithoutLosingRootOrderTreeStateOrPolicies() throws Exception {
        DockLayoutCodec codec = codec();
        var original = WorkspacePersistenceFixtures.workspace();

        String encoded = codec.encode(original);
        DockLayoutCodec.DecodedLayout decoded = codec.decode(encoded);

        assertEquals(DockLayoutDto.CURRENT_VERSION, decoded.sourceVersion());
        assertFalse(decoded.migrated());
        assertFalse(decoded.reconciled());
        assertFalse(decoded.needsRewrite());
        assertEquals(original, decoded.workspace());
        assertTrue(encoded.indexOf("root-a") < encoded.indexOf("root-bc"));
    }

    @Test
    void roundTripsPerModuleRuntimePolicy() throws Exception {
        var base = WorkspacePersistenceFixtures.workspace();
        var policies = new java.util.LinkedHashMap<>(base.policies());
        policies.put("a", new ModuleLayoutPolicy(true, false, true));
        var configured = new com.lhy.mest.client.dock.model.DockWorkspace(base.roots(), policies);

        var decoded = codec().decode(codec().encode(configured)).workspace();

        assertEquals(new ModuleLayoutPolicy(true, false, true), decoded.policyFor("a"));
        assertEquals(ModuleLayoutPolicy.defaults(), decoded.policyFor("b"));
    }

    @Test
    void roundTripsRestoreSizes() throws Exception {
        var base = WorkspacePersistenceFixtures.workspace();
        var restoreSizes = new java.util.LinkedHashMap<String, com.lhy.mest.client.dock.model.DockSize>();
        restoreSizes.put("leaf-a", new com.lhy.mest.client.dock.model.DockSize(100, 80));
        restoreSizes.put("leaf-b", new com.lhy.mest.client.dock.model.DockSize(90, 60));
        var original = new com.lhy.mest.client.dock.model.DockWorkspace(
                base.roots(), base.policies(), restoreSizes);

        String encoded = codec().encode(original);
        var decoded = codec().decode(encoded).workspace();

        assertEquals(original.restoreSizes(), decoded.restoreSizes());
        assertTrue(encoded.contains("restoreSizes"));
        assertFalse(codec().decode(encoded).needsRewrite());
    }

    @Test
    void roundTripsWindowModes() throws Exception {
        var base = WorkspacePersistenceFixtures.workspace();
        var roots = new java.util.ArrayList<>(base.roots());
        roots.set(0, MestLists.first(roots).withMode(true, true));
        var original = base.withRoots(roots);

        String encoded = codec().encode(original);
        var decoded = codec().decode(encoded);

        assertTrue(MestLists.first(decoded.workspace().roots()).floating());
        assertTrue(MestLists.first(decoded.workspace().roots()).pinned());
        assertFalse(MestLists.last(decoded.workspace().roots()).floating());
        assertFalse(decoded.needsRewrite());
    }

    @Test
    void roundTripsCombinedModulesAndDropsDuplicateSlots() throws Exception {
        var base = WorkspacePersistenceFixtures.workspace();
        var roots = new java.util.ArrayList<>(base.roots());
        roots.set(0, MestLists.first(roots).withMode(true, false).withGroup(2).withHidden(true));
        String encoded = codec().encode(base.withRoots(roots));

        var decoded = MestLists.first(codec().decode(encoded).workspace().roots());
        assertEquals(2, decoded.group());
        assertTrue(decoded.hidden());

        int last = encoded.lastIndexOf("\"rootId\"");
        String duplicated = encoded.substring(0, last)
                + "\"group\": 2, \"floating\": true, " + encoded.substring(last);
        var twice = codec().decode(duplicated).workspace().roots();
        assertEquals(1, twice.stream().filter(root -> root.group() == 2).count());
    }

    @Test
    void olderLayoutsTakeTheirWindowModeFromTheModules() throws Exception {
        String persisted = """
                {
                  "version": 6,
                  "roots": [{
                    "rootId": "root-a",
                    "bounds": {"x": 0, "y": 0, "width": 100, "height": 80},
                    "content": {"type": "leaf", "nodeId": "leaf-a", "moduleId": "a", "visible": true}
                  }, {
                    "rootId": "root-bc",
                    "bounds": {"x": 160, "y": 40, "width": 220, "height": 140},
                    "content": {
                      "type": "split", "nodeId": "split-bc", "axis": "VERTICAL", "ratio": 0.35,
                      "first": {"type": "leaf", "nodeId": "leaf-b", "moduleId": "b", "visible": true},
                      "second": {"type": "leaf", "nodeId": "leaf-c", "moduleId": "c", "visible": true}
                    }
                  }],
                  "policies": {
                    "a": {"visible": true, "movable": true, "resizable": true, "floating": false, "pinned": false, "showTerminalButton": true},
                    "b": {"visible": true, "movable": true, "resizable": true, "floating": true, "pinned": true, "showTerminalButton": true},
                    "c": {"visible": false, "movable": true, "resizable": true, "floating": false, "pinned": false, "showTerminalButton": true}
                  }
                }
                """;

        var decoded = codec().decode(persisted);

        assertFalse(MestLists.first(decoded.workspace().roots()).floating());
        // "c" is hidden, so only the visible, pinned "b" decides the window's mode.
        assertTrue(MestLists.last(decoded.workspace().roots()).floating());
        assertTrue(MestLists.last(decoded.workspace().roots()).pinned());
        assertTrue(decoded.needsRewrite());
    }

    @Test
    void noLongerWritesASpliceMode() throws Exception {
        String encoded = codec().encode(WorkspacePersistenceFixtures.workspace());

        assertFalse(encoded.contains("spliceMode"));
        assertFalse(encoded.contains("compactSplice"));
        assertFalse(codec().decode(encoded).needsRewrite());
    }

    @Test
    void roundTripsContentOffsets() throws Exception {
        var original = WorkspacePersistenceFixtures.workspace().withContentOffsets(java.util.Map.of(
                "a", new com.lhy.mest.client.dock.model.ContentOffset(6, 2)));

        String encoded = codec().encode(original);
        var decoded = codec().decode(encoded).workspace();

        assertEquals(original.contentOffsets(), decoded.contentOffsets());
        assertTrue(encoded.contains("contentOffsets"));
        assertEquals(6, decoded.contentOffset("a").x());
        assertEquals(2, decoded.contentOffset("a").y());
        assertEquals(com.lhy.mest.client.dock.model.ContentOffset.ZERO, decoded.contentOffset("b"));
    }

    @Test
    void missingContentOffsetsDecodeAsEmpty() throws Exception {
        var original = WorkspacePersistenceFixtures.workspace();
        String encoded = codec().encode(original);
        assertFalse(encoded.contains("contentOffsets"));
        assertTrue(codec().decode(encoded).workspace().contentOffsets().isEmpty());
    }

    @Test
    void fitsOnlyLayoutsSavedOutsideTheShellMode() throws Exception {
        String stretch = legacySplicedDocument("");
        String compact = legacySplicedDocument(", \"compactSplice\": true");
        String shell = legacySplicedDocument(", \"spliceMode\": \"shell\"");
        var fitted = new java.util.ArrayList<com.lhy.mest.client.dock.model.DockWorkspace>();
        var codec = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog(),
                WorkspacePersistenceFixtures.migrationContext(),
                workspace -> {
                    fitted.add(workspace);
                    return workspace;
                });

        codec.decode(stretch);
        codec.decode(compact);
        assertEquals(2, fitted.size());

        codec.decode(shell);
        var current = codec.decode(codec.encode(MestLists.first(fitted)));
        assertEquals(2, fitted.size(), "shell and current-version documents keep their window sizes");
        assertFalse(current.needsRewrite());
        assertTrue(codec.decode(stretch).needsRewrite(), "legacy documents are rewritten as v6");
    }

    private static String legacySplicedDocument(String modeField) {
        return """
                {
                  "version": 5,
                  "roots": [{
                    "rootId": "root-a",
                    "bounds": {"x": 0, "y": 0, "width": 100, "height": 80},
                    "content": {
                      "type": "leaf", "nodeId": "leaf-a", "moduleId": "a", "visible": true
                    }
                  }, {
                    "rootId": "root-bc",
                    "bounds": {"x": 160, "y": 40, "width": 220, "height": 140},
                    "content": {
                      "type": "split", "nodeId": "split-bc", "axis": "VERTICAL", "ratio": 0.35,
                      "first": {"type": "leaf", "nodeId": "leaf-b", "moduleId": "b", "visible": true},
                      "second": {"type": "leaf", "nodeId": "leaf-c", "moduleId": "c", "visible": true}
                    }
                  }],
                  "policies": {
                    "a": {"visible": true, "movable": true, "resizable": true, "floating": false, "pinned": false, "showTerminalButton": true},
                    "b": {"visible": true, "movable": true, "resizable": true, "floating": false, "pinned": false, "showTerminalButton": true},
                    "c": {"visible": true, "movable": true, "resizable": true, "floating": false, "pinned": false, "showTerminalButton": true}
                  }%s
                }
                """.formatted(modeField);
    }

    @Test
    void migratesV2LeafVisibilityIntoV3ModulePolicy() throws Exception {
        String persisted = """
                {
                  "version": 2,
                  "roots": [{
                    "rootId": "root-a",
                    "bounds": {"x": 0, "y": 0, "width": 100, "height": 80},
                    "content": {
                      "type": "leaf", "nodeId": "leaf-a", "moduleId": "a", "visible": false
                    }
                  }]
                }
                """;

        DockLayoutCodec.DecodedLayout decoded = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("a"),
                WorkspacePersistenceFixtures.migrationContext()).decode(persisted);
        LeafNode leaf = assertInstanceOf(LeafNode.class, MestLists.first(decoded.workspace().roots()).content());

        assertEquals(2, decoded.sourceVersion());
        assertTrue(decoded.needsRewrite());
        assertTrue(leaf.visible(), "v3 normalizes structural leaf visibility");
        assertFalse(decoded.workspace().policyFor("a").visible());
    }

    @Test
    void migratesOlderLayoutsToTheCurrentNetworkToolkitPlacement() throws Exception {
        String persisted = """
                {
                  "version": 3,
                  "roots": [{
                    "rootId": "root-nt",
                    "bounds": {"x": 501, "y": 284, "width": 71, "height": 66},
                    "content": {
                      "type": "leaf", "nodeId": "leaf-nt", "moduleId": "network_toolkit",
                      "visible": true
                    }
                  }],
                  "policies": {
                    "network_toolkit": {"visible": true, "movable": true, "resizable": true,
                                        "floating": true, "pinned": true, "showTerminalButton": true}
                  }
                }
                """;
        DockLayoutCodec codec = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("network_toolkit"),
                WorkspacePersistenceFixtures.migrationContext());

        DockLayoutCodec.DecodedLayout decoded = codec.decode(persisted);

        assertEquals(3, decoded.sourceVersion());
        assertTrue(decoded.needsRewrite());
        DockRect migrated = MestLists.first(decoded.workspace().roots()).bounds();
        assertEquals(501 - DockWorkspaceDefaults.NETWORK_TOOLKIT_SHIFT_X, migrated.x());
        assertEquals(284, migrated.y());
        assertEquals(71, migrated.width());
        assertEquals(66, migrated.height());

        // The migrated position is persisted, so the shift is only ever applied once.
        DockLayoutCodec.DecodedLayout again = codec.decode(codec.encode(decoded.workspace()));
        assertFalse(again.needsRewrite());
        assertEquals(migrated, MestLists.first(again.workspace().roots()).bounds());
    }

    @Test
    void leavesASplicedNetworkToolkitWindowWhereThePlayerPutIt() throws Exception {
        String persisted = """
                {
                  "version": 3,
                  "roots": [{
                    "rootId": "root-mixed",
                    "bounds": {"x": 501, "y": 284, "width": 160, "height": 66},
                    "content": {
                      "type": "split", "nodeId": "split-mixed", "axis": "HORIZONTAL", "ratio": 0.5,
                      "first": {"type": "leaf", "nodeId": "leaf-nt", "moduleId": "network_toolkit", "visible": true},
                      "second": {"type": "leaf", "nodeId": "leaf-a", "moduleId": "a", "visible": true}
                    }
                  }],
                  "policies": {
                    "network_toolkit": {"visible": true, "movable": true, "resizable": true,
                                        "floating": false, "pinned": false, "showTerminalButton": true},
                    "a": {"visible": true, "movable": true, "resizable": true,
                          "floating": false, "pinned": false, "showTerminalButton": true}
                  }
                }
                """;
        DockLayoutCodec codec = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("network_toolkit", "a"),
                WorkspacePersistenceFixtures.migrationContext());

        DockRect bounds = MestLists.first(codec.decode(persisted).workspace().roots()).bounds();

        assertEquals(501, bounds.x());
    }

    @Test
    void v3PolicyIsTheSingleVisibilitySource() throws Exception {
        String persisted = """
                {
                  "version": 3,
                  "roots": [{
                    "rootId": "root-a",
                    "bounds": {"x": 0, "y": 0, "width": 100, "height": 80},
                    "content": {
                      "type": "leaf", "nodeId": "leaf-a", "moduleId": "a", "visible": false
                    }
                  }],
                  "policies": {
                    "a": {"visible": true, "movable": false, "resizable": true}
                  }
                }
                """;

        DockLayoutCodec.DecodedLayout decoded = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("a"),
                WorkspacePersistenceFixtures.migrationContext()).decode(persisted);
        LeafNode leaf = assertInstanceOf(LeafNode.class, MestLists.first(decoded.workspace().roots()).content());

        assertTrue(decoded.needsRewrite());
        assertTrue(leaf.visible());
        assertEquals(new ModuleLayoutPolicy(true, false, true), decoded.workspace().policyFor("a"));
    }

    @Test
    void migratesLegacyPairsDeterministicallyAndPreservesFileOrder() throws Exception {
        String legacy = """
                {
                  "b+c": {
                    "x": 20,
                    "y": 30,
                    "width": 120,
                    "height": 104,
                    "visible": true,
                    "orientation": "VERTICAL",
                    "split": 40,
                    "children": ["b", "c"]
                  },
                  "a": {
                    "x": 4,
                    "y": 5,
                    "width": 80,
                    "height": 60,
                    "visible": false
                  }
                }
                """;
        DockLayoutCodec codec = codec();

        var first = codec.decode(legacy);
        var second = codec.decode(legacy);

        assertEquals(1, first.sourceVersion());
        assertTrue(first.migrated());
        assertEquals(first.workspace(), second.workspace());
        SplitNode pair = assertInstanceOf(SplitNode.class, MestLists.first(first.workspace().roots()).content());
        assertEquals(0.4, pair.ratio(), 0.000001);
        assertEquals("b", ((LeafNode) pair.first()).moduleId());
        assertEquals("c", ((LeafNode) pair.second()).moduleId());
        LeafNode standalone = assertInstanceOf(
                LeafNode.class,
                first.workspace().roots().get(1).content());
        assertEquals("a", standalone.moduleId());
        assertTrue(standalone.visible());
        assertFalse(first.workspace().policyFor("a").visible());

        assertEquals(first.workspace(), codec.decode(codec.encode(first.workspace())).workspace());
    }

    @Test
    void appendsRegisteredModulesMissingFromLegacyDocument() throws Exception {
        String legacy = """
                {
                  "a": {"x": 4, "y": 5, "width": 80, "height": 60, "visible": true},
                  "b": {"x": 20, "y": 20, "width": 90, "height": 60, "visible": true}
                }
                """;

        var decoded = codec().decode(legacy).workspace();

        assertEquals(3, decoded.roots().size());
        assertEquals("c", ((LeafNode) MestLists.last(decoded.roots()).content()).moduleId());
        assertTrue(((LeafNode) MestLists.last(decoded.roots()).content()).visible());
    }

    @Test
    void rejectsDuplicateLegacyModulesAndUnknownV2Fields() {
        String duplicateLegacy = """
                {
                  "a+b": {
                    "x": 0, "y": 0, "width": 120, "height": 80,
                    "orientation": "HORIZONTAL", "split": 40, "children": ["a", "b"]
                  },
                  "a": {"x": 0, "y": 0, "width": 80, "height": 60}
                }
                """;
        String unknownV2Field = """
                {"version": 2, "roots": [], "extra": true}
                """;

        assertThrows(DockLayoutFormatException.class, () -> codec().decode(duplicateLegacy));
        assertThrows(DockLayoutFormatException.class, () -> codec().decode(unknownV2Field));
    }

    @Test
    void rejectsUnsupportedVersion() {
        assertThrows(
                DockLayoutFormatException.class,
                () -> codec().decode("{\"version\":4,\"roots\":[]}"));
    }

    @Test
    void reconcilesAddedAndRemovedModulesWithoutDiscardingRetainedRoots() throws Exception {
        String persisted = """
                {
                  "version": 2,
                  "roots": [
                    {
                      "rootId": "root-a",
                      "bounds": {"x": 10, "y": 20, "width": 100, "height": 80},
                      "content": {
                        "type": "leaf", "nodeId": "leaf-a", "moduleId": "a", "visible": false
                      }
                    },
                    {
                      "rootId": "root-old",
                      "bounds": {"x": 120, "y": 20, "width": 100, "height": 80},
                      "content": {
                        "type": "leaf", "nodeId": "leaf-old", "moduleId": "old_name", "visible": true
                      }
                    }
                  ]
                }
                """;

        DockLayoutCodec.DecodedLayout decoded = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("a", "new_name"),
                WorkspacePersistenceFixtures.migrationContext()).decode(persisted);

        assertTrue(decoded.reconciled());
        assertTrue(decoded.needsRewrite());
        assertEquals(2, decoded.workspace().roots().size());
        LeafNode retained = assertInstanceOf(
                LeafNode.class,
                MestLists.first(decoded.workspace().roots()).content());
        assertEquals("a", retained.moduleId());
        assertTrue(retained.visible());
        assertFalse(decoded.workspace().policyFor("a").visible());
        LeafNode added = assertInstanceOf(
                LeafNode.class,
                MestLists.last(decoded.workspace().roots()).content());
        assertEquals("new_name", added.moduleId());
        assertTrue(added.visible());
    }

    @Test
    void collapsesSplitsWhenOnlyOneSideReferencesADeletedModule() throws Exception {
        String persisted = """
                {
                  "version": 2,
                  "roots": [{
                    "rootId": "root-main",
                    "bounds": {"x": 0, "y": 0, "width": 200, "height": 100},
                    "content": {
                      "type": "split", "nodeId": "split-main", "axis": "HORIZONTAL",
                      "ratio": 0.5,
                      "first": {"type": "leaf", "nodeId": "leaf-old", "moduleId": "removed", "visible": true},
                      "second": {"type": "leaf", "nodeId": "leaf-a", "moduleId": "a", "visible": true}
                    }
                  }]
                }
                """;

        DockLayoutCodec.DecodedLayout decoded = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("a"),
                WorkspacePersistenceFixtures.migrationContext()).decode(persisted);

        DockWorkspaceCodecAssertions.assertSingleLeaf(decoded.workspace(), "a");
        assertEquals("root-main", MestLists.first(decoded.workspace().roots()).rootId());
        assertEquals(200, MestLists.first(decoded.workspace().roots()).bounds().width());
        assertEquals(100, MestLists.first(decoded.workspace().roots()).bounds().height());
        assertTrue(decoded.reconciled());
        assertTrue(decoded.needsRewrite());
    }

    @Test
    void renamedCatalogIdsFallBackToDefaultsInsteadOfMakingTheDocumentInvalid() throws Exception {
        String persisted = """
                {
                  "version": 2,
                  "roots": [{
                    "rootId": "root-old",
                    "bounds": {"x": 0, "y": 0, "width": 200, "height": 100},
                    "content": {
                      "type": "leaf", "nodeId": "leaf-old", "moduleId": "old_id", "visible": false
                    }
                  }]
                }
                """;

        DockLayoutCodec.DecodedLayout decoded = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("new_id"),
                WorkspacePersistenceFixtures.migrationContext()).decode(persisted);
        assertEquals(1, decoded.workspace().roots().size());
        LeafNode leaf = assertInstanceOf(LeafNode.class, MestLists.first(decoded.workspace().roots()).content());
        assertEquals("new_id", leaf.moduleId());
        assertTrue(leaf.visible());
    }

    @Test
    void rejectsDuplicateModuleAndIdentifierStateBeforeReconciliation() {
        String duplicateModule = """
                {
                  "version": 2,
                  "roots": [
                    {
                      "rootId": "root-a",
                      "bounds": {"x": 0, "y": 0, "width": 100, "height": 80},
                      "content": {
                        "type": "leaf", "nodeId": "leaf-a-1", "moduleId": "a", "visible": true
                      }
                    },
                    {
                      "rootId": "root-a-duplicate-module",
                      "bounds": {"x": 110, "y": 0, "width": 100, "height": 80},
                      "content": {
                        "type": "leaf", "nodeId": "leaf-a-2", "moduleId": "a", "visible": true
                      }
                    }
                  ]
                }
                """;
        String duplicateIdentifier = """
                {
                  "version": 2,
                  "roots": [{
                    "rootId": "same-id",
                    "bounds": {"x": 0, "y": 0, "width": 200, "height": 100},
                    "content": {
                      "type": "split", "nodeId": "split-main", "axis": "HORIZONTAL",
                      "ratio": 0.5,
                      "first": {
                        "type": "leaf", "nodeId": "same-id", "moduleId": "a", "visible": true
                      },
                      "second": {
                        "type": "leaf", "nodeId": "leaf-b", "moduleId": "b", "visible": true
                      }
                    }
                  }]
                }
                """;

        assertThrows(DockLayoutFormatException.class, () -> codec().decode(duplicateModule));
        assertThrows(DockLayoutFormatException.class, () -> codec().decode(duplicateIdentifier));
    }

    @Test
    void allocatesCollisionFreeIdentifiersForNewDefaultRoots() throws Exception {
        String preferredNewRootId = com.lhy.mest.client.dock.workspace.DockWorkspaceDefaults.rootId("new");
        String preferredNewLeafId = com.lhy.mest.client.dock.workspace.DockWorkspaceDefaults.leafNodeId("new");
        String persisted = """
                {
                  "version": 2,
                  "roots": [{
                    "rootId": "%s",
                    "bounds": {"x": 7, "y": 9, "width": 100, "height": 80},
                    "content": {
                      "type": "leaf", "nodeId": "%s", "moduleId": "a", "visible": true
                    }
                  }]
                }
                """.formatted(preferredNewRootId, preferredNewLeafId);

        DockLayoutCodec.DecodedLayout decoded = new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog("a", "new"),
                WorkspacePersistenceFixtures.migrationContext()).decode(persisted);

        assertEquals(2, decoded.workspace().roots().size());
        assertEquals(preferredNewRootId, MestLists.first(decoded.workspace().roots()).rootId());
        assertEquals(preferredNewLeafId, MestLists.first(decoded.workspace().roots()).content().nodeId());
        assertFalse(preferredNewRootId.equals(MestLists.last(decoded.workspace().roots()).rootId()));
        assertFalse(preferredNewLeafId.equals(MestLists.last(decoded.workspace().roots()).content().nodeId()));
        assertTrue(decoded.reconciled());
    }

    private static final class DockWorkspaceCodecAssertions {
        private DockWorkspaceCodecAssertions() {
        }

        static void assertSingleLeaf(com.lhy.mest.client.dock.model.DockWorkspace workspace, String moduleId) {
            assertEquals(1, workspace.roots().size());
            LeafNode leaf = assertInstanceOf(LeafNode.class, MestLists.first(workspace.roots()).content());
            assertEquals(moduleId, leaf.moduleId());
        }
    }

    private static DockLayoutCodec codec() {
        return new DockLayoutCodec(
                WorkspacePersistenceFixtures.catalog(),
                WorkspacePersistenceFixtures.migrationContext());
    }
}
