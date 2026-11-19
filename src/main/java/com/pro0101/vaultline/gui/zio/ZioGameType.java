package com.pro0101.vaultline.gui.zio;

/**
 * The games the copied client's Game panel cycles through, in its own order:
 * {@code GameType { TETRIS("Tetris"), SNAKE("Snake"), MINESWEEPER("Minesweeper") }}.
 *
 * <p>Its panel shows the current name on a button and moves to the next entry on a click, which is
 * the order kept here.
 */
public enum ZioGameType {
    TETRIS("Tetris"),
    SNAKE("Snake"),
    MINESWEEPER("Minesweeper");

    private final String gameName;

    ZioGameType(String gameName) {
        this.gameName = gameName;
    }

    public String gameName() {
        return gameName;
    }

    /** The next game a click on the panel's title moves to. */
    public ZioGameType next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
