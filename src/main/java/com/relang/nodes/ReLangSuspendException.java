package com.relang.nodes;

import com.oracle.truffle.api.nodes.ControlFlowException;
import java.util.Stack;

/**
 * Exception thrown when a checkpoint is hit.
 * Carries the ResumableState up the call stack as each RootNode catches and re-throws.
 * 
 * During unwinding:
 * - BlockNodes push their statement index onto currentPath
 * - RootNodes drain currentPath into a new FrameState (along with locals) and push it onto state
 */
public final class ReLangSuspendException extends ControlFlowException {

    private final ResumableState state;
    
    // Temporary accumulator for path indices within the current frame during unwinding
    private final Stack<Integer> currentPath = new Stack<>();

    public ReLangSuspendException() {
        this.state = new ResumableState();
    }

    public ResumableState getState() {
        return state;
    }

    /**
     * Called by BlockNode during unwinding to record which statement we were at.
     */
    public void pushPathIndex(int index) {
        currentPath.push(index);
    }

    /**
     * Called by RootNode to get the accumulated path and clear it for the next frame.
     */
    public Stack<Integer> drainCurrentPath() {
        Stack<Integer> copy = new Stack<>();
        copy.addAll(currentPath);
        currentPath.clear();
        return copy;
    }
}
