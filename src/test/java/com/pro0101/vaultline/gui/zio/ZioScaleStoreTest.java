package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The zoom that survives a restart, checked against a real file. */
class ZioScaleStoreTest {
    @TempDir
    Path folder;

    @Test
    void aMissingFileStartsFromTheDefault() {
        Path file = folder.resolve(ZioScaleStore.FILE_NAME);

        assertEquals(ZioScale.DEFAULT, ZioScaleStore.load(file), 1e-6);
    }

    @Test
    void aWrittenZoomIsReadBack() throws IOException {
        Path file = folder.resolve(ZioScaleStore.FILE_NAME);

        ZioScaleStore.save(file, 0.5f);
        assertEquals(0.5f, ZioScaleStore.load(file), 1e-6);

        ZioScaleStore.save(file, 1.25f);
        assertEquals(1.25f, ZioScaleStore.load(file), 1e-6);
    }

    @Test
    void everyAllowedStepSurvivesTheRoundTrip() throws IOException {
        Path file = folder.resolve(ZioScaleStore.FILE_NAME);

        for (float scale = ZioScale.MIN; scale <= ZioScale.MAX; scale += ZioScale.STEP) {
            ZioScaleStore.save(file, scale);
            assertEquals(ZioScale.clamp(scale), ZioScaleStore.load(file), 1e-6);
        }
    }

    @Test
    void aCorruptedFileFallsBackToTheDefault() throws IOException {
        Path file = folder.resolve(ZioScaleStore.FILE_NAME);

        Files.writeString(file, "not a number", StandardCharsets.UTF_8);
        assertEquals(ZioScale.DEFAULT, ZioScaleStore.load(file), 1e-6);

        Files.writeString(file, "", StandardCharsets.UTF_8);
        assertEquals(ZioScale.DEFAULT, ZioScaleStore.load(file), 1e-6);
    }

    @Test
    void aValueOutOfRangeIsBroughtBackIn() throws IOException {
        Path file = folder.resolve(ZioScaleStore.FILE_NAME);

        Files.writeString(file, "9.5", StandardCharsets.UTF_8);
        assertEquals(ZioScale.MAX, ZioScaleStore.load(file), 1e-6);

        Files.writeString(file, "0.05", StandardCharsets.UTF_8);
        assertEquals(ZioScale.MIN, ZioScaleStore.load(file), 1e-6);
    }

    @Test
    void theStoreWritesWhatItIsGivenAndReadsItBack() {
        Path file = folder.resolve("nested").resolve(ZioScaleStore.FILE_NAME);

        ZioScaleStore.bind(file);
        ZioScaleStore.set(1.0f);

        assertEquals(1.0f, ZioScaleStore.current(), 1e-6);
        assertEquals(1.0f, ZioScaleStore.load(file), 1e-6);

        // A rebound store reads the file again rather than the old value.
        ZioScaleStore.bind(file);
        assertEquals(1.0f, ZioScaleStore.current(), 1e-6);
    }

    @Test
    void theStoreClampsWhatItIsAskedToSave() {
        Path file = folder.resolve(ZioScaleStore.FILE_NAME);

        ZioScaleStore.bind(file);
        ZioScaleStore.set(7f);

        assertEquals(ZioScale.MAX, ZioScaleStore.current(), 1e-6);
        assertEquals(ZioScale.MAX, ZioScaleStore.load(file), 1e-6);
    }
}
