package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "bytes")
public class BytesLiteralNode extends ReLangNode {
    private final byte[] data;

    public BytesLiteralNode(byte[] data) {
        this.data = data;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return new ReLangBytes(data);
    }
}
