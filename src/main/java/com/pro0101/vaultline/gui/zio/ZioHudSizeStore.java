package com.pro0101.vaultline.gui.zio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The sizes the HUD editor gave to elements, remembered between launches.
 *
 * <p>One line per resized element, {@code name width height}, in a small file next to the game's
 * config: {@code flight-status 120 42}. The name keeps its spaces (Meteor's own element names have
 * them, like {@code Active Modules}); the last two fields are always the size, so the line is read from
 * the right. Malformed lines are dropped rather than guessed at.
 */
public final class ZioHudSizeStore {
    public static final String FILE_NAME = "vaultline-hud-sizes.txt";

    /** Nothing is stored outside this, so a broken file cannot make an element unusable. */
    public static final int MAX_SIZE = 4096;

    private ZioHudSizeStore() {
    }

    /** Reads every {@code name width height} line, keeping only the valid ones. */
    static Map<String, int[]> parse(String text) {
        Map<String, int[]> sizes = new LinkedHashMap<>();

        if (text == null) return sizes;

        for (String line : text.split("\n")) {
            String trimmed = line.trim();

            if (trimmed.isEmpty()) continue;

            int lastSpace = trimmed.lastIndexOf(' ');
            if (lastSpace <= 0) continue;

            int secondSpace = trimmed.lastIndexOf(' ', lastSpace - 1);
            if (secondSpace <= 0) continue;

            String name = trimmed.substring(0, secondSpace).trim();

            if (name.isEmpty()) continue;

            try {
                int width = Integer.parseInt(trimmed.substring(secondSpace + 1, lastSpace));
                int height = Integer.parseInt(trimmed.substring(lastSpace + 1));

                sizes.put(name, clamp(width, height));
            } catch (NumberFormatException malformed) {
                // A line that does not read as a size is left out; the element keeps its own measure.
            }
        }

        return sizes;
    }

    static String format(Map<String, int[]> sizes) {
        StringBuilder builder = new StringBuilder();

        sizes.forEach((name, size) -> builder.append(name).append(' ').append(size[0]).append(' ')
            .append(size[1]).append('\n'));

        return builder.toString();
    }

    static int[] clamp(int width, int height) {
        return new int[]{
            Math.max(ZioHudLayout.MIN_WIDTH, Math.min(MAX_SIZE, width)),
            Math.max(ZioHudLayout.MIN_HEIGHT, Math.min(MAX_SIZE, height))
        };
    }

    static Map<String, int[]> load(Path path) {
        if (path == null || !Files.isRegularFile(path)) return new LinkedHashMap<>();

        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            return new LinkedHashMap<>();
        }
    }

    static void save(Path path, Map<String, int[]> sizes) {
        if (path == null) return;

        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            Files.writeString(path, format(sizes), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // A folder that cannot be written only costs the sizes their memory; the editor works.
        }
    }
}
