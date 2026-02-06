package com.relang.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.NodeInfo;

import java.util.LinkedHashMap;

/**
 * AST node for constructing a record value: {@code TypeName { field1: expr1, field2: expr2 }}.
 */
@NodeInfo(shortName = "construct", description = "Constructs a record value")
public final class ReLangConstructNode extends ReLangNode {

    private final String typeName;
    private final String[] fieldNames;
    @Children private final ReLangNode[] fieldValues;

    public ReLangConstructNode(String typeName, String[] fieldNames, ReLangNode[] fieldValues) {
        this.typeName = typeName;
        this.fieldNames = fieldNames;
        this.fieldValues = fieldValues;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        var fields = new LinkedHashMap<String, Object>();
        for (int i = 0; i < fieldNames.length; i++) {
            fields.put(fieldNames[i], fieldValues[i].executeGeneric(frame));
        }
        return new ReLangRecord(typeName, fields);
    }
}
