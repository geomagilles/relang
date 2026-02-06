package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "negate")
@NodeChild(value = "operand", type = ReLangNode.class)
public abstract class NegateNode extends ReLangNode {

    @Specialization
    protected long negateLong(long value) {
        return -value;
    }

    @Specialization
    protected double negateDouble(double value) {
        return -value;
    }
}
