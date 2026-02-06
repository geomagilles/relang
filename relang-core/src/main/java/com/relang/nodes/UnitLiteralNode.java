package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "unit")
public class UnitLiteralNode extends ReLangNode {
    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return ReLangUnit.SINGLETON;
    }
}
