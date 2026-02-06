package com.relang.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.ReLang;

/**
 * Built-in function: {@code now()}
 * Returns the current timestamp.
 */
public final class ReLangBuiltinNowNode extends RootNode {

    public ReLangBuiltinNowNode(ReLang language) {
        super(language, FrameDescriptor.newBuilder().build());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return ReLangTimestamp.now();
    }

    @Override
    public String getName() {
        return "now";
    }
}
