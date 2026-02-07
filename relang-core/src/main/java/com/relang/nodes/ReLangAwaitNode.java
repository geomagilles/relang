package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * Await node: resolves an awaitable or suspends if pending.
 * {@code await expr} - if resolved, returns result; if pending, suspends (auto-checkpoint).
 * <p>
 * On resume, the block re-enters at this same statement index. The child expression
 * re-evaluates (reading the handle from a restored local), and if the host has resolved
 * it between suspensions, the handle will now be resolved and the result is returned.
 */
@NodeInfo(shortName = "await", description = "The node implementing await expression")
public final class ReLangAwaitNode extends ReLangNode {

    @Child private ReLangNode awaitableExpr;

    public ReLangAwaitNode(ReLangNode awaitableExpr) {
        this.awaitableExpr = awaitableExpr;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var value = awaitableExpr.executeGeneric(frame);

        if (!(value instanceof AwaitableHandle handle)) {
            throw new ReLangTypeError(this, RuntimeDiagnostics.invalidAwaitOperand(value));
        }

        if (handle.isResolved()) {
            return handle.getResult();
        } else if (handle.isFailed()) {
            return handle.getResult();
        } else {
            // PENDING - suspend execution (auto-checkpoint)
            var suspend = new ReLangSuspendException();
            suspend.setAwaitedHandleId(handle.getId());
            throw suspend;
        }
    }
}
