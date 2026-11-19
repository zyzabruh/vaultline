package com.pro0101.vaultline.modules.hyperglide;

import com.pro0101.vaultline.modules.hyperglide.navigation.Highways;
import com.pro0101.vaultline.modules.hyperglide.navigation.Route;
import com.pro0101.vaultline.modules.hyperglide.navigation.Search;
import com.pro0101.vaultline.modules.hyperglide.navigation.Segment;
import java.util.List;
import net.minecraft.util.math.Vec2f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The A* search over the highway graph, on a network small enough to know the answer by hand.
 *
 * <p>This is the code Hyperglide replaced Dijkstra with in v2026.11.15; the test pins what both
 * algorithms must agree on: a highway that is fast enough is worth the walk to it, and one that is
 * slower than walking is not.
 */
class SearchTest {
    /** One straight highway along x, from -500 to 500 at z = 0. */
    private static final List<Highways.Road> ROADS = List.of(new Highways.Road("test", Highways.Kind.Axis,
        List.of(new Segment(new Vec2f(-500, 0), new Vec2f(500, 0)))));

    @Test
    void aFastHighwayIsWorthTheWalkToIt() {
        Route route = new Search(ROADS).find(new Vec2f(-400, 30), new Vec2f(400, 30), 10f, 1f);

        assertTrue(route.legs().stream().anyMatch(leg -> leg.type() == Route.Type.Highway),
            "the route should use the highway");

        // Thirty down, eight hundred at ten times the walking speed, thirty up: about a hundred and
        // forty seconds, against eight hundred walking straight there.
        assertTrue(route.time() < 200f, "time was " + route.time());
        assertTrue(route.time() > 100f, "time was " + route.time());
    }

    @Test
    void aSlowHighwayIsLeftAlone() {
        Route route = new Search(ROADS).find(new Vec2f(-400, 30), new Vec2f(400, 30), 10f, 20f);

        assertFalse(route.legs().stream().anyMatch(leg -> leg.type() == Route.Type.Highway));
        assertEquals(800f / 20f, route.time(), 1f);
    }

    @Test
    void aDestinationOnTopOfTheStartIsNoRouteAtAll() {
        Route route = new Search(ROADS).find(new Vec2f(0, 0), new Vec2f(0.01f, 0), 10f, 1f);

        assertTrue(route.legs().isEmpty());
        assertEquals(0f, route.time(), 0f);
    }

    @Test
    void theRouteStartsAndEndsAtTheRequestedPoints() {
        Route route = new Search(ROADS).find(new Vec2f(-400, 30), new Vec2f(400, 30), 10f, 1f);

        assertFalse(route.legs().isEmpty());
        assertEquals(-400f, route.legs().getFirst().start().x, 0.5f);
        assertEquals(30f, route.legs().getFirst().start().y, 0.5f);
        assertEquals(400f, route.legs().getLast().end().x, 0.5f);
        assertEquals(30f, route.legs().getLast().end().y, 0.5f);
    }
}
