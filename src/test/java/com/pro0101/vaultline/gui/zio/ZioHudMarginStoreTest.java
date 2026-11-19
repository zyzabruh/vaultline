package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The safe margins surviving a restart. */
class ZioHudMarginStoreTest {
    @TempDir
    Path folder;

    @Test
    void theDefaultsAreTheCopiedClientsDefaults() {
        assertArrayEquals(new int[]{1, 1}, ZioHudMarginStore.load(folder.resolve("missing.txt")));
    }

    @Test
    void whatIsWrittenIsReadBack() throws IOException {
        Path file = folder.resolve(ZioHudMarginStore.FILE_NAME);

        ZioHudMarginStore.save(file, 7, 4);

        assertEquals("7 4\n", Files.readString(file, StandardCharsets.UTF_8));
        assertArrayEquals(new int[]{7, 4}, ZioHudMarginStore.load(file));
    }

    @Test
    void aValueOutsideTheBoundsIsBroughtBackInside() {
        assertArrayEquals(new int[]{ZioHudMargins.MAX_SAFE_X, 0}, ZioHudMarginStore.parse("999 -3"));
        assertArrayEquals(new int[]{0, ZioHudMargins.MAX_SAFE_Y}, ZioHudMarginStore.parse("0 999"));
    }

    @Test
    void garbageFallsBackToTheDefaults() {
        assertArrayEquals(new int[]{1, 1}, ZioHudMarginStore.parse("nonsense"));
        assertArrayEquals(new int[]{1, 1}, ZioHudMarginStore.parse(""));
        assertArrayEquals(new int[]{1, 1}, ZioHudMarginStore.parse(null));
        assertArrayEquals(new int[]{1, 1}, ZioHudMarginStore.parse("3"));
    }

    @Test
    void savingClampsBeforeWriting() throws IOException {
        Path file = folder.resolve("hud.txt");

        ZioHudMarginStore.save(file, 500, -20);

        assertArrayEquals(new int[]{ZioHudMargins.MAX_SAFE_X, 0}, ZioHudMarginStore.load(file));
    }

    @Test
    void theStoreKeepsWhatWasSet() {
        ZioHudMarginStore.bind(folder.resolve(ZioHudMarginStore.FILE_NAME));

        ZioHudMarginStore.set(3, 2);

        assertArrayEquals(new int[]{3, 2}, ZioHudMarginStore.current());
        assertArrayEquals(new int[]{3, 2}, ZioHudMarginStore.load(folder.resolve(ZioHudMarginStore.FILE_NAME)));
    }
}
