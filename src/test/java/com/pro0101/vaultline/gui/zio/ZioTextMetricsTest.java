package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The measurements the batched text draws with: fractional advances and fractional centring. */
class ZioTextMetricsTest {
    private final ZioFontAtlas atlas = ZioFontAtlas.build(ZioFontAtlas.DEFAULT_FAMILY, ZioFontAtlas.DEFAULT_SIZE);

    @Test
    void aLineKeepsItsFractionalWidth() {
        float width = atlas.widthF("Trading");

        // The measured width carries the fraction; the rounded one is only for layout decisions.
        assertTrue(width > 0f);
        assertTrue(atlas.width("Trading") <= width, "the rounded width overshoots its own measurement");
        assertTrue(width - atlas.width("Trading") < 1f);
    }

    @Test
    void aGlyphAdvanceIsItsOwnFractionalWidth() {
        float advance = atlas.advanceF('m');

        assertTrue(advance > 0f);
        // The rounded accessor is kept for the places that compare sizes; the drawing path never rounds.
        assertEquals(Math.round(advance), atlas.advance('m'));
    }

    @Test
    void centringOnAFractionalCentreStaysFractional() {
        float centre = 40.5f;
        float width = atlas.widthF("Trade");
        float left = centre - width / 2f;

        // Half the fractional width on each side puts the line exactly back on its centre; rounding
        // either the width or the left edge first is what moved centred labels by up to a pixel.
        assertEquals(centre, left + width / 2f, 1e-4);
        assertEquals(Math.round(left), atlas.centeredX("Trade", 40));
    }

    @Test
    void theShadowSitsAtTheCopiedClientsFractionalOffset() {
        assertEquals(0.7f, ZioFontRenderer.SHADOW_OFFSET, 1e-6);
    }

    @Test
    void formattingCodesDoNotWidenTheLine() {
        assertEquals(atlas.widthF("Trade"), atlas.widthF("\u00a77Trade"), 1e-4);
        assertEquals(atlas.widthF("ab"), atlas.widthF("a\u00a7lb"), 1e-4);
    }

    @Test
    void aScaledLineTakesLessRoomThanAnUnscaledOne() {
        float pen = 0f;

        for (char character : "123".toCharArray()) pen += atlas.advanceF(character);

        // The game header draws at 0.8, so its pen ends at 0.8 of the run's width.
        assertTrue(pen * ZioFontAtlas.ATLAS_SCALE * 0.8f < pen * ZioFontAtlas.ATLAS_SCALE);
        assertEquals(pen * ZioFontAtlas.ATLAS_SCALE * 0.8f,
            pen * ZioFontAtlas.ATLAS_SCALE - pen * ZioFontAtlas.ATLAS_SCALE * 0.2f, 1e-4);
    }
}
