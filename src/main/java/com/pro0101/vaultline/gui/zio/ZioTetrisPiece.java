package com.pro0101.vaultline.gui.zio;

import java.util.Random;

/**
 * A falling piece, ported from the copied client's {@code TetrisPiece}: the same coordinate table, the
 * same rotations and the same random choice.
 *
 * <p>The table is indexed by {@link ZioTetrisShape#ordinal()} and gives the four blocks of each shape
 * in its own coordinate frame, where the piece sits at {@code (pieceX + x, pieceY - y)} on the board:
 *
 * <pre>
 * NoShape       (0,0) (0,0) (0,0) (0,0)
 * ZShape        (0,-1) (0,0) (-1,0) (-1,1)
 * SShape        (0,-1) (0,0) (1,0) (1,1)
 * LineShape     (0,-1) (0,0) (0,1) (0,2)
 * TShape        (-1,0) (0,0) (1,0) (0,1)
 * SquareShape   (0,0) (1,0) (0,1) (1,1)
 * LShape        (-1,-1) (0,-1) (0,0) (0,1)
 * MirroredLShape (1,-1) (0,-1) (0,0) (0,1)
 * </pre>
 *
 * <p>A rotation turns every block around the piece's own frame — left {@code (x, y) -> (y, -x)}, right
 * {@code (x, y) -> (-y, x)} — and the square never turns, as in the copied class.
 */
public final class ZioTetrisPiece {
    private static final int[][][] SHAPES = {
        {{0, 0}, {0, 0}, {0, 0}, {0, 0}},
        {{0, -1}, {0, 0}, {-1, 0}, {-1, 1}},
        {{0, -1}, {0, 0}, {1, 0}, {1, 1}},
        {{0, -1}, {0, 0}, {0, 1}, {0, 2}},
        {{-1, 0}, {0, 0}, {1, 0}, {0, 1}},
        {{0, 0}, {1, 0}, {0, 1}, {1, 1}},
        {{-1, -1}, {0, -1}, {0, 0}, {0, 1}},
        {{1, -1}, {0, -1}, {0, 0}, {0, 1}}
    };

    public static final int BLOCKS = 4;

    private final int[][] coordinates = new int[BLOCKS][2];
    private ZioTetrisShape shape = ZioTetrisShape.NoShape;

    public ZioTetrisShape shape() {
        return shape;
    }

    public void setShape(ZioTetrisShape shape) {
        this.shape = shape;

        int[][] source = SHAPES[shape.ordinal()];

        for (int block = 0; block < BLOCKS; block++) {
            System.arraycopy(source[block], 0, coordinates[block], 0, 2);
        }
    }

    public int x(int block) {
        return coordinates[block][0];
    }

    public int y(int block) {
        return coordinates[block][1];
    }

    public void setX(int block, int x) {
        coordinates[block][0] = x;
    }

    public void setY(int block, int y) {
        coordinates[block][1] = y;
    }

    /** A copy of this piece, so rotations never touch the falling piece. */
    public ZioTetrisPiece copy() {
        ZioTetrisPiece copy = new ZioTetrisPiece();

        copy.shape = shape;

        for (int block = 0; block < BLOCKS; block++) {
            copy.setX(block, x(block));
            copy.setY(block, y(block));
        }

        return copy;
    }

    /**
     * Picks one of the seven shapes, never {@code NoShape}: the copied class draws
     * {@code shapes[random.nextInt(1, shapes.length)]}.
     */
    public void setRandomShape(Random random) {
        ZioTetrisShape[] shapes = ZioTetrisShape.values();

        setShape(shapes[random.nextInt(1, shapes.length)]);
    }

    /** The lowest block, used to put a new piece at the top of the board. */
    public int minimumY() {
        int minimum = coordinates[0][1];

        for (int block = 1; block < BLOCKS; block++) minimum = Math.min(minimum, coordinates[block][1]);

        return minimum;
    }

    public ZioTetrisPiece rotateLeft() {
        return rotated(true);
    }

    public ZioTetrisPiece rotateRight() {
        return rotated(false);
    }

    private ZioTetrisPiece rotated(boolean left) {
        if (shape == ZioTetrisShape.SquareShape) return this;

        ZioTetrisPiece result = new ZioTetrisPiece();

        result.shape = shape;

        for (int block = 0; block < BLOCKS; block++) {
            int x = x(block);
            int y = y(block);

            result.setX(block, left ? y : -y);
            result.setY(block, left ? -x : x);
        }

        return result;
    }
}
