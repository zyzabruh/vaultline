package com.pro0101.vaultline.gui.zio;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * The minesweeper board the copied client's Game panel plays, ported from its
 * {@code gui/components/MinesweeperGameComponent} and the three enums it hangs on, and now running on
 * the shared {@link ZioGridGame} tick and score.
 *
 * <p>Everything the game does is here and nothing else: generation after the first click (so the first
 * click is never a mine — {@code generateBoard(firstRow, firstColumn)} skips
 * {@code isInFirstClickNeighborhood}), the fanning out of empty cells, flags capped at the mine count,
 * the score counting down once per second, the explosion that opens every mine and the pause before the
 * board resets. The screen only draws the cells and forwards clicks.
 *
 * <p>The three difficulties are the copied client's own: {@code EASY("Easy", 10, 10, 15, 600)},
 * {@code MEDIUM("Medium", 16, 16, 40, 2400)}, {@code HARD("Hard", 16, 30, 99, 5940)} — columns, rows,
 * mines and starting score.
 */
public final class ZioMinesweeper extends ZioGridGame {
    /** How long the board stays on screen after a loss, and after a win. */
    public static final long RESET_AFTER_LOSS_MS = 2000L;
    public static final long RESET_AFTER_WIN_MS = 3000L;

    /** The score loses one point per second while a board is being played. */
    public static final long SCORE_TICK_MS = 1000L;

    public enum Level {
        EASY("Easy", 10, 10, 15, 600),
        MEDIUM("Medium", 16, 16, 40, 2400),
        HARD("Hard", 16, 30, 99, 5940);

        private final String name;
        private final int columns;
        private final int rows;
        private final int mines;
        private final int initialScore;

        Level(String name, int columns, int rows, int mines, int initialScore) {
            this.name = name;
            this.columns = columns;
            this.rows = rows;
            this.mines = mines;
            this.initialScore = initialScore;
        }

        public String levelName() {
            return name;
        }

        public int columns() {
            return columns;
        }

        public int rows() {
            return rows;
        }

        public int mines() {
            return mines;
        }

        public int initialScore() {
            return initialScore;
        }

        public Level next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public enum Content {
        BOMB,
        NUMBER_0,
        NUMBER_1,
        NUMBER_2,
        NUMBER_3,
        NUMBER_4,
        NUMBER_5,
        NUMBER_6,
        NUMBER_7,
        NUMBER_8;

        static Content fromAdjacentMines(int adjacentMines) {
            return values()[adjacentMines + 1];
        }

        public int number() {
            return this == BOMB ? -1 : ordinal() - 1;
        }
    }

    public enum State {
        COVERED,
        FLAGGED,
        OPEN,
        BLOWN
    }

    /** One cell: what it hides and what it shows. */
    public static final class Cell {
        private Content content = Content.NUMBER_0;
        private State state = State.COVERED;

        public Content content() {
            return content;
        }

        public State state() {
            return state;
        }
    }

    private final Random random;

    private Level level;
    private Cell[][] board;
    private boolean generated;
    private boolean won;
    private boolean lost;
    private int flags;

    public ZioMinesweeper(Level level) {
        this(level, new Random());
    }

    /** The random source is given so a board can be replayed in a test. */
    public ZioMinesweeper(Level level, Random random) {
        super(level.columns(), level.rows(), SCORE_TICK_MS);

        this.random = random;
        this.level = level;
        reset();
    }

    public Level level() {
        return level;
    }

    /** Switches difficulty and starts a fresh board, which is what the header does. */
    public void setLevel(Level level) {
        this.level = level;
        setGrid(level.columns(), level.rows());
        restart();
    }

    @Override
    public void reset() {
        board = new Cell[rows()][columns()];

        for (int row = 0; row < rows(); row++) {
            for (int column = 0; column < columns(); column++) board[row][column] = new Cell();
        }

        generated = false;
        won = false;
        lost = false;
        flags = 0;
        setScore(level.initialScore());
    }

    public Cell cell(int row, int column) {
        if (row < 0 || row >= rows() || column < 0 || column >= columns()) return null;

        return board[row][column];
    }

    public boolean generated() {
        return generated;
    }

    public boolean won() {
        return won;
    }

    public boolean lost() {
        return lost;
    }

    public int flags() {
        return flags;
    }

    /** The number the header shows on the right: mines minus the flags placed. */
    public int remainingMines() {
        return level.mines() - flags;
    }

    public boolean finished() {
        return won || lost;
    }

    /** The score only runs while a generated board is being played. */
    @Override
    protected boolean ticking() {
        return generated && !won && !lost;
    }

    @Override
    protected void onTick() {
        setScore(Math.max(score() - 1, 0));
    }

    /** A left click: reveal, or open the neighbours of an already open cell. */
    public void reveal(int row, int column) {
        if (legal(row, column) && !waitingForReset()) {
            if (!generated) generateBoard(row, column);

            openCell(row, column);

            if (!lost && hasWon()) {
                won = true;
                scheduleReset(RESET_AFTER_WIN_MS);
            }
        }
    }

    /**
     * A right click: place or take back a flag, never more than there are mines.
     *
     * <p>Flagging before the first reveal lays the board out first, with the flagged cell as the safe
     * first click, because the copied client builds its board on the first click on a cell whichever
     * mouse button it was.
     */
    public void flag(int row, int column) {
        if (!legal(row, column) || waitingForReset()) return;

        if (!generated) generateBoard(row, column);

        Cell cell = board[row][column];

        if (cell.state == State.OPEN || cell.state == State.BLOWN) return;

        if (cell.state == State.FLAGGED) {
            cell.state = State.COVERED;
            flags--;
        } else if (flags < level.mines()) {
            cell.state = State.FLAGGED;
            flags++;
        }
    }

