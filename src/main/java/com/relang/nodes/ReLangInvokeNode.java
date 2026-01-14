package com.relang.nodes;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.relang.ReLangContext;
import com.relang.ReLang;

@NodeInfo(shortName = "invoke", description = "The node implementing a function call")
public final class ReLangInvokeNode extends ReLangNode {

    @Child
    private DirectCallNode callNode;
    @Children
    private final ReLangNode[] argumentNodes;
    private final String functionName;

    public ReLangInvokeNode(String functionName, ReLangNode[] argumentNodes) {
        this.functionName = functionName;
        this.argumentNodes = argumentNodes;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        if (callNode == null) {
            CompilerAsserts.neverPartOfCompilation("CallNode lookup should happen only once in interpreter");
            // Lookup function
            ReLangContext context = ReLangContext.get(this);
            CallTarget target = context.getFunctionRegistry().get(functionName);
            if (target == null) {
                throw new RuntimeException("Function not found: " + functionName);
            }
            callNode = insert(Truffle.getRuntime().createDirectCallNode(target));
        }

        Object[] args = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            args[i] = argumentNodes[i].executeGeneric(frame);
        }

        return callNode.call(args);
    }
}
