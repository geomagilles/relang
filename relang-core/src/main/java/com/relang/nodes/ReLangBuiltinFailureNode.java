package com.relang.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.ReLang;

/**
 * Built-in function: {@code Failure(message)}
 * Creates a Failure value wrapping the given message string.
 */
public final class ReLangBuiltinFailureNode extends RootNode {

    public ReLangBuiltinFailureNode(ReLang language) {
        super(language, FrameDescriptor.newBuilder().build());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        var args = frame.getArguments();
        var message = args.length > 0 ? args[0].toString() : "unknown error";
        return new ReLangFailure(message);
    }

    @Override
    public String getName() {
        return "Failure";
    }
}
