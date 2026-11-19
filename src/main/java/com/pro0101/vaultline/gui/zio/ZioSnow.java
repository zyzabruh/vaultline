package com.pro0101.vaultline.gui.zio;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The copied client's winter flakes, without the drawing.
 *
 * <p>{@code SnowflakeParticle} keeps twenty-five flakes that fall by seven to eighteen hundredths of
 * the screen a step, drift sideways by at most three hundredths, start above the top edge and are
 * replaced once they reach the bottom. The arithmetic is here so it can be checked; the screen only
 * draws what this hands it.
 */
public final class ZioSnow {
    /** {@code MAX_SEASONAL_PARTICLES}. */
    public static final int FLAKES = 25;

    private static final float MIN_SPEED = 0.07f;
    private static final float MAX_SPEED = 0.18f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 1.15f;
    private static final float MAX_DRIFT = 0.033f;

    /** How far above the top edge a flake may start, as a share of the screen. */
    private static final double START_ABOVE = 0.45;

    /**
     * One flake: where it started across the screen, how far down it is, how fast it falls, how large
     * it is and how far it drifts sideways on the way.
     */
    public record Flake(double startX, double y, float speed, float scale, float drift) {
        /** Where it is across the screen, as a share of the width. */
        public double x() {
            return startX + y * drift;
        }

        public boolean expired() {
            return y >= 1;
        }

        /** Its opacity: full at the top edge, never below four, as the client clamps it. */
        public int alpha() {
            return (int) Math.ceil(Math.max(255 * (1 - y), 4));
        }
    }

    private final List<Flake> flakes = new ArrayList<>();
    private final Random random;

    public ZioSnow() {
        this(new Random());
    }

    public ZioSnow(Random random) {
        this.random = random;
        reset();
    }

    public List<Flake> flakes() {
        return flakes;
    }

    /** Fills the screen's share of flakes, spread above the top edge as the client starts them. */
    public void reset() {
        flakes.clear();

        for (int index = 0; index < FLAKES; index++) flakes.add(spawn());
    }

    /**
     * One step, in the client's own units: the frame's share of half a second times the flake's speed.
     * A flake that reached the bottom edge is replaced above the top one.
     */
    public void advance(float frameFactor) {
        for (int index = 0; index < flakes.size(); index++) {
            Flake flake = flakes.get(index);

            double next = flake.y() + frameFactor * flake.speed();

            // Reaching the bottom replaces it there and then, as the copied client's screen does when
            // it drops an expired flake and adds a fresh one.
            flakes.set(index, next >= 1
                ? spawn()
                : new Flake(flake.startX(), next, flake.speed(), flake.scale(), flake.drift()));
        }
    }

    private Flake spawn() {
        float speed = MIN_SPEED + random.nextFloat() * (MAX_SPEED - MIN_SPEED);
        float scale = MIN_SCALE + random.nextFloat() * (MAX_SCALE - MIN_SCALE);
        float drift = (random.nextFloat() * 2 - 1) * MAX_DRIFT;
        double startX = random.nextFloat();
        double y = -START_ABOVE + random.nextFloat() * START_ABOVE;

        return new Flake(startX, y, speed, scale, drift);
    }
}
