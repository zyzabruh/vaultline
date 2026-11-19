package com.pro0101.vaultline.modules.hyperglide.utilities;

import com.mojang.authlib.GameProfile;
import com.pro0101.vaultline.mixins.EntityAccessor;
import com.pro0101.vaultline.mixins.InputAccessor;
import net.minecraft.block.Block;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix3x2fStack;

public final class API {
    private static final int points = 32;

    private static final double[] cos = new double[points];
    private static final double[] sin = new double[points];
    private static final double[] angle = new double[points];

    private static final double chord = 2.0 * Math.sin(Math.PI / points);

    private API() {}

    static {
        points();
        angles();
    }

    public static String name(GameProfile profile) {
        return profile.name();
    }

    public static void move(Input input, float forward, float sideways) {
        ((InputAccessor) input).hyperglide$movement(
            new Vec2f(sideways, forward)
        );
    }

    public static Vec3d pos(Entity entity) {
        return entity.getEntityPos();
    }

    /**
     * Returns the position of an entity from the previous tick.
     *
     */
    public static Vec3d prev(Entity entity) {
        EntityAccessor access = (EntityAccessor) entity;
        return new Vec3d(
            access.hyperglide$getX(),
            access.hyperglide$getY(),
            access.hyperglide$getZ()
        );
    }

    /**
     * Sends the specified sneaking state to the server.
     *
     * @param player player whose state is sent
     */
    public static void sneak(ClientPlayerEntity player, boolean value) {
        Packets.input(Packets.sneak(value));
    }

    public static int slot(PlayerInventory inventory) {
        return inventory.getSelectedSlot();
    }

    /**
     * Plays the local placement sound for a block.
     *
     * @param block placed block
     */
    public static void sound(World world, Block block, BlockPos pos) {
        BlockSoundGroup sound = block.getDefaultState().getSoundGroup();

        world.playSoundClient(
            pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
            sound.getPlaceSound(), SoundCategory.BLOCKS,
            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F,
            false
        );
    }

    /**
     * Draws a rotated 2D line at the specified position and angle.
     *
     */
    public static void line(DrawContext context, double px, double py,
        double length, double angle, int width, int color) {

        Matrix3x2fStack matrices = context.getMatrices();

        matrices.pushMatrix();
        matrices.translate((float) px, (float) py);
        matrices.rotate((float) angle);
        matrices.translate(0.0F, (float) (-width * 0.5));

        context.fill(0, 0, (int) Math.ceil(length), width, color);
        matrices.popMatrix();
    }

    /**
     * Draws a 2D circle using the specified position and radius.
     *
     */
    public static void circle(DrawContext context, double px,
        double py, double radius, int width, int color) {

        if (radius <= 0.0) return;
        double length = chord * radius;

        for (int idx = 0; idx < points; idx++) {
            double dx = px + cos[idx] * radius;
            double dy = py + sin[idx] * radius;
            line(context, dx, dy, length, angle[idx], width, color);
        }
    }

    /**
     * Draws an item icon at the specified position and scale.
     *
     */
    public static void item(DrawContext context,
        ItemStack stack, int px, int py, float scale) {

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();

        try {
            matrices.translate(px, py);
            matrices.scale(scale, scale);
            context.drawItem(stack, 0, 0);
        } finally {
            matrices.popMatrix();
        }
    }

    public static void border(DrawContext context,
        int px, int py, int width, int height, int color) {
        context.drawStrokedRectangle(px, py, width, height, color);
    }

    private static void points() {
        for (int idx = 0; idx < points; idx++) {
            double value = 2.0 * Math.PI * idx / points;
            cos[idx] = Math.cos(value);
            sin[idx] = Math.sin(value);
        }
    }

    private static void angles() {
        for (int idx = 0; idx < points; idx++) {
            int next = (idx + 1) & (points - 1);

            double dx = sin[next] - sin[idx];
            double dy = cos[next] - cos[idx];
            angle[idx] = Math.atan2(dx, dy);
        }
    }
}
