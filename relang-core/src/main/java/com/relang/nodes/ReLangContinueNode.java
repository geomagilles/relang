package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "continue")
public final class ReLangContinueNode extends ReLangNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new ReLangContinueException();
    }
}
