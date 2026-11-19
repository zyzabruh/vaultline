package com.pro0101.vaultline.gui.zio;

/**
 * Where the Game panel and its cells sit, without any drawing.
 *
 * <p>The panel keeps the copied client's sizes: a minesweeper cell is eight pixels and the panel is as
 * wide as its columns, while Snake and Tetris are given a fixed 110 pixels and a square board, so
 * their twenty-two cells come out at five pixels — {@code GamePanel.getWidth()} returns 110 for
 * anything but minesweeper, and those two games report the panel's width as their height.
 *
 * <p>The one thing added here is the title bar every other panel in this screen has, so the panel is
 * fourteen pixels taller than its board.
 */
public final class ZioGameLayout {
    /** The copied client's cell size: {@code 8 * gridColumns} for the width. */
    public static final int MINESWEEPER_CELL = 8;

    /** {@code GamePanel.getWidth()} for the two games that are not minesweeper. */
    public static final int FIXED_BOARD = 110;

    public static final int HEADER_HEIGHT = 14;
    public static final int MARGIN = 8;

    /**
     * The button the copied panel keeps under its board: its label is the game in use, and a click on
     * it steps to the next one. One row of this screen.
     */
    public static final int BUTTON_HEIGHT = 13;

    private ZioGameLayout() {
    }

    public static int boardWidth(ZioGameType type, int minesweeperColumns) {
        return type == ZioGameType.MINESWEEPER ? MINESWEEPER_CELL * minesweeperColumns : FIXED_BOARD;
    }

    /** Minesweeper carries a row of its own above the minefield; the other two are square. */
    public static int boardHeight(ZioGameType type, int minesweeperColumns, int minesweeperRows) {
        return type == ZioGameType.MINESWEEPER ? MINESWEEPER_CELL * (minesweeperRows + 1) : boardWidth(type, minesweeperColumns);
    }

    public static int panelWidth(ZioGameType type, int minesweeperColumns) {
        return Math.max(boardWidth(type, minesweeperColumns), 60);
    }

    public static int panelHeight(ZioGameType type, int minesweeperColumns, int minesweeperRows) {
        return HEADER_HEIGHT + boardHeight(type, minesweeperColumns, minesweeperRows) + BUTTON_HEIGHT;
    }

    /** The side of one cell: eight on the minefield, five on the 110 pixel boards. */
    public static int cellSize(ZioGameType type, int columns, int minesweeperColumns) {
        if (type == ZioGameType.MINESWEEPER) return MINESWEEPER_CELL;

        return columns <= 0 ? 0 : FIXED_BOARD / columns;
    }

    public static int panelX(int viewWidth, int width) {
        return viewWidth - width - MARGIN;
    }

    /** Sits above the strip the chrome keeps free at the bottom, like the presets panel at the top. */
    public static int panelY(int viewHeight, int height, int stripMargin) {
        return Math.max(2, viewHeight - stripMargin - height - 4);
    }
}
