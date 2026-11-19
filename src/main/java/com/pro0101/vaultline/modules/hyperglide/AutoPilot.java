package com.pro0101.vaultline.modules.hyperglide;

import com.pro0101.vaultline.VaultlineAddon;
import com.pro0101.vaultline.modules.hyperglide.navigation.Route;
import com.pro0101.vaultline.modules.hyperglide.navigation.Route.Leg;
import com.pro0101.vaultline.modules.hyperglide.navigation.Segment;
import com.pro0101.vaultline.modules.hyperglide.utilities.Baritone;
import com.pro0101.vaultline.modules.hyperglide.utilities.Client;
import com.pro0101.vaultline.modules.hyperglide.utilities.Elytra;
import com.pro0101.vaultline.modules.hyperglide.utilities.Flight;
import com.pro0101.vaultline.modules.hyperglide.utilities.Hotbar;
import com.pro0101.vaultline.modules.hyperglide.utilities.Placement;
import com.pro0101.vaultline.modules.hyperglide.utilities.Player;
import com.pro0101.vaultline.util.BaritoneCompat;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import java.util.ArrayList;
import java.util.List;

public class AutoPilot extends Module {
    private static final double approach = 128.0;
    private static final double proximity = 3.0;

    private static final int halt = 25;
    private static final int lava = 3;
    private static final int level = 120;

    private static final int width = 24;
    private static final int height = 8;
    private static final int depth = 24;

    private static final int floor = 32;
    private static final int search = 64;
    private static final int retry = 10;
    private static final int settle = 5;

    private final Flight flight = Flight.get();

    private State state = State.Idle;
    private Route route;

    private BlockPos target;
    private BlockPos goal;
    private BlockPos point;

    private List<BlockPos> blocks;
    private MiningTweaks mining;

    private int leg = -1;
    private int timer;
    private int bottom;

    private boolean join;
    private boolean enabled;
    private boolean emergency;

    /** True once activation got past its checks, so deactivation only undoes what this flight did. */
    private boolean started;

    /** Module and setting states from before activation, put back by {@link #restore()}. */
    private boolean miningBefore;
    private boolean navigationBefore;
    private boolean tweaksBefore;
    private boolean deployBefore;
    private boolean bounceBefore;

    /** True once the transition halt is applied, so it happens once per leg instead of every tick. */
    private boolean halted;

    /** Column being excavated, kept so a scan cut short by an unloaded chunk can be retried. */
    private BlockPos columnPoint;
    private int columnBottom = Integer.MIN_VALUE;
    private boolean columnBlocked;

    private enum State {
        Idle,
        Seek,
        Flight,
        Land,
        Entry,
        Highway,
        Exit,
        Drop,
        Final,
        Done
    }

    public AutoPilot() {
        super(VaultlineAddon.CATEGORY, "auto-pilot",
            "Automatically travels toward the navigation goal."
        );
    }

    @Override
    public void onActivate() {
        if (!BaritoneCompat.isAvailable()) {
            this.error("Baritone is required for Auto Pilot.");
            this.toggle();
            return;
        }

        if (this.netherrack() < 0) {
            this.error("No netherrack in hotbar.");
            this.toggle();
            return;
        }

        this.reset();
        this.capture();
        Baritone.settings(1.43, 0.4, false);

        this.mining = Modules.get().get(MiningTweaks.class);

        if (this.mining != null && !this.miningBefore) {
            this.mining.toggle();
        }

        this.setup();
        this.load();

        this.started = true;
    }

    @Override
    public void onDeactivate() {
        if (!this.started) {
            this.reset();
            return;
        }

        if (this.emergency) {
            this.bounce(false);
            this.release();
        } else {
            this.abort();
        }

        this.restore();
        this.reset();
    }


    /**
     * @param event pre-tick event
     */
    @EventHandler
    private void tick(TickEvent.Pre event) {
        if (!this.valid()) return;

        if (!Elytra.equipped() && Elytra.hotbar() < 0) {
            this.error("Elytra flight is unavailable.");
            this.toggle();
            return;
        }

        this.setup();

        if (this.route == null || this.target == null) {
            this.load();
        }

        if (this.route == null || this.target == null) {
            return;
        }

        switch (this.state) {
            case Idle -> this.idle();
            case Seek -> this.seek();
            case Flight -> this.flight();
            case Land -> this.land();
            case Entry -> this.entry();
            case Highway -> this.highway();
            case Exit -> this.exit();
            case Drop -> this.drop();
            case Final -> this.finish();
            case Done -> this.toggle();
        }
    }

