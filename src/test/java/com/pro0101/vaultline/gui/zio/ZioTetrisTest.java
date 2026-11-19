package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Tetris, as the copied client's Game panel plays it. */
class ZioTetrisTest {
    private static ZioTetris tetris(long seed) {
        return new ZioTetris(new Random(seed));
    }

    private static void fillRow(ZioTetris game, int row) {
        for (int column = 0; column < game.columns(); column++) {
            game.setShapeAt(column, row, ZioTetrisShape.LShape);
        }
    }

    @Test
    void theBoardIsTwentyTwoSquareOnAFlatFiveHundredMillisecondPace() {
        ZioTetris game = tetris(1);

        assertEquals(22, game.columns());
        assertEquals(22, game.rows());
        assertEquals(500L, game.tickDelayMillis());
        assertEquals(1, game.level());
        assertEquals(0, game.clearedLines());
        assertFalse(game.gameOver());
    }

    @Test
    void theShapesKeepTheCopiedCoordinateTable() {
        ZioTetrisPiece piece = new ZioTetrisPiece();

        piece.setShape(ZioTetrisShape.ZShape);
        assertEquals(0, piece.x(0));
        assertEquals(-1, piece.y(0));
        assertEquals(-1, piece.x(3));
        assertEquals(1, piece.y(3));

        piece.setShape(ZioTetrisShape.LineShape);
        assertEquals(-1, piece.y(0));
        assertEquals(2, piece.y(3));
        assertEquals(-1, piece.minimumY());

        piece.setShape(ZioTetrisShape.SquareShape);
        assertEquals(1, piece.x(1));
        assertEquals(1, piece.y(2));
        assertEquals(0, piece.minimumY());

        piece.setShape(ZioTetrisShape.MirroredLShape);
        assertEquals(1, piece.x(0));
        assertEquals(-1, piece.y(0));
    }

    @Test
    void rotationsTurnTheBlocksAroundThePieceAndTheSquareNeverTurns() {
        ZioTetrisPiece piece = new ZioTetrisPiece();

        piece.setShape(ZioTetrisShape.LShape);

        ZioTetrisPiece left = piece.rotateLeft();
        // (-1,-1) left is (y, -x) = (-1, 1); (0,1) left is (1, 0).
        assertEquals(-1, left.x(0));
        assertEquals(1, left.y(0));
        assertEquals(1, left.x(3));
        assertEquals(0, left.y(3));

        ZioTetrisPiece right = piece.rotateRight();
        // (-1,-1) right is (-y, x) = (1, -1); (0,1) right is (-1, 0).
        assertEquals(1, right.x(0));
        assertEquals(-1, right.y(0));
        assertEquals(-1, right.x(3));
        assertEquals(0, right.y(3));

        ZioTetrisPiece square = new ZioTetrisPiece();
        square.setShape(ZioTetrisShape.SquareShape);
        assertSame(square, square.rotateLeft());
        assertSame(square, square.rotateRight());
    }

    @Test
    void fourRotationsComeBackToTheSamePiece() {
        for (ZioTetrisShape shape : ZioTetrisShape.values()) {
            if (shape == ZioTetrisShape.NoShape) continue;

            ZioTetrisPiece piece = new ZioTetrisPiece();
            piece.setShape(shape);

            ZioTetrisPiece turned = piece;

            for (int turn = 0; turn < 4; turn++) turned = turned.rotateLeft();

            for (int block = 0; block < ZioTetrisPiece.BLOCKS; block++) {
                assertEquals(piece.x(block), turned.x(block), shape + " block " + block);
                assertEquals(piece.y(block), turned.y(block), shape + " block " + block);
            }
        }
    }

    @Test
    void theRandomShapeIsNeverTheEmptyOne() {
        ZioTetrisPiece piece = new ZioTetrisPiece();
        Random random = new Random(4);

        for (int draw = 0; draw < 50; draw++) {
            piece.setRandomShape(random);
            assertTrue(piece.shape() != ZioTetrisShape.NoShape);
        }
    }

    @Test
    void aPieceSpawnsAtTheMiddleOfTheTopRow() {
        for (long seed = 0; seed < 10; seed++) {
            ZioTetris game = tetris(seed);
            ZioTetrisPiece piece = game.piece();

            assertEquals(12, game.pieceX(), "seed " + seed);
            assertEquals(21 + piece.minimumY(), game.pieceY(), "seed " + seed);
            assertTrue(piece.shape() != ZioTetrisShape.NoShape);
        }
    }

    @Test
    void movingMovesWithinTheWallsAndStopsAtThem() {
        ZioTetris game = tetris(6);

        for (int step = 0; step < 12; step++) game.moveLeft();
        assertEquals(0, game.pieceX());

        for (int step = 0; step < 30; step++) game.moveRight();
        assertTrue(game.pieceX() + game.piece().x(3) < game.columns());
        assertFalse(game.gameOver());
    }

