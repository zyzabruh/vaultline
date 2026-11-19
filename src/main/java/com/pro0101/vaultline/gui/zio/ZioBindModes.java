package com.pro0101.vaultline.gui.zio;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The mode of every bind, remembered between launches.
 *
 * <p>Meteor's {@code Keybind} holds a key and nothing else, so the copied client's toggle/hold choice
 * is kept here, one line per bind: {@code module/setting=MODE}, and {@code module=MODE} for a
 * module's own bind. A bind that was never changed is {@link ZioBindMode#TOGGLE}.
 */
public final class ZioBindModes {
    public static final String FILE_NAME = "vaultline-binds.txt";

    private static Path file;
    private static Map<String, ZioBindMode> cached;

    private ZioBindModes() {
    }

    /** Points the store at its file; called once while the mod starts. */
    public static synchronized void bind(Path path) {
        file = path;
        cached = null;
    }

    /** The mode of a bind, the default one until it is changed. */
    public static synchronized ZioBindMode of(String id) {
        return modes().getOrDefault(id, ZioBindMode.TOGGLE);
    }

    /** Steps a bind to its next mode, keeps it and writes the file out. */
    public static synchronized ZioBindMode cycle(String id) {
        ZioBindMode next = of(id).next();

        modes().put(id, next);
        save(file, modes());

        return next;
    }

    private static Map<String, ZioBindMode> modes() {
        if (cached == null) cached = load(file);

        return cached;
    }

    static Map<String, ZioBindMode> load(Path path) {
        if (path == null || !Files.isRegularFile(path)) return new LinkedHashMap<>();

        try {
            return parse(Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            return new LinkedHashMap<>();
        }
    }

    static void save(Path path, Map<String, ZioBindMode> modes) {
        if (path == null) return;

        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);

            Files.writeString(path, format(modes), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // A folder that cannot be written only costs the modes their memory; the screen still works.
        }
    }

    /** One {@code id=MODE} line per bind, in the order the binds were first changed. */
    public static String format(Map<String, ZioBindMode> modes) {
        StringBuilder text = new StringBuilder();

        for (Map.Entry<String, ZioBindMode> entry : modes.entrySet()) {
            text.append(entry.getKey()).append('=').append(entry.getValue().name()).append('\n');
        }

        return text.toString();
    }

    /** Every line that reads as a bind; a damaged line is skipped, not the whole file. */
    public static Map<String, ZioBindMode> parse(String text) {
        Map<String, ZioBindMode> modes = new LinkedHashMap<>();
        if (text == null) return modes;

        for (String line : text.split("\n")) {
            int equals = line.indexOf('=');
            if (equals <= 0 || equals == line.length() - 1) continue;

            String id = line.substring(0, equals).trim();
            String name = line.substring(equals + 1).trim();

            if (id.isEmpty() || !isMode(name)) continue;

            modes.put(id, ZioBindMode.parse(name));
        }

        return modes;
    }

    private static boolean isMode(String name) {
        for (ZioBindMode mode : ZioBindMode.values()) {
            if (mode.name().equalsIgnoreCase(name)) return true;
        }

        return false;
    }
}
