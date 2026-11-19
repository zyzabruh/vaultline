package com.pro0101.vaultline.gui.zio;

import com.pro0101.vaultline.VaultlineAddon;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Whether the Game panel has been unlocked, remembered between launches.
 *
 * <p>The copied client writes a hidden marker file in the user's home the first time the secret
 * sequence is typed, and the panel is there from then on. This keeps the same fact in the mod's own
 * config directory instead, next to the zoom and the binds: one line, {@code unlocked}, and no file
 * at all until the sequence has been typed.
 */
public final class ZioGames {
    public static final String FILE_NAME = "vaultline-games.txt";

    /** The single line the file holds once the panel is unlocked. */
    public static final String UNLOCKED = "unlocked";

    private static Path file;
    private static Boolean cached;

    private ZioGames() {
    }

    /** Points the store at its file; called once while the mod starts. */
    public static synchronized void bind(Path path) {
        file = path;
        cached = null;
    }

    /** Whether the Game panel may be shown: locked until the sequence has been typed. */
    public static synchronized boolean unlocked() {
        if (cached == null) cached = load(file);

        return cached;
    }

    /** Unlocks the panel for good and writes the file out. */
    public static synchronized void unlock() {
        cached = true;
        save(file, true);
    }

    static boolean load(Path path) {
        if (path == null || !Files.isRegularFile(path)) return false;

        try {
            return UNLOCKED.equals(Files.readString(path, StandardCharsets.UTF_8).trim());
        } catch (IOException failure) {
            VaultlineAddon.LOG.warn("Could not read the ZIO game unlock from {}", path, failure);
            return false;
        }
    }

    static void save(Path path, boolean unlocked) {
        if (path == null) return;

        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            if (unlocked) {
                Files.writeString(path, UNLOCKED + "\n", StandardCharsets.UTF_8);
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException failure) {
            // The panel stays unlocked for this session; the file is only how it survives a restart.
        }
    }
}
