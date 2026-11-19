package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The minesweeper board carried by the copied client's Game panel. */
class ZioMinesweeperTest {
    private static ZioMinesweeper game(ZioMinesweeper.Level level, long seed) {
        return new ZioMinesweeper(level, new Random(seed));
    }

    private static int bombCount(ZioMinesweeper game) {
        int bombs = 0;

        for (int row = 0; row < game.rows(); row++) {
            for (int column = 0; column < game.columns(); column++) {
                if (game.cell(row, column).content() == ZioMinesweeper.Content.BOMB) bombs++;
            }
        }

        return bombs;
    }

    @Test
    void theThreeDifficultiesAreTheCopiedClients() {
        assertEquals(10, ZioMinesweeper.Level.EASY.columns());
        assertEquals(10, ZioMinesweeper.Level.EASY.rows());
        assertEquals(15, ZioMinesweeper.Level.EASY.mines());
        assertEquals(600, ZioMinesweeper.Level.EASY.initialScore());

        assertEquals(16, ZioMinesweeper.Level.MEDIUM.columns());
        assertEquals(16, ZioMinesweeper.Level.MEDIUM.rows());
        assertEquals(40, ZioMinesweeper.Level.MEDIUM.mines());
        assertEquals(2400, ZioMinesweeper.Level.MEDIUM.initialScore());

        assertEquals(16, ZioMinesweeper.Level.HARD.columns());
        assertEquals(30, ZioMinesweeper.Level.HARD.rows());
        assertEquals(99, ZioMinesweeper.Level.HARD.mines());
        assertEquals(5940, ZioMinesweeper.Level.HARD.initialScore());

        assertEquals(ZioMinesweeper.Level.MEDIUM, ZioMinesweeper.Level.EASY.next());
        assertEquals(ZioMinesweeper.Level.HARD, ZioMinesweeper.Level.MEDIUM.next());
        assertEquals(ZioMinesweeper.Level.EASY, ZioMinesweeper.Level.HARD.next());
    }