    /**
     * Stops Auto Pilot when Baritone starts an emergency landing.
     *
     */
    @EventHandler
    private void message(ReceiveMessageEvent event) {
        if (!event.getMessage().getString().contains(
            "Emergency landing - almost out of " +
            "elytra durability or fireworks")) {
            return;
        }

        this.emergency = true;
        this.toggle();
    }



    private void reset() {
        this.route = null;
        this.state = State.Idle;

        this.target = null;
        this.goal = null;
        this.point = null;
        this.blocks = null;
        this.mining = null;

        this.leg = -1;
        this.timer = 0;
        this.bottom = 0;

        this.join = false;
        this.enabled = false;
        this.emergency = false;

        this.started = false;
        this.halted = false;

        this.columnPoint = null;
        this.columnBottom = Integer.MIN_VALUE;
        this.columnBlocked = false;
    }

    private void setup() {
        Navigation navigation = this.navigation();
        if (navigation != null && !navigation.isActive()) {
            navigation.toggle();
        }

        ElytraTweaks tweaks = Modules.get().get(ElytraTweaks.class);
        if (tweaks != null) {
            if (!tweaks.isActive()) tweaks.toggle();
            tweaks.deploy(true);
        }

        if (this.mining != null && !this.mining.isActive()) {
            this.mining.toggle();
        }
    }

    /**
     * Remembers every module and setting this flight is about to change.
     */
    private void capture() {
        Navigation navigation = this.navigation();
        this.navigationBefore = navigation != null && navigation.isActive();

        ElytraTweaks tweaks = Modules.get().get(ElytraTweaks.class);
        this.tweaksBefore = tweaks != null && tweaks.isActive();
        this.deployBefore = tweaks != null && tweaks.autoDeploy();

        BounceFly bounce = Modules.get().get(BounceFly.class);
        this.bounceBefore = bounce != null && bounce.isActive();

        MiningTweaks mining = Modules.get().get(MiningTweaks.class);
        this.miningBefore = mining != null && mining.isActive();
        this.enabled = this.miningBefore;
    }

    /**
     * Puts back every module and setting captured by {@link #capture()}, so a finished flight does
     * not leave Bounce Fly, Navigation or auto-deploy in a state the player never asked for.
     */
    private void restore() {
        if (!this.started) return;

        MiningTweaks mining = Modules.get().get(MiningTweaks.class);
        if (mining != null && mining.isActive() && !this.miningBefore) {
            mining.toggle();
        }

        ElytraTweaks tweaks = Modules.get().get(ElytraTweaks.class);
        if (tweaks != null) {
            tweaks.deploy(this.deployBefore);
            if (tweaks.isActive() && !this.tweaksBefore) tweaks.toggle();
        }

        Navigation navigation = this.navigation();
        if (navigation != null && navigation.isActive() &&
            !this.navigationBefore) {
            navigation.toggle();
        }

        // An emergency landing is the one case where Bounce Fly must stay off, whatever it was on
        // before: the whole point is to come down, and bouncing would launch us again.
        if (this.emergency) return;

        BounceFly bounce = Modules.get().get(BounceFly.class);
        if (bounce != null && bounce.isActive() != this.bounceBefore) {
            bounce.toggle();
        }
    }



    private void load() {
        Navigation navigation = this.navigation();
        if (navigation == null) return;

        navigation.refresh();

        Route route = navigation.route();
        if (route == null) return;

        this.route = route;
        this.target = navigation.point().toImmutable();
    }

    /**
     * @return true when route progress is available
     */
    private boolean refresh() {
        Navigation navigation = this.navigation();
        if (navigation == null) return false;

        navigation.refresh();

        Route route = navigation.route();
        if (route == null) return false;

        this.route = route;
        this.leg = -1;

        int progress = this.progress();
        if (progress < 0) return false;

        this.leg = progress;
        return true;
    }

    /**
     * @return route leg index, or -1 when unavailable
     */
    private int progress() {
        List<Leg> legs = this.route.legs();

        int size = legs.size();
        if (size == 0) return -1;

        int start = Math.max(0, this.leg);
        if (start >= size) return -1;

        Vec2f position = Player.position();
        float distance = Float.MAX_VALUE;
        int best = -1;

        for (int index = start; index < size; index++) {
            Route.Leg leg = legs.get(index);
            Vec2f point = this.project(leg, position);

            float current = position.distanceSquared(point);
            if (current >= distance) continue;

            distance = current;
            best = index;
        }

        return best;
    }

