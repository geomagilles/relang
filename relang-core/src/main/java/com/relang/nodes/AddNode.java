package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "+")
@NodeChild(value = "left", type = ReLangNode.class)
@NodeChild(value = "right", type = ReLangNode.class)
public abstract class AddNode extends ReLangNode {

    @Specialization
    protected long add(long left, long right) {
        return left + right;
    }

    @Specialization
    protected double add(double left, double right) {
        return left + right;
    }

    @Specialization
    protected String concat(String left, String right) {
        return left + right;
    }
}
