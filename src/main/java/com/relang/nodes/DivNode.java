package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "/")
@NodeChild(value = "left", type = ReLangNode.class)
@NodeChild(value = "right", type = ReLangNode.class)
public abstract class DivNode extends ReLangNode {

    @Specialization
    protected long div(long left, long right) {
        return left / right;
    }
}
