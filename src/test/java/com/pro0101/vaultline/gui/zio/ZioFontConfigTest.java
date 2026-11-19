package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZioFontConfigTest {
    @BeforeEach
    @AfterEach
    void resetConfig() {
        ZioFontConfig.reset();
    }

    @Test
    void theDefaultsAreTheCopiedClientsDefaults() {
        assertEquals("Arial", ZioFontConfig.DEFAULT_NAME);
        assertEquals(16, ZioFontConfig.DEFAULT_SIZE);
        assertEquals("Arial", ZioFontConfig.name());
        assertEquals(16, ZioFontConfig.size());
    }

    @Test
    void theSizeIsClampedToTheCopiedClientsBounds() {
        ZioFontConfig.set("Arial", 200);
        assertEquals(ZioFontConfig.MAX_SIZE, ZioFontConfig.size());

        ZioFontConfig.set("Arial", 1);
        assertEquals(ZioFontConfig.MIN_SIZE, ZioFontConfig.size());

        ZioFontConfig.set("Arial", 14);
        assertEquals(14, ZioFontConfig.size());
    }

    @Test
    void aBlankNameFallsBackToTheDefault() {
        ZioFontConfig.set("  ", 14);

        assertEquals(ZioFontConfig.DEFAULT_NAME, ZioFontConfig.name());
    }

    @Test
    void aChangeIsAnnouncedOnce() {
        int[] calls = {0};

        ZioFontConfig.onChanged(() -> calls[0]++);

        try {
            ZioFontConfig.set("Dialog", 18);
            ZioFontConfig.set("Dialog", 18);

            assertEquals(1, calls[0]);
        } finally {
            ZioFontConfig.onChanged(null);
        }
    }

    @Test
    void theConfiguredFontDrivesTheAtlasMetrics() {
        ZioFontAtlas defaults = ZioFontAtlas.build(ZioFontConfig.name(), ZioFontConfig.size());

        // The copied client's rule: the tallest cell at a quarter of its size, less one.
        assertEquals(8, defaults.lineHeight());
        assertTrue(defaults.width("Trade") > 0);

        ZioFontConfig.set("Arial", 20);
        ZioFontAtlas large = ZioFontAtlas.build(ZioFontConfig.name(), ZioFontConfig.size());

        assertTrue(large.lineHeight() > defaults.lineHeight());
        assertTrue(large.width("Trade") > defaults.width("Trade"));

        ZioFontConfig.set("Serif", 20);
        ZioFontAtlas serif = ZioFontAtlas.build(ZioFontConfig.name(), ZioFontConfig.size());

        assertNotEquals(large.width("Trade"), serif.width("Trade"));
    }
}
