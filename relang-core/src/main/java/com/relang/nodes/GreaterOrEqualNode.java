package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = ">=")
@NodeChild(value = "left", type = ReLangNode.class)
@NodeChild(value = "right", type = ReLangNode.class)
public abstract class GreaterOrEqualNode extends ReLangNode {

    @Specialization
    protected boolean greaterOrEqual(long left, long right) {
        return left >= right;
    }

    @Specialization
    protected boolean greaterOrEqual(double left, double right) {
        return left >= right;
    }
}
