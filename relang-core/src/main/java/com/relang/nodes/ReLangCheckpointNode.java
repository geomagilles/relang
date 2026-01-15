package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * A checkpoint statement that suspends execution.
 * When executed, throws ReLangSuspendException which unwinds the call stack,
 * allowing each RootNode to capture its frame state.
 */
@NodeInfo(shortName = "checkpoint", description = "The node implementing a checkpoint statement")
public final class ReLangCheckpointNode extends ReLangNode {

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // Suspend execution by throwing
        throw new ReLangSuspendException();
    }
}
