package com.pro0101.vaultline.gui.zio;

/**
 * The tiles of the game sprite sheet, in the copied client's own order and coordinates.
 *
 * <p>Its {@code MinesweeperSprite} is a plain enum over one texture: sixteen tiles in a grid of eight
 * columns and two rows, each tile a twelfth of the way across and half the way down
 * ({@code U_STEP = 0.125F}, {@code V_STEP = 0.5F}). The order is kept exactly, because the tiles are
 * only meaningful at their index into the sheet: the ones, the cover tile, the empty tile, the bombs,
 * the higher numbers, the flag, and the two special tiles at the end.
 */
public enum ZioGameSprite {
    ONE,
    TWO,
    THREE,
    FOUR,
    UNCOVERED,
    EMPTY,
    BOMB,
    BOMB_HIDDEN,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    FLAG,
    HOVERED,
    BOMB_BLEW,
    FLAG_NO_BG;

    private static final int COLUMNS = 8;

    /** Where the tile starts in the sheet, as a texture coordinate. */
    public float u() {
        return (ordinal() % COLUMNS) / (float) COLUMNS;
    }

    public float v() {
        return (ordinal() / COLUMNS) / 2f;
    }

    /** The tile a cell shows, the way the copied client's {@code MinesweeperCell.getSprite()} picks it. */
    public static ZioGameSprite of(ZioMinesweeper.State state, ZioMinesweeper.Content content) {
        return switch (state) {
            case COVERED -> UNCOVERED;
            case FLAGGED -> FLAG;
            case BLOWN -> BOMB_BLEW;
            case OPEN -> switch (content) {
                case BOMB -> BOMB;
                case NUMBER_0 -> EMPTY;
                case NUMBER_1 -> ONE;
                case NUMBER_2 -> TWO;
                case NUMBER_3 -> THREE;
                case NUMBER_4 -> FOUR;
                case NUMBER_5 -> FIVE;
                case NUMBER_6 -> SIX;
                case NUMBER_7 -> SEVEN;
                case NUMBER_8 -> EIGHT;
            };
        };
    }
}
