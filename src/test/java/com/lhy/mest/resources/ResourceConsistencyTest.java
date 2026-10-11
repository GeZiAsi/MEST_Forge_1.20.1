package com.lhy.mest.resources;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class ResourceConsistencyTest {
    private static final Path RESOURCES = Path.of("src", "main", "resources");
    private static final Path MOD_ASSETS = RESOURCES.resolve(Path.of("assets", "mesplicedterminal"));
    private static final Path MOD_DATA = RESOURCES.resolve(Path.of("data", "mesplicedterminal"));

    @Test
    void allResourceJsonDocumentsParse() throws IOException {
        try (var files = Files.walk(RESOURCES)) {
            for (Path path : files.filter(file -> file.toString().endsWith(".json")).toList()) {
                JsonParser.parseString(Files.readString(path));
            }
        }
    }

    @Test
    void recipeAdvancementUsesTheLoadableDirectoryAndRewardsTheRecipe() throws IOException {
        // Minecraft 1.20.1 loads advancements from the plural "advancements/recipes" directory.
        Path advancement = MOD_DATA.resolve(Path.of("advancements", "recipes", "spliced_terminal.json"));
        Path obsoletePath = MOD_DATA.resolve(Path.of("advancements", "recipe", "spliced_terminal.json"));

        assertTrue(Files.isRegularFile(advancement));
        assertFalse(Files.exists(obsoletePath));
        JsonObject document = readObject(advancement);
        assertEquals("minecraft:recipes/root", document.get("parent").getAsString());
        assertEquals(
                "mesplicedterminal:spliced_terminal",
                document.getAsJsonObject("rewards").getAsJsonArray("recipes").get(0).getAsString());
        assertTrue(document.getAsJsonObject("criteria").has("has_wireless_universal_terminal"));
    }

    @Test
    void itemModelReferencesAnExistingTexture() throws IOException {
        JsonObject model = readObject(MOD_ASSETS.resolve(Path.of("models", "item", "spliced_terminal.json")));
        String textureId = model.getAsJsonObject("textures").get("layer0").getAsString();
        String relativeTexture = textureId.substring("mesplicedterminal:".length()) + ".png";

        assertTrue(textureId.startsWith("mesplicedterminal:"));
        assertTrue(Files.isRegularFile(MOD_ASSETS.resolve("textures").resolve(relativeTexture)));
    }

    @Test
    void languageFilesExposeTheSameKeys() throws IOException {
        Set<String> english = readObject(MOD_ASSETS.resolve(Path.of("lang", "en_us.json"))).keySet();
        Set<String> chinese = readObject(MOD_ASSETS.resolve(Path.of("lang", "zh_cn.json"))).keySet();

        assertEquals(english, chinese);
        assertTrue(english.contains("item.mesplicedterminal.spliced_terminal"));
        assertFalse(english.stream().anyMatch(key -> key.startsWith("mesplicedterminal.configuration.")));
    }

    @Test
    void recipeAndCuriosTagUseTheRegisteredItemId() throws IOException {
        JsonObject recipe = readObject(MOD_DATA.resolve(Path.of("recipes", "spliced_terminal.json")));
        JsonObject curios = readObject(RESOURCES.resolve(Path.of("data", "curios", "tags", "items", "curio.json")));

        assertEquals(
                "mesplicedterminal:spliced_terminal",
                recipe.getAsJsonObject("result").get("item").getAsString());
        Set<String> taggedItems = curios.getAsJsonArray("values").asList().stream()
                .map(element -> element.getAsString())
                .collect(Collectors.toSet());
        assertEquals(Set.of("mesplicedterminal:spliced_terminal"), taggedItems);
    }

    private static JsonObject readObject(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}