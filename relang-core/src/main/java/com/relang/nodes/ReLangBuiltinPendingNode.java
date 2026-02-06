package com.relang.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.ReLang;
import com.relang.ReLangContext;

/**
 * Built-in function: {@code pending()}
 * Creates a pending (unresolved) awaitable.
 * Registers the handle in the context's awaitable table and returns it.
 */
public final class ReLangBuiltinPendingNode extends RootNode {

    public ReLangBuiltinPendingNode(ReLang language) {
        super(language, FrameDescriptor.newBuilder().build());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        var handle = new AwaitableHandle();

        var context = ReLangContext.get(this);
        context.getAwaitableTable().register(handle);

        return handle;
    }

    @Override
    public String getName() {
        return "pending";
    }
}
