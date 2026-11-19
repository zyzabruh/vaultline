package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The copied client's winter flakes: how many, where they start, how they fall and how they fade. */
class ZioSnowTest {
    @Test
    void winterIsDecemberJanuaryAndFebruary() {
        assertTrue(ZioSeason.isWinter(12));
        assertTrue(ZioSeason.isWinter(1));
        assertTrue(ZioSeason.isWinter(2));

        for (int month : new int[]{3, 4, 5, 6, 7, 8, 9, 10, 11}) {
            assertFalse(ZioSeason.isWinter(month), "month " + month + " is not winter");
        }
    }

    @Test
    void aScreenStartsWithTwentyFiveFlakesAboveTheTopEdge() {
        ZioSnow snow = new ZioSnow(new Random(7));

        assertEquals(ZioSnow.FLAKES, snow.flakes().size());

        for (ZioSnow.Flake flake : snow.flakes()) {
            assertTrue(flake.y() >= -0.45, "a flake starts at most 0.45 above the top edge");
            assertTrue(flake.y() < 0, "a flake starts above the screen");
            assertTrue(flake.startX() >= 0 && flake.startX() < 1);
            assertTrue(flake.speed() >= 0.07f && flake.speed() <= 0.18f);
            assertTrue(flake.scale() >= 0.5f && flake.scale() <= 1.15f);
            assertTrue(Math.abs(flake.drift()) <= 0.033f);
        }
    }

    @Test
    void aStepFallsByTheFrameFactorTimesTheSpeedAndDriftsSideways() {
        ZioSnow snow = new ZioSnow(new Random(11));
        ZioSnow.Flake before = snow.flakes().get(0);

        snow.advance(0.5f);

        ZioSnow.Flake after = snow.flakes().get(0);
        assertEquals(before.y() + 0.5 * before.speed(), after.y(), 1e-9);
        assertEquals(after.startX() + after.y() * after.drift(), after.x(), 1e-9);
        assertEquals(before.speed(), after.speed());
        assertEquals(before.drift(), after.drift());
    }

    @Test
    void aFlakeNeverFallsPastTheBottomEdge() {
        ZioSnow snow = new ZioSnow(new Random(3));

        for (int step = 0; step < 200; step++) snow.advance(1f);

        for (ZioSnow.Flake flake : snow.flakes()) {
            assertTrue(flake.y() <= 1);
        }
    }

    @Test
    void aFlakeThatReachedTheBottomIsReplacedAboveTheTop() {
        ZioSnow snow = new ZioSnow(new Random(5));

        // Every flake starts above the screen, so one full step of a second puts none of them at an
        // end; the clamp is what carries them there.
        for (int step = 0; step < 60; step++) snow.advance(1f);

        ZioSnow.Flake first = snow.flakes().get(0);
        assertNotSame(null, first);

        for (ZioSnow.Flake flake : snow.flakes()) {
            assertTrue(flake.y() < 1, "a flake at the bottom has been replaced");
        }
    }

    @Test
    void opacityFallsWithTheDepthAndNeverGoesBelowFour() {
        ZioSnow.Flake top = new ZioSnow.Flake(0.5, 0, 0.1f, 1f, 0);
        ZioSnow.Flake middle = new ZioSnow.Flake(0.5, 0.5, 0.1f, 1f, 0);
        ZioSnow.Flake bottom = new ZioSnow.Flake(0.5, 1, 0.1f, 1f, 0);

        assertEquals(255, top.alpha());
        assertEquals(128, middle.alpha());
        assertEquals(4, bottom.alpha());
        assertTrue(bottom.expired());
        assertFalse(top.expired());
    }

    @Test
    void resetBringsTheScreenBackToAFullSet() {
        ZioSnow snow = new ZioSnow(new Random(13));

        for (int step = 0; step < 40; step++) snow.advance(1f);
        snow.reset();

        assertEquals(ZioSnow.FLAKES, snow.flakes().size());

        for (ZioSnow.Flake flake : snow.flakes()) assertTrue(flake.y() < 0);
    }
}
