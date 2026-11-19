package com.pro0101.vaultline.modules.hyperglide.navigation;

import net.minecraft.util.math.Vec2f;
import java.util.ArrayList;
import java.util.List;

public record Route(List<Leg> legs, float distance, float time) {
    private static final float epsilon = 0.05F;

    public Route {
        legs = simplify(legs);
    }

    /**
     * Defines the type of route travel.
     */
    public enum Type {
        Normal,
        Highway
    }

    public boolean empty() {
        return this.legs.isEmpty();
    }

    /**
     * Merges connected legs that continue in the same direction.
     *
     * @param legs route legs to simplify
     */
    private static List<Leg> simplify(List<Leg> legs) {
        List<Leg> result = new ArrayList<>();

        for (Leg leg : legs) {
            if (!result.isEmpty() && merge(result.getLast(), leg)) {
                Leg previous = result.getLast();

                result.set(result.size() - 1, new Leg(
                    previous.start(), leg.end(),
                    previous.road(), previous.type()
                ));
            } else {
                result.add(leg);
            }
        }

        return List.copyOf(result);
    }

    /**
     * Checks whether two route legs can be represented as one.
     *
     * @param first previous route leg
     * @return true when both legs continue in the same direction
     */
    private static boolean merge(Leg first, Leg second) {
        if (first.type() != second.type()) return false;

        float dist = first.end().distanceSquared(second.start());
        if (dist > epsilon * epsilon) return false;

        Segment sa = new Segment(first.start(), first.end());
        Segment sb = new Segment(second.start(), second.end());

        return sa.forward(sb);
    }

    public record Leg(Vec2f start, Vec2f end, Highways.Road road, Type type) {
        public float length() {
            return this.end.add(this.start.negate()).length();
        }
    }
}
