package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "break")
public final class ReLangBreakNode extends ReLangNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        throw new ReLangBreakException();
    }
}
