package com.pro0101.vaultline.gui.zio;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The unlocked Game panel, and the one-line file that remembers it. */
class ZioGamesTest {
    @TempDir
    Path directory;

    @Test
    void aFreshStoreHasNoGamePanel() {
        assertEquals(false, ZioGames.load(directory.resolve("missing.txt")));
        assertFalse(ZioGames.load(null));
    }

    @Test
    void theFileSaysUnlockedOrItSaysNothing() throws Exception {
        Path file = directory.resolve(ZioGames.FILE_NAME);

        assertFalse(ZioGames.load(file));

        ZioGames.save(file, true);

        assertEquals(ZioGames.UNLOCKED, Files.readString(file, StandardCharsets.UTF_8).trim());
        assertTrue(ZioGames.load(file));

        Files.writeString(file, "something else", StandardCharsets.UTF_8);
        assertFalse(ZioGames.load(file));
    }

    @Test
    void unlockingSurvivesAFreshLaunch() throws Exception {
        Path file = directory.resolve(ZioGames.FILE_NAME);

        ZioGames.bind(file);
        assertFalse(ZioGames.unlocked());

        ZioGames.unlock();
        assertTrue(ZioGames.unlocked());

        // A new launch binds the same file and finds the panel unlocked.
        ZioGames.bind(file);
        assertTrue(ZioGames.unlocked());
    }

    @Test
    void thePathIsCreatedIfItIsNotThereYet() {
        Path file = directory.resolve("nested").resolve(ZioGames.FILE_NAME);

        ZioGames.save(file, true);

        assertTrue(Files.isRegularFile(file));
    }

    @Test
    void unlockingWithoutAFilePathStillUnlocksThisSession() {
        ZioGames.bind(null);

        assertFalse(ZioGames.unlocked());

        ZioGames.unlock();

        assertTrue(ZioGames.unlocked());
    }
}
