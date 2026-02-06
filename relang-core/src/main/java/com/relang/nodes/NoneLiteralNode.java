package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "none")
public class NoneLiteralNode extends ReLangNode {

    public static final Object NONE = ReLangNone.SINGLETON;

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return NONE;
    }
}
