package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "*")
@NodeChild(value = "left", type = ReLangNode.class)
@NodeChild(value = "right", type = ReLangNode.class)
public abstract class MulNode extends ReLangNode {

    @Specialization
    protected long mul(long left, long right) {
        return left * right;
    }

    @Specialization
    protected double mul(long left, double right) {
        return left * right;
    }

    @Specialization
    protected double mul(double left, long right) {
        return left * right;
    }

    @Specialization
    protected double mul(double left, double right) {
        return left * right;
    }
}
