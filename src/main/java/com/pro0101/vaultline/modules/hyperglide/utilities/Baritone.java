package com.pro0101.vaultline.modules.hyperglide.utilities;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.calc.IPath;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalNear;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.movement.IMovement;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.input.Input;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.List;

/**
 * Handles Baritone pathing, elytra flight and movement inputs.
 */
public final class Baritone {
    private static final MinecraftClient client = MinecraftClient.getInstance();

    private static boolean paused;

    private Baritone() {}

    public static void settings(double speed, double avoid, boolean predict) {
        Settings config = BaritoneAPI.getSettings();

        config.allowPlace.value = true;
        config.allowInventory.value = true;
        config.elytraTermsAccepted.value = true;

        config.elytraFireworkSpeed.value = speed;
        config.elytraMinimumAvoidance.value = avoid;
        config.elytraPredictTerrain.value = predict;
    }


    public static boolean loaded() {
        return instance().getElytraProcess().isLoaded();
    }

    public static boolean elytra() {
        return destination() != null;
    }

    public static BlockPos destination() {
        return instance().getElytraProcess().currentDestination();
    }

    public static boolean pathing() {
        IBaritone baritone = instance();
        return baritone.getCustomGoalProcess().isActive()
            || baritone.getPathingBehavior().isPathing();
    }

    public static void fly(BlockPos pos, boolean exact) {
        instance().getElytraProcess().pathTo(goal(pos, exact));
    }

    /**
     * Starts walking to an exact block using normal pathing.
     *
     */
    public static void walk(BlockPos pos) {
        walk(pos, true);
    }

    public static void walk(BlockPos pos, boolean exact) {
        instance().getCustomGoalProcess().setGoalAndPath(goal(pos, exact));
    }

    /**
     * Starts walking to a destination within a given radius.
     *
     */
    public static void near(BlockPos pos, int radius) {
        GoalNear goal = new GoalNear(pos, radius);
        instance().getCustomGoalProcess().setGoalAndPath(goal);
    }

    public static void cancel() {
        instance().getPathingBehavior().cancelEverything();
    }

    /**
     * Pauses the current pathing process until it is resumed.
     */
    public static void pause() {
        if (paused) return;
        paused = instance().getCommandManager().execute("pause");
    }

    /**
     * Resumes the pathing process after it has been paused.
     */
    public static void resume() {
        if (!paused) return;
        client.options.forwardKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        paused = !instance().getCommandManager().execute("resume");
    }

    public static void stop() {
        IBaritone baritone = instance();

        baritone.getElytraProcess().onLostControl();
        baritone.getCustomGoalProcess().onLostControl();
        baritone.getBuilderProcess().onLostControl();
        baritone.getPathingBehavior().forceCancel();
    }

    public static void clear() {
        instance().getInputOverrideHandler().clearAllKeys();
    }

    public static void forward(boolean state) {
        input(Input.MOVE_FORWARD, state);
    }

    public static void back(boolean state) {
        input(Input.MOVE_BACK, state);
    }

    public static void left(boolean state) {
        input(Input.MOVE_LEFT, state);
    }

    public static void right(boolean state) {
        input(Input.MOVE_RIGHT, state);
    }

    public static void sprint(boolean state) {
        input(Input.SPRINT, state);
    }

    private static Goal goal(BlockPos pos, boolean exact) {
        if (exact) return new GoalBlock(pos);
        return new GoalXZ(pos.getX(), pos.getZ());
    }

    private static void input(Input input, boolean state) {
        instance().getInputOverrideHandler().setInputForceState(input, state);
    }

    private static IBaritone instance() {
        return BaritoneAPI.getProvider().getPrimaryBaritone();
    }



    /**
     * Helps Baritone step onto blocks it needs to place.
     */
    public static void assist() {
        if (!supporting()) resume();
    }

    public static boolean supporting() {
        IBaritone baritone = instance();

        IMovement movement = movement(baritone);
        if (movement == null || !rising(movement)) {
            return false;
        }

        IPlayerContext context = baritone.getPlayerContext();
        if (context.player() == null || context.player().isGliding() ||
            !context.player().isOnGround() || context.world() == null) {
            return false;
        }

        BlockPos pos = movement.getDest().down();
        if (!context.world().getBlockState(pos).isReplaceable()) {
            return false;
        }

        BlockPos source = movement.getSrc();
        return !aligned(context, source) && move(context, source);
    }

    private static IMovement movement(IBaritone baritone) {
        IPathingBehavior behaviour = baritone.getPathingBehavior();

        IPathExecutor current = behaviour.getCurrent();
        if (current == null) return null;

        IPath path = current.getPath();
        int index = current.getPosition();

        List<IMovement> movements = path.movements();
        if (index >= 0 && index < movements.size()) {
            return movements.get(index);
        } else {
            return null;
        }
    }

    private static boolean move(IPlayerContext context, BlockPos pos) {
        pause();

        double dx = pos.getX() - context.player().getX();
        double dz = pos.getZ() - context.player().getZ();
        double yaw = Math.atan2(-(dx + 0.5), dz + 0.5);

        Player.rotate((float) Math.toDegrees(yaw));
        client.options.sneakKey.setPressed(true);
        client.options.forwardKey.setPressed(true);

        return true;
    }

    /**
     * Checks whether the player is fully positioned on a block.
     *
     * @param pos source block position
     * @return true when the player is above the block
     */
    private static boolean aligned(IPlayerContext context, BlockPos pos) {
        if (context.player().getBlockY() != pos.getY()) return false;

        Box box = context.player().getBoundingBox().expand(0.05);
        return box.minX > pos.getX() && box.maxX < pos.getX() + 1.0
            && box.minZ > pos.getZ() && box.maxZ < pos.getZ() + 1.0;
    }

    private static boolean rising(IMovement movement) {
        int target = movement.getDest().getY();
        int source = movement.getSrc().getY();
        return target == source + 1;
    }

}
