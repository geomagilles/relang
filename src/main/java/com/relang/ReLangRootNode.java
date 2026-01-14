package com.relang;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.nodes.RootNode;
import com.relang.nodes.ReLangNode;

public class ReLangRootNode extends RootNode {

    @Child
    private ReLangNode bodyNode;

    public ReLangRootNode(ReLang language, FrameDescriptor frameDescriptor, ReLangNode bodyNode) {
        super(language, frameDescriptor);
        this.bodyNode = bodyNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return bodyNode.executeGeneric(frame);
        } catch (com.relang.nodes.ReLangReturnException e) {
            return e.getResult();
        }
    }
}
