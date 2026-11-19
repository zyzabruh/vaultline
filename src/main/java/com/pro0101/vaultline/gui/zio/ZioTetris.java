package com.pro0101.vaultline.gui.zio;

import java.util.Random;

/**
 * Tetris, ported from the copied client's {@code TetrisGameComponent}.
 *
 * <p>Copied from it: a twenty-two by twenty-two board, a piece spawning at {@code columns / 2 + 1} and
 * {@code rows - 1 + piece.minimumY()}, the board indexed {@code row * columns + column} with row 0 at
 * the bottom, {@code pieceY - piece.y(block)} as a block's row, a step every 500 ms and a lock when the
 * piece cannot fall further, a one-tick pause after a line clear before the next piece, a hard drop on
 * space, a manual step on D, turns on the left and right arrows, rotations on the up and down arrows,
 * R starting over, the number of cleared lines shown as the score, and a piece that cannot spawn ending
 * the game.
 *
 * <p>Two deliberate differences, both because the copied code cannot do what the panel needs: its
 * {@code removeFullLines} shifts rows while it scans them, so a double, triple or tetris left the board
 * broken — here every full row is removed at once and the rows above fall by the number removed, which
 * is what the same method is trying to do. And its game has no level, running at a flat 500 ms; since
 * the panel shows speed through a level, the level rises every ten lines and shaves 40 ms off the tick
 * down to a floor of 120 ms.
 */
public final class ZioTetris extends ZioGridGame {
    /** The grid the copied component passes: {@code super(panel, yOffset, 22, 22)}. */
    public static final int COLUMNS = 22;
    public static final int ROWS = 22;

    public static final long TICK_MS = 500L;
    public static final long MIN_TICK_MS = 120L;
    public static final long LEVEL_SPEEDUP_MS = 40L;
    public static final int LINES_PER_LEVEL = 10;

    private final Random random;

    private ZioTetrisShape[] board = new ZioTetrisShape[COLUMNS * ROWS];
    private ZioTetrisPiece piece = new ZioTetrisPiece();
    private int pieceX;
    private int pieceY;
    private int clearedLines;
    private boolean waitingAfterLineClear;
    private boolean gameOver;

    public ZioTetris() {
        this(new Random());
    }

    /** The random source is given so a sequence of pieces can be replayed in a test. */
    public ZioTetris(Random random) {
        super(COLUMNS, ROWS, TICK_MS);
        this.random = random;
        reset();
    }

    public ZioTetrisPiece piece() {
        return piece;
    }

    public int pieceX() {
        return pieceX;
    }

    public int pieceY() {
        return pieceY;
    }

    public int clearedLines() {
        return clearedLines;
    }

    public boolean gameOver() {
        return gameOver;
    }

    /** Whether a line was cleared and the next piece is waiting for the next tick. */
    public boolean waitingAfterLineClear() {
        return waitingAfterLineClear;
    }

    /** The level the panel shows: one, then one more every ten cleared lines. */
    public int level() {
        return 1 + clearedLines / LINES_PER_LEVEL;
    }

    @Override
    protected long currentTickDelayMillis() {
        return Math.max(MIN_TICK_MS, TICK_MS - (level() - 1) * LEVEL_SPEEDUP_MS);
    }

    @Override
    protected boolean ticking() {
        return !gameOver;
    }

    /** What sits at a board cell: row 0 is the bottom row. */
    public ZioTetrisShape shapeAt(int column, int row) {
        if (column < 0 || column >= columns() || row < 0 || row >= rows()) return ZioTetrisShape.NoShape;

        return board[row * columns() + column];
    }

    /** Board editing, used by the tests to set a row up before clearing it. */
    void setShapeAt(int column, int row, ZioTetrisShape shape) {
        board[row * columns() + column] = shape;
    }

    @Override
    public void reset() {
        gameOver = false;
        waitingAfterLineClear = false;
        clearedLines = 0;
        setScore(0);
        piece = new ZioTetrisPiece();
        board = new ZioTetrisShape[columns() * rows()];

        for (int cell = 0; cell < board.length; cell++) board[cell] = ZioTetrisShape.NoShape;

        spawnPiece();
    }

    @Override
    protected void onTick() {
        if (piece == null) {
            reset();

            return;
        }

        if (waitingAfterLineClear) {
            waitingAfterLineClear = false;
            spawnPiece();
        } else {
            stepDown();
        }
    }

