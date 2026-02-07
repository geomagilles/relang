package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "match{}", description = "Subjectless match expression (conditional dispatch)")
public final class ReLangMatchSubjectlessNode extends ReLangNode {

    @Children private final SubjectlessArmNode[] arms;

    public ReLangMatchSubjectlessNode(SubjectlessArmNode[] arms) {
        this.arms = arms;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        for (var arm : arms) {
            if (arm.evaluateCondition(frame)) {
                return arm.executeBody(frame);
            }
        }
        throw new ReLangTypeError(this, RuntimeDiagnostics.nonExhaustiveSubjectlessMatch());
    }

    public static final class SubjectlessArmNode extends ReLangNode {
        @Child private ReLangNode conditionNode; // null for wildcard _
        @Child private ReLangNode bodyNode;
        private final boolean isWildcard;

        public SubjectlessArmNode(boolean isWildcard, ReLangNode conditionNode, ReLangNode bodyNode) {
            this.isWildcard = isWildcard;
            this.conditionNode = conditionNode;
            this.bodyNode = bodyNode;
        }

        public boolean evaluateCondition(VirtualFrame frame) {
            if (isWildcard) return true;
            var val = conditionNode.executeGeneric(frame);
            if (!(val instanceof Boolean b)) {
                throw new ReLangTypeError(this, RuntimeDiagnostics.invalidSubjectlessMatchCondition(val));
            }
            return b;
        }

        public Object executeBody(VirtualFrame frame) {
            return bodyNode.executeGeneric(frame);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            throw new UnsupportedOperationException("SubjectlessArmNode should not be executed directly");
        }
    }
}
