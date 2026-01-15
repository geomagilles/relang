package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "return", description = "The node implementing a return statement")
public final class ReLangReturnNode extends ReLangNode {

    @Child
    private ReLangNode valueNode;

    public ReLangReturnNode(ReLangNode valueNode) {
        this.valueNode = valueNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object result = valueNode.executeGeneric(frame);
        throw new ReLangReturnException(result);
    }
}
