package com.pro0101.vaultline.gui.zio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The mode a bind remembers: the default, the cycle, the file it lives in and its damaged lines. */
class ZioBindModesTest {
    @TempDir
    Path folder;

    private Path file() {
        return folder.resolve(ZioBindModes.FILE_NAME);
    }

    @Test
    void aBindThatWasNeverChangedIsAToggle() {
        ZioBindModes.bind(file());

        assertEquals(ZioBindMode.TOGGLE, ZioBindModes.of("combat/killaura"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindModes.of("combat/killaura"), "asking twice changes nothing");
    }

    @Test
    void cyclingStepsTheModeAndWritesItOut() {
        Path file = file();
        ZioBindModes.bind(file);

        assertEquals(ZioBindMode.HOLD, ZioBindModes.cycle("combat/killaura"));
        assertEquals(ZioBindMode.HOLD, ZioBindModes.of("combat/killaura"));
        assertEquals(ZioBindMode.HOLD, ZioBindModes.load(file).get("combat/killaura"));

        // Two steps come back to the behaviour this addon had before modes existed.
        assertEquals(ZioBindMode.TOGGLE, ZioBindModes.cycle("combat/killaura"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindModes.load(file).get("combat/killaura"));
    }

    @Test
    void theModeOfOneBindIsNotTheModeOfAnother() {
        ZioBindModes.bind(file());

        ZioBindModes.cycle("combat/killaura");

        assertEquals(ZioBindMode.HOLD, ZioBindModes.of("combat/killaura"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindModes.of("combat/velocity"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindModes.of("movement/sprint"));
    }

    @Test
    void aWrittenModeIsReadBackByAFreshStore() {
        Path file = file();
        ZioBindModes.bind(file);

        ZioBindModes.cycle("movement/sprint");
        ZioBindModes.cycle("combat/killaura");
        ZioBindModes.cycle("combat/killaura");

        // Rebinding drops the cache, so this reads exactly what is on disk.
        ZioBindModes.bind(file);

        assertEquals(ZioBindMode.HOLD, ZioBindModes.of("movement/sprint"));
        assertEquals(ZioBindMode.TOGGLE, ZioBindModes.of("combat/killaura"));
    }

    @Test
    void aMissingFileIsNoModesAtAll() {
        assertEquals(Map.of(), ZioBindModes.load(folder.resolve("nothing-here.txt")));
    }

    @Test
    void aDamagedLineIsSkippedAndTheOthersSurvive() throws IOException {
        Path file = file();
        Files.writeString(file, """
            combat/killaura=HOLD
            movement/sprint
            =HOLD
            combat/velocity=sideways
            combat/nofall=
            combat/step=TOGGLE
            """, StandardCharsets.UTF_8);

        Map<String, ZioBindMode> modes = ZioBindModes.load(file);

        assertEquals(2, modes.size());
        assertEquals(ZioBindMode.HOLD, modes.get("combat/killaura"));
        assertEquals(ZioBindMode.TOGGLE, modes.get("combat/step"));
    }

    @Test
    void theFileIsOneModePerLineAndReadsBackWhole() {
        Map<String, ZioBindMode> modes = new LinkedHashMap<>();
        modes.put("combat/killaura", ZioBindMode.HOLD);
        modes.put("movement/sprint", ZioBindMode.TOGGLE);

        String text = ZioBindModes.format(modes);

        assertEquals("combat/killaura=HOLD\nmovement/sprint=TOGGLE\n", text);
        assertEquals(modes, ZioBindModes.parse(text));
        assertEquals(Map.of(), ZioBindModes.parse(null));
    }
}
