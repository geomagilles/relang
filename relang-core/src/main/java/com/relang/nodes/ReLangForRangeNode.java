package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "for", description = "For loop over integer range")
public final class ReLangForRangeNode extends ReLangNode {

    @Child private ReLangNode startNode;
    @Child private ReLangNode endNode;
    @Child private ReLangNode bodyNode;
    private final int varSlot;
    private final boolean inclusive;

    public ReLangForRangeNode(ReLangNode startNode, ReLangNode endNode, ReLangNode bodyNode, int varSlot, boolean inclusive) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.bodyNode = bodyNode;
        this.varSlot = varSlot;
        this.inclusive = inclusive;
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var startVal = startNode.executeGeneric(frame);
        var endVal = endNode.executeGeneric(frame);

        if (!(startVal instanceof Long start)) {
            throw new ReLangTypeError(this, RuntimeDiagnostics.rangeBoundMustBeInt("start", startVal));
        }
        if (!(endVal instanceof Long end)) {
            throw new ReLangTypeError(this, RuntimeDiagnostics.rangeBoundMustBeInt("end", endVal));
        }

        long limit = inclusive ? end + 1 : end;
        for (long i = start; i < limit; i++) {
            frame.setObject(varSlot, i);
            try {
                bodyNode.executeGeneric(frame);
            } catch (ReLangBreakException e) {
                break;
            } catch (ReLangContinueException e) {
                // continue to next iteration
            }
        }
        return UNIT;
    }
}
