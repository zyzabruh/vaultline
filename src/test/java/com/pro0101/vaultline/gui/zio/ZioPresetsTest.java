package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Preset naming and ordering, checked without a game client. */
class ZioPresetsTest {
    @Test
    void aTypedNameIsTrimmedAndSqueezed() {
        assertEquals("my preset", ZioPresets.normalise("  my preset  "));
        assertEquals("a b", ZioPresets.normalise("a    b"));
        assertEquals("pvp-1", ZioPresets.normalise("pvp-1"));
        assertEquals("stash_scan", ZioPresets.normalise("stash_scan"));
    }

    @Test
    void charactersThatCannotBeAFolderNameAreDropped() {
        assertEquals("mypreset", ZioPresets.normalise("my/preset!"));
        assertEquals("abc", ZioPresets.normalise("a:b*c"));
        assertEquals("", ZioPresets.normalise("///"));
        assertEquals("", ZioPresets.normalise(null));
    }

    @Test
    void aNameIsCutToTheAcceptedLength() {
        String longName = "abcdefghijklmnopqrstuvwxyz0123";
        String normalised = ZioPresets.normalise(longName);

        assertEquals(ZioPresets.MAX_NAME, normalised.length());
        assertEquals(longName.substring(0, ZioPresets.MAX_NAME), normalised);
    }

    @Test
    void onlyNamesThatSurviveNormalisingAreAccepted() {
        assertTrue(ZioPresets.isValid("pvp"));
        assertTrue(ZioPresets.isValid("  pvp  "));
        assertFalse(ZioPresets.isValid(""));
        assertFalse(ZioPresets.isValid("   "));
        assertFalse(ZioPresets.isValid("///"));
        assertFalse(ZioPresets.isValid(null));
    }

    @Test
    void savingUnderAFreeNameKeepsIt() {
        assertEquals("pvp", ZioPresets.uniqueName("pvp", List.of()));
        assertEquals("pvp", ZioPresets.uniqueName("pvp", List.of("stash", "base")));
    }

    @Test
    void savingUnderATakenNameTakesTheNextFreeOne() {
        assertEquals("pvp 2", ZioPresets.uniqueName("pvp", List.of("pvp")));
        assertEquals("pvp 3", ZioPresets.uniqueName("pvp", List.of("pvp", "pvp 2")));
        // Names are compared the way the client compares them.
        assertEquals("Pvp 2", ZioPresets.uniqueName("Pvp", List.of("PVP")));
    }

    @Test
    void aSuffixedNameStillFitsTheAcceptedLength() {
        String base = "abcdefghijklmnopqrstuvwx";
        String name = ZioPresets.uniqueName(base, List.of(base));

        assertTrue(name.length() <= ZioPresets.MAX_NAME);
        assertFalse(name.equals(base));
    }

    @Test
    void theListIsOrderedWithoutRegardToCase() {
        assertEquals(List.of("Alpha", "beta", "Gamma", "Zeta"),
            ZioPresets.sorted(List.of("Zeta", "beta", "Gamma", "Alpha")));
    }

    @Test
    void theListKeepsOneEntryPerNameAndDropsBlanks() {
        assertEquals(List.of("Pvp", "stash"),
            ZioPresets.sorted(java.util.Arrays.asList("stash", "Pvp", "pvp", "", "   ", null)));
    }
}
