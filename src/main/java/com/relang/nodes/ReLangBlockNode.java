package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

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
            return 0L;
        }

        // Execute all but the last one
        for (int i = 0; i < bodyNodes.length - 1; i++) {
            bodyNodes[i].executeGeneric(frame);
        }

        // Return result of the last one
        return bodyNodes[bodyNodes.length - 1].executeGeneric(frame);
    }
}
