package com.pro0101.vaultline.gui.zio;

import com.pro0101.vaultline.VaultlineAddon;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The HUD safe margins the mod remembers between launches.
 *
 * <p>Two numbers in one small file next to the game's config, written whenever a drag in the editor
 * changes them, read once on the first drag or draw that asks for them, and clamped to the copied
 * client's own bounds when the file is missing, out of range or garbage: {@code SafeX 0..20} and
 * {@code SafeY 0..10}, both defaulting to 1.
 */
public final class ZioHudMarginStore {
    public static final String FILE_NAME = "vaultline-hud-margins.txt";

    private static Path file;
    private static int[] cached;

    private ZioHudMarginStore() {
    }

    /** Points the store at its file; called once while the mod starts. */
    public static synchronized void bind(Path path) {
        file = path;
        cached = null;
    }

    /** The margins to use now, read from disk the first time they are asked for. */
    public static synchronized int[] current() {
        if (cached == null) cached = load(file);

        return cached.clone();
    }

    /** Stores the margins, clamped, and writes them out immediately. */
    public static synchronized void set(int safeX, int safeY) {
        int[] next = clamp(safeX, safeY);

        if (cached != null && cached[0] == next[0] && cached[1] == next[1]) return;

        cached = next;
        save(file, next[0], next[1]);
    }

    static int[] clamp(int safeX, int safeY) {
        return new int[]{
            Math.max(0, Math.min(ZioHudMargins.MAX_SAFE_X, safeX)),
            Math.max(0, Math.min(ZioHudMargins.MAX_SAFE_Y, safeY))
        };
    }

    /** Reads {@code safeX safeY} from one line, falling back to the defaults. */
    static int[] parse(String text) {
        if (text == null) return clamp(ZioHudMargins.DEFAULT_SAFE_X, ZioHudMargins.DEFAULT_SAFE_Y);

        String[] parts = text.trim().split("\\s+");

        if (parts.length < 2) return clamp(ZioHudMargins.DEFAULT_SAFE_X, ZioHudMargins.DEFAULT_SAFE_Y);

        try {
            return clamp(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        } catch (NumberFormatException garbage) {
            // A pair that does not parse is treated like a missing file: the default margins apply.
            return clamp(ZioHudMargins.DEFAULT_SAFE_X, ZioHudMargins.DEFAULT_SAFE_Y);
        }
    }

    static String format(int safeX, int safeY) {
        return safeX + " " + safeY + "\n";
    }

    static int[] load(Path path) {
        if (path == null || !Files.isRegularFile(path)) {
            return clamp(ZioHudMargins.DEFAULT_SAFE_X, ZioHudMargins.DEFAULT_SAFE_Y);
        }

        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            VaultlineAddon.LOG.warn("Could not read the ZIO safe margins from {}", path, failure);
            return clamp(ZioHudMargins.DEFAULT_SAFE_X, ZioHudMargins.DEFAULT_SAFE_Y);
        }
    }

    static void save(Path path, int safeX, int safeY) {
        if (path == null) return;

        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            int[] clamped = clamp(safeX, safeY);

            Files.writeString(path, format(clamped[0], clamped[1]), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // A folder that cannot be written only costs the margins their memory; the editor works.
        }
    }
}
