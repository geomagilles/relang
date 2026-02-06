package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "not")
@NodeChild(value = "operand", type = ReLangNode.class)
public abstract class NotNode extends ReLangNode {

    @Specialization
    protected boolean not(boolean value) {
        return !value;
    }
}
