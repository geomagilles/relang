package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "readArg", description = "The node for reading a function argument")
public class ReLangReadArgumentNode extends ReLangNode {

    private final int index;

    public ReLangReadArgumentNode(int index) {
        this.index = index;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        Object[] args = frame.getArguments();
        if (index < args.length) {
            return args[index];
        } else {
            return 0L; // Default value if missing
        }
    }
}
