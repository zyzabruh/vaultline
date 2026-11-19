package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The glyph atlas and the metrics the GUI measures text with. */
class ZioFontAtlasTest {
    private static ZioFontAtlas atlas;

    @BeforeAll
    static void build() {
        atlas = ZioFontAtlas.build(ZioFontAtlas.DEFAULT_FAMILY, ZioFontAtlas.DEFAULT_SIZE);
    }

    @Test
    void theLineHeightIsTheTallestCellScaledBackLessOne() {
        int tallest = 0;

        for (char character = 32; character <= 126; character++) {
            tallest = Math.max(tallest, atlas.glyph(character).height());
        }

        // The copied client's own rule: FontAtlas keeps the tallest cell, and FontRenderer draws the
        // line one pixel shorter than that cell measures once scaled back.
        assertEquals(Math.max(1, (int) (tallest * ZioFontAtlas.ATLAS_SCALE) - 1), atlas.lineHeight());
        assertTrue(atlas.lineHeight() > 0);
    }

    @Test
    void everyPrintableCharacterHasAGlyph() {
        for (char character = 32; character <= 126; character++) {
            assertNotNull(atlas.glyph(character), "no glyph for '" + character + "'");
            assertTrue(atlas.advance(character) >= 0);
        }

        // Anything outside the covered range falls back to the question mark.
        assertEquals(atlas.glyph('?'), atlas.glyph('\u4E00'));
    }

    @Test
    void aSpaceMovesThePenAndDrawsNothing() {
        assertTrue(atlas.advance(' ') > 0);
        assertEquals(0, atlas.width(""));
        assertTrue(atlas.width("a ") > atlas.width("a"));
    }

    @Test
    void aWiderGlyphTakesMoreRoom() {
        assertTrue(atlas.advance('W') > atlas.advance('i'));
        assertTrue(atlas.width("WWW") > atlas.width("iii"));
    }

    @Test
    void theWidthOfALineIsTheSumOfItsGlyphsAtDrawnSize() {
        String text = "Trade";
        float sum = 0f;

        for (char character : text.toCharArray()) sum += atlas.advanceF(character);

        // The copied client measures a line the same way — the fractional advances summed, scaled back
        // and one pixel wider — and only rounds when it hands the number out.
        assertEquals(sum * ZioFontAtlas.ATLAS_SCALE + 1, atlas.widthF(text), 1e-4);
        assertEquals((int) atlas.widthF(text), atlas.width(text));
    }

    @Test
    void theSpaceBetweenWordsKeepsItsOwnAdvance() {
        assertEquals(atlas.advanceF(' ') * ZioFontAtlas.ATLAS_SCALE,
            atlas.widthF("a b") - atlas.widthF("ab"), 1e-4);
        assertTrue(atlas.advanceF('i') > 0);
    }

    @Test
    void formattingCodesAreNotMeasured() {
        assertEquals(atlas.width("Trade"), atlas.width("\u00a77Trade"));
        assertEquals(atlas.width("ab"), atlas.width("a\u00a7lb"));
    }

    @Test
    void aColourWithoutAlphaIsDrawnOpaque() {
        assertEquals(0xFF123456, ZioFontAtlas.opaque(0x00123456));
        assertEquals(0x80123456, ZioFontAtlas.opaque(0x80123456));
    }

    @Test
    void theShadowKeepsAlphaAndQuartersTheChannels() {
        // The copied client's own formula: alpha | ((argb & 0x00FCFCFC) >>> 2).
        assertEquals(0xFF000000 | ((0xFFE1E1E1 & 0x00FCFCFC) >>> 2), ZioFontAtlas.shadow(0xFFE1E1E1));
        assertEquals(0x5A3F3F3F, ZioFontAtlas.shadow(0x5AFFFFFF));
        assertEquals(0x80000000, ZioFontAtlas.shadow(0x80000000));
    }

    @Test
    void centringPutsTheMiddleOfTheLineOnThePoint() {
        String text = "Vaultline";
        int centre = 60;

        int left = atlas.centeredX(text, centre);

        assertEquals(centre - atlas.width(text) / 2, left);
        assertTrue(left + atlas.width(text) / 2 <= centre);
        assertEquals(atlas.centeredX(text, 60), atlas.centeredX(text, 120) - 60);
    }

    @Test
    void theAtlasHoldsEveryGlyphItRecords() {
        BufferedImage image = atlas.image();

        assertNotNull(image);
        assertEquals(image.getHeight(), atlas.imageHeight());

        for (char character = 32; character <= 126; character++) {
            ZioFontAtlas.Glyph glyph = atlas.glyph(character);

            assertTrue(glyph.x() >= 0 && glyph.y() >= 0);
            assertTrue(glyph.x() + glyph.width() <= image.getWidth(), "glyph overflows at x");
            assertTrue(glyph.y() + glyph.height() <= image.getHeight(), "glyph overflows at y");
        }
    }

    @Test
    void aSizeChangeChangesTheDrawnHeight() {
        ZioFontAtlas small = ZioFontAtlas.build(ZioFontAtlas.DEFAULT_FAMILY, 12);
        ZioFontAtlas large = ZioFontAtlas.build(ZioFontAtlas.DEFAULT_FAMILY, 20);

        assertTrue(small.lineHeight() < large.lineHeight());
        assertTrue(small.width("Trade") < large.width("Trade"));
    }
}
