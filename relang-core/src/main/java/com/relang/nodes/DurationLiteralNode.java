package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "duration")
public class DurationLiteralNode extends ReLangNode {
    private final long millis;

    public DurationLiteralNode(long millis) {
        this.millis = millis;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return new ReLangDuration(millis);
    }
}
