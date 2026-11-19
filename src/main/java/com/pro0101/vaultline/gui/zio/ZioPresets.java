package com.pro0101.vaultline.gui.zio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Names and ordering for the preset panel.
 *
 * <p>A preset is a Meteor profile, so its name ends up as a folder on disk: the panel cleans what is
 * typed here before anything is written, and this is the part of that work that can be checked
 * without a game client.
 */
public final class ZioPresets {
    public static final int MAX_NAME = 24;

    private ZioPresets() {
    }

    /**
     * A typed name reduced to what a profile can be called: letters, digits, dashes and single
     * spaces, trimmed and cut to {@link #MAX_NAME}.
     */
    public static String normalise(String raw) {
        if (raw == null) return "";

        StringBuilder builder = new StringBuilder();
        boolean space = false;

        for (char character : raw.trim().toCharArray()) {
            if (builder.length() >= MAX_NAME) break;

            if (Character.isLetterOrDigit(character) || character == '-' || character == '_') {
                builder.append(character);
                space = false;
            } else if (Character.isWhitespace(character) && !space && !builder.isEmpty()) {
                builder.append(' ');
                space = true;
            }
        }

        String name = builder.toString();

        return name.endsWith(" ") ? name.substring(0, name.length() - 1) : name;
    }

    /** Whether a typed name leaves anything usable once normalised. */
    public static boolean isValid(String name) {
        return !normalise(name).isEmpty();
    }

    /**
     * The name to save under: the wanted one when it is free, otherwise the first free
     * {@code name 2}, {@code name 3}… so saving never overwrites a preset by accident.
     */
    public static String uniqueName(String wanted, Collection<String> taken) {
        String base = normalise(wanted);
        if (base.isEmpty()) return "";

        List<String> existing = new ArrayList<>();

        for (String name : taken) {
            if (name != null) existing.add(name.toLowerCase(Locale.ROOT));
        }

        if (!existing.contains(base.toLowerCase(Locale.ROOT))) return base;

        for (int suffix = 2; suffix < 10_000; suffix++) {
            String tail = " " + suffix;
            String root = base.length() + tail.length() > MAX_NAME
                ? base.substring(0, Math.max(1, MAX_NAME - tail.length()))
                : base;
            String candidate = root + tail;

            if (!existing.contains(candidate.toLowerCase(Locale.ROOT))) return candidate;
        }

        return base;
    }

    /** The names to show, in display order, without blanks or case insensitive duplicates. */
    public static List<String> sorted(Collection<String> names) {
        List<String> kept = new ArrayList<>();
        List<String> seen = new ArrayList<>();

        for (String name : names) {
            if (name == null || name.isBlank()) continue;

            String key = name.toLowerCase(Locale.ROOT);
            if (seen.contains(key)) continue;

            seen.add(key);
            kept.add(name);
        }

        kept.sort((first, second) -> first.compareToIgnoreCase(second));

        return kept;
    }
}
