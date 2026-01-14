package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "readVar", description = "The node for reading a local variable")
@NodeField(name = "slot", type = int.class)
public abstract class ReLangReadLocalVarNode extends ReLangNode {

    protected abstract int getSlot();

    @Specialization
    protected Object readGeneric(VirtualFrame frame) {
        return frame.getObject(getSlot());
    }
}
