package com.pro0101.vaultline.gui.zio;

/**
 * The seven pieces, in the copied client's own order and colours
 * ({@code TetrisShape { NoShape, ZShape, SShape, LineShape, TShape, SquareShape, LShape,
 * MirroredLShape }}), because {@code TetrisPiece} indexes its coordinate table by this ordinal.
 *
 * <p>The colours are its {@code java.awt.Color} values as ARGB: Z {@code (204, 102, 102)}, S
 * {@code (102, 204, 102)}, line {@code (102, 102, 204)}, T {@code (204, 204, 102)}, square
 * {@code (204, 102, 204)}, L {@code (102, 204, 204)}, mirrored L {@code (218, 170, 0)}.
 */
public enum ZioTetrisShape {
    NoShape(0xFF000000),
    ZShape(0xFFCC6666),
    SShape(0xFF66CC66),
    LineShape(0xFF6666CC),
    TShape(0xFFCCCC66),
    SquareShape(0xFFCC66CC),
    LShape(0xFF66CCCC),
    MirroredLShape(0xFFDAAA00);

    private final int argb;

    ZioTetrisShape(int argb) {
        this.argb = argb;
    }

    public int argb() {
        return argb;
    }

    public boolean isEmpty() {
        return this == NoShape;
    }
}