    @Test
    void aStepDownDropsByOneUntilThePieceLocks() {
        ZioTetris game = tetris(8);
        int startY = game.pieceY();

        game.stepDown();
        assertEquals(startY - 1, game.pieceY());

        for (int step = 0; step < 60; step++) game.stepDown();

        // Locked: the board holds blocks and a new piece is at the top again.
        boolean boardHasBlocks = false;

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < game.columns(); column++) {
                if (!game.shapeAt(column, row).isEmpty()) boardHasBlocks = true;
            }
        }

        assertTrue(boardHasBlocks);
    }

    @Test
    void aHardDropLocksThePieceAtTheBottom() {
        ZioTetris game = tetris(10);

        game.hardDrop();

        boolean bottomRowUsed = false;

        for (int column = 0; column < game.columns(); column++) {
            if (!game.shapeAt(column, 0).isEmpty()) bottomRowUsed = true;
        }

        assertTrue(bottomRowUsed);
    }

    @Test
    void aFullRowIsClearedAndTheRowsAboveFall() {
        ZioTetris game = tetris(12);

        fillRow(game, 0);
        game.setShapeAt(3, 1, ZioTetrisShape.TShape);

        assertEquals(1, game.removeFullLines());
        assertEquals(1, game.clearedLines());
        assertEquals(1, game.score());
        assertEquals(ZioTetrisShape.TShape, game.shapeAt(3, 0));
        assertTrue(game.shapeAt(3, 1).isEmpty());
        assertTrue(game.waitingAfterLineClear());
        assertEquals(ZioTetrisShape.NoShape, game.piece().shape());
    }

    @Test
    void twoThreeAndFourRowsClearTogether() {
        for (int rows = 1; rows <= 4; rows++) {
            ZioTetris game = tetris(14);

            for (int row = 0; row < rows; row++) fillRow(game, row);

            game.setShapeAt(5, rows, ZioTetrisShape.SShape);

            assertEquals(rows, game.removeFullLines(), rows + " rows");
            assertEquals(rows, game.clearedLines());
            assertEquals(rows, game.score());
            assertEquals(ZioTetrisShape.SShape, game.shapeAt(5, 0));
            assertTrue(game.shapeAt(5, 1).isEmpty());
        }
    }

    @Test
    void aRowWithAGapIsNotCleared() {
        ZioTetris game = tetris(16);

        fillRow(game, 0);
        game.setShapeAt(7, 0, ZioTetrisShape.NoShape);

        assertEquals(0, game.removeFullLines());
        assertEquals(0, game.clearedLines());
        assertFalse(game.waitingAfterLineClear());
    }

    @Test
    void theLevelRisesEveryTenLinesAndThePaceWithIt() {
        ZioTetris game = tetris(18);

        for (int row = 0; row < 10; row++) {
            fillRow(game, row);
            game.removeFullLines();
        }

        assertEquals(10, game.clearedLines());
        assertEquals(2, game.level());
        assertEquals(460L, game.currentTickDelayMillis());

        // Four hundred lines is level 41, and the pace stops shaving at 120 ms.
        for (int round = 0; round < 39; round++) {
            for (int row = 0; row < 10; row++) fillRow(game, row);

            game.removeFullLines();
        }

        assertEquals(400, game.clearedLines());
        assertEquals(41, game.level());
        assertEquals(120L, game.currentTickDelayMillis());
    }

    @Test
    void aBoardThatCannotTakeANewPieceEndsTheGame() {
        ZioTetris game = tetris(20);

        // Dropping piece after piece fills the board until nothing can spawn any more.
        for (int piece = 0; piece < 300 && !game.gameOver(); piece++) {
            game.hardDrop();
            game.tick(1000L + piece * 500L);
        }

        assertTrue(game.gameOver());
        assertFalse(game.ticking());
    }

    @Test
    void theNextTickSpawnsAfterALineClear() {
        ZioTetris game = tetris(22);

        fillRow(game, 0);
        game.removeFullLines();

        assertEquals(ZioTetrisShape.NoShape, game.piece().shape());

        game.tick(1000);
        game.tick(1500);

        assertTrue(game.piece().shape() != ZioTetrisShape.NoShape);
        assertFalse(game.gameOver());
    }

    @Test
    void keysMoveTurnDropAndStartOver() {
        ZioTetris game = tetris(24);
        int startX = game.pieceX();

        assertTrue(game.keyPressed(262));
        assertEquals(startX + 1, game.pieceX());

        assertTrue(game.keyPressed(263));
        assertEquals(startX, game.pieceX());

        assertTrue(game.keyPressed(68));
        assertTrue(game.keyPressed(32));

        assertTrue(game.keyPressed(82));
        assertEquals(0, game.clearedLines());
        assertEquals(0, game.score());
        assertFalse(game.gameOver());

        assertFalse(game.keyPressed(65));
    }

    @Test
    void theGhostFallsAsFarAsThePiece() {
        ZioTetris game = tetris(26);

        int ghostY = game.pieceY();

        while (ghostY > 0 && game.canPlaceAt(game.pieceX(), ghostY - 1)) ghostY--;

        // The ghost stops when the highest block of the shape reaches the bottom row.
        int highestBlock = 0;

        for (int block = 0; block < ZioTetrisPiece.BLOCKS; block++) {
            highestBlock = Math.max(highestBlock, game.piece().y(block));
        }

        assertEquals(highestBlock, ghostY);
        if (ghostY > 0) assertTrue(!game.canPlaceAt(game.pieceX(), ghostY - 1));
    }
}
