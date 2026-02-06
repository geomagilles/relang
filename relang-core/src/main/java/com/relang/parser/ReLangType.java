package com.relang.parser;

/**
 * Sealed type hierarchy for the ReLang static type checker.
 * Each concrete type is a record with a singleton INSTANCE for parameterless types.
 */
public sealed interface ReLangType
        permits ReLangType.IntType, ReLangType.FloatType, ReLangType.BoolType,
                ReLangType.StringType, ReLangType.UnitType, ReLangType.NoneType,
                ReLangType.BytesType, ReLangType.DurationType, ReLangType.TimestampType,
                ReLangType.JsonType, ReLangType.FailureType,
                ReLangType.OptionalType, ReLangType.AwaitableType, ReLangType.UserType,
                ReLangType.ProductType, ReLangType.UnionType,
                ReLangType.UnknownType {

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
        // Implicit widening: Int is assignable to Float
        if (this instanceof IntType && target instanceof FloatType) return true;
        if (target instanceof OptionalType opt) {
            return this.isAssignableTo(opt.inner());
        }
        // T is assignable to T | U (union contains T)
        if (target instanceof UnionType union) {
            return union.alternatives().stream().anyMatch(this::isAssignableTo);
        }
        // Product A & B is assignable to Product A & B if components match pairwise
        if (this instanceof ProductType thisProd && target instanceof ProductType targetProd) {
            if (thisProd.components().size() != targetProd.components().size()) return false;
            for (int i = 0; i < thisProd.components().size(); i++) {
                if (!thisProd.components().get(i).isAssignableTo(targetProd.components().get(i))) return false;
            }
            return true;
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
        return switch (ctx) {
            case ReLangParser.TypeRefSimpleContext simple -> fromTypeRefAtom(simple.typeRefAtom());
            case ReLangParser.TypeRefProductContext product -> {
                var components = new java.util.ArrayList<ReLangType>();
                for (var atom : product.typeRefAtom()) {
                    components.add(fromTypeRefAtom(atom));
                }
                yield new ProductType(components);
            }
            case ReLangParser.TypeRefUnionContext union -> {
                var alternatives = new java.util.ArrayList<ReLangType>();
                for (var atom : union.typeRefAtom()) {
                    alternatives.add(fromTypeRefAtom(atom));
                }
                yield new UnionType(alternatives);
            }
            default -> UnknownType.INSTANCE;
        };
    }

    static ReLangType fromTypeRefAtom(ReLangParser.TypeRefAtomContext ctx) {
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
            case "Bytes" -> BytesType.INSTANCE;
            case "Duration" -> DurationType.INSTANCE;
            case "Timestamp" -> TimestampType.INSTANCE;
            case "Json" -> JsonType.INSTANCE;
            case "Failure" -> FailureType.INSTANCE;
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

    record BytesType() implements ReLangType {
        static final BytesType INSTANCE = new BytesType();
        @Override public String displayName() { return "Bytes"; }
    }

    record DurationType() implements ReLangType {
        static final DurationType INSTANCE = new DurationType();
        @Override public String displayName() { return "Duration"; }
    }

    record TimestampType() implements ReLangType {
        static final TimestampType INSTANCE = new TimestampType();
        @Override public String displayName() { return "Timestamp"; }
    }

    record JsonType() implements ReLangType {
        static final JsonType INSTANCE = new JsonType();
        @Override public String displayName() { return "Json"; }
    }

    record FailureType() implements ReLangType {
        static final FailureType INSTANCE = new FailureType();
        @Override public String displayName() { return "Failure"; }
    }

    record OptionalType(ReLangType inner) implements ReLangType {
        @Override public String displayName() { return inner.displayName() + "?"; }
    }

    record AwaitableType(ReLangType inner) implements ReLangType {
        @Override public String displayName() { return "Awaitable<" + inner.displayName() + ">"; }
    }

    record UserType(String name) implements ReLangType {
        @Override public String displayName() { return name; }
    }

    record ProductType(java.util.List<ReLangType> components) implements ReLangType {
        @Override public String displayName() {
            return components.stream().map(ReLangType::displayName).collect(java.util.stream.Collectors.joining(" & "));
        }
    }

    record UnionType(java.util.List<ReLangType> alternatives) implements ReLangType {
        @Override public String displayName() {
            return alternatives.stream().map(ReLangType::displayName).collect(java.util.stream.Collectors.joining(" | "));
        }
    }

    record UnknownType() implements ReLangType {
        static final UnknownType INSTANCE = new UnknownType();
        @Override public String displayName() { return "Unknown"; }
    }
}
