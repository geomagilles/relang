package com.relang.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.ReLang;

/**
 * Built-in function: {@code json(text)}
 * Creates a Json value wrapping the given string.
 */
public final class ReLangBuiltinJsonNode extends RootNode {

    public ReLangBuiltinJsonNode(ReLang language) {
        super(language, FrameDescriptor.newBuilder().build());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        var args = frame.getArguments();
        var text = args.length > 0 ? args[0].toString() : "null";
        return new ReLangJson(text);
    }

    @Override
    public String getName() {
        return "json";
    }
}
