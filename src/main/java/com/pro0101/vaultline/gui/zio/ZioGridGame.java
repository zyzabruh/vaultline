package com.pro0101.vaultline.gui.zio;

/**
 * The shared shape of the games the copied client's Game panel plays, ported from its
 * {@code gui/components/GridGameComponent}.
 *
 * <p>The copied base class owns three things and nothing else: the grid (columns and rows), the tick
 * timer ({@code ElapsedTimer.hasElapsed(getTickDelayMillis())} then {@code tickGame()} then
 * {@code reset()}), and the cell size derived from the panel. Here it also owns the score and the
 * pause a finished board keeps before it starts over, because every game in the panel has both.
 *
 * <p>Time is passed in rather than read, so a test can play a whole game without waiting.
 */
public abstract class ZioGridGame {
    private int columns;
    private int rows;
    private final long tickDelayMillis;
    private long lastTickMillis = -1L;
    private long resetAtMillis = -1L;
    private int score;

    protected ZioGridGame(int columns, int rows, long tickDelayMillis) {
        this.columns = columns;
        this.rows = rows;
        this.tickDelayMillis = tickDelayMillis;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    /** How long the game waits between two ticks at its normal pace. */
    public long tickDelayMillis() {
        return tickDelayMillis;
    }

    /** The pace right now: a game may change it (a held key, a level) without changing its base. */
    protected long currentTickDelayMillis() {
        return tickDelayMillis;
    }

    public int score() {
        return score;
    }

    protected void setScore(int score) {
        this.score = score;
    }

    /** Moves the clock on: one {@link #onTick()} per elapsed delay, as the copied timer does. */
    public final void tick(long nowMillis) {
        if (resetAtMillis >= 0L) {
            if (nowMillis > resetAtMillis) {
                resetAtMillis = -1L;
                lastTickMillis = -1L;
                reset();
            }

            return;
        }

        if (!ticking()) return;

        if (lastTickMillis < 0L) {
            lastTickMillis = nowMillis;
        } else if (nowMillis - lastTickMillis >= currentTickDelayMillis()) {
            lastTickMillis = nowMillis;
            onTick();
        }
    }

    /** Clears the clock and starts a fresh game, which is what a difficulty change does too. */
    public final void restart() {
        resetAtMillis = -1L;
        lastTickMillis = -1L;
        reset();
    }

    /** Whether the board is waiting out the pause after a win or a loss. */
    public final boolean waitingForReset() {
        return resetAtMillis >= 0L;
    }

    protected final void scheduleReset(long delayMillis) {
        resetAtMillis = System.currentTimeMillis() + delayMillis;
    }

    /** Changes the grid, for a game whose size comes from a difficulty. */
    protected final void setGrid(int columns, int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    /** Whether the game wants ticks at all right now. */
    protected boolean ticking() {
        return true;
    }

    /** One step of the game, at its own pace. */
    protected abstract void onTick();

    /** A fresh game: empty board, starting score, no pause. */
    public abstract void reset();
}
