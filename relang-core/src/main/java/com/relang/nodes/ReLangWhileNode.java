package com.relang.nodes;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.LoopNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RepeatingNode;

@NodeInfo(shortName = "while", description = "The node implementing a while loop")
public final class ReLangWhileNode extends ReLangNode {

    @Child private LoopNode loopNode;

    public ReLangWhileNode(ReLangNode conditionNode, ReLangNode bodyNode) {
        this.loopNode = Truffle.getRuntime().createLoopNode(new ReLangRepeatingNode(conditionNode, bodyNode));
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        loopNode.execute(frame);
        return UNIT;
    }

    private static class ReLangRepeatingNode extends ReLangNode implements RepeatingNode {
        @Child private ReLangNode conditionNode;
        @Child private ReLangNode bodyNode;

        ReLangRepeatingNode(ReLangNode conditionNode, ReLangNode bodyNode) {
            this.conditionNode = conditionNode;
            this.bodyNode = bodyNode;
        }

        @Override
        public boolean executeRepeating(VirtualFrame frame) {
            if (evaluateAsBoolean(conditionNode.executeGeneric(frame))) {
                try {
                    bodyNode.executeGeneric(frame);
                } catch (ReLangBreakException e) {
                    return false; // exit loop
                } catch (ReLangContinueException e) {
                    // skip rest of body, continue loop
                }
                return true;
            }
            return false;
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            throw new UnsupportedOperationException("Should not be called directly");
        }
    }
}
