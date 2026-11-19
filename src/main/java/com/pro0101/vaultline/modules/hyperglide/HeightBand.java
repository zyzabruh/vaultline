package com.pro0101.vaultline.modules.hyperglide;

/**
 * Decides which phase a glide cycle should be in, from the altitude band or from the player's keys.
 *
 * <p>Keeping a height band is what stops the cycle from sinking out of the sky: the previous version decided on
 * vertical speed alone, so a dive could end with the player low and nothing asked for a climb. The band makes the
 * altitude itself the goal, with a floor that wins over everything else.
 */
public final class HeightBand {
    public enum Phase {
        CLIMB,
        DIVE,
        CRUISE
    }

    private HeightBand() {}

    /**
     * @param band half-height of the band, in blocks
     * @param minimum altitude below which the cycle always climbs
     */
    public static Phase automatic(double altitude, double target, double band, double minimum) {
        if (altitude <= minimum) return Phase.CLIMB;
        if (altitude < target - band) return Phase.CLIMB;
        if (altitude > target + band) return Phase.DIVE;

        return Phase.CRUISE;
    }

    /**
     * @param climb whether the climb key is held
     * @param dive whether the dive key is held
     */
    public static Phase manual(boolean climb, boolean dive) {
        if (climb && !dive) return Phase.CLIMB;
        if (dive && !climb) return Phase.DIVE;

        return Phase.CRUISE;
    }
}
