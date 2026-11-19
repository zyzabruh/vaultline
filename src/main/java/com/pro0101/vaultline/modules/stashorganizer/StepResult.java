package com.pro0101.vaultline.modules.stashorganizer;

/**
 * What a {@link Step} wants the script to do next.
 */
public record StepResult(Kind kind, int target) {
    public enum Kind {
        RUNNING,
        /** The step is complete: move to the next one. */
        DONE,
        FAILED,
        /** Continue at the given step, for the loops (take another stack, reopen the box, retry). */
        JUMP
    }

    public static final StepResult RUNNING = new StepResult(Kind.RUNNING, -1);
    public static final StepResult DONE = new StepResult(Kind.DONE, -1);
    public static final StepResult FAILED = new StepResult(Kind.FAILED, -1);

    public static StepResult jumpTo(int step) {
        return new StepResult(Kind.JUMP, step);
    }
}
