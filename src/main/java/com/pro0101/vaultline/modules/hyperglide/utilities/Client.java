package com.pro0101.vaultline.modules.hyperglide.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.World;

/**
 * Provides common checks for the current client state.
 */
public final class Client {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private Client() {}

    public static boolean loaded() {
        return client.player != null && client.world != null;
    }

    /**
     * Checks whether the player, world and connection are available.
     *
     * @return true when the client is ready for network actions
     */
    public static boolean ready() {
        return loaded() && client.getNetworkHandler() != null;
    }

    public static boolean interaction() {
        return loaded() && client.interactionManager != null;
    }

    public static boolean overworld() {
        return client.world != null && World.OVERWORLD.equals(
            client.world.getRegistryKey()
        );
    }

    public static boolean nether() {
        return client.world != null && World.NETHER.equals(
            client.world.getRegistryKey()
        );
    }
}
