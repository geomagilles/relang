package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "==")
@NodeChild(value = "left", type = ReLangNode.class)
@NodeChild(value = "right", type = ReLangNode.class)
public abstract class EqualsNode extends ReLangNode {

    @Specialization
    protected boolean equals(long left, long right) {
        return left == right;
    }

    @Specialization
    protected boolean equals(boolean left, boolean right) {
        return left == right;
    }

    @Specialization
    protected boolean equals(long left, double right) {
        return left == right;
    }

    @Specialization
    protected boolean equals(double left, long right) {
        return left == right;
    }

    @Specialization
    protected boolean equals(double left, double right) {
        return left == right;
    }

    @Specialization
    protected boolean equals(String left, String right) {
        return left.equals(right);
    }

    @Specialization(guards = "isRecord(left, right)")
    protected boolean equalsRecord(Object left, Object right) {
        return left.equals(right);
    }

    @Specialization(guards = "isProduct(left, right)")
    protected boolean equalsProduct(Object left, Object right) {
        return left.equals(right);
    }

    static boolean isRecord(Object left, Object right) {
        return left instanceof ReLangRecord && right instanceof ReLangRecord;
    }

    static boolean isProduct(Object left, Object right) {
        return left instanceof ReLangProduct && right instanceof ReLangProduct;
    }
}
