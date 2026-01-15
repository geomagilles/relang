package com.relang.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.relang.ReLangContext;

/**
 * Function call node.
 * With exception-based suspension, we don't need any special handling here -
 * ReLangSuspendException propagates naturally through the call.
 */
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
            ReLangContext context = ReLangContext.get(this);
            var target = context.getFunctionRegistry().get(functionName);
            if (target == null) {
                throw new RuntimeException("Function not found: " + functionName);
            }
            callNode = insert(Truffle.getRuntime().createDirectCallNode(target));
        }

        // Evaluate arguments - exceptions propagate naturally
        Object[] args = new Object[argumentNodes.length];
        for (int i = 0; i < argumentNodes.length; i++) {
            args[i] = argumentNodes[i].executeGeneric(frame);
        }

        // Call function - ReLangSuspendException propagates naturally
        return callNode.call(args);
    }
}
