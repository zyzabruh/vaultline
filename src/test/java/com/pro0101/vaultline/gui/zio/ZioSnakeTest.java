package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Snake, as the copied client's Game panel plays it. */
class ZioSnakeTest {
    private static final int LEFT = 263;
    private static final int RIGHT = 262;
    private static final int UP = 265;
    private static final int DOWN = 264;

    private static ZioSnake snake(long seed) {
        return new ZioSnake(new Random(seed));
    }

    /** Turns the snake the short way towards its apple and lets it step. */
    private static void chase(ZioSnake snake, long clock) {
        int dx = Math.floorMod(snake.food().x() - snake.head().x() + 11, 22) - 11;
        int dy = Math.floorMod(snake.food().y() - snake.head().y() + 11, 22) - 11;

        if (dx != 0) snake.keyPressed(dx > 0 ? RIGHT : LEFT);
        else if (dy != 0) snake.keyPressed(dy > 0 ? DOWN : UP);

        snake.tick(clock);
    }

    /** Eats apples until the snake has the wanted length, or the board ends the game. */
    private static void growTo(ZioSnake snake, int length) {
        long clock = 1000L;

        for (int step = 0; step < 400 && snake.segments().size() < length && !snake.gameOver(); step++) {
            clock += 500L;
            chase(snake, clock);
        }
    }

    @Test
    void theBoardIsTwentyTwoSquareAndStartsAsOneSegmentAtEleven() {
        ZioSnake snake = snake(1);

        assertEquals(22, snake.columns());
        assertEquals(22, snake.rows());
        assertEquals(1, snake.segments().size());
        assertEquals(new ZioSnake.Segment(11, 11), snake.head());
        assertEquals(ZioSnake.Direction.UP, snake.direction());
        assertEquals(0, snake.score());
        assertFalse(snake.gameOver());
        assertEquals(500L, snake.tickDelayMillis());
    }

    @Test
    void theAppleIsNeverUnderTheSnake() {
        for (long seed = 0; seed < 20; seed++) {
            ZioSnake snake = snake(seed);

            assertFalse(snake.segments().contains(snake.food()));
            assertTrue(snake.food().x() >= 0 && snake.food().x() < 22);
            assertTrue(snake.food().y() >= 0 && snake.food().y() < 22);
        }
    }

    @Test
    void aTickMovesTheHeadOnceAndDoesNotGrowWithoutAnApple() {
        ZioSnake snake = snake(3);

        assertNull(snake.pendingDirection());

        snake.tick(1000);
        assertEquals(new ZioSnake.Segment(11, 11), snake.head());

        snake.tick(2000);
        assertEquals(new ZioSnake.Segment(11, 10), snake.head());
        assertEquals(1, snake.segments().size());
        assertEquals(0, snake.score());
    }

    @Test
    void theGameStepsEveryFiveHundredMillisecondsAndFasterWithSpaceHeld() {
        ZioSnake snake = snake(5);

        snake.tick(1000);
        snake.tick(1499);
        assertEquals(new ZioSnake.Segment(11, 11), snake.head());

        snake.tick(1500);
        assertEquals(new ZioSnake.Segment(11, 10), snake.head());

        // Space held: the same gap of 200 ms is now a step.
        snake.setFast(true);
        snake.tick(1700);
        assertEquals(new ZioSnake.Segment(11, 9), snake.head());

        snake.setFast(false);
        snake.tick(1900);
        assertEquals(new ZioSnake.Segment(11, 9), snake.head());
        snake.tick(2200);
        assertEquals(new ZioSnake.Segment(11, 8), snake.head());
    }

    @Test
    void anArrowTurnsTheSnakeAndTheTurnBackIsRefused() {
        ZioSnake snake = snake(7);

        snake.keyPressed(RIGHT);
        assertEquals(ZioSnake.Direction.RIGHT, snake.pendingDirection());

        snake.tick(1000);
        snake.tick(1500);
        assertEquals(ZioSnake.Direction.RIGHT, snake.direction());
        assertNull(snake.pendingDirection());
        assertEquals(new ZioSnake.Segment(12, 11), snake.head());

        // Straight back on itself is ignored.
        snake.keyPressed(LEFT);
        assertNull(snake.pendingDirection());
        assertEquals(ZioSnake.Direction.RIGHT, snake.direction());
    }

    @Test
    void theEdgesWrapAround() {
        ZioSnake snake = snake(11);

        // Walking up from (11, 11) reaches row 0, then wraps to row 21.
        for (int step = 0; step < 12; step++) snake.tick((step + 2) * 500L);

        assertEquals(0, snake.head().y());

        snake.tick(100_000);
        assertEquals(21, snake.head().y());
        assertFalse(snake.gameOver());
    }

    @Test
    void eatingTheAppleScoresAndGrowsTheSnake() {
        for (long seed = 13; seed < 18; seed++) {
            ZioSnake snake = snake(seed);

            growTo(snake, 2);

            assertEquals(1, snake.score(), "seed " + seed);
            assertEquals(2, snake.segments().size(), "seed " + seed);
            assertFalse(snake.segments().contains(snake.food()), "seed " + seed);
        }
    }

    @Test
    void runningIntoItselfEndsTheGame() {
        ZioSnake snake = snake(17);

        growTo(snake, 6);

        assertEquals(6, snake.segments().size());

        // A tight square, turning clockwise at every step: with six segments the head comes back to
        // its own body on the second lap.
        for (int turn = 0; turn < 12 && !snake.gameOver(); turn++) {
            snake.keyPressed(switch (snake.direction()) {
                case UP -> RIGHT;
                case RIGHT -> DOWN;
                case DOWN -> LEFT;
                case LEFT -> UP;
            });
            snake.tick(1_000_000L + turn * 600L);
        }

        assertTrue(snake.gameOver());
    }

    @Test
    void rStartsOver() {
        ZioSnake snake = snake(19);

        snake.tick(1000);
        snake.tick(2000);

        assertTrue(snake.keyPressed(82));
        assertEquals(new ZioSnake.Segment(11, 11), snake.head());
        assertEquals(ZioSnake.Direction.UP, snake.direction());
        assertEquals(0, snake.score());
        assertEquals(1, snake.segments().size());
        assertFalse(snake.gameOver());
        assertNotNull(snake.food());
    }

    @Test
    void otherKeysAreNotTheGames() {
        ZioSnake snake = snake(23);

        assertFalse(snake.keyPressed(65));
        assertEquals(ZioSnake.Direction.UP, snake.direction());
        assertNull(snake.pendingDirection());
    }
}