    /**
     * Checks whether the player is aligned with a highway leg.
     *
     * @return true when at highway level and within proximity
     */
    private boolean aligned(Route.Leg leg) {
        if (this.mc.player.getY() < level - 1.0) {
            return false;
        }

        Vec2f position = Player.position();
        Vec2f point = this.project(leg, position);

        float distance = position.distanceSquared(point);
        return distance <= proximity * proximity;
    }

    /**
     * Projects a point onto the nearest position of a route leg.
     *
     * @return closest point on the leg
     */
    private Vec2f project(Route.Leg leg, Vec2f point) {
        Segment segment = new Segment(leg.start(), leg.end());
        return segment.point(segment.projection(point));
    }

    /**
     * @return closest point shifted forward along the leg
     */
    private Vec2f entry(Route.Leg leg) {
        Vec2f position = Player.position();
        Segment segment = new Segment(leg.start(), leg.end());

        Vec2f point = segment.point(segment.projection(position));
        return point.add(segment.unit().multiply(3.0F));
    }

    /**
     * Converts a route point to a block position at highway level.
     *
     * @return block position at highway level
     */
    private BlockPos waypoint(Vec2f point) {
        return new BlockPos(
            Math.round(point.x), level,
            Math.round(point.y)
        );
    }



    private void idle() {
        if (this.mc.player.isOnGround() &&
            this.close(this.target, approach)) {
            this.finish();
            return;
        }

        int progress = this.progress();
        if (progress < 0) return;

        this.leg = progress;
        if (!this.access()) this.begin();
    }

    private void begin() {
        if (this.mc.player.isGliding()) {
            this.launch();
            this.flight();
            return;
        }

        if (!this.open()) {
            if (this.timer > 0 && --this.timer > 0) {
                return;
            }

            this.point = this.space(true);
            if (this.point == null) {
                this.timer = retry;
                return;
            }

            this.blocks = null;
            this.timer = settle;
            this.state = State.Seek;
            return;
        }

        this.timer = 0;
        this.flight();
    }

    /**
     * Moves to a clear launch area before starting elytra travel.
     */
    private void seek() {
        if (this.point == null) {
            this.state = State.Idle;
            return;
        }

        if (this.reach(this.point, lava)) {
            this.fill();
        }

        if (this.goal == null ||
            !this.goal.equals(this.point)) {

            if (this.pathing()) this.abort();
            this.walk(this.point, true);
            return;
        }

        if (!this.pathing()) {
            this.prepare();
        }
    }

    private void prepare() {
        if (this.blocks == null && !this.settled()) {
            return;
        }

        if (this.excavate()) {
            this.launch();
            this.flight();
        }
    }

    /**
     * @return true when the support column is ready
     */
    private boolean settled() {
        if (!this.close(this.point, 1.5)) {
            this.goal = null;
            this.timer = settle;
            return false;
        }

        if (this.timer > 0 && --this.timer >= 0) {
            return false;
        }

        this.blocks = this.column(this.point, this.bottom);
        return true;
    }

    private void launch() {
        this.abort();

        this.point = null;
        this.blocks = null;
        this.timer = 0;

        this.state = State.Flight;
    }



    private void flight() {
        if (this.goal == null || this.resume()) {
            this.follow();
        }
    }

    /**
     * Waits for the flight to finish before choosing another leg.
     *
     * @return true when travel can continue
     */
    private boolean resume() {
        if (this.join && this.landing()) {
            this.cancel();
            this.glide();

            this.blocks = null;
            this.timer = this.rocket() ? 3 : 0;
            this.mc.player.setPitch(-90.0F);

            this.state = State.Land;
            return false;
        }

        if (this.pathing() ||
            !this.mc.player.isOnGround()) {
            return false;
        }

        this.goal = null;
        this.join = false;

        if (this.close(this.target, approach)) {
            this.finish();
            return false;
        }

        int progress = this.progress();
        if (progress < 0) return false;

        this.leg = progress;
        return true;
    }

    /**
     * Moves toward the current route leg or enters its highway.
     */
    private void follow() {
        List<Leg> legs = this.route.legs();
        if (this.leg >= legs.size() ||
            this.leg < 0 || this.access()) {
            return;
        }

        Route.Leg leg = legs.get(this.leg);
        boolean highway = leg.type() == Route.Type.Highway;

        int next = this.leg + 1;
        boolean joining = highway || next < legs.size() &&
            legs.get(next).type() == Route.Type.Highway;

        Vec2f point = highway ? this.entry(leg) : leg.end();
        if (this.fly(this.waypoint(point), joining)) {
            this.state = State.Flight;
        }
    }

