package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.ConditionProfile;

@NodeInfo(shortName = "if", description = "The node implementing a conditional statement")
public final class ReLangIfNode extends ReLangNode {

    @Child
    private ReLangNode conditionNode;
    @Child
    private ReLangNode thenPartNode;
    @Child
    private ReLangNode elsePartNode;

    private final ConditionProfile condition = ConditionProfile.createBinaryProfile();

    public ReLangIfNode(ReLangNode conditionNode, ReLangNode thenPartNode, ReLangNode elsePartNode) {
        this.conditionNode = conditionNode;
        this.thenPartNode = thenPartNode;
        this.elsePartNode = elsePartNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        // We expect the condition to return a boolean.
        // In a real language, we would handle types more carefully (e.g.,
        // ImplicitCast).
        boolean cond;
        try {
            // Need to expose executeBoolean in ReLangNode or cast here.
            // For now, let's cast.
            Object res = conditionNode.executeGeneric(frame);
            if (res instanceof Boolean) {
                cond = (Boolean) res;
            } else if (res instanceof Long) {
                // C-style boolean: 0 is false, anything else is true
                cond = ((Long) res) != 0;
            } else {
                throw new RuntimeException("Condition must be boolean or long, got: " + res);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error evaluating condition", e);
        }

        if (condition.profile(cond)) {
            return thenPartNode.executeGeneric(frame);
        } else {
            if (elsePartNode != null) {
                return elsePartNode.executeGeneric(frame);
            } else {
                return 0L; // Default return for if without else?
            }
        }
    }
}
