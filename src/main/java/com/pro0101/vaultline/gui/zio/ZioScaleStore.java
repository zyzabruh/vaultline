package com.pro0101.vaultline.gui.zio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The zoom the mod remembers between launches.
 *
 * <p>One number in one small file next to the game's config: written on every change, read once at
 * startup and brought back inside the allowed range when it is missing, out of range or garbage.
 */
public final class ZioScaleStore {
    public static final String FILE_NAME = "vaultline-gui.txt";

    private static Path file;
    private static Float cached;

    private ZioScaleStore() {
    }

    /** Points the store at its file; called once while the mod starts. */
    public static synchronized void bind(Path path) {
        file = path;
        cached = null;
    }

    /** The zoom to use now, read from disk the first time it is asked for. */
    public static synchronized float current() {
        if (cached == null) cached = load(file);

        return cached;
    }

    /** Stores a new zoom, clamped, and writes it out immediately. */
    public static synchronized void set(float value) {
        float scale = ZioScale.clamp(value);
        if (cached != null && cached == scale) return;

        cached = scale;
        save(file, scale);
    }

    static float load(Path path) {
        if (path == null || !Files.isRegularFile(path)) return ZioScale.DEFAULT;

        try {
            return ZioScale.parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            return ZioScale.DEFAULT;
        }
    }

    static void save(Path path, float value) {
        if (path == null) return;

        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            Files.writeString(path, ZioScale.format(value), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // A folder that cannot be written only costs the zoom its memory; the screen still works.
        }
    }
}
