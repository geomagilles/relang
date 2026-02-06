package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "readArg", description = "The node for reading a function argument")
public class ReLangReadArgumentNode extends ReLangNode {

    private final int index;
    @Child private ReLangNode defaultValueNode;

    public ReLangReadArgumentNode(int index) {
        this(index, null);
    }

    public ReLangReadArgumentNode(int index, ReLangNode defaultValueNode) {
        this.index = index;
        this.defaultValueNode = defaultValueNode;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        if (index < args.length && args[index] != null) {
            return args[index];
        } else if (defaultValueNode != null) {
            return defaultValueNode.executeGeneric(frame);
        } else {
            return 0L; // Default value if missing
        }
    }
}
