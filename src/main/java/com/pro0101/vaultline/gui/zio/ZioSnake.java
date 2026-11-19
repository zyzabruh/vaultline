package com.pro0101.vaultline.gui.zio;

import java.util.Random;

/**
 * Snake, ported from the copied client's {@code SnakeGameComponent}.
 *
 * <p>Its rules, copied rather than invented: a twenty-two by twenty-two grid, the snake starting as a
 * single segment at (11, 11) and heading up, an apple placed at random anywhere the snake is not, a
 * step every 500 ms (200 ms while space is held), edges that wrap to the other side, an apple eaten
 * growing the snake by one and adding one point, and running into its own body ending the game. R
 * starts over.
 */
public final class ZioSnake extends ZioGridGame {
    /** {@code GridGameComponent}'s default grid, which Snake uses. */
    public static final int COLUMNS = 22;
    public static final int ROWS = 22;

    public static final long TICK_MS = 500L;
    public static final long FAST_TICK_MS = 200L;

    /** The head's starting cell: {@code segments.add(new IntPair(11, 11))}. */
    public static final int START_X = 11;
    public static final int START_Y = 11;

    public enum Direction {
        UP(0, -1),
        RIGHT(1, 0),
        DOWN(0, 1),
        LEFT(-1, 0);

        private final int xOffset;
        private final int yOffset;

        Direction(int xOffset, int yOffset) {
            this.xOffset = xOffset;
            this.yOffset = yOffset;
        }

        public int xOffset() {
            return xOffset;
        }

        public int yOffset() {
            return yOffset;
        }

        public Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case RIGHT -> LEFT;
                case DOWN -> UP;
                case LEFT -> RIGHT;
            };
        }
    }

    public record Segment(int x, int y) {
    }

    private final Random random;
    private final java.util.List<Segment> segments = new java.util.ArrayList<>();

    private Direction direction = Direction.UP;
    private Direction pendingDirection;
    private Segment food = new Segment(0, 0);
    private boolean gameOver;
    private boolean fast;

    public ZioSnake() {
        this(new Random());
    }

    /** The random source is given so a game can be replayed in a test. */
    public ZioSnake(Random random) {
        super(COLUMNS, ROWS, TICK_MS);
        this.random = random;
        reset();
    }

    public Direction direction() {
        return direction;
    }

    /** The turn waiting for the next tick, or null. */
    public Direction pendingDirection() {
        return pendingDirection;
    }

    public java.util.List<Segment> segments() {
        return java.util.List.copyOf(segments);
    }

    public Segment head() {
        return segments.isEmpty() ? null : segments.getFirst();
    }

    public Segment food() {
        return food;
    }

    public boolean gameOver() {
        return gameOver;
    }

    /** Space held: the snake steps faster, as {@code getTickDelayMillis()} does. */
    public void setFast(boolean fast) {
        this.fast = fast;
    }

    @Override
    protected long currentTickDelayMillis() {
        return fast ? FAST_TICK_MS : TICK_MS;
    }

    @Override
    protected boolean ticking() {
        return !gameOver;
    }

    @Override
    public void reset() {
        segments.clear();
        segments.add(new Segment(START_X, START_Y));
        direction = Direction.UP;
        pendingDirection = null;
        food = findFoodPosition();
        gameOver = false;
        setScore(0);
    }

    /** Turns the snake, unless the turn is straight back on itself. */
    public void turn(Direction requested) {
        if (requested != null && requested != direction.opposite()) pendingDirection = requested;
    }

    /**
     * One step: the pending turn applies, the head moves and wraps around the edges, running into the
     * body ends the game, and eating the apple grows the snake and scores a point.
     */
    @Override
    protected void onTick() {
        if (gameOver) return;

        if (pendingDirection != null) {
            direction = pendingDirection;
            pendingDirection = null;
        }

        if (segments.isEmpty()) return;

        Segment current = segments.getFirst();
        int nextX = current.x() + direction.xOffset();
        int nextY = current.y() + direction.yOffset();

        if (nextX > columns() - 1) nextX = 0;
        else if (nextX < 0) nextX = columns() - 1;
        else if (nextY > rows() - 1) nextY = 0;
        else if (nextY < 0) nextY = rows() - 1;

        Segment nextHead = new Segment(nextX, nextY);

        if (segments.contains(nextHead)) {
            gameOver = true;

            return;
        }

        segments.addFirst(nextHead);

        if (segments.getFirst().equals(food)) {
            food = findFoodPosition();
            setScore(score() + 1);
        } else {
            segments.removeLast();
        }
    }

    /**
     * The keys the copied component reads: R starts over and the four arrows turn.
     *
     * @param keyCode a GLFW key code
     * @return whether the game used the key
     */
    public boolean keyPressed(int keyCode) {
        return switch (keyCode) {
            case 82 -> {
                restart();
                yield true;
            }
            case 262 -> {
                turn(Direction.RIGHT);
                yield true;
            }
            case 263 -> {
                turn(Direction.LEFT);
                yield true;
            }
            case 264 -> {
                turn(Direction.DOWN);
                yield true;
            }
            case 265 -> {
                turn(Direction.UP);
                yield true;
            }
            default -> false;
        };
    }

    private Segment findFoodPosition() {
        Segment candidate = segments.isEmpty() ? new Segment(START_X, START_Y) : segments.getFirst();

        while (segments.contains(candidate)) {
            candidate = new Segment(random.nextInt(columns()), random.nextInt(rows()));
        }

        return candidate;
    }
}
