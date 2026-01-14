package com.relang.nodes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * Represents the suspended execution state that can be serialized and restored later.
 * Contains a stack of FrameState objects representing the call stack at suspension.
 * 
 * Supports both Java serialization and JSON serialization for flexibility.
 */
public class ResumableState implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
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
     * Serialize this state to a JSON string.
     */
    public String toJson() {
        JsonObject root = new JsonObject();
        JsonArray framesArray = new JsonArray();
        
        for (FrameState frame : frames) {
            framesArray.add(frame.toJsonObject());
        }
        
        root.add("frames", framesArray);
        return GSON.toJson(root);
    }
    
    /**
     * Deserialize a ResumableState from a JSON string.
     */
    public static ResumableState fromJson(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray framesArray = root.getAsJsonArray("frames");
        
        ResumableState state = new ResumableState();
        for (JsonElement elem : framesArray) {
            state.pushFrame(FrameState.fromJsonObject(elem.getAsJsonObject()));
        }
        
        return state;
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
            validateAndCopyLocals(locals);
            this.executionPath = new ArrayList<>(path);
        }
        
        /**
         * Constructor for deserialization or direct construction with Lists.
         */
        public FrameState(Map<String, Object> locals, List<Integer> path) {
            this.locals = new HashMap<>();
            validateAndCopyLocals(locals);
            this.executionPath = new ArrayList<>(path);
        }
        
        private void validateAndCopyLocals(Map<String, Object> source) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                Object value = entry.getValue();
                if (value == null || value instanceof Long || value instanceof Boolean) {
                    this.locals.put(entry.getKey(), value);
                } else {
                    throw new IllegalArgumentException(
                        "Local variable '" + entry.getKey() + "' has non-serializable type: " 
                        + value.getClass().getName() + ". Only Long and Boolean are supported.");
                }
            }
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
        
        /**
         * Convert this frame to a JSON object.
         */
        JsonObject toJsonObject() {
            JsonObject obj = new JsonObject();
            
            // Locals with type information
            JsonObject localsObj = new JsonObject();
            for (Map.Entry<String, Object> entry : locals.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Long) {
                    localsObj.addProperty(entry.getKey(), (Long) value);
                } else if (value instanceof Boolean) {
                    localsObj.addProperty(entry.getKey(), (Boolean) value);
                } else {
                    localsObj.add(entry.getKey(), null);
                }
            }
            obj.add("locals", localsObj);
            
            // Execution path
            JsonArray pathArray = new JsonArray();
            for (Integer idx : executionPath) {
                pathArray.add(idx);
            }
            obj.add("executionPath", pathArray);
            
            return obj;
        }
        
        /**
         * Create a FrameState from a JSON object.
         */
        static FrameState fromJsonObject(JsonObject obj) {
            Map<String, Object> locals = new HashMap<>();
            JsonObject localsObj = obj.getAsJsonObject("locals");
            
            for (String key : localsObj.keySet()) {
                JsonElement elem = localsObj.get(key);
                if (elem.isJsonNull()) {
                    locals.put(key, null);
                } else if (elem.getAsJsonPrimitive().isBoolean()) {
                    locals.put(key, elem.getAsBoolean());
                } else if (elem.getAsJsonPrimitive().isNumber()) {
                    locals.put(key, elem.getAsLong());
                }
            }
            
            List<Integer> path = new ArrayList<>();
            JsonArray pathArray = obj.getAsJsonArray("executionPath");
            for (JsonElement elem : pathArray) {
                path.add(elem.getAsInt());
            }
            
            return new FrameState(locals, path);
        }
    }
}
