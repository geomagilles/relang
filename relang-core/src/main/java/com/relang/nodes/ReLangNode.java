package com.relang.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.relang.ReLangTypeSystem;
import com.relang.ReLangTypeSystemGen;

@TypeSystemReference(ReLangTypeSystem.class)
@NodeInfo(language = "ReLang", description = "The abstract base node for all language nodes")
public abstract class ReLangNode extends Node {

    /**
     * Unit value returned by statements that don't produce a meaningful result.
     * Used by control flow nodes (if without else, while, empty blocks).
     */
    public static final long UNIT = 0L;

    /**
     * The execute method that every node must implement.
     * Frame state for resumability is passed via ReLangContext.getActiveFrameState().
     */
    public abstract Object executeGeneric(VirtualFrame frame);

    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return ReLangTypeSystemGen.expectLong(executeGeneric(frame));
    }

    /**
     * Evaluate a value as a boolean condition.
     * Supports ReLang's C-style truthiness: booleans directly, longs where 0 is false.
     *
     * @param value the result of evaluating a condition expression
     * @return the boolean interpretation
     * @throws IllegalArgumentException if the value is not a valid condition type
     */
    protected static boolean evaluateAsBoolean(Object value) {
        return switch (value) {
            case Boolean b -> b;
            case Long l -> l != 0;
            case null -> throw new IllegalArgumentException("Condition cannot be null");
            default -> throw new IllegalArgumentException(
                    "Condition must be boolean or long, got: " + value.getClass().getSimpleName());
        };
    }
}
