package com.pro0101.vaultline.modules.hyperglide;

import com.pro0101.vaultline.VaultlineAddon;
import com.pro0101.vaultline.modules.huntbuddy.ElytraMace;
import com.pro0101.vaultline.util.ModuleSettings;
import com.pro0101.vaultline.modules.hyperglide.utilities.Client;
import com.pro0101.vaultline.modules.hyperglide.utilities.Elytra;
import com.pro0101.vaultline.modules.hyperglide.utilities.Flight;
import com.pro0101.vaultline.modules.hyperglide.utilities.Hotbar;
import com.pro0101.vaultline.modules.hyperglide.utilities.Player;
import com.pro0101.vaultline.modules.hyperglide.utilities.Takeoff;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FireworksComponent;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class ControlFly extends Module {
    private static final double epsilon = 1.0E-3;
    private static final double ceiling = 120.0;
    private static final double ticks = 20.0;

    private static final float bound = 60.0F;
    private static final int priority = 100;
    private static final int timeout = 4;

    private static final double gain = 0.60;
    private static final double push = 0.025;
    private static final double sharp = Math.cos(Math.toRadians(45.0));
    private static final float porpoisePitch = 22.5F;

    private final SettingGroup movement = this.settings.createGroup("Movement");
    private final SettingGroup automation = this.settings.createGroup("Automation");
    private final SettingGroup porpoiseGroup = this.settings.createGroup("Porpoise");
    private final SettingGroup chestSwapGroup = this.settings.createGroup("Chest Swap");

    private final Setting<Double> maximum = this.movement.add(new DoubleSetting.Builder()
        .name("maximum-speed")
        .description("Maximum controlled speed in blocks per second. The default 34 is the safe elytra limit; raise it for porpoise or RocketBoost flights.")
        .defaultValue(34.0)
        .min(10.0)
        .sliderMax(120.0)
        .build()
    );

    private final Setting<Double> minimum = this.movement.add(new DoubleSetting.Builder()
        .name("minimum-speed")
        .description("Uses a rocket when speed drops below this value.")
        .defaultValue(27.0)
        .min(10.0)
        .sliderMax(120.0)
        .build()
    );

    private final Setting<Double> penalty = this.movement.add(new DoubleSetting.Builder()
        .name("ascent-penalty")
        .description("Speed removed from the limit while flying upward.")
        .defaultValue(2.0)
        .min(0.0)
        .sliderMax(5.0)
        .build()
    );

    private final Setting<Integer> offset = this.movement.add(new IntSetting.Builder()
        .name("latency-offset")
        .description("Reduces boost time by milliseconds for ping variation.")
        .defaultValue(50)
        .min(0)
        .sliderMax(250)
        .build()
    );

    private final Setting<Double> alignment = this.movement.add(new DoubleSetting.Builder()
        .name("alignment")
        .description("How much of the steering yaw is sent to the server. 1 aligns the flight fully; 0 leaves your yaw alone, which is what Baritone needs so it keeps its own heading.")
        .defaultValue(1.0)
        .min(0.0)
        .sliderRange(0.0, 1.0)
        .build()
    );

    private final Setting<Boolean> forward = this.automation.add(new BoolSetting.Builder()
        .name("keep-forward")
        .description("Moves forward when no movement key is held.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> starter = this.automation.add(new BoolSetting.Builder()
        .name("auto-takeoff")
        .description("Starts gliding after holding jump while airborne.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Integer> timer = this.automation.add(new IntSetting.Builder()
        .name("takeoff-timer")
        .description("Jump hold ticks required before starting flight.")
        .defaultValue(5)
        .min(1)
        .sliderMax(10)
        .visible(this.starter::get)
        .build()
    );

    private final Setting<Boolean> porpoise = this.porpoiseGroup.add(new BoolSetting.Builder()
        .name("porpoise")
        .description("Steers climb and dive legs while travelling instead of holding one altitude. Each leg is checked against terrain and the swing recentres when both ends are obstructed.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Double> porpoiseHeight = this.porpoiseGroup.add(new DoubleSetting.Builder()
        .name("porpoise-height")
        .description("Vertical size of the swing in blocks. Larger swings trade more height for more speed.")
        .defaultValue(24.0)
        .min(4.0)
        .sliderRange(4.0, 250.0)
        .visible(this.porpoise::get)
        .build()
    );

    private final Setting<Boolean> chestSwap = this.chestSwapGroup.add(new BoolSetting.Builder()
        .name("chest-swap")
        .description("Hands no-durability flight to Elytra Tweaks' spoof mode: a chestplate stays worn while the elytra is only swapped in for the server's glide restart. Needs the elytra and a chestplate, one worn and the other in a hand.")
        .defaultValue(false)
        .onChanged(this::chestSwapChanged)
        .build()
    );

    private final Boost boost = new Boost();
    private final Flight flight = Flight.get();
    private final Takeoff input = new Takeoff();
    private final Motion motion = new Motion();
    private final Turn turn = new Turn();
    private final View view = new View();

    private ElytraTweaks tweaks;
    private Setting<Boolean> spoof;
    private Boolean spoofBefore;
    private boolean tweaksEnabledByUs;

    private int jump;

    public ControlFly() {
        super(VaultlineAddon.CATEGORY, "control-fly",
            "Provides controlled flight with automatic rocket boosting, porpoise and a no-durability chest swap."
        );
    }

    /**
     * Resets flight state and captures the current view orientation.
     */
    @Override
    public void onActivate() {
        this.reset();
        if (this.mc.player == null) return;

        this.view.yaw = this.mc.player.getYaw();
        this.view.pitch = this.mc.player.getPitch();

        this.motion.yaw = this.view.yaw;
        this.motion.aligned = this.view.yaw;
        this.motion.pitch = this.view.pitch;
        this.motion.altitude = this.mc.player.getY();

        this.applyChestSwap();
    }

    /**
     * Restores the player view and clears all runtime state.
     */
    @Override
    public void onDeactivate() {
        this.releaseChestSwap();
        this.restore();
        this.reset();
    }


    /**
     * @param event pre-tick event
     */
    @EventHandler
    private void tick(TickEvent.Pre event) {
        if (!Client.ready() || ElytraMace.isDiving()) return;

        this.update();
        this.input.pulse();

        if (!this.mc.player.isGliding()) {
            this.restore();
            this.clear();
            this.takeoff();
            return;
        }

        this.jump = 0;
        this.input.reset();

        if (this.halted()) return;
        Vec3d input = this.direction();

        this.view(input);
        this.control(input);
    }

    @EventHandler
    private void move(PlayerMoveEvent event) {
        if (!Client.ready() || ElytraMace.isDiving() ||
            event.type != MovementType.SELF ||
            !this.mc.player.isGliding() ||
            this.halted()) return;

        Vec3d input = this.direction();
        if (input.lengthSquared() < epsilon) {
            this.stop(event);
            return;
        }

        if (this.motion.brake) {
            this.motion.brake = false;
            this.stop(event);
            return;
        }

        this.limit(event, input.normalize());
    }

    @EventHandler
    private void packet(PacketEvent.Send event) {
        if (!Client.ready() || this.boost.automatic || !this.mc.player.isGliding() ||
            !(event.packet instanceof PlayerInteractItemC2SPacket packet)) {
            return;
        }

        ItemStack stack = this.mc.player.getStackInHand(packet.getHand());
        if (!stack.isOf(Items.FIREWORK_ROCKET)) return;

        this.await();
        this.duration(stack);
    }



    public boolean deploying() {
        return this.isActive() && this.input.active();
    }

    public boolean jumping() {
        return this.input.pressed();
    }

    /**
     * Resets all runtime flight, camera, boost and rotation state.
     */
    private void reset() {
        this.clear();

        this.jump = 0;
        this.input.reset();

        this.motion.yaw = 0.0F;
        this.motion.aligned = 0.0F;
        this.motion.pitch = 0.0F;
        this.motion.altitude = 0.0;

        this.motion.porpoise = false;
        this.motion.porpoiseUp = false;
        this.motion.porpoiseBase = 0.0;

        this.view.active = false;
        this.view.yaw = 0.0F;
        this.view.pitch = 0.0F;

        this.turn.yaw = 0.0F;
        this.turn.pitch = 0.0F;
    }

    private void clear() {
        this.boost.expiry = 0;
        this.boost.endtime = 0;

        this.boost.pending = false;
        this.boost.automatic = false;
        this.boost.rocket = null;

        this.motion.leveling = false;
        this.motion.steering = false;

        this.motion.input = 0;
        this.motion.dir = Vec3d.ZERO;
        this.motion.brake = false;

        this.motion.porpoise = false;

        this.turn.active = false;
    }



    private void control(Vec3d input) {
        if (input.lengthSquared() < epsilon) {
            this.rest();
            return;
        }

        int state = this.state();
        boolean redirect =
            this.motion.input != 0 &&
            this.motion.input != state;

        this.motion.input = state;

        boolean manual =
            this.mc.options.jumpKey.isPressed() ||
            this.mc.options.sneakKey.isPressed();

        Vec3d steer = this.steer(input.normalize());
        this.turn(steer);
        this.motion.aligned = this.aligned(this.motion.yaw);

        boolean boosted = this.active();
        this.aim(steer, boosted, manual);

        boolean launch = this.launch(steer, boosted, redirect);
        if (launch) this.prepare(manual);

        this.mc.player.setYaw(this.motion.aligned);
        this.mc.player.setPitch(this.motion.pitch);

        this.rotate(launch);
    }

    /**
     * Tracks steering changes and marks sharp turns for braking.
     */
    private void turn(Vec3d direction) {
        if (this.motion.dir.lengthSquared() >= epsilon &&
            this.sharp(this.motion.dir, direction)) {
            this.motion.brake = true;
        }
        this.motion.dir = direction;
    }

    /**
     * Keeps the view normal and renews boost while stopped.
     */
    private void rest() {
        this.motion.leveling = false;
        this.motion.steering = false;

        this.motion.yaw = this.mc.player.getYaw();
        this.motion.aligned = this.motion.yaw;
        this.motion.pitch = this.mc.player.getPitch();

        if (!this.renew()) return;

        this.await();
        this.rocket(this.motion.yaw, this.motion.pitch);
    }

    private void stop(PlayerMoveEvent event) {
        Player.velocity(event, Vec3d.ZERO);
    }

    private void limit(PlayerMoveEvent event, Vec3d dir) {
        double length = event.movement.horizontalLength();
        Vec3d velocity = new Vec3d(0.0, event.movement.y, 0.0);

        boolean ceiling = !this.mc.player.isOnGround()
            && this.mc.player.verticalCollision;

        if (dir.horizontalLength() < epsilon && !ceiling) {
            if (length < epsilon) return;
            Player.velocity(event, velocity);
            return;
        }

        Vec3d flat = this.adjust(event.movement, dir, length, ceiling);
        if (flat != null) {
            velocity = new Vec3d(flat.x, event.movement.y, flat.z);
            Player.velocity(event, velocity);
        }
    }

    /**
     * @return adjusted horizontal movement, or null when unchanged
     */
    private Vec3d adjust(Vec3d motion, Vec3d dir, double length, boolean ceiling) {
        double maximum = this.maximum(dir);

        double amount = ceiling ? length + push : length;
        amount = Math.min(maximum, amount);

        if (Math.abs(amount - length) <= epsilon) return null;

        if (ceiling && dir.horizontalLength() < epsilon) {
            dir = Vec3d.fromPolar(0.0F, this.yaw());
            return dir.multiply(amount);
        } else {
            return this.flat(motion, amount, length);
        }
    }

    private Vec3d flat(Vec3d motion, double amount, double length) {
        if (length > epsilon) {
            double scale = amount / length;
            return new Vec3d(motion.x * scale, 0.0, motion.z * scale);
        } else {
            motion = Vec3d.fromPolar(0.0F, this.motion.yaw);
            return motion.multiply(amount);   
        }
    }



    public void track(FireworkRocketEntity rocket) {
        if (!Client.ready() || this.boost.rocket == rocket) {
            return;
        }

        if (this.boost.rocket != null &&
            this.boost.rocket.isAlive() &&
            rocket.age >= this.boost.rocket.age) {
            return;
        }

        this.boost.rocket = rocket;
        this.boost.pending = false;
    }

    private void update() {
        if (this.boost.pending &&
            this.mc.player.age > this.boost.expiry) {
            this.boost.pending = false;
        }

        if (this.boost.rocket != null &&
            !this.boost.rocket.isAlive()) {
            this.boost.rocket = null;
        }
    }

    /**
     * Tracks a pending rocket until it appears or times out.
     */
    private void await() {
        this.boost.pending = true;
        this.boost.expiry = this.mc.player.age + timeout;
    }

    private void duration(ItemStack stack) {
        FireworksComponent fireworks = stack.get(DataComponentTypes.FIREWORKS);

        int duration = fireworks == null ? 1 : fireworks.flightDuration();
        double time = Math.max(0.0, (duration + 1) * 500.0 - this.offset.get());

        int ticks = Math.max(1, (int) Math.ceil(time / 50.0));
        this.boost.endtime = this.mc.player.age + ticks;
    }

    private boolean active() {
        return this.boost.rocket != null && this.boost.rocket.isAlive();
    }

    /**
     * Checks whether a rocket is pending or still boosting.
     *
     * @return true when another rocket should not be launched
     */
    private boolean busy() {
        return this.boost.pending || this.active()
            || this.boost.endtime > this.mc.player.age;
    }

    /**
     * Checks whether a stopped flight should renew its boost.
     *
     * @return true when the current boost is about to expire
     */
    private boolean renew() {
        return this.boost.endtime > 0
            && this.mc.player.age >= this.boost.endtime - 1
            && !this.boost.pending && this.stocked();
    }



    public Vec3d direction() {
        float yaw = this.yaw();
        int state = this.state();

        Vec3d direction = Vec3d.ZERO;
        Vec3d forward = Vec3d.fromPolar(0.0F, yaw);
        Vec3d right = Vec3d.fromPolar(0.0F, yaw + 90.0F);

        if ((state & 1) != 0) {
            direction = direction.add(forward);
        }

        if ((state & 2) != 0) {
            direction = direction.subtract(forward);
        }

        if ((state & 4) != 0) {
            direction = direction.add(right);
        }

        if ((state & 8) != 0) {
            direction = direction.subtract(right);
        }

        if ((state & 16) != 0) {
            direction = direction.add(0.0, 1.0, 0.0);
        }

        if ((state & 32) != 0) {
            direction = direction.add(0.0, -1.0, 0.0);
        }

        return direction;
    }

    /**
     * Retrieves the current movement input state as a bitmask.
     */
    private int state() {
        int state = 0;

        state |= this.mc.options.forwardKey.isPressed() ? 1 : 0;
        state |= this.mc.options.backKey.isPressed() ? 2 : 0;
        state |= this.mc.options.rightKey.isPressed() ? 4 : 0;
        state |= this.mc.options.leftKey.isPressed() ? 8 : 0;
        state |= this.mc.options.jumpKey.isPressed() ? 16 : 0;
        state |= this.mc.options.sneakKey.isPressed() ? 32 : 0;

        return state != 0 ? state : this.forward.get() ? 1 : 0;
    }

    /**
     * Starts automatic takeoff after jump is held long enough.
     */
    private void takeoff() {
        if (!this.starter.get() || !this.flight.available()) {
            this.jump = 0;
            this.input.reset();
            return;
        }

        if (this.input.active()) return;

        if (!this.mc.options.jumpKey.isPressed() ||
            this.mc.player.isOnGround()) {
            this.jump = 0;
            return;
        }

        if (++this.jump >= this.timer.get()) {
            this.input.start();
        }
    }

    /**
     * Updates flight yaw from input while preserving vertical input.
     */
    private Vec3d steer(Vec3d input) {
        double length = input.horizontalLength();
        if (length < epsilon) {
            if (!this.motion.steering) {
                this.motion.yaw = this.mc.player.getYaw();
                this.motion.steering = true;
            }
            return input;
        }

        this.motion.steering = true;
        this.motion.yaw = (float) (Math.toDegrees(
            Math.atan2(input.z, input.x)) - 90.0
        );

        Vec3d flat = Vec3d.fromPolar(0.0F, this.motion.yaw);
        flat = flat.multiply(length);

        return flat.add(0.0, input.y, 0.0).normalize();
    }

    /**
     * @return true when the turn requires a brake tick
     */
    private boolean sharp(Vec3d first, Vec3d second) {
        if (first.horizontalLength() < epsilon &&
            second.horizontalLength() < epsilon) {
            return false;
        }

        double prev = first.length(), next = second.length();
        if (prev < epsilon || next < epsilon) return false;

        double dot = first.dotProduct(second);
        return dot / (prev * next) < sharp - epsilon;
    }

    /**
     * Converts a direction vector into a clamped flight pitch.
     *
     * @return pitch angle in degrees
     */
    private float angle(Vec3d dir) {
        return MathHelper.clamp((float) -Math.toDegrees(
            Math.atan2(dir.y, dir.horizontalLength())
        ), -90.0F, 90.0F);
    }



    private void aim(Vec3d dir, boolean boosted, boolean manual) {
        if (manual) {
            this.motion.porpoise = false;
            this.motion.leveling = false;
            this.motion.altitude = this.mc.player.getY();
            this.motion.pitch = this.angle(dir);
            return;
        }

        if (this.porpoise.get()) {
            this.porpoise();
            return;
        }

        this.motion.porpoise = false;

        if (!this.motion.leveling) {
            this.motion.altitude = this.mc.player.getY();
            this.motion.leveling = true;
        }

        this.motion.pitch = this.level(
            this.mc.player.getVelocity(), boosted
        );
    }

    /**
     * Finds the pitch that most closely maintains the target altitude.
     */
    private float level(Vec3d velocity, boolean boosted) {
        double error = this.motion.altitude - this.mc.player.getY();
        double desired = MathHelper.clamp(error * gain, -0.2, 0.2);

        Choice choice = new Choice(
            this.motion.pitch, Double.POSITIVE_INFINITY
        );

        choice = this.search(velocity, boosted, desired,
            -bound, 5.0F, 16, choice
        );

        choice = this.search(velocity, boosted, desired,
            choice.pitch() - 5.0F, 0.5F, 20, choice
        );

        choice = this.search(velocity, boosted, desired,
            choice.pitch() - 0.5F, 0.05F, 20, choice
        );

        return choice.pitch();
    }

    /**
     * Steers one porpoise leg at a time, reversing at the configured height
     * and at terrain. A swing with both ends obstructed recentres on the
     * player instead of looping into the obstruction.
     */
    private void porpoise() {
        double y = this.mc.player.getY();
        double half = Math.max(2.0, this.porpoiseHeight.get() / 2.0);

        if (!this.motion.porpoise ||
            Math.abs(y - this.motion.porpoiseBase) > half * 3.0) {
            this.motion.porpoise = true;
            this.motion.porpoiseBase = y;
            this.motion.porpoiseUp = true;
        }

        double low = this.motion.porpoiseBase - half;
        double high = this.motion.porpoiseBase + half;

        boolean up = this.clear(y, high);
        boolean down = this.clear(y, low);

        if (!up && !down) {
            this.motion.porpoiseBase = y;
            this.motion.porpoiseUp = true;
            this.motion.pitch = -porpoisePitch;
            return;
        }

        if (this.motion.porpoiseUp && (!up || y >= high)) {
            this.motion.porpoiseUp = false;
        } else if (!this.motion.porpoiseUp && (!down || y <= low)) {
            this.motion.porpoiseUp = true;
        }

        this.motion.pitch = this.motion.porpoiseUp
            ? -porpoisePitch : porpoisePitch;
    }

    /**
     * Checks the column between two heights for blocks the swing would hit.
     *
     * @param from first height
     */
    private boolean clear(double from, double to) {
        if (this.mc.world == null || this.mc.player == null) return false;

        int min = MathHelper.floor(Math.min(from, to));
        int max = MathHelper.floor(Math.max(from, to)) + 1;
        int x = MathHelper.floor(this.mc.player.getX());
        int z = MathHelper.floor(this.mc.player.getZ());
        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int y = min; y <= max; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    pos.set(x + dx, y, z + dz);
                    if (!this.mc.world.getBlockState(pos)
                        .getCollisionShape(this.mc.world, pos).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Blends the steering yaw toward the player's current yaw. Zero leaves the
     * yaw untouched so Baritone can keep its own heading.
     */
    private float aligned(float yaw) {
        double factor = this.alignment.get();
        if (factor >= 1.0) return yaw;

        float base = this.view.active
            ? this.view.yaw : this.mc.player.getYaw();

        return base + MathHelper.wrapDegrees(yaw - base) * (float) factor;
    }

    /**
     * @param start first pitch value
     */
    private Choice search(Vec3d velocity, boolean boosted,
        double desired, float start, float step, int count, Choice choice) {

        for (int index = 0; index <= count; index++) {
            float pitch = MathHelper.clamp(start + index * step, -bound, 20.0F);

            double score = this.score(velocity, pitch, desired, boosted);
            if (score < choice.score()) choice = new Choice(pitch, score);
        }

        return choice;
    }

    private double score(Vec3d velocity, float pitch, double desired, boolean boosted) {
        Vec3d next = this.predict(velocity, this.motion.aligned, pitch, boosted);
        double score = Math.abs(next.y - desired);

        score += Math.max(0.0,
            this.minimum.get() / ticks - next.horizontalLength()
        ) * 0.01;

        score += Math.abs(pitch - this.motion.pitch) * 1.0E-5;
        return score;
    }



    /**
     * Synchronizes the rotation and launches a rocket when requested.
     */
    private void rotate(boolean launch) {
        boolean changed = this.changed();

        if (launch) {
            changed = true;
            float yaw = this.motion.aligned, pitch = this.motion.pitch;
            Rotations.rotate(yaw, pitch, priority, () -> this.rocket(yaw, pitch));
        } else if (changed) {
            Rotations.rotate(this.motion.aligned, this.motion.pitch, priority);
        }

        if (changed) this.remember();
    }

    private boolean changed() {
        return !this.turn.active ||
            Math.abs(MathHelper.wrapDegrees(
                this.motion.aligned - this.turn.yaw)) > 0.05F ||
            Math.abs(
                this.motion.pitch - this.turn.pitch) > 0.05F;
    }

    private void remember() {
        this.turn.yaw = this.motion.aligned;
        this.turn.pitch = this.motion.pitch;
        this.turn.active = true;
    }



    /**
     * Calculates the maximum allowed speed for a flight direction.
     *
     * @return maximum speed in blocks per tick
     */
    private double maximum(Vec3d dir) {
        double speed = ceiling;

        if (dir.y > 0.0) {
            speed -= this.penalty.get() * dir.y;
        }

        speed = Math.min(this.maximum.get(), speed);
        return Math.max(0.0, speed / ticks);
    }

    /**
     * Predicts the next velocity from elytra and rocket physics.
     */
    private Vec3d predict(Vec3d velocity, float yaw, float pitch, boolean boosted) {
        Vec3d rotation = Vec3d.fromPolar(pitch, yaw);
        Vec3d next = Elytra.glide(velocity, rotation);
        return boosted ? this.firework(next, rotation) : next;
    }

    private Vec3d firework(Vec3d velocity, Vec3d rotation) {
        return velocity.add(
            this.thrust(velocity.x, rotation.x),
            this.thrust(velocity.y, rotation.y),
            this.thrust(velocity.z, rotation.z)
        );
    }

    private double thrust(double velocity, double rotation) {
        return rotation * 0.1 + (rotation * 1.5 - velocity) * 0.5;
    }



    private boolean launch(Vec3d dir, boolean boosted, boolean redirect) {
        if (!this.stocked()) return false;

        boolean diving = (this.motion.input & 33) == 33;
        if (diving && this.renew()) return true;

        if (this.busy()) return false;
        if (diving || !boosted && redirect) return true;

        Vec3d next = this.predict(this.mc.player.getVelocity(),
            this.motion.aligned, this.motion.pitch, boosted
        );

        return next.horizontalLength() < Math.min(
            this.minimum.get() / ticks, this.maximum(dir)
        );
    }

    /**
     * Marks a rocket launch as pending and adjusts leveling.
     */
    private void prepare(boolean manual) {
        this.await();
        if (manual || this.porpoise.get()) return;

        Vec3d velocity = this.mc.player.getVelocity();
        this.motion.pitch = this.level(velocity, true);
    }

    private void rocket(float yaw, float pitch) {
        if (!Client.ready() || !Client.interaction()) {
            this.cancel();
            return;
        }

        if (!this.flight.request(() -> this.firework(yaw, pitch))) {
            this.cancel();
        }
    }

    /**
     * Sends a firework and updates the current boost timer.
     */
    private boolean firework(float yaw, float pitch) {
        ItemStack stack = this.stack();
        if (!stack.isOf(Items.FIREWORK_ROCKET)) {
            return false;
        }

        this.boost.automatic = true;

        try {
            if (Elytra.firework(yaw, pitch)) {
                this.duration(stack);
            } else {
                return false;
            }
        } finally {
            this.boost.automatic = false;
        }

        this.boost.expiry = this.mc.player.age + timeout;
        return true;
    }

    private void cancel() {
        this.boost.pending = false;
    }



    public boolean view() {
        return this.view(this.direction());
    }

    private boolean view(Vec3d input) {
        if (!this.isActive() ||
            this.mc.player == null ||
            !this.mc.player.isGliding()) {
            return false;
        }

        if (input.lengthSquared() < epsilon) {
            this.restore();
            return false;
        }

        if (!this.view.active) {
            this.view.yaw = this.mc.player.getYaw();
            this.view.pitch = this.mc.player.getPitch();
            this.view.active = true;
        }

        return true;
    }

    public void look(double mx, double my) {
        this.view.yaw += (float) (mx * 0.15);

        this.view.pitch = MathHelper.clamp(
            this.view.pitch + (float) (my * 0.15), -90.0F, 90.0F
        );
    }

    /**
     * @return true when the custom camera rotation should be used
     */
    public boolean camera() {
        return this.isActive() && this.mc.player != null
            && this.view.active && this.mc.player.isGliding();
    }

    public float yaw() {
        return this.view.active ? this.view.yaw : this.mc.player.getYaw();
    }

    public float pitch() {
        return this.view.active ? this.view.pitch : this.mc.player.getPitch();
    }

    private void restore() {
        if (!this.view.active) return;

        if (this.mc.player != null) {
            this.mc.player.setYaw(this.view.yaw);
            this.mc.player.setPitch(this.view.pitch);
        }

        this.view.active = false;
    }



    private boolean halted() {
        ElytraTweaks tweaks = Modules.get().get(ElytraTweaks.class);
        return tweaks != null && tweaks.halted();
    }

    private boolean stocked() {
        return this.stack().isOf(Items.FIREWORK_ROCKET);
    }

    /**
     * Finds the firework stack that controlled flight can use.
     */
    private ItemStack stack() {
        ItemStack main = this.mc.player.getMainHandStack();
        if (main.isOf(Items.FIREWORK_ROCKET)) return main;

        ItemStack offhand = this.mc.player.getOffHandStack();
        if (offhand.isOf(Items.FIREWORK_ROCKET)) return offhand;

        int slot = Hotbar.find(Items.FIREWORK_ROCKET);
        return slot >= 0 ? Hotbar.stack(slot) : ItemStack.EMPTY;
    }



    /**
     * Enables the shared no-durability spoof mode while chest swap is on.
     */
    private void applyChestSwap() {
        if (!this.chestSwap.get() || this.mc.player == null) return;

        if (this.tweaks == null) {
            this.tweaks = Modules.get().get(ElytraTweaks.class);
        }

        if (this.tweaks == null) return;

        if (!this.tweaks.isActive()) {
            this.tweaks.toggle();
            this.tweaksEnabledByUs = true;
        }

        this.spoof = ModuleSettings.get(this.tweaks, "spoof-mode");
        if (this.spoof == null) return;

        if (this.spoofBefore == null) {
            this.spoofBefore = this.spoof.get();
        }

        if (!this.spoof.get()) this.spoof.set(true);
    }

    /**
     * Releases the shared spoof mode once the glide is over.
     */
    private void releaseChestSwap() {
        if (this.tweaks == null) return;

        if (this.mc.player != null && this.mc.player.isGliding()) {
            this.info("Chest swap stays with Elytra Tweaks until you land.");
            return;
        }

        if (this.spoof != null && Boolean.FALSE.equals(this.spoofBefore)) {
            this.spoof.set(false);
        }

        if (this.tweaksEnabledByUs && this.tweaks.isActive()) {
            this.tweaks.toggle();
        }

        this.tweaks = null;
        this.spoof = null;
        this.spoofBefore = null;
        this.tweaksEnabledByUs = false;
    }

    /**
     * Applies the chest swap setting only while this module is running.
     */
    private void chestSwapChanged(boolean enabled) {
        if (!this.isActive()) return;

        if (enabled) {
            this.applyChestSwap();
        } else {
            this.releaseChestSwap();
        }
    }

    /**
     * Stores a candidate leveling pitch and its prediction score.
     */
    private record Choice(float pitch, double score) {}

    private static class Motion {
        private int input;
        private Vec3d dir;

        private boolean leveling;
        private boolean steering;
        private boolean brake;

        private boolean porpoise;
        private boolean porpoiseUp;
        private double porpoiseBase;

        private double altitude;

        private float yaw;
        private float aligned;
        private float pitch;
    }

    private static class Turn {
        private boolean active;
        private float yaw;
        private float pitch;
    }

    private static class View {
        private boolean active;
        private float yaw;
        private float pitch;
    }

    private static class Boost {
        private int expiry;
        private int endtime;

        private boolean pending;
        private boolean automatic;

        private FireworkRocketEntity rocket;
    }

}
