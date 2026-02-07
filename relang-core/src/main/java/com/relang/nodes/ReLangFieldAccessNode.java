package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

/**
 * AST node for field access on a record value: {@code expr.fieldName}.
 */
@NodeInfo(shortName = "fieldAccess", description = "Accesses a field on a record value")
public final class ReLangFieldAccessNode extends ReLangNode {

    @Child private ReLangNode receiver;
    private final String fieldName;

    public ReLangFieldAccessNode(ReLangNode receiver, String fieldName) {
        this.receiver = receiver;
        this.fieldName = fieldName;
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        var obj = receiver.executeGeneric(frame);
        if (obj instanceof ReLangRecord record) {
            return record.getField(fieldName);
        }
        throw new ReLangTypeError(this, RuntimeDiagnostics.invalidFieldAccess(fieldName, obj));
    }
}
