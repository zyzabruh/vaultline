package com.pro0101.vaultline.gui.zio;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The size the editor gave each HUD element, or nothing when the element measures itself.
 *
 * <p>This is the whole of the override: a name keyed table of two numbers. It is read by a mixin on
 * Meteor's {@code HudElement.setSize} — the call every element makes at the end of its own render to
 * report the size it just measured — so an entry here replaces that measurement instead of being
 * overwritten by it on the next frame. Nothing else about the elements changes.
 *
 * <p>An element keeps its forced size until the editor clears it, and the table is written straight to
 * its file so a restart comes back at the same size.
 */
public final class ZioHudSizes {
    private static final Map<String, int[]> sizes = new LinkedHashMap<>();

    private static Path file;
    private static boolean loaded;

    private ZioHudSizes() {
    }

    /** Points the table at its file; called once while the mod starts. */
    public static synchronized void bind(Path path) {
        file = path;
        sizes.clear();
        loaded = false;
    }

    /** The forced size of an element, or {@code null} when it measures itself. */
    public static synchronized int[] get(String name) {
        ensureLoaded();

        int[] size = sizes.get(name);

        return size == null ? null : size.clone();
    }

    /** Whether this element was resized in the editor. */
    public static synchronized boolean isForced(String name) {
        ensureLoaded();

        return sizes.containsKey(name);
    }

    /**
     * Forces a size for this frame only, without writing the table: what a resize drag needs, since a
     * drag would otherwise have to save a file on every mouse move.
     */
    public static synchronized void force(String name, int width, int height) {
        if (name == null || name.isBlank()) return;

        ensureLoaded();

        sizes.put(name, ZioHudSizeStore.clamp(width, height));
    }

    /** Forces a size on an element and writes the table out. */
    public static synchronized void set(String name, int width, int height) {
        if (name == null || name.isBlank()) return;

        ensureLoaded();

        sizes.put(name, ZioHudSizeStore.clamp(width, height));
        ZioHudSizeStore.save(file, sizes);
    }

    /** Lets an element measure itself again. */
    public static synchronized void clear(String name) {
        ensureLoaded();

        if (sizes.remove(name) != null) ZioHudSizeStore.save(file, sizes);
    }

    /** Everything currently forced, for tests and for a reset. */
    public static synchronized Map<String, int[]> all() {
        ensureLoaded();

        Map<String, int[]> copy = new LinkedHashMap<>();
        sizes.forEach((name, size) -> copy.put(name, size.clone()));

        return copy;
    }

    private static void ensureLoaded() {
        if (loaded) return;

        sizes.clear();
        sizes.putAll(ZioHudSizeStore.load(file));
        loaded = true;
    }
}
