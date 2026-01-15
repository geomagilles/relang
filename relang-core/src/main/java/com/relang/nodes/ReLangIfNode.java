package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

@NodeInfo(shortName = "if", description = "The node implementing a conditional statement")
public final class ReLangIfNode extends ReLangNode {

    @Child private ReLangNode conditionNode;
    @Child private ReLangNode thenPartNode;
    @Child private ReLangNode elsePartNode;

    private final ConditionProfile conditionProfile = ConditionProfile.create();

    public ReLangIfNode(ReLangNode conditionNode, ReLangNode thenPartNode, ReLangNode elsePartNode) {
        this.conditionNode = conditionNode;
        this.thenPartNode = thenPartNode;
        this.elsePartNode = elsePartNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        boolean cond = evaluateAsBoolean(conditionNode.executeGeneric(frame));

        if (conditionProfile.profile(cond)) {
            return thenPartNode.executeGeneric(frame);
        } else if (elsePartNode != null) {
            return elsePartNode.executeGeneric(frame);
        } else {
            return UNIT;
        }
    }
}