    private void finish() {
        if (this.state != State.Final) {
            if (!this.mc.player.isOnGround() || this.done()) {
                return;
            }

            this.bounce(false);
            this.walk(this.target, false);
            this.state = State.Final;
            return;
        }

        if (this.goal != null && this.pathing()) {
            return;
        }

        this.goal = null;

        if (this.done()) return;
        this.walk(this.target, false);
    }

    /**
     * Completes travel when the player is close to the target.
     *
     * @return true when the route is complete
     */
    private boolean done() {
        if (!this.close(this.target, 2.0)) {
            return false;
        }

        this.release();
        this.state = State.Done;
        return true;
    }



    /**
     * Uses the highway directly when it is already within reach.
     *
     * @return true when highway entry has taken over
     */
    private boolean access() {
        List<Leg> legs = this.route.legs();
        if (this.leg < 0 || this.leg >= legs.size()) {
            return false;
        }

        Route.Leg leg = legs.get(this.leg);
        boolean highway = leg.type() == Route.Type.Highway;

        if (highway && this.aligned(leg)) {
            this.start(this.leg);
            return true;
        }

        int next = this.leg + 1;
        boolean joining = highway || next < legs.size() &&
            legs.get(next).type() == Route.Type.Highway;

        if (!joining || !this.mc.player.isOnGround()) {
            return false;
        }

        Vec2f point = highway ? this.entry(leg) : leg.end();
        if (!this.close(point, approach)) return false;

        this.state = State.Entry;
        return true;
    }

    /**
     * Finishes the normal flight and creates a landing block.
     */
    private void land() {
        this.mc.player.setPitch(-90.0F);
        this.glide();

        if (this.timer > 0 && --this.timer >= 0) {
            return;
        }

        if (this.blocks == null) {
            if (this.mc.player.isOnGround() ||
                !this.mc.player.verticalCollision) {
                return;
            }
            this.blocks = this.platform();
            this.columnBlocked = false;
        }

        if (!this.support()) return;

        if (this.mc.player.isOnGround()) {
            this.mc.player.stopGliding();

            this.blocks = null;
            this.state = State.Entry;
        }
    }

    private void entry() {
        if (this.goal != null) {
            if (this.pathing()) return;
            this.goal = null;
        }

        if (!this.refresh()) return;

        int next = this.leg;
        List<Leg> legs = this.route.legs();
        Route.Leg road = legs.get(next);

        if (road.type() != Route.Type.Highway) {
            if (++next >= legs.size()) {
                this.state = State.Flight;
                return;
            }

            road = legs.get(next);
            if (road.type() != Route.Type.Highway) {
                this.state = State.Flight;
                return;
            }
        }

        if (!this.aligned(road)) {
            Vec2f entry = this.entry(road);
            this.walk(this.waypoint(entry), true);
            return;
        }

        this.start(next);
    }

    private void glide() {
        if (this.blocks != null ||
            !Elytra.equipped() ||
            this.mc.player.isOnGround() ||
            this.mc.player.isGliding() ||
            this.mc.player.verticalCollision) {
            return;
        }

        Elytra.start();
        this.mc.player.startGliding();
    }



    /**
     * Starts Bounce Fly toward the end of a highway leg.
     *
     */
    private void start(int leg) {
        List<Leg> legs = this.route.legs();
        if (leg < 0 || leg >= legs.size()) {
            return;
        }

        Route.Leg road = legs.get(leg);
        if (road.type() != Route.Type.Highway) {
            return;
        }

        this.leg = leg;
        this.halted = false;

        if (this.state != State.Highway) {
            this.point = null;
            this.blocks = null;
        }

        this.rotate(road.end());
        this.bounce(true);

        this.state = State.Highway;
    }

    private void highway() {
        List<Leg> legs = this.route.legs();
        if (this.leg < 0 || this.leg >= legs.size()) {
            return;
        }

        Route.Leg road = legs.get(this.leg);
        if (road.type() != Route.Type.Highway) {
            return;
        }

        int next = this.leg + 1;
        boolean exiting = next < legs.size() &&
            legs.get(next).type() != Route.Type.Highway;

        if (this.delay(exiting) || this.timer <= 0 &&
            !this.arrive(road, exiting)) {
            return;
        }

        this.stop();
        if (--this.timer <= 0) this.cross();
    }

