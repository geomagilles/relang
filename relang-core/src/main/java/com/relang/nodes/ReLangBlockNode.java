package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.relang.ReLangContext;

/**
 * Executes a sequence of statements.
 * Handles path tracking for resumability:
 * - REWINDING (resume): Pops path index from FrameState to skip forward
 * - UNWINDING (suspend): Catches exception, pushes current index, re-throws
 */
@NodeInfo(shortName = "block", description = "The node implementing a source code block")
public final class ReLangBlockNode extends ReLangNode {

    @Children
    private final ReLangNode[] bodyNodes;

    public ReLangBlockNode(ReLangNode[] bodyNodes) {
        this.bodyNodes = bodyNodes;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        if (bodyNodes.length == 0) {
            return UNIT;
        }

        // Get frame state from context (set by RootNode during resume)
        ReLangContext context = ReLangContext.get(this);
        FrameState frameState = context.getActiveFrameState();

        int startIndex = 0;

        // REWINDING: Skip to where we left off
        if (frameState != null && frameState.hasPath()) {
            startIndex = frameState.popPathIndex();
        }

        if (startIndex >= bodyNodes.length) {
            return UNIT;
        }

        // Execute statements
        Object result = UNIT;
        for (int i = startIndex; i < bodyNodes.length; i++) {
            try {
                result = bodyNodes[i].executeGeneric(frame);
            } catch (ReLangSuspendException e) {
                // UNWINDING: Record which statement we were at
                // For checkpoint, resume at NEXT statement; for nested calls, resume at same statement
                if (bodyNodes[i] instanceof ReLangCheckpointNode) {
                    e.pushPathIndex(i + 1);
                } else {
                    e.pushPathIndex(i);
                }
                throw e;
            }
        }
        return result;
    }
}
