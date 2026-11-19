package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The sizes the editor forces on HUD elements, and their file. */
class ZioHudSizeStoreTest {
    @TempDir
    Path folder;

    @AfterEach
    void unbind() {
        ZioHudSizes.bind(null);
    }

    @Test
    void aLineKeepsTheWholeElementName() {
        Map<String, int[]> sizes = ZioHudSizeStore.parse("flight-status 120 42\nActive Modules 84 30\n");

        assertArrayEquals(new int[]{120, 42}, sizes.get("flight-status"));
        assertArrayEquals(new int[]{84, 30}, sizes.get("Active Modules"));
        assertEquals(2, sizes.size());
    }

    @Test
    void malformedLinesAreDropped() {
        assertTrue(ZioHudSizeStore.parse("nonsense\n\nonly-name\nname x y\n  \n").isEmpty());
        assertTrue(ZioHudSizeStore.parse(null).isEmpty());
    }

    @Test
    void sizesAreBoundedOnTheWayIn() {
        assertEquals(ZioHudLayout.MIN_WIDTH, ZioHudSizeStore.parse("a 0 0").get("a")[0]);
        assertEquals(ZioHudSizeStore.MAX_SIZE, ZioHudSizeStore.parse("a 99999 99999").get("a")[0]);
    }

    @Test
    void theFileRoundTripsThroughWriteAndRead() throws IOException {
        Path file = folder.resolve(ZioHudSizeStore.FILE_NAME);
        Map<String, int[]> sizes = new LinkedHashMap<>();

        sizes.put("flight-status", new int[]{120, 42});
        sizes.put("Active Modules", new int[]{84, 30});

        ZioHudSizeStore.save(file, sizes);

        Map<String, int[]> read = ZioHudSizeStore.load(file);

        assertArrayEquals(new int[]{120, 42}, read.get("flight-status"));
        assertArrayEquals(new int[]{84, 30}, read.get("Active Modules"));
        assertEquals(sizes.keySet(), read.keySet());
        assertEquals("flight-status 120 42\nActive Modules 84 30\n",
            Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void theRegistryKeepsWhatWasSetAndWritesItOut() {
        Path file = folder.resolve(ZioHudSizeStore.FILE_NAME);

        ZioHudSizes.bind(file);
        ZioHudSizes.set("flight-status", 120, 42);

        assertArrayEquals(new int[]{120, 42}, ZioHudSizes.get("flight-status"));
        assertTrue(ZioHudSizes.isForced("flight-status"));
        assertNull(ZioHudSizes.get("stash"));

        // A fresh bind reads the same file again, which is what a restart does.
        ZioHudSizes.bind(file);

        assertArrayEquals(new int[]{120, 42}, ZioHudSizes.get("flight-status"));
    }

    @Test
    void aForcedSizeDuringADragIsNotWrittenOut() {
        Path file = folder.resolve(ZioHudSizeStore.FILE_NAME);

        ZioHudSizes.bind(file);
        ZioHudSizes.force("flight-status", 150, 60);

        assertArrayEquals(new int[]{150, 60}, ZioHudSizes.get("flight-status"));
        assertTrue(ZioHudSizeStore.load(file).isEmpty());

        ZioHudSizes.set("flight-status", 150, 60);

        assertArrayEquals(new int[]{150, 60}, ZioHudSizeStore.load(file).get("flight-status"));
    }

    @Test
    void clearingHandsTheElementBackItsOwnMeasurement() {
        Path file = folder.resolve(ZioHudSizeStore.FILE_NAME);

        ZioHudSizes.bind(file);
        ZioHudSizes.set("flight-status", 120, 42);
        ZioHudSizes.clear("flight-status");

        assertFalse(ZioHudSizes.isForced("flight-status"));
        assertNull(ZioHudSizes.get("flight-status"));
        assertTrue(ZioHudSizeStore.load(file).isEmpty());
    }

    @Test
    void aSetSizeIsClampedBeforeItIsStored() {
        ZioHudSizes.bind(folder.resolve("sizes.txt"));

        ZioHudSizes.set("stash", -5, 99999);

        assertArrayEquals(new int[]{ZioHudLayout.MIN_WIDTH, ZioHudSizeStore.MAX_SIZE},
            ZioHudSizes.get("stash"));
        assertTrue(ZioHudSizes.all().containsKey("stash"));
    }
}
