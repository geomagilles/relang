package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "match", description = "Match expression with subject")
public final class ReLangMatchNode extends ReLangNode {

    @Child private ReLangNode subjectNode;
    @Children private final MatchArmNode[] arms;

    public ReLangMatchNode(ReLangNode subjectNode, MatchArmNode[] arms) {
        this.subjectNode = subjectNode;
        this.arms = arms;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        var subject = subjectNode.executeGeneric(frame);

        for (var arm : arms) {
            if (arm.matches(frame, subject)) {
                return arm.executeBody(frame);
            }
        }
        throw new ReLangTypeError(this, "Non-exhaustive match: no arm matched value " + subject);
    }

    /**
     * A single arm in a match expression.
     */
    public static final class MatchArmNode extends ReLangNode {
        @Child private ReLangNode patternNode; // null for wildcard
        @Child private ReLangNode bodyNode;
        private final MatchPatternKind kind;

        public enum MatchPatternKind {
            WILDCARD,   // _ -> matches anything
            NONE,       // none -> matches ReLangNone
            LITERAL     // expr -> matches if subject == expr value
        }

        public MatchArmNode(MatchPatternKind kind, ReLangNode patternNode, ReLangNode bodyNode) {
            this.kind = kind;
            this.patternNode = patternNode;
            this.bodyNode = bodyNode;
        }

        public boolean matches(VirtualFrame frame, Object subject) {
            return switch (kind) {
                case WILDCARD -> true;
                case NONE -> subject instanceof ReLangNone;
                case LITERAL -> {
                    var patternValue = patternNode.executeGeneric(frame);
                    yield valuesEqual(subject, patternValue);
                }
            };
        }

        public Object executeBody(VirtualFrame frame) {
            return bodyNode.executeGeneric(frame);
        }

        @Override
        public Object executeGeneric(VirtualFrame frame) {
            throw new UnsupportedOperationException("MatchArmNode should not be executed directly");
        }

        private static boolean valuesEqual(Object a, Object b) {
            if (a instanceof Long la && b instanceof Long lb) return la.longValue() == lb.longValue();
            if (a instanceof Double da && b instanceof Double db) return da.doubleValue() == db.doubleValue();
            if (a instanceof Boolean ba && b instanceof Boolean bb) return ba.booleanValue() == bb.booleanValue();
            if (a instanceof String sa && b instanceof String sb) return sa.equals(sb);
            if (a instanceof ReLangNone && b instanceof ReLangNone) return true;
            return false;
        }
    }
}