    private void cross() {
        this.halted = false;

        List<Leg> legs = this.route.legs();

        int next = this.leg + 1;
        if (next >= legs.size()) {
            this.state = State.Done;
            return;
        }

        Route.Leg following = legs.get(next);
        if (following.type() == Route.Type.Highway) {
            Vec2f entry = this.entry(following);
            this.walk(this.waypoint(entry), true);
            this.state = State.Entry;
            return;
        }

        this.leg = next;
        this.timer = 0;

        this.goal = null;
        this.blocks = null;
        this.join = false;

        this.state = State.Exit;
    }

    /**
     * @param exiting whether the route leaves the highway next
     * @return true when the player can stop for the transition
     */
    private boolean arrive(Route.Leg road, boolean exiting) {
        if (!this.aligned(road) ||
            !this.close(road.end(), proximity)) {
            // Still on the way: make sure Bounce Fly carries us the rest of it. Done only after the
            // arrival test has failed, so it is never switched on for the tick that switches it off.
            BounceFly bounce = Modules.get().get(BounceFly.class);
            if (bounce != null && !bounce.isActive()) {
                this.rotate(road.end());
                this.bounce(true);
            }

            return false;
        }

        if (exiting && this.point == null) {
            this.point = this.space(false);
            if (this.point == null) {
                this.timer = retry;
                return false;
            }
        }

        this.bounce(false);
        this.cancel();

        this.timer = halt;
        return true;
    }

    /**
     * @param exiting whether the route leaves the highway next
     * @return true while the retry delay is still active
     */
    private boolean delay(boolean exiting) {
        return exiting && this.point == null
            && this.timer > 0 && --this.timer > 0;
    }



    /**
     * Moves to the selected exit block before starting the drop.
     */
    private void exit() {
        if (this.point == null || this.leg < 0 ||
            this.leg >= this.route.legs().size()) {
            return;
        }

        if (this.reach(this.point, lava)) this.fill();

        if (this.goal == null || !this.goal.equals(this.point)) {
            if (this.pathing()) this.abort();
            this.walk(this.point, true);
            return;
        }

        if (this.pathing() || !this.mc.player.isOnGround()) {
            return;
        }

        if (!this.close(this.point, 1.5)) {
            this.goal = null;
            return;
        }

        int bot = this.point.getY() - height;
        this.blocks = this.column(this.point, bot);

        this.abort();
        this.timer = settle;
        this.state = State.Drop;
    }

    private void drop() {
        if (this.point == null) return;

        if (this.timer <= 0 || --this.timer < 0) {
            if (this.excavate()) this.depart();
        }
    }

    /**
     * Stops exit and starts flying to the next route point.
     */
    private void depart() {
        List<Leg> legs = this.route.legs();
        if (this.leg < 0 || this.leg >= legs.size()) {
            return;
        }

        Vec2f point = legs.get(this.leg).end();
        BlockPos destination = this.waypoint(point);

        this.release();

        if (this.fly(destination, false)) {
            this.point = null;
            this.blocks = null;

            this.timer = 0;
            this.state = State.Flight;
        }
    }



    /**
     * @param start whether the first box starts from player
     * @return closest point in a clear space, or null
     */
    private BlockPos space(boolean start) {
        BlockPos origin = this.mc.player.getBlockPos();

        int limit = origin.getY() - floor;
        limit -= start ? 0 : height - 1;
        if (limit < 0) return null;

        int base = start ? height / 2 : -(height - 1) / 2;
        int reach = Math.max(width / 2, depth / 2);

        BlockPos best = null;
        double distance = Double.MAX_VALUE;

        for (int range = 0; range <= search; range++) {
            BlockPos point = this.scan(
                origin, range, base, limit, distance
            );

            if (point != null) {
                best = this.adjust(point, start);
                distance = point.getSquaredDistance(origin);
            }

            if (best != null) {
                int next = Math.max(0, range + 1 - reach);
                if (distance <= (double) next * next) break;
            }
        }

        return best != null ? best.toImmutable() : null;
    }

