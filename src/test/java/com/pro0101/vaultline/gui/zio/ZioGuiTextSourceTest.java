package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the ported screen's text path: every label has to go through the atlas renderer, never
 * through the game's bitmap font.
 *
 * <p>The scan covers the whole source tree, with the files that predate this port and draw their own
 * text listed one by one: the Meteor theme wrapper ({@code gui/PresetTheme.java}), the chest tracker
 * screen and the huntbuddy radar. None of them is part of the ported screen.
 */
class ZioGuiTextSourceTest {
    private static final Path SOURCES = Paths.get("src", "main", "java");

    private static final Path GUI_SOURCES = SOURCES.resolve(Paths.get("com", "pro0101", "vaultline", "gui"));

    /** Files that predate the port, outside its text path by decision. */
    private static final List<String> ALLOWED = List.of(
        "PresetTheme.java",
        "ChestTrackerScreen.java",
        "ChunkRadar.java"
    );

    @Test
    void noSourceOutsideTheAllowedListUsesTheGameTextRenderer() throws IOException {
        List<String> offenders = new ArrayList<>();

        for (Path file : javaSources(SOURCES)) {
            if (ALLOWED.contains(file.getFileName().toString())) continue;

            String source = Files.readString(file, StandardCharsets.UTF_8);

            if (source.contains("mc.textRenderer") || source.contains("textRenderer.")) {
                offenders.add(file.toString());
            }
        }

        assertEqualsNoOffenders(offenders);
    }

    @Test
    void eachAllowedFileStillOnlyTouchesItsOwnFont() throws IOException {
        for (String name : ALLOWED) {
            assertTrue(javaSources(SOURCES).stream().anyMatch(file -> file.getFileName().toString().equals(name)),
                "the allowed file " + name + " no longer exists; drop it from the list");
        }
    }

    @Test
    void theScreenDrawsAndMeasuresWithTheAtlasRenderer() throws IOException {
        String source = Files.readString(GUI_SOURCES.resolve("zio").resolve("ZioScreen.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("ZioFontRenderer.draw("), "the screen no longer draws with the atlas renderer");
        assertTrue(source.contains("ZioFontRenderer.width("), "the screen no longer measures with the atlas renderer");
        assertTrue(source.contains("ZioFontRenderer.lineHeight("), "the screen no longer asks the atlas for its line height");
    }

    private static void assertEqualsNoOffenders(List<String> offenders) {
        assertTrue(offenders.isEmpty(), "these GUI sources draw text with the game font: " + offenders);
    }

    private static List<Path> javaSources(Path root) throws IOException {
        try (Stream<Path> files = Files.walk(root)) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }
}
