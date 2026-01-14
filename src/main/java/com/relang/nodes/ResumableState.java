package com.relang.nodes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Represents the suspended execution state that can be serialized and restored later.
 * Contains a stack of FrameState objects representing the call stack at suspension.
 */
public class ResumableState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Stack of frames stored as ArrayList (last element = top of stack)
    private final ArrayList<FrameState> frames = new ArrayList<>();

    public void pushFrame(FrameState frame) {
        frames.add(frame);
    }

    public FrameState popFrame() {
        if (frames.isEmpty()) {
            throw new IllegalStateException("Cannot pop from empty frame stack");
        }
        return frames.remove(frames.size() - 1);
    }

    public FrameState peekFrame() {
        if (frames.isEmpty()) {
            return null;
        }
        return frames.get(frames.size() - 1);
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }
    
    public int getFrameCount() {
        return frames.size();
    }

    /**
     * Represents the state of a single stack frame.
     * Contains local variables and the execution path (statement indices).
     */
    public static class FrameState implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        // Local variables - only Long and Boolean values are supported
        private final HashMap<String, Object> locals;
        
        // Execution path indices stored as ArrayList (last element = top of stack)
        private final ArrayList<Integer> executionPath;

        public FrameState(Map<String, Object> locals, Stack<Integer> path) {
            this.locals = new HashMap<>();
            // Validate and copy locals - only allow serializable ReLang types
            for (Map.Entry<String, Object> entry : locals.entrySet()) {
                Object value = entry.getValue();
                if (value == null || value instanceof Long || value instanceof Boolean) {
                    this.locals.put(entry.getKey(), value);
                } else {
                    throw new IllegalArgumentException(
                        "Local variable '" + entry.getKey() + "' has non-serializable type: " 
                        + value.getClass().getName() + ". Only Long and Boolean are supported.");
                }
            }
            // Convert Stack to ArrayList
            this.executionPath = new ArrayList<>(path);
        }
        
        /**
         * Constructor for deserialization or direct construction with Lists.
         */
        public FrameState(Map<String, Object> locals, List<Integer> path) {
            this.locals = new HashMap<>();
            for (Map.Entry<String, Object> entry : locals.entrySet()) {
                Object value = entry.getValue();
                if (value == null || value instanceof Long || value instanceof Boolean) {
                    this.locals.put(entry.getKey(), value);
                } else {
                    throw new IllegalArgumentException(
                        "Local variable '" + entry.getKey() + "' has non-serializable type: " 
                        + value.getClass().getName() + ". Only Long and Boolean are supported.");
                }
            }
            this.executionPath = new ArrayList<>(path);
        }

        public Map<String, Object> getLocals() {
            return locals;
        }

        public void pushPathIndex(int index) {
            executionPath.add(index);
        }

        public int popPathIndex() {
            if (executionPath.isEmpty()) {
                return -1;
            }
            return executionPath.remove(executionPath.size() - 1);
        }

        public boolean hasPath() {
            return !executionPath.isEmpty();
        }
        
        public List<Integer> getExecutionPath() {
            return new ArrayList<>(executionPath);
        }
    }
}
