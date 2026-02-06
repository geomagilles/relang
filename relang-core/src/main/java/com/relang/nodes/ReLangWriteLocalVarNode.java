package com.relang.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "writeVar", description = "The node for writing a local variable")
@NodeChild(value = "valueNode", type = ReLangNode.class)
@NodeField(name = "slot", type = int.class)
public abstract class ReLangWriteLocalVarNode extends ReLangNode {

    protected abstract int getSlot();

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    @Specialization
    protected long writeLong(VirtualFrame frame, long value) {
        frame.setObject(getSlot(), value);
        return value;
    }

    @Specialization(replaces = "writeLong")
    protected Object writeGeneric(VirtualFrame frame, Object value) {
        frame.setObject(getSlot(), value);
        return value;
    }
}
