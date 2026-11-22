package com.pro0101.vaultline.util;

import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.systems.modules.Module;

/**
 * Typed access to another module's settings.
 *
 * <p>Meteor hands back {@code Setting<?>}; routing the reads through one named method keeps the unchecked cast in a
 * single documented place instead of scattering it through the flight code.
 */
public final class ModuleSettings {
    private ModuleSettings() {}

    @SuppressWarnings("unchecked")
    public static <T> Setting<T> get(Module module, String name) {
        return (Setting<T>) module.settings.get(name);
    }
}