    /**
     * Searches one horizontal ring for the nearest clear space.
     *
     *
     * @param range horizontal search range
     * @return a closer usable point, or null when none is found
     */
    private BlockPos scan(BlockPos origin, int range,
        int base, int limit, double distance) {

        BlockPos best = null;

        for (int px = -range; px <= range; px++) {
            for (int pz = -range; pz <= range; pz++) {

                int test = Math.max(Math.abs(px), Math.abs(pz));
                if (test != range) continue;

                BlockPos point = this.vertical(
                    origin, px, pz, base, limit, distance
                );

                if (point != null) {
                    distance = point.getSquaredDistance(origin);
                    best = point;
                }
            }
        }

        return best;
    }

    /**
     * @return closest clear position, or null
     */
    private BlockPos vertical(BlockPos origin, int px,
        int pz, int base, int limit, double distance) {

        BlockPos best = null;

        for (int offset = 0; offset <= limit; offset++) {
            BlockPos center = origin.add(px, base - offset, pz);
            BlockPos point = this.closest(center, origin);

            double current = point.getSquaredDistance(origin);
            if (current >= distance || !this.clear(center)) {
                continue;
            }

            this.bottom = center.getY() - height / 2;
            distance = current;
            best = point;
        }

        return best;
    }

    /**
     * @param center center of the tested box
     * @return box point closest to the reference position
     */
    private BlockPos closest(BlockPos center, BlockPos origin) {
        int minx = center.getX() - width / 2;
        int miny = center.getY() - height / 2;
        int minz = center.getZ() - depth / 2;

        int maxx = minx + width - 1;
        int maxy = miny + height - 1;
        int maxz = minz + depth - 1;

        return new BlockPos(
            MathHelper.clamp(origin.getX(), minx, maxx),
            MathHelper.clamp(origin.getY(), miny, maxy),
            MathHelper.clamp(origin.getZ(), minz, maxz)
        );
    }

    /**
     * Adjusts a clear space point for launch or exit use.
     *
     * @param start whether selecting a launch point
     * @return adjusted point to traverse to
     */
    private BlockPos adjust(BlockPos point, boolean start) {
        if (!start) return point.up();
        return this.solid(point.down()) ? point : point.up();
    }

    /**
     * Checks whether the launch box above the player is clear.
     *
     * @return true when flight can start from the current position
     */
    private boolean open() {
        BlockPos origin = this.mc.player.getBlockPos();
        return this.clear(origin.up(height / 2));
    }

    /**
     * Checks whether a space contains only air or flowing lava.
     *
     *
     * @return true when the entire space is loaded and clear
     */
    private boolean clear(BlockPos center) {
        int minx = center.getX() - width / 2;
        int miny = center.getY() - height / 2;
        int minz = center.getZ() - depth / 2;

        int maxx = minx + width - 1;
        int maxy = miny + height - 1;
        int maxz = minz + depth - 1;

        if (miny < this.mc.world.getBottomY() ||
            maxy > this.mc.world.getTopYInclusive()) {
            return false;
        }

        return this.loaded(minx, minz, maxx, maxz)
            && this.empty(minx, miny, minz, maxx, maxy, maxz);
    }

