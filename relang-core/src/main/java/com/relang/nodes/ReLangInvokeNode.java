package com.relang.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.relang.FunctionDescriptor;
import com.relang.ReLangContext;

import java.util.List;

/**
 * Function call node.
 * Supports positional arguments, named arguments, and mixed calls.
 * With exception-based suspension, ReLangSuspendException propagates naturally through the call.
 */
@NodeInfo(shortName = "invoke", description = "The node implementing a function call")
public final class ReLangInvokeNode extends ReLangNode {

    @Child private DirectCallNode callNode;
    @Children private final ReLangNode[] argumentNodes;
    private final String functionName;
    private final String[] argumentNames; // null entry means positional
    private FunctionDescriptor descriptor;

    public ReLangInvokeNode(String functionName, ReLangNode[] argumentNodes, String[] argumentNames) {
        this.functionName = functionName;
        this.argumentNodes = argumentNodes;
        this.argumentNames = argumentNames;
    }

    // Backward-compatible constructor (all positional)
    public ReLangInvokeNode(String functionName, ReLangNode[] argumentNodes) {
        this(functionName, argumentNodes, new String[argumentNodes.length]);
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.CallTag.class || tag == StandardTags.ExpressionTag.class;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        if (callNode == null) {
            CompilerAsserts.neverPartOfCompilation("CallNode lookup should happen only once in interpreter");
            ReLangContext context = ReLangContext.get(this);
            descriptor = context.getFunctionRegistry().get(functionName);
            if (descriptor == null) {
                throw new ReLangTypeError(this, RuntimeDiagnostics.unknownFunction(functionName));
            }
            callNode = insert(Truffle.getRuntime().createDirectCallNode(descriptor.callTarget()));
        }

        // Check if any named arguments exist
        boolean hasNamedArgs = false;
        for (var name : argumentNames) {
            if (name != null) {
                hasNamedArgs = true;
                break;
            }
        }

        Object[] args;

        if (!hasNamedArgs) {
            // All positional - simple path
            args = new Object[argumentNodes.length];
            for (int i = 0; i < argumentNodes.length; i++) {
                args[i] = argumentNodes[i].executeGeneric(frame);
            }
        } else {
            // Named argument resolution
            List<String> paramNames = descriptor.parameterNames();
            args = new Object[paramNames.size()];

            // First, fill positional args in order
            int positionalIndex = 0;
            for (int i = 0; i < argumentNodes.length; i++) {
                if (argumentNames[i] == null) {
                    args[positionalIndex] = argumentNodes[i].executeGeneric(frame);
                    positionalIndex++;
                }
            }

            // Then, fill named args by matching parameter name
            for (int i = 0; i < argumentNodes.length; i++) {
                if (argumentNames[i] != null) {
                    int paramIndex = paramNames.indexOf(argumentNames[i]);
                    if (paramIndex < 0) {
                        throw new ReLangTypeError(this, RuntimeDiagnostics.unknownNamedArgument(functionName, argumentNames[i]));
                    }
                    args[paramIndex] = argumentNodes[i].executeGeneric(frame);
                }
            }
        }

        // Call function - ReLangSuspendException propagates naturally
        return callNode.call(args);
    }
}
