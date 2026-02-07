package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

@NodeInfo(shortName = "or")
public final class LogicalOrNode extends ReLangNode {

    @Child private ReLangNode left;
    @Child private ReLangNode right;
    private final ConditionProfile leftProfile = ConditionProfile.create();

    public LogicalOrNode(ReLangNode left, ReLangNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var leftValue = left.executeGeneric(frame);
        if (!(leftValue instanceof Boolean leftBool)) {
            throw new ReLangTypeError(this, RuntimeDiagnostics.logicalOperandMustBeBool("or", leftValue));
        }
        if (leftProfile.profile(leftBool)) {
            return true;
        }
        var rightValue = right.executeGeneric(frame);
        if (!(rightValue instanceof Boolean rightBool)) {
            throw new ReLangTypeError(this, RuntimeDiagnostics.logicalOperandMustBeBool("or", rightValue));
        }
        return rightBool;
    }
}
