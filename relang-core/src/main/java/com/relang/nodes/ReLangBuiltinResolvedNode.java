package com.relang.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.ReLang;
import com.relang.ReLangContext;

/**
 * Built-in function: {@code resolved(value)}
 * Creates a resolved awaitable wrapping the given value.
 * Registers the handle in the context's awaitable table and returns it.
 */
public final class ReLangBuiltinResolvedNode extends RootNode {

    public ReLangBuiltinResolvedNode(ReLang language) {
        super(language, FrameDescriptor.newBuilder().build());
    }

    @Override
    public Object execute(VirtualFrame frame) {
        var args = frame.getArguments();
        if (args.length < 1) {
            throw new ReLangTypeError(this, RuntimeDiagnostics.resolvedArity(args.length));
        }

        var handle = new AwaitableHandle();
        handle.resolve(args[0]);

        var context = ReLangContext.get(this);
        context.getAwaitableTable().register(handle);

        return handle;
    }

    @Override
    public String getName() {
        return "resolved";
    }
}
