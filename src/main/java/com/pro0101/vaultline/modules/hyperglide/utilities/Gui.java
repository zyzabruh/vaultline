package com.pro0101.vaultline.modules.hyperglide.utilities;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.math.Vec2f;
import org.lwjgl.glfw.GLFW;

public final class Gui {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private Gui() {}

    public static boolean interactive() {
        if (client.currentScreen == null) return false;
        if (client.currentScreen instanceof ChatScreen) return true;

        return client.currentScreen.getClass().getName().startsWith(
            "meteordevelopment.meteorclient.gui."
        );
    }

    public static boolean vanilla() {
        if (client.currentScreen == null ||
            client.currentScreen instanceof ChatScreen) return false;

        return client.currentScreen.getClass().getName().startsWith(
            "net.minecraft.client.gui.screen."
        );
    }

    public static boolean pressed(int button) {
        return GLFW.glfwGetMouseButton(
            client.getWindow().getHandle(), button
        ) == GLFW.GLFW_PRESS;
    }

    public static Vec2f mouse() {
        int width = client.getWindow().getWidth();
        int height = client.getWindow().getHeight();

        int swidth = client.getWindow().getScaledWidth();
        int sheight = client.getWindow().getScaledHeight();

        return new Vec2f(
            (float) (client.mouse.getX() * swidth / width),
            (float) (client.mouse.getY() * sheight / height)
        );
    }
}