    /** One row down, locking the piece when it cannot go any further. */
    public void stepDown() {
        if (!tryMove(piece, pieceX, pieceY - 1)) lockPiece();
    }

    /** Space: fall as far as it goes, then lock. */
    public void hardDrop() {
        while (tryMove(piece, pieceX, pieceY - 1)) {
            // Falls until it cannot.
        }

        lockPiece();
    }

    public void moveLeft() {
        tryMove(piece, pieceX - 1, pieceY);
    }

    public void moveRight() {
        tryMove(piece, pieceX + 1, pieceY);
    }

    public void rotateLeft() {
        tryMove(piece.rotateLeft(), pieceX, pieceY);
    }

    public void rotateRight() {
        tryMove(piece.rotateRight(), pieceX, pieceY);
    }

    /**
     * The keys the copied component reads: space drops, D steps, R starts over, the left and right
     * arrows move and the up and down arrows turn.
     *
     * @param keyCode a GLFW key code
     * @return whether the game used the key
     */
    public boolean keyPressed(int keyCode) {
        switch (keyCode) {
            case 32 -> hardDrop();
            case 68 -> stepDown();
            case 82 -> {
                restart();
                return true;
            }
            case 262 -> moveRight();
            case 263 -> moveLeft();
            case 264 -> rotateRight();
            case 265 -> rotateLeft();
            default -> {
                return false;
            }
        }

        return true;
    }

    /** Places the piece in the board and clears the rows it completed. */
    private void lockPiece() {
        if (piece.shape() == ZioTetrisShape.NoShape) return;

        for (int block = 0; block < ZioTetrisPiece.BLOCKS; block++) {
            int blockX = pieceX + piece.x(block);
            int blockY = pieceY - piece.y(block);

            if (blockX >= 0 && blockX < columns() && blockY >= 0 && blockY < rows()) {
                setShapeAt(blockX, blockY, piece.shape());
            }
        }

        removeFullLines();

        if (!waitingAfterLineClear) spawnPiece();
    }

    /**
     * Removes every full row at once and drops the rows above it by as many rows as were removed.
     *
     * @return how many rows went
     */
    int removeFullLines() {
        int removed = 0;
        int writeRow = 0;

        for (int readRow = 0; readRow < rows(); readRow++) {
            if (!isFull(readRow)) {
                if (readRow != writeRow) {
                    for (int column = 0; column < columns(); column++) {
                        setShapeAt(column, writeRow, shapeAt(column, readRow));
                    }
                }

                writeRow++;
            } else {
                removed++;
            }
        }

        for (int row = writeRow; row < rows(); row++) {
            for (int column = 0; column < columns(); column++) setShapeAt(column, row, ZioTetrisShape.NoShape);
        }

        if (removed > 0) {
            clearedLines += removed;
            setScore(clearedLines);
            waitingAfterLineClear = true;
            piece.setShape(ZioTetrisShape.NoShape);
        }

        return removed;
    }

    private boolean isFull(int row) {
        for (int column = 0; column < columns(); column++) {
            if (shapeAt(column, row).isEmpty()) return false;
        }

        return true;
    }

    private void spawnPiece() {
        piece.setRandomShape(random);
        pieceX = columns() / 2 + 1;
        pieceY = rows() - 1 + piece.minimumY();

        if (!canPlace(piece, pieceX, pieceY)) {
            piece.setShape(ZioTetrisShape.NoShape);
            gameOver = true;
        }
    }

    /** Whether the falling piece could stand at a cell: what the ghost's drop loop asks. */
    public boolean canPlaceAt(int targetX, int targetY) {
        return canPlace(piece, targetX, targetY);
    }

    private boolean canPlace(ZioTetrisPiece candidate, int targetX, int targetY) {
        for (int block = 0; block < ZioTetrisPiece.BLOCKS; block++) {
            int blockX = targetX + candidate.x(block);
            int blockY = targetY - candidate.y(block);

            if (blockX < 0 || blockX >= columns() || blockY < 0 || blockY >= rows()) return false;
            if (!shapeAt(blockX, blockY).isEmpty()) return false;
        }

        return true;
    }

    private boolean tryMove(ZioTetrisPiece candidate, int targetX, int targetY) {
        if (!canPlace(candidate, targetX, targetY)) return false;

        piece = candidate;
        pieceX = targetX;
        pieceY = targetY;

        return true;
    }
}