    @Test
    void aFreshBoardIsCoveredAndScoresItsStartingValue() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 1);

        assertFalse(game.generated());
        assertEquals(600, game.score());
        assertEquals(0, game.flags());
        assertEquals(15, game.remainingMines());

        for (int row = 0; row < game.rows(); row++) {
            for (int column = 0; column < game.columns(); column++) {
                assertEquals(ZioMinesweeper.State.COVERED, game.cell(row, column).state());
            }
        }
    }

    @Test
    void theFirstClickAndItsNeighboursAreAlwaysSafe() {
        for (long seed = 0; seed < 20; seed++) {
            ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, seed);

            game.reveal(4, 4);

            assertTrue(game.generated());
            assertFalse(game.lost());

            for (int row = 3; row <= 5; row++) {
                for (int column = 3; column <= 5; column++) {
                    assertTrue(game.cell(row, column).content() != ZioMinesweeper.Content.BOMB,
                        "mine next to the first click at " + row + "," + column);
                }
            }

            assertEquals(15, bombCount(game));
        }
    }

    @Test
    void everyNumberCountsItsNeighbouringMines() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 7);

        game.reveal(0, 0);

        for (int row = 0; row < game.rows(); row++) {
            for (int column = 0; column < game.columns(); column++) {
                ZioMinesweeper.Cell cell = game.cell(row, column);
                if (cell.content() == ZioMinesweeper.Content.BOMB) continue;

                int neighbours = 0;

                for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
                    for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                        ZioMinesweeper.Cell neighbour = game.cell(row + rowOffset, column + columnOffset);

                        if (neighbour != null && neighbour.content() == ZioMinesweeper.Content.BOMB) neighbours++;
                    }
                }

                assertEquals(neighbours, cell.content().number(), "at " + row + "," + column);
            }
        }
    }

    @Test
    void flagsStopAtTheMineCountAndCanBeTakenBack() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 3);

        int flagged = 0;

        for (int row = 0; row < game.rows() && flagged < 15; row++) {
            for (int column = 0; column < game.columns() && flagged < 15; column++) {
                game.flag(row, column);
                flagged++;
            }
        }

        assertEquals(15, game.flags());
        assertEquals(0, game.remainingMines());

        // A sixteenth flag is refused.
        game.flag(9, 9);
        assertEquals(15, game.flags());

        game.flag(0, 0);
        assertEquals(14, game.flags());

        // Flagging first lays the board out, with that cell and its neighbours safe.
        ZioMinesweeper fresh = game(ZioMinesweeper.Level.EASY, 31);

        fresh.flag(4, 4);

        assertTrue(fresh.generated());
        assertEquals(ZioMinesweeper.State.FLAGGED, fresh.cell(4, 4).state());

        for (int row = 3; row <= 5; row++) {
            for (int column = 3; column <= 5; column++) {
                assertTrue(fresh.cell(row, column).content() != ZioMinesweeper.Content.BOMB);
            }
        }
    }

    @Test
    void revealingAFlaggedOrOpenCellLeavesItAlone() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 5);

        game.flag(2, 2);
        game.reveal(2, 2);

        assertEquals(ZioMinesweeper.State.FLAGGED, game.cell(2, 2).state());
    }

    @Test
    void anEmptyCellFansOutOverItsWholeRegion() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 11);

        game.reveal(5, 5);

        boolean openedAnEmpty = false;

        for (int row = 0; row < game.rows(); row++) {
            for (int column = 0; column < game.columns(); column++) {
                ZioMinesweeper.Cell cell = game.cell(row, column);

                if (cell.state() == ZioMinesweeper.State.OPEN && cell.content() == ZioMinesweeper.Content.NUMBER_0) {
                    openedAnEmpty = true;

                    for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
                        for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                            ZioMinesweeper.Cell neighbour = game.cell(row + rowOffset, column + columnOffset);

                            if (neighbour == null || neighbour.content() == ZioMinesweeper.Content.BOMB) continue;

                            assertEquals(ZioMinesweeper.State.OPEN, neighbour.state(),
                                "an empty cell left a neighbour covered at " + (row + rowOffset) + "," + (column + columnOffset));
                        }
                    }
                }
            }
        }

        assertTrue(openedAnEmpty, "the first click at the middle opened no empty region");
    }

    @Test
    void steppingOnAMineOpensEveryMineAndZeroesTheScore() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 9);

        game.reveal(0, 0);

        int bombRow = -1;
        int bombColumn = -1;

        for (int row = 0; row < game.rows() && bombRow < 0; row++) {
            for (int column = 0; column < game.columns(); column++) {
                if (game.cell(row, column).content() == ZioMinesweeper.Content.BOMB
                    && game.cell(row, column).state() == ZioMinesweeper.State.COVERED) {
                    bombRow = row;
                    bombColumn = column;
                    break;
                }
            }
        }

        assertTrue(bombRow >= 0);

        game.reveal(bombRow, bombColumn);

        assertTrue(game.lost());
        assertTrue(game.finished());
        assertEquals(0, game.score());
        assertEquals(ZioMinesweeper.State.BLOWN, game.cell(bombRow, bombColumn).state());

        for (int row = 0; row < game.rows(); row++) {
            for (int column = 0; column < game.columns(); column++) {
                if (game.cell(row, column).content() == ZioMinesweeper.Content.BOMB) {
                    assertTrue(game.cell(row, column).state() == ZioMinesweeper.State.OPEN
                        || game.cell(row, column).state() == ZioMinesweeper.State.BLOWN);
                }
            }
        }
    }

    @Test
    void openingEverySafeCellWins() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 13);

        game.reveal(0, 0);

        for (int row = 0; row < game.rows(); row++) {
            for (int column = 0; column < game.columns(); column++) {
                if (game.cell(row, column).content() != ZioMinesweeper.Content.BOMB
                    && game.cell(row, column).state() == ZioMinesweeper.State.COVERED) {
                    game.reveal(row, column);
                }
            }
        }

        assertTrue(game.won());
        assertTrue(game.finished());
    }

    @Test
    void anOpenNumberOnlyOpensItsNeighboursOnceItsFlagsMatch() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 29);

        game.reveal(0, 0);

        // A number with no flags around it: a second click leaves the board as it is.
        int numberRow = -1;
        int numberColumn = -1;

        for (int row = 0; row < game.rows() && numberRow < 0; row++) {
            for (int column = 0; column < game.columns(); column++) {
                ZioMinesweeper.Cell cell = game.cell(row, column);

                if (cell.state() == ZioMinesweeper.State.OPEN && cell.content().number() > 0) {
                    numberRow = row;
                    numberColumn = column;
                    break;
                }
            }
        }

        assertTrue(numberRow >= 0);

        int openBefore = countOpen(game);

        game.reveal(numberRow, numberColumn);

        assertEquals(openBefore, countOpen(game));
        assertFalse(game.lost());

        // Now flag every mine around it and click again: the rest of the rim opens, no mine does.
        int number = game.cell(numberRow, numberColumn).content().number();
        int placed = 0;

        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                ZioMinesweeper.Cell neighbour = game.cell(numberRow + rowOffset, numberColumn + columnOffset);

                if (neighbour != null && neighbour.content() == ZioMinesweeper.Content.BOMB
                    && placed < number && neighbour.state() == ZioMinesweeper.State.COVERED) {
                    game.flag(numberRow + rowOffset, numberColumn + columnOffset);
                    placed++;
                }
            }
        }

        game.reveal(numberRow, numberColumn);

        assertFalse(game.lost());

        for (int rowOffset = -1; rowOffset <= 1; rowOffset++) {
            for (int columnOffset = -1; columnOffset <= 1; columnOffset++) {
                ZioMinesweeper.Cell neighbour = game.cell(numberRow + rowOffset, numberColumn + columnOffset);

                if (neighbour != null && neighbour.content() != ZioMinesweeper.Content.BOMB) {
                    assertEquals(ZioMinesweeper.State.OPEN, neighbour.state());
                }
            }
        }
    }

    private static int countOpen(ZioMinesweeper game) {
        int open = 0;

        for (int row = 0; row < game.rows(); row++) {
            for (int column = 0; column < game.columns(); column++) {
                if (game.cell(row, column).state() == ZioMinesweeper.State.OPEN) open++;
            }
        }

        return open;
    }

    @Test
    void theScoreCountsDownOnceASecondAndStopsAtZero() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 17);

        game.reveal(0, 0);

        game.tick(1000);
        assertEquals(600, game.score());

        game.tick(2000);
        assertEquals(599, game.score());

        game.tick(2001);
        assertEquals(599, game.score());

        game.tick(3000);
        assertEquals(598, game.score());

        for (int second = 4; second <= 1000; second++) game.tick(second * 1000L);

        assertEquals(0, game.score());
    }

    @Test
    void aLostBoardStartsOverAfterItsPause() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 19);

        game.reveal(0, 0);
        game.flag(9, 9);

        int bombRow = -1;
        int bombColumn = -1;

        for (int row = 0; row < game.rows() && bombRow < 0; row++) {
            for (int column = 0; column < game.columns(); column++) {
                if (game.cell(row, column).content() == ZioMinesweeper.Content.BOMB) {
                    bombRow = row;
                    bombColumn = column;
                    break;
                }
            }
        }

        game.flag(bombRow, bombColumn);
        game.reveal(bombRow, bombColumn);

        // A flagged mine is not opened, so force the loss on another one.
        if (!game.lost()) {
            for (int row = 0; row < game.rows() && !game.lost(); row++) {
                for (int column = 0; column < game.columns(); column++) {
                    if (!game.lost() && game.cell(row, column).content() == ZioMinesweeper.Content.BOMB) {
                        game.flag(row, column);
                        game.reveal(row, column);
                    }
                }
            }
        }

        assertTrue(game.lost());

        game.tick(System.currentTimeMillis() + ZioMinesweeper.RESET_AFTER_LOSS_MS + 10);

        assertFalse(game.lost());
        assertFalse(game.generated());
        assertEquals(0, game.flags());
        assertEquals(600, game.score());
    }

    @Test
    void clicksOutsideTheBoardDoNothing() {
        ZioMinesweeper game = game(ZioMinesweeper.Level.EASY, 23);

        assertNull(game.cell(-1, 0));
        assertNull(game.cell(0, 10));

        game.reveal(-1, 0);
        game.reveal(0, 10);
        game.flag(10, 0);

        assertFalse(game.generated());
        assertEquals(0, game.flags());
    }

    @Test
    void theSpritesKeepTheClientsSheetOrder() {
        assertEquals(0f, ZioGameSprite.ONE.u());
        assertEquals(0f, ZioGameSprite.ONE.v());
        assertEquals(3f / 8f, ZioGameSprite.EIGHT.u());
        assertEquals(0.5f, ZioGameSprite.FIVE.v());
        assertEquals(0.5f, ZioGameSprite.FLAG.u());
        assertEquals(0.5f, ZioGameSprite.FLAG.v());
        assertEquals(0.75f, ZioGameSprite.BOMB_BLEW.u());
        assertEquals(0.5f, ZioGameSprite.BOMB_BLEW.v());

        assertEquals(ZioGameSprite.UNCOVERED,
            ZioGameSprite.of(ZioMinesweeper.State.COVERED, ZioMinesweeper.Content.NUMBER_0));
        assertEquals(ZioGameSprite.FLAG,
            ZioGameSprite.of(ZioMinesweeper.State.FLAGGED, ZioMinesweeper.Content.BOMB));
        assertEquals(ZioGameSprite.BOMB_BLEW,
            ZioGameSprite.of(ZioMinesweeper.State.BLOWN, ZioMinesweeper.Content.BOMB));
        assertEquals(ZioGameSprite.BOMB,
            ZioGameSprite.of(ZioMinesweeper.State.OPEN, ZioMinesweeper.Content.BOMB));
        assertEquals(ZioGameSprite.EMPTY,
            ZioGameSprite.of(ZioMinesweeper.State.OPEN, ZioMinesweeper.Content.NUMBER_0));
        assertEquals(ZioGameSprite.THREE,
            ZioGameSprite.of(ZioMinesweeper.State.OPEN, ZioMinesweeper.Content.NUMBER_3));
        assertNotNull(ZioGameSprite.valueOf("HOVERED"));
    }

    @Test
    void theGameButtonSitsRightOfTheSizeControls() {
        ZioChrome.Rect plus = ZioChrome.zoomPlus(320, 240);
        ZioChrome.Rect button = ZioChrome.gameButton(320, 240);

        assertEquals(plus.right() + 4, button.x());
        assertEquals(ZioChrome.GAME_BUTTON_WIDTH, button.width());
        assertEquals(ZioListLayout.ROW_HEIGHT, button.height());
        assertTrue(ZioChrome.inStrip(button, 240));
    }
}
