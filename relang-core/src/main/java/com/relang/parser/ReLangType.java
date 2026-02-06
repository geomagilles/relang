package com.relang.parser;

/**
 * Sealed type hierarchy for the ReLang static type checker.
 * Each concrete type is a record with a singleton INSTANCE for parameterless types.
 */
public sealed interface ReLangType
        permits ReLangType.IntType, ReLangType.FloatType, ReLangType.BoolType,
                ReLangType.StringType, ReLangType.UnitType, ReLangType.NoneType,
                ReLangType.OptionalType, ReLangType.AwaitableType, ReLangType.UnknownType {

    /** Human-readable name: "Int", "Float?", "Awaitable<Int>", etc. */
    String displayName();

    /** True for IntType or FloatType. */
    default boolean isNumeric() {
        return this instanceof IntType || this instanceof FloatType;
    }

    /**
     * Can a value of {@code this} type be assigned where {@code target} is expected?
     * <ul>
     *   <li>Same concrete type -> true</li>
     *   <li>NoneType -> OptionalType(T) -> true</li>
     *   <li>T -> OptionalType(T) -> true</li>
     *   <li>UnknownType on either side -> true</li>
     *   <li>Otherwise -> false</li>
     * </ul>
     */
    default boolean isAssignableTo(ReLangType target) {
        if (this.equals(target)) return true;
        if (this instanceof UnknownType || target instanceof UnknownType) return true;
        // NoneType is assignable to any type (ReLang allows none in any variable)
        if (this instanceof NoneType) return true;
        if (target instanceof OptionalType opt) {
            return this.isAssignableTo(opt.inner());
        }
        return false;
    }

    /** Unwrap OptionalType to its inner type; otherwise return self. */
    default ReLangType unwrapOptional() {
        if (this instanceof OptionalType opt) return opt.inner();
        return this;
    }

    /**
     * Map a typeRef parse-tree context to a ReLangType.
     * "Int" -> IntType, "Float" -> FloatType, etc.
     * Suffix "?" wraps in OptionalType. Unknown names -> UnknownType.
     */
    static ReLangType fromTypeRef(ReLangParser.TypeRefContext ctx) {
        if (ctx == null) return UnknownType.INSTANCE;
        var name = ctx.ID().getText();
        boolean optional = ctx.getText().endsWith("?");
        var base = switch (name) {
            case "Int" -> IntType.INSTANCE;
            case "Float" -> FloatType.INSTANCE;
            case "Bool" -> BoolType.INSTANCE;
            case "String" -> StringType.INSTANCE;
            case "Unit" -> UnitType.INSTANCE;
            case "None" -> NoneType.INSTANCE;
            default -> UnknownType.INSTANCE;
        };
        return optional ? new OptionalType(base) : base;
    }

    // --- Concrete types ---

    record IntType() implements ReLangType {
        static final IntType INSTANCE = new IntType();
        @Override public String displayName() { return "Int"; }
    }

    record FloatType() implements ReLangType {
        static final FloatType INSTANCE = new FloatType();
        @Override public String displayName() { return "Float"; }
    }

    record BoolType() implements ReLangType {
        static final BoolType INSTANCE = new BoolType();
        @Override public String displayName() { return "Bool"; }
    }

    record StringType() implements ReLangType {
        static final StringType INSTANCE = new StringType();
        @Override public String displayName() { return "String"; }
    }

    record UnitType() implements ReLangType {
        static final UnitType INSTANCE = new UnitType();
        @Override public String displayName() { return "Unit"; }
    }

    record NoneType() implements ReLangType {
        static final NoneType INSTANCE = new NoneType();
        @Override public String displayName() { return "None"; }
    }

    record OptionalType(ReLangType inner) implements ReLangType {
        @Override public String displayName() { return inner.displayName() + "?"; }
    }

    record AwaitableType(ReLangType inner) implements ReLangType {
        @Override public String displayName() { return "Awaitable<" + inner.displayName() + ">"; }
    }

    record UnknownType() implements ReLangType {
        static final UnknownType INSTANCE = new UnknownType();
        @Override public String displayName() { return "Unknown"; }
    }
}
