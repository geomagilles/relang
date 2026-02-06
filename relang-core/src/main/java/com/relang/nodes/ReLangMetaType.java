package com.relang.nodes;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

/**
 * Meta type object for the Truffle interop metaobject protocol.
 * Allows IDEs to show type information on hover via hasMetaObject/getMetaObject.
 */
@ExportLibrary(InteropLibrary.class)
public final class ReLangMetaType implements TruffleObject {

    public static final ReLangMetaType INT = new ReLangMetaType("Int");
    public static final ReLangMetaType FLOAT = new ReLangMetaType("Float");
    public static final ReLangMetaType BOOL = new ReLangMetaType("Bool");
    public static final ReLangMetaType STRING = new ReLangMetaType("String");
    public static final ReLangMetaType UNIT = new ReLangMetaType("Unit");
    public static final ReLangMetaType NONE = new ReLangMetaType("None");
    public static final ReLangMetaType BYTES = new ReLangMetaType("Bytes");
    public static final ReLangMetaType DURATION = new ReLangMetaType("Duration");
    public static final ReLangMetaType TIMESTAMP = new ReLangMetaType("Timestamp");
    public static final ReLangMetaType JSON = new ReLangMetaType("Json");
    public static final ReLangMetaType FAILURE = new ReLangMetaType("Failure");
    public static final ReLangMetaType PRODUCT = new ReLangMetaType("Product");

    private final String name;

    public ReLangMetaType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @ExportMessage
    boolean isMetaObject() {
        return true;
    }

    @ExportMessage
    Object getMetaSimpleName() {
        return name;
    }

    @ExportMessage
    Object getMetaQualifiedName() {
        return name;
    }

    @ExportMessage
    boolean isMetaInstance(Object instance) {
        return switch (name) {
            case "Int" -> instance instanceof Long;
            case "Float" -> instance instanceof Double;
            case "Bool" -> instance instanceof Boolean;
            case "String" -> instance instanceof String;
            case "Unit" -> instance instanceof ReLangUnit;
            case "None" -> instance instanceof ReLangNone;
            case "Bytes" -> instance instanceof ReLangBytes;
            case "Duration" -> instance instanceof ReLangDuration;
            case "Timestamp" -> instance instanceof ReLangTimestamp;
            case "Json" -> instance instanceof ReLangJson;
            case "Failure" -> instance instanceof ReLangFailure;
            case "Product" -> instance instanceof ReLangProduct;
            default -> instance instanceof ReLangRecord r && r.getTypeName().equals(name);
        };
    }

    @ExportMessage
    Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        return name;
    }
}
