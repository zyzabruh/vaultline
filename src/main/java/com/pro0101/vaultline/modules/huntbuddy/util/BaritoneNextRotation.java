package com.pro0101.vaultline.modules.huntbuddy.util;

import java.lang.reflect.Method;

public final class BaritoneNextRotation {
    private static Method method;
    private static boolean unavailable;

    private BaritoneNextRotation() {
    }

    public static float[] get() {
        if (unavailable) return null;

        try {
            baritone.api.IBaritone primary = baritone.api.BaritoneAPI.getProvider().getPrimaryBaritone();
            if (primary == null) return null;

            Object lookBehavior = primary.getLookBehavior();
            if (lookBehavior == null) return null;

            if (method == null) method = lookBehavior.getClass().getMethod("hunterbuddyNextRotation");
            return (float[]) method.invoke(lookBehavior);
        } catch (NoSuchMethodException | LinkageError e) {
            // The method only exists in the patched Baritone: remember it is missing and stop asking.
            unavailable = true;
            return null;
        } catch (ReflectiveOperationException | RuntimeException e) {
            // The call itself failed this tick; the next one asks again.
            return null;
        }
    }
}
