package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Game panel's three games, the order its title cycles them in, and where its cells sit. */
class ZioGamePanelTest {
    @Test
    void theGamesCycleInTheCopiedClientsOrder() {
        assertEquals("Tetris", ZioGameType.TETRIS.gameName());
        assertEquals("Snake", ZioGameType.SNAKE.gameName());
        assertEquals("Minesweeper", ZioGameType.MINESWEEPER.gameName());

        assertEquals(ZioGameType.SNAKE, ZioGameType.TETRIS.next());
        assertEquals(ZioGameType.MINESWEEPER, ZioGameType.SNAKE.next());
        assertEquals(ZioGameType.TETRIS, ZioGameType.MINESWEEPER.next());

        ZioGameType type = ZioGameType.TETRIS;

        for (int step = 0; step < 3; step++) type = type.next();

        assertEquals(ZioGameType.TETRIS, type);
    }

    @Test
    void theMinefieldIsAsWideAsItsColumnsAndEightPixelsACell() {
        assertEquals(80, ZioGameLayout.boardWidth(ZioGameType.MINESWEEPER, 10));
        assertEquals(120, ZioGameLayout.boardWidth(ZioGameType.MINESWEEPER, 15));
        assertEquals(72, ZioGameLayout.boardHeight(ZioGameType.MINESWEEPER, 10, 8));
        assertEquals(8, ZioGameLayout.cellSize(ZioGameType.MINESWEEPER, 10, 10));
        assertEquals(14 + 72 + ZioGameLayout.BUTTON_HEIGHT, ZioGameLayout.panelHeight(ZioGameType.MINESWEEPER, 10, 8));
    }

    @Test
    void snakeAndTetrisAreTheCopiedHundredAndTenPixelSquareBoards() {
        for (ZioGameType type : new ZioGameType[]{ZioGameType.SNAKE, ZioGameType.TETRIS}) {
            assertEquals(110, ZioGameLayout.boardWidth(type, 10));
            assertEquals(110, ZioGameLayout.boardHeight(type, 10, 10));
            assertEquals(5, ZioGameLayout.cellSize(type, 22, 10));
            assertEquals(14 + 110 + ZioGameLayout.BUTTON_HEIGHT, ZioGameLayout.panelHeight(type, 10, 10));
            // A minesweeper difficulty change never resizes these two.
            assertEquals(110, ZioGameLayout.boardWidth(type, 30));
        }
    }

    @Test
    void thePanelSitsAtTheRightAboveTheStrip() {
        assertEquals(320 - 110 - 8, ZioGameLayout.panelX(320, 110));
        assertEquals(240 - 50 - 124 - 4, ZioGameLayout.panelY(240, 124, 50));
        // Never above the top edge on a short screen.
        assertEquals(2, ZioGameLayout.panelY(60, 124, 50));
    }

    @Test
    void aVeryNarrowBoardKeepsAReadableTitle() {
        assertEquals(60, ZioGameLayout.panelWidth(ZioGameType.MINESWEEPER, 1));
        assertTrue(ZioGameLayout.panelWidth(ZioGameType.TETRIS, 1) >= 110);
    }
}