    /**
     * Checks whether a space contains only air or flowing lava.
     *
     * @return true when no solid block occupies the space
     */
    private boolean empty(int minx, int miny,
        int minz, int maxx, int maxy, int maxz) {

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int py = miny; py <= maxy; py++) {
            for (int px = minx; px <= maxx; px++) {
                for (int pz = minz; pz <= maxz; pz++) {
                    pos.set(px, py, pz);
                    if (!this.empty(pos)) return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks whether all chunks in an area are loaded.
     *
     * @return true when every required chunk is loaded
     */
    private boolean loaded(int minx, int minz, int maxx, int maxz) {
        for (int px = minx >> 4; px <= maxx >> 4; px++) {
            for (int pz = minz >> 4; pz <= maxz >> 4; pz++) {
                if (!this.mc.world.isChunkLoaded(px, pz)) {
                    return false;
                }
            }
        }
        return true;
    }



    /**
     * @param point upper position of the column
     * @param bottom lowest included block level
     * @return column blocks ordered from highest to lowest
     */
    private List<BlockPos> column(BlockPos point, int bottom) {
        List<BlockPos> blocks = new ArrayList<>();

        this.columnPoint = point;
        this.columnBottom = bottom;
        this.columnBlocked = false;

        int px = point.getX(), pz = point.getZ();
        for (int py = point.getY() - 1; py >= bottom; py--) {
            if (!this.mc.world.isChunkLoaded(px >> 4, pz >> 4)) {
                // Everything below an unloaded chunk reads as air, so the rest of the column is
                // unknown rather than clear. Stop here and let excavate() rescan it once it loads.
                this.columnBlocked = true;
                break;
            }

            BlockPos pos = new BlockPos(px, py, pz);
            if (!this.empty(pos)) blocks.add(pos);
        }

        return blocks;
    }

    /**
     * @return true when no blocks remain
     */
    private boolean excavate() {
        if (this.blocks == null) return false;

        this.blocks.removeIf(pos -> this.empty(pos));

        // A scan cut short by an unloaded chunk is not a clear column: rescan it as the chunk
        // arrives instead of dropping through terrain nobody has loaded yet.
        if (this.blocks.isEmpty() && this.columnBlocked &&
            this.columnPoint != null) {
            this.blocks = this.column(this.columnPoint, this.columnBottom);
        }

        if (this.blocks.isEmpty()) {
            if (this.columnBlocked) return false;

            this.release();
            return true;
        }

        BlockPos pos = this.blocks.get(0);
        this.toward(pos);

        if (this.mining != null) {
            this.mining.mine(pos, Direction.UP);
        }

        return false;
    }

    private void fill() {
        int slot = this.netherrack();
        if (slot < 0) return;

        AirPlace air = Modules.get().get(AirPlace.class);
        if (air == null) return;

        BlockPos origin = this.mc.player.getBlockPos();
        BlockPos.Mutable pos = new BlockPos.Mutable();

        int bx = origin.getX(), by = origin.getY();
        int bz = origin.getZ();

        for (int px = -lava; px <= lava; px++) {
            for (int py = -lava; py <= lava; py++) {
                for (int pz = -lava; pz <= lava; pz++) {

                    int radius = px * px + py * py + pz * pz;
                    if (radius > lava * lava) continue;

                    pos.set(bx + px, by + py, bz + pz);

                    FluidState state = this.mc.world.getFluidState(pos);
                    if (state.getFluid() != Fluids.LAVA) continue;

                    air.place(pos.toImmutable(), slot);
                }
            }
        }
    }

    /**
     * @return one to four fixed platform blocks
     */
    private List<BlockPos> platform() {
        Box box = this.mc.player.getBoundingBox();

        int minx = MathHelper.floor(box.minX);
        int maxx = MathHelper.floor(Math.nextDown(box.maxX));
        int minz = MathHelper.floor(box.minZ);
        int maxz = MathHelper.floor(Math.nextDown(box.maxZ));

        int py = this.mc.player.getBlockPos().down(2).getY();
        List<BlockPos> blocks = new ArrayList<>(4);

        for (int px = minx; px <= maxx; px++) {
            for (int pz = minz; pz <= maxz; pz++) {
                blocks.add(new BlockPos(px, py, pz));
            }
        }

        return blocks;
    }

    /**
     * @return true when every required block is present
     */
    private boolean support() {
        if (this.blocks == null || this.blocks.isEmpty()) {
            return false;
        }

        for (BlockPos pos : this.blocks) {
            if (!Placement.open(pos)) continue;
            if (!this.place(pos)) return false;
        }

        return true;
    }

    /**
     * @param pos target block position
     * @return true when the placement packet was sent
     */
    private boolean place(BlockPos pos) {
        int slot = this.netherrack();

        AirPlace air = Modules.get().get(AirPlace.class);
        return air != null && slot >= 0 && air.place(pos, slot);
    }



    /**
     * @param exact whether Y must be part of the goal
     * @return true when elytra pathing was started
     */
    private boolean fly(BlockPos pos, boolean exact) {
        if (!Baritone.loaded() || !this.flight.normal()) {
            return false;
        }

        if (this.goal != null && this.goal.equals(pos) &&
            Baritone.elytra()) {
            return true;
        }

        if (this.pathing()) this.cancel();

        try {
            Baritone.fly(pos, exact);
            this.goal = pos.toImmutable();
            this.join = exact;
            return true;
        } catch (IllegalArgumentException ignored) {
            // The command did not name a block position.
            return false;
        }
    }

    /**
     * @param exact whether Y must be part of the goal
     */
    private void walk(BlockPos pos, boolean exact) {
        if (this.goal != null && this.goal.equals(pos) &&
            this.pathing()) {
            return;
        }

        if (this.pathing()) this.cancel();
        Baritone.walk(pos, exact);

        this.goal = pos.toImmutable();
        this.join = false;
    }

    private void abort() {
        this.cancel();
        this.release();
    }

    private void cancel() {
        Baritone.stop();
        this.goal = null;
        this.join = false;
    }

    /**
     * Checks whether Baritone is processing or following a path.
     *
     * @return true while any owned path is active
     */
    private boolean pathing() {
        return Baritone.elytra() || Baritone.pathing();
    }

    /**
     * Checks whether elytra pathing switched to a landing target.
     *
     * @return true when the elytra destination changed internally
     */
    private boolean landing() {
        BlockPos current = Baritone.destination();
        return current != null && this.goal != null
            && !current.equals(this.goal);
    }



    /**
     * @param pos block position to move toward
     */
    private void toward(BlockPos pos) {
        this.release();

        if (this.close(pos, 0.2)) return;

        float px = pos.getX() + 0.5F;
        float pz = pos.getZ() + 0.5F;

        this.rotate(new Vec2f(px, pz));
        this.mc.options.forwardKey.setPressed(true);
    }

    private void stop() {
        if (this.halted) return;
        this.halted = true;

        this.release();

        this.mc.player.stopGliding();
        this.mc.player.setVelocity(0.0, 0.0, 0.0);
        this.mc.player.setSprinting(false);

        float yaw = this.mc.player.getYaw();
        yaw = MathHelper.wrapDegrees(yaw + 180.0F);

        this.mc.player.setYaw(yaw);
        this.mc.player.setPitch(0.0F);
    }

    private void rotate(Vec2f point) {
        double px = point.x - this.mc.player.getX();
        double pz = point.y - this.mc.player.getZ();
        double yaw = Math.atan2(-px, pz);

        Player.rotate((float) Math.toDegrees(yaw));
    }

    /**
     * @return true when the request was accepted
     */
    private boolean rocket() {
        return this.mc.interactionManager != null
            && this.flight.request(Elytra::firework);
    }

    private void bounce(boolean active) {
        BounceFly bounce = Modules.get().get(BounceFly.class);
        if (bounce != null && bounce.isActive() != active) {
            bounce.toggle();
        }
    }

    private void release() {
        if (this.mc.options == null) return;

        this.mc.options.forwardKey.setPressed(false);
        this.mc.options.backKey.setPressed(false);
        this.mc.options.leftKey.setPressed(false);
        this.mc.options.rightKey.setPressed(false);
        this.mc.options.jumpKey.setPressed(false);
        this.mc.options.sneakKey.setPressed(false);
    }



    /**
     * @return Navigation module, or null when unavailable
     */
    private Navigation navigation() {
        return Modules.get().get(Navigation.class);
    }

    /**
     * @return hotbar slot, or -1 when unavailable
     */
    private int netherrack() {
        return this.mc.player == null ? -1
            : Hotbar.find(Items.NETHERRACK);
    }

    /**
     * @return true when the point is within range
     */
    private boolean close(Vec2f point, double radius) {
        double px = point.x - this.mc.player.getX();
        double pz = point.y - this.mc.player.getZ();
        return px * px + pz * pz <= radius * radius;
    }

    /**
     * @param pos block position
     * @return true when the position is within range
     */
    private boolean close(BlockPos pos, double radius) {
        double px = pos.getX() + 0.5 - this.mc.player.getX();
        double pz = pos.getZ() + 0.5 - this.mc.player.getZ();
        return px * px + pz * pz <= radius * radius;
    }

    /**
     * @param pos block position
     * @return true when the position is within range
     */
    private boolean reach(BlockPos pos, double radius) {
        double px = pos.getX() + 0.5 - this.mc.player.getX();
        double py = pos.getY() - this.mc.player.getY();
        double pz = pos.getZ() + 0.5 - this.mc.player.getZ();
        return px * px + py * py + pz * pz <= radius * radius;
    }

    /**
     * Checks whether a block is air or flowing lava.
     *
     * @param pos block position to check
     * @return true when the block is air or flowing lava
     */
    private boolean empty(BlockPos pos) {
        BlockState state = this.mc.world.getBlockState(pos);
        Fluid fluid = state.getFluidState().getFluid();
        return state.isAir() || fluid == Fluids.FLOWING_LAVA;
    }

    /**
     * Checks whether a block can collide with the player.
     *
     * @param pos block position to check
     * @return true when the block is solid and collidable
     */
    private boolean solid(BlockPos pos) {
        BlockState state = this.mc.world.getBlockState(pos);
        return !state.getCollisionShape(this.mc.world, pos).isEmpty();
    }

    /**
     * @return true when ready to run the module
     */
    private boolean valid() {
        return Client.ready() && Client.nether();
    }

}
