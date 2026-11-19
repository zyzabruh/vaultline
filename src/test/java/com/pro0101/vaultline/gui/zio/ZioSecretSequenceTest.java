package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The copied client's secret sequence: its ten keys, and the index its screen keeps. */
class ZioSecretSequenceTest {
    @Test
    void theSequenceIsTheCopiedClients() {
        assertEquals(10, ZioSecretSequence.length());
        assertEquals(265, ZioSecretSequence.KEYS[0]);
        assertEquals(265, ZioSecretSequence.KEYS[1]);
        assertEquals(264, ZioSecretSequence.KEYS[2]);
        assertEquals(264, ZioSecretSequence.KEYS[3]);
        assertEquals(263, ZioSecretSequence.KEYS[4]);
        assertEquals(262, ZioSecretSequence.KEYS[5]);
        assertEquals(263, ZioSecretSequence.KEYS[6]);
        assertEquals(262, ZioSecretSequence.KEYS[7]);
        assertEquals(66, ZioSecretSequence.KEYS[8]);
        assertEquals(65, ZioSecretSequence.KEYS[9]);
    }

    @Test
    void thePanelIsUnlockedOnlyByTheLastKey() {
        int progress = 0;

        for (int index = 0; index < ZioSecretSequence.length(); index++) {
            assertFalse(ZioSecretSequence.complete(progress));

            progress = ZioSecretSequence.accept(progress, ZioSecretSequence.KEYS[index]);
            assertEquals(index + 1, progress);
        }

        assertTrue(ZioSecretSequence.complete(progress));
    }

    @Test
    void aWrongKeyTakesTheIndexBackToTheStart() {
        int progress = 0;

        for (int index = 0; index < 4; index++) {
            progress = ZioSecretSequence.accept(progress, ZioSecretSequence.KEYS[index]);
        }

        assertEquals(4, progress);
        assertEquals(0, ZioSecretSequence.accept(progress, ZioSecretSequence.DOWN));
        assertEquals(0, ZioSecretSequence.accept(2, ZioSecretSequence.B));
    }

    @Test
    void theFirstUpBeginsTheSequenceAgain() {
        // The client's own check resets the index and does not retest the key that reset it, so the
        // next up press is what starts the sequence over.
        assertEquals(0, ZioSecretSequence.accept(1, ZioSecretSequence.LEFT));
        assertEquals(1, ZioSecretSequence.accept(0, ZioSecretSequence.UP));
    }

    @Test
    void anIndexPastTheEndStartsOverRatherThanRunningOn() {
        int complete = ZioSecretSequence.length();

        assertEquals(0, ZioSecretSequence.accept(complete, ZioSecretSequence.KEYS[0]));
        assertEquals(0, ZioSecretSequence.accept(-1, ZioSecretSequence.KEYS[0]));
    }

    @Test
    void theTwoLetterEndNeedsBothLettersInOrder() {
        int progress = 8;

        assertEquals(9, ZioSecretSequence.accept(progress, ZioSecretSequence.B));
        assertEquals(0, ZioSecretSequence.accept(9, ZioSecretSequence.B));
        assertEquals(10, ZioSecretSequence.accept(9, ZioSecretSequence.A));
    }
}
