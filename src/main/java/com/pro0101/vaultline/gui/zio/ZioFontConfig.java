package com.pro0101.vaultline.gui.zio;

/**
 * The font the copied screen draws its text with, the same two values its own font module carries.
 *
 * <p>The defaults are that module's defaults: Arial at size 16, which the atlas rasterises at 32 and
 * draws back at a quarter of the size. Changing either value throws the built atlas away so the next
 * frame rebuilds it.
 */
public final class ZioFontConfig {
    /** The family the copied client defaults to. */
    public static final String DEFAULT_NAME = "Arial";

    /** The size the copied client defaults to, before the atlas doubles it. */
    public static final int DEFAULT_SIZE = 16;

    /** The size the copied client's own size setting refuses to go under. */
    public static final int MIN_SIZE = 12;

    /** The size the copied client's own size setting refuses to go over. */
    public static final int MAX_SIZE = 20;

    private static String name = DEFAULT_NAME;
    private static int size = DEFAULT_SIZE;
    private static Runnable listener = () -> {};

    private ZioFontConfig() {
    }

    public static String name() {
        return name;
    }

    public static int size() {
        return size;
    }

    /** Sets both values, clamps the size and rebuilds the atlas on the next frame. */
    public static void set(String name, int size) {
        if (name == null || name.isBlank()) name = DEFAULT_NAME;

        int clamped = Math.max(MIN_SIZE, Math.min(MAX_SIZE, size));

        if (name.equals(ZioFontConfig.name) && clamped == ZioFontConfig.size) return;

        ZioFontConfig.name = name;
        ZioFontConfig.size = clamped;
        listener.run();
    }

    /** What the atlas calls once a value changes, so the old raster is dropped. */
    static void onChanged(Runnable listener) {
        ZioFontConfig.listener = listener != null ? listener : () -> {};
    }

    /** Restores the copied client's defaults, for tests and for a reset from the screen. */
    public static void reset() {
        set(DEFAULT_NAME, DEFAULT_SIZE);
    }
}
