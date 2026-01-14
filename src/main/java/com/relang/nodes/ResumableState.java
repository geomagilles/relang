package com.relang.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class ResumableState {
    // Stack of frames. Top of stack is the current (deepest) frame.
    private final Stack<FrameState> frames = new Stack<>();

    public void pushFrame(FrameState frame) {
        frames.push(frame);
    }

    public FrameState popFrame() {
        return frames.pop();
    }

    public FrameState peekFrame() {
        if (frames.isEmpty())
            return null;
        return frames.peek();
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }

    public static class FrameState {
        private final Map<String, Object> locals;
        // Path of indices taken in this frame (block indices, if branches)
        // We pop from this stack as we descend.
        private final Stack<Integer> executionPath;

        public FrameState(Map<String, Object> locals, Stack<Integer> path) {
            this.locals = locals;
            this.executionPath = path;
        }

        public Map<String, Object> getLocals() {
            return locals;
        }

        public void pushPathIndex(int index) {
            executionPath.push(index);
        }

        public int popPathIndex() {
            if (executionPath.isEmpty())
                return -1;
            return executionPath.pop();
        }

        public boolean hasPath() {
            return !executionPath.isEmpty();
        }
    }
}
