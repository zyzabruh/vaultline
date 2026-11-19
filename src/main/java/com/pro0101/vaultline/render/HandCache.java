package com.pro0101.vaultline.render;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

/**
 * One lazily built object per hand look, with no memory of a failed build.
 *
 * <p>Building a pipeline or a layer can fail — a shader the pack replaced, a sampler the driver
 * refuses — and a failure must cost the effect, not the frame. The rule this class enforces is the
 * one the renderer needs: a failed build stores nothing, so the vanilla path is used for that frame
 * and the build is retried on the next one, while a successful build is kept and shared.
 *
 * <p>The look is the only key. A look never reuses another look's object, and
 * {@link HandFx.Look#OFF} never reaches the builder at all, so switching the toggles cannot carry a
 * pipeline, a sampler or a shader from the previous look into the next one.
 *
 */
public final class HandCache<T> {
    private final Map<HandFx.Look, T> built = new EnumMap<>(HandFx.Look.class);

    /**
     * Returns the object of a look, building it once if needed.
     *
     * @param builder build to run on a miss
     * @param fallback value used for {@link HandFx.Look#OFF} and after a failed build
     * @return the cached object, the freshly built one, or the fallback
     */
    public T resolve(HandFx.Look look, Function<HandFx.Look, T> builder, T fallback) {
        if (!HandFx.retargets(look)) return fallback;

        T cached = this.built.get(look);

        if (cached != null) return cached;

        T made;

        try {
            made = builder.apply(look);
        } catch (RuntimeException exception) {
            // Nothing is stored: the next call retries instead of serving a broken object forever.
            return fallback;
        }

        if (made == null) return fallback;

        this.built.put(look, made);

        return made;
    }

    public boolean has(HandFx.Look look) {
        return this.built.containsKey(look);
    }

    public int size() {
        return this.built.size();
    }

    public void clear() {
        this.built.clear();
    }
}
