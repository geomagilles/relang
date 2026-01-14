package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.RepeatingNode;

@NodeInfo(shortName = "while", description = "The node implementing a while loop")
public final class ReLangWhileNode extends ReLangNode {

    @Child
    private LoopNode loopNode;

    public ReLangWhileNode(ReLangNode conditionNode, ReLangNode bodyNode) {
        this.loopNode = Truffle.getRuntime().createLoopNode(new ReLangRepeatingNode(conditionNode, bodyNode));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        loopNode.execute(frame);
        return 0L; // Loops return 0 for now
    }

    // Inner class for the repeating logic
    private static class ReLangRepeatingNode extends ReLangNode implements RepeatingNode {
        @Child
        private ReLangNode conditionNode;
        @Child
        private ReLangNode bodyNode;

        public ReLangRepeatingNode(ReLangNode conditionNode, ReLangNode bodyNode) {
            this.conditionNode = conditionNode;
            this.bodyNode = bodyNode;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            Object res = conditionNode.executeGeneric(frame);
            boolean cond;
            if (res instanceof Boolean) {
                cond = (Boolean) res;
            } else if (res instanceof Long) {
                cond = ((Long) res) != 0;
            } else {
                throw new RuntimeException("Condition must be boolean or long");
            }

            if (cond) {
                bodyNode.executeGeneric(frame);
                return true; // continue loop
            } else {
                return false; // break loop
            }
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            throw new UnsupportedOperationException("Should not be called directly");
        }
    }
}
