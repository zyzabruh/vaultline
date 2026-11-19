package com.pro0101.vaultline.gui.zio;

/**
 * The copied client's winter, which is the only season its screen reacts to.
 *
 * <p>{@code Season.WINTER(1, 2, 12)}: December, January and February. Its screen draws its flakes
 * only inside those months.
 */
public final class ZioSeason {
    private ZioSeason() {
    }

    /** Whether a calendar month is in the copied client's winter. */
    public static boolean isWinter(int month) {
        return month == 12 || month == 1 || month == 2;
    }
}