    /**
     * Left clicking an open number opens its neighbours once it has as many flags around it as it
     * counts — the usual way to clear a rim, and the guard the copied client's {@code openNeighbors}
     * lacks (it opens the neighbours of any open cell, mines included, so a stray click on a number
     * exploded the board).
     */
    private void chord(int row, int column) {
        Cell cell = board[row][column];
        int number = cell.content.number();

        if (number <= 0 || nearbyFlags(row, column) != number) return;

        openNeighbors(row, column);
    }

    private int nearbyFlags(int row, int column) {
        int nearby = 0;

        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                int neighbourRow = row + rowOffset;
                int neighbourColumn = column + columnOffset;

                if (legal(neighbourRow, neighbourColumn) && board[neighbourRow][neighbourColumn].state == State.FLAGGED) {
                    nearby++;
                }
            }
        }

        return nearby;
    }

    private boolean legal(int row, int column) {
        return row >= 0 && row < rows() && column >= 0 && column < columns();
    }

    /** Places the mines away from the first click and counts the neighbours of every other cell. */
    private void generateBoard(int firstRow, int firstColumn) {
        Set<Integer> mines = new HashSet<>();
        int cellCount = rows() * columns();

        while (mines.size() < level.mines()) {
            int position = random.nextInt(cellCount);

            if (!inFirstClickNeighborhood(firstRow, firstColumn, position)) mines.add(position);
        }

        for (int row = 0; row < rows(); row++) {
            for (int column = 0; column < columns(); column++) {
                Cell cell = board[row][column];

                cell.content = mines.contains(row * columns() + column) ? Content.BOMB : Content.NUMBER_0;
                cell.state = State.COVERED;
            }
        }

        for (int row = 0; row < rows(); row++) {
            for (int column = 0; column < columns(); column++) {
                Cell cell = board[row][column];

                if (cell.content != Content.BOMB) {
                    cell.content = Content.fromAdjacentMines(countAdjacentMines(row, column));
                }
            }
        }

        generated = true;
    }

    /** The clicked cell and its eight neighbours never get a mine. */
    private boolean inFirstClickNeighborhood(int row, int column, int position) {
        int mineRow = position / columns();
        int mineColumn = position % columns();

        return Math.abs(mineRow - row) <= 1 && Math.abs(mineColumn - column) <= 1;
    }

    private void openCell(int row, int column) {
        Cell cell = board[row][column];

        if (cell.state == State.OPEN) {
            chord(row, column);

            return;
        }

        if (cell.state != State.COVERED) return;

        if (cell.content == Content.BOMB) {
            loseGame(cell);

            return;
        }

        if (cell.content == Content.NUMBER_0) openEmptyRegion(row, column);

        cell.state = State.OPEN;
    }

    private void openNeighbors(int row, int column) {
        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                int neighbourRow = row + rowOffset;
                int neighbourColumn = column + columnOffset;

                if (legal(neighbourRow, neighbourColumn) && board[neighbourRow][neighbourColumn].state == State.COVERED) {
                    openCell(neighbourRow, neighbourColumn);
                }
            }
        }
    }

    private void loseGame(Cell explodedCell) {
        for (Cell[] row : board) {
            for (Cell cell : row) {
                if (cell.content == Content.BOMB) cell.state = State.OPEN;
            }
        }

        explodedCell.state = State.BLOWN;
        lost = true;
        setScore(0);
        scheduleReset(RESET_AFTER_LOSS_MS);
    }

    /** Fans out from an empty cell, leaving flags and the numbers on its rim standing. */
    private void openEmptyRegion(int row, int column) {
        ArrayDeque<int[]> pending = new ArrayDeque<>();

        pending.add(new int[]{row, column});

        while (!pending.isEmpty()) {
            int[] position = pending.remove();
            int currentRow = position[0];
            int currentColumn = position[1];
            Cell cell = board[currentRow][currentColumn];

            if (cell.state == State.OPEN) continue;

            cell.state = State.OPEN;

            if (cell.content != Content.NUMBER_0) continue;

            for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
                for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                    if (rowOffset == 0 && columnOffset == 0) continue;

                    int neighbourRow = currentRow + rowOffset;
                    int neighbourColumn = currentColumn + columnOffset;

                    if (legal(neighbourRow, neighbourColumn)) {
                        State state = board[neighbourRow][neighbourColumn].state;

                        if (state != State.FLAGGED && state != State.OPEN) {
                            pending.add(new int[]{neighbourRow, neighbourColumn});
                        }
                    }
                }
            }
        }
    }

    private int countAdjacentMines(int row, int column) {
        int mines = 0;

        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                if (rowOffset == 0 && columnOffset == 0) continue;

                int neighbourRow = row + rowOffset;
                int neighbourColumn = column + columnOffset;

                if (legal(neighbourRow, neighbourColumn) && board[neighbourRow][neighbourColumn].content == Content.BOMB) {
                    mines++;
                }
            }
        }

        return mines;
    }

    /** Every cell that is not a mine is open. */
    private boolean hasWon() {
        for (Cell[] row : board) {
            for (Cell cell : row) {
                if (cell.content != Content.BOMB && cell.state != State.OPEN) return false;
            }
        }

        return true;
    }
}
