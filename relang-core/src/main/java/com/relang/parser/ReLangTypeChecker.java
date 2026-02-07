package com.relang.parser;

import com.relang.diagnostics.DiagnosticCode;
import com.relang.diagnostics.DiagnosticNote;
import com.relang.diagnostics.SourceRange;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Static type checker for ReLang, implemented as an ANTLR visitor.
 * <p>
 * Like Kotlin, every expression must have a concrete type. Untyped code is rejected:
 * <ul>
 *   <li>All function parameters must have type annotations</li>
 *   <li>Return types may be omitted for non-recursive functions (inferred from body)</li>
 *   <li>Recursive functions without explicit return types are rejected</li>
 *   <li>Variables must be declared with {@code let} before use</li>
 * </ul>
 * <p>
 * Four-pass approach:
 * <ol>
 *   <li>Register builtins + collect all function signatures (return types may be null if omitted)</li>
 *   <li>Infer return types for functions without explicit annotation</li>
 *   <li>Check all function bodies (with all return types now known)</li>
 *   <li>Check top-level commands</li>
 * </ol>
 * <p>
 * UnknownType is used ONLY for error recovery: when an error has already been reported,
 * UnknownType prevents cascading errors. Valid code never produces UnknownType.
 */
public class ReLangTypeChecker extends ReLangBaseVisitor<ReLangType> {

    private final List<TypeError> errors = new ArrayList<>();
    private final Map<String, FunctionSignature> functionSignatures = new HashMap<>();
    private Scope currentScope = new Scope(null);
    private ReLangType currentFunctionReturnType = null;
    private boolean insideLoop = false;

    /** Tracks which functions are currently being inferred, to detect recursion. */
    private final Set<String> inferring = new HashSet<>();
    /** During return type inference, tracks the type seen in explicit return statements. */
    private ReLangType inferredReturnFromStatements = null;

    /** Registry of user-declared types (both sealed markers and concrete record types). */
    private final Map<String, UserTypeInfo> userTypes = new HashMap<>();

    record UserTypeInfo(String name, String sealedParent, Map<String, ReLangType> fields, SourceRange declarationRange) {}

    // ---- Inner classes ----

    private static class Scope {
        private final Scope parent;
        private final Map<String, ReLangType> variables = new HashMap<>();
        private final Map<String, SourceRange> declarations = new HashMap<>();
        private final Set<String> parameters = new HashSet<>();

        Scope(Scope parent) {
            this.parent = parent;
        }

        void define(String name, ReLangType type, SourceRange declarationRange) {
            variables.put(name, type);
            declarations.put(name, declarationRange);
        }

        void defineParameter(String name, ReLangType type, SourceRange declarationRange) {
            define(name, type, declarationRange);
            parameters.add(name);
        }

        ReLangType lookup(String name) {
            if (variables.containsKey(name)) return variables.get(name);
            if (parent != null) return parent.lookup(name);
            return null;
        }

        boolean isDefined(String name) {
            return lookup(name) != null;
        }

        SourceRange lookupDeclaration(String name) {
            if (declarations.containsKey(name)) return declarations.get(name);
            if (parent != null) return parent.lookupDeclaration(name);
            return null;
        }

        boolean isParameter(String name) {
            if (parameters.contains(name)) return true;
            if (parent != null) return parent.isParameter(name);
            return false;
        }

        /** Update an existing variable (walks up scope chain). Returns false if not found. */
        boolean assign(String name, ReLangType type) {
            if (variables.containsKey(name)) {
                variables.put(name, type);
                return true;
            }
            if (parent != null) return parent.assign(name, type);
            return false;
        }
    }

    record FunctionSignature(
            String name,
            List<ParamSignature> params,
            int requiredCount,
            ReLangType returnType,
            SourceRange declarationRange
    ) {
        /** Return a new signature with the given return type. */
        FunctionSignature withReturnType(ReLangType rt) {
            return new FunctionSignature(name, params, requiredCount, rt, declarationRange);
        }
    }

    record ParamSignature(String name, ReLangType type, SourceRange declarationRange) {}

    // ---- Public API ----

    /**
     * Run the type checker on a parsed source tree.
     * @return list of type errors (empty if code is well-typed)
     */
    public List<TypeError> check(ReLangParser.SourceContext tree) {
        // Pass 0: collect user type declarations
        for (var typeDecl : tree.typeDecl()) {
            collectTypeDeclaration(typeDecl);
        }

        // Pass 1: register builtins and collect function signatures
        registerBuiltins();
        for (var func : tree.function()) {
            collectFunctionSignature(func);
        }

        // Pass 1.5: infer return types for functions without explicit annotation
        for (var func : tree.function()) {
            inferReturnTypeIfNeeded(func);
        }

        // Pass 2: check function bodies
        for (var func : tree.function()) {
            checkFunctionBody(func);
        }

        // Pass 3: check top-level commands
        var topScope = new Scope(null);
        currentScope = topScope;
        currentFunctionReturnType = null;
        for (var cmd : tree.command()) {
            visit(cmd.statement());
        }

        return List.copyOf(errors);
    }

    // ---- Pass 1: Signature collection ----

    private void registerBuiltins() {
        // resolved(value) -> Awaitable<Unknown>
        functionSignatures.put("resolved", new FunctionSignature(
                "resolved",
                List.of(new ParamSignature("value", ReLangType.UnknownType.INSTANCE, null)),
                1,
                new ReLangType.AwaitableType(ReLangType.UnknownType.INSTANCE),
                null
        ));
        // pending() -> Awaitable<Unknown>
        functionSignatures.put("pending", new FunctionSignature(
                "pending",
                List.of(),
                0,
                new ReLangType.AwaitableType(ReLangType.UnknownType.INSTANCE),
                null
        ));
        // now() -> Timestamp
        functionSignatures.put("now", new FunctionSignature(
                "now",
                List.of(),
                0,
                ReLangType.TimestampType.INSTANCE,
                null
        ));
        // Failure(message: String) -> Failure
        functionSignatures.put("Failure", new FunctionSignature(
                "Failure",
                List.of(new ParamSignature("message", ReLangType.StringType.INSTANCE, null)),
                1,
                ReLangType.FailureType.INSTANCE,
                null
        ));
        // json(text: String) -> Json
        functionSignatures.put("json", new FunctionSignature(
                "json",
                List.of(new ParamSignature("text", ReLangType.StringType.INSTANCE, null)),
                1,
                ReLangType.JsonType.INSTANCE,
                null
        ));
    }

    private void collectTypeDeclaration(ReLangParser.TypeDeclContext ctx) {
        switch (ctx) {
            case ReLangParser.DeclSealedContext sd -> {
                var name = sd.ID().getText();
                userTypes.put(name, new UserTypeInfo(name, null, Map.of(), sourceRangeFromToken(sd.ID().getSymbol(), sd.ID().getText())));
            }
            case ReLangParser.DeclTypeContext td -> {
                var name = td.ID(0).getText();
                String parent = td.ID().size() > 1 ? td.ID(1).getText() : null;
                var fields = new LinkedHashMap<String, ReLangType>();
                for (var field : td.fieldDecl()) {
                    var fieldName = field.ID().getText();
                    var fieldType = resolveType(field.typeRef());
                    fields.put(fieldName, fieldType);
                }
                userTypes.put(name, new UserTypeInfo(name, parent, fields, sourceRangeFromToken(td.ID(0).getSymbol(), td.ID(0).getText())));
            }
            default -> {}
        }
    }

    /**
     * Resolve a typeRef to a ReLangType, including user-defined types.
     * Replaces direct calls to {@code ReLangType.fromTypeRef()} in this checker.
     */
    private ReLangType resolveType(ReLangParser.TypeRefContext ctx) {
        if (ctx == null) return ReLangType.UnknownType.INSTANCE;
        return switch (ctx) {
            case ReLangParser.TypeRefSimpleContext simple -> resolveTypeAtom(simple.typeRefAtom());
            case ReLangParser.TypeRefProductContext product -> {
                var components = new ArrayList<ReLangType>();
                for (var atom : product.typeRefAtom()) {
                    components.add(resolveTypeAtom(atom));
                }
                yield new ReLangType.ProductType(components);
            }
            case ReLangParser.TypeRefUnionContext union -> {
                var alternatives = new ArrayList<ReLangType>();
                for (var atom : union.typeRefAtom()) {
                    alternatives.add(resolveTypeAtom(atom));
                }
                yield new ReLangType.UnionType(alternatives);
            }
            default -> ReLangType.UnknownType.INSTANCE;
        };
    }

    private ReLangType resolveTypeAtom(ReLangParser.TypeRefAtomContext ctx) {
        if (ctx == null) return ReLangType.UnknownType.INSTANCE;
        var name = ctx.ID().getText();
        boolean optional = ctx.getText().endsWith("?");
        var base = switch (name) {
            case "Int" -> ReLangType.IntType.INSTANCE;
            case "Float" -> ReLangType.FloatType.INSTANCE;
            case "Bool" -> ReLangType.BoolType.INSTANCE;
            case "String" -> ReLangType.StringType.INSTANCE;
            case "Unit" -> ReLangType.UnitType.INSTANCE;
            case "None" -> ReLangType.NoneType.INSTANCE;
            case "Bytes" -> ReLangType.BytesType.INSTANCE;
            case "Duration" -> ReLangType.DurationType.INSTANCE;
            case "Timestamp" -> ReLangType.TimestampType.INSTANCE;
            case "Json" -> ReLangType.JsonType.INSTANCE;
            case "Failure" -> ReLangType.FailureType.INSTANCE;
            default -> {
                if (userTypes.containsKey(name)) {
                    yield new ReLangType.UserType(name);
                }
                yield ReLangType.UnknownType.INSTANCE;
            }
        };
        return optional ? new ReLangType.OptionalType(base) : base;
    }

    private void collectFunctionSignature(ReLangParser.FunctionContext func) {
        switch (func) {
            case ReLangParser.FunctionBlockContext fb -> {
                var name = fb.ID().getText();
                var params = collectParams(fb.typedParameters(), fb);
                int requiredCount = countRequired(fb.typedParameters());
                var returnType = fb.typeRef() != null
                        ? resolveType(fb.typeRef())
                        : null; // null = needs inference
                functionSignatures.put(name, new FunctionSignature(
                        name,
                        params,
                        requiredCount,
                        returnType,
                        sourceRangeFromToken(fb.ID().getSymbol(), fb.ID().getText())
                ));
            }
            case ReLangParser.FunctionExprContext fe -> {
                var name = fe.ID().getText();
                var params = collectParams(fe.typedParameters(), fe);
                int requiredCount = countRequired(fe.typedParameters());
                var returnType = fe.typeRef() != null
                        ? resolveType(fe.typeRef())
                        : null; // null = needs inference
                functionSignatures.put(name, new FunctionSignature(
                        name,
                        params,
                        requiredCount,
                        returnType,
                        sourceRangeFromToken(fe.ID().getSymbol(), fe.ID().getText())
                ));
            }
            default -> {}
        }
    }

    private List<ParamSignature> collectParams(ReLangParser.TypedParametersContext ctx,
                                                org.antlr.v4.runtime.ParserRuleContext funcCtx) {
        if (ctx == null) return List.of();
        var result = new ArrayList<ParamSignature>();
        for (var p : ctx.typedParam()) {
            var name = p.ID().getText();
            if (p.typeRef() == null) {
                addError(p, TypeDiagnostics.missingTypeAnnotation(name));
                result.add(new ParamSignature(
                        name,
                        ReLangType.UnknownType.INSTANCE,
                        sourceRangeFromToken(p.ID().getSymbol(), p.ID().getText())
                ));
            } else {
                result.add(new ParamSignature(
                        name,
                        resolveType(p.typeRef()),
                        sourceRangeFromToken(p.ID().getSymbol(), p.ID().getText())
                ));
            }
        }
        return result;
    }

    private int countRequired(ReLangParser.TypedParametersContext ctx) {
        if (ctx == null) return 0;
        int count = 0;
        for (var p : ctx.typedParam()) {
            if (p.expr() == null) count++;
            else break; // once we see a default, all subsequent have defaults
        }
        return count;
    }

    // ---- Pass 1.5: Return type inference ----

    private void inferReturnTypeIfNeeded(ReLangParser.FunctionContext func) {
        switch (func) {
            case ReLangParser.FunctionBlockContext fb -> {
                var name = fb.ID().getText();
                var sig = functionSignatures.get(name);
                if (sig.returnType() == null) {
                    inferReturnType(name, sig, fb.block(), null);
                }
            }
            case ReLangParser.FunctionExprContext fe -> {
                var name = fe.ID().getText();
                var sig = functionSignatures.get(name);
                if (sig.returnType() == null) {
                    inferReturnType(name, sig, null, fe.expr());
                }
            }
            default -> {}
        }
    }

    private void inferReturnType(String name, FunctionSignature sig,
                                  ReLangParser.BlockContext blockCtx,
                                  ReLangParser.ExprContext exprCtx) {
        // Mark as currently inferring to detect recursion
        inferring.add(name);

        // Set up function scope with param types
        var funcScope = new Scope(null);
        for (var p : sig.params()) {
            funcScope.defineParameter(p.name(), p.type(), p.declarationRange());
        }
        var savedScope = currentScope;
        var savedReturn = currentFunctionReturnType;
        var savedInferred = inferredReturnFromStatements;
        currentScope = funcScope;
        // During inference, we don't know the return type yet
        currentFunctionReturnType = null;
        inferredReturnFromStatements = null;

        ReLangType bodyType;
        if (exprCtx != null) {
            bodyType = visit(exprCtx);
        } else {
            bodyType = visitBlock(blockCtx);
        }

        // If body type is Unit but we saw explicit return statements, use the return type
        if (bodyType instanceof ReLangType.UnitType && inferredReturnFromStatements != null) {
            bodyType = inferredReturnFromStatements;
        }

        currentScope = savedScope;
        currentFunctionReturnType = savedReturn;
        inferredReturnFromStatements = savedInferred;
        inferring.remove(name);

        // Store the inferred return type
        functionSignatures.put(name, sig.withReturnType(bodyType));
    }

    // ---- Pass 2: Function body checking ----

    private void checkFunctionBody(ReLangParser.FunctionContext func) {
        switch (func) {
            case ReLangParser.FunctionBlockContext fb -> {
                var sig = functionSignatures.get(fb.ID().getText());
                var funcScope = new Scope(null);
                for (var p : sig.params()) {
                    funcScope.defineParameter(p.name(), p.type(), p.declarationRange());
                }
                var savedScope = currentScope;
                var savedReturn = currentFunctionReturnType;
                currentScope = funcScope;
                currentFunctionReturnType = sig.returnType();
                visitBlock(fb.block());
                currentScope = savedScope;
                currentFunctionReturnType = savedReturn;
            }
            case ReLangParser.FunctionExprContext fe -> {
                var sig = functionSignatures.get(fe.ID().getText());
                var funcScope = new Scope(null);
                for (var p : sig.params()) {
                    funcScope.defineParameter(p.name(), p.type(), p.declarationRange());
                }
                var savedScope = currentScope;
                var savedReturn = currentFunctionReturnType;
                currentScope = funcScope;
                currentFunctionReturnType = sig.returnType();
                var bodyType = visit(fe.expr());
                checkReturnType(bodyType, sig.returnType(), fe.expr());
                currentScope = savedScope;
                currentFunctionReturnType = savedReturn;
            }
            default -> {}
        }
    }

    private void checkReturnType(ReLangType actual, ReLangType expected, org.antlr.v4.runtime.ParserRuleContext ctx) {
        if (expected instanceof ReLangType.UnknownType || actual instanceof ReLangType.UnknownType) return;
        if (expected == null) return; // should not happen after inference, but guard
        if (!actual.isAssignableTo(expected)) {
            addError(ctx, TypeDiagnostics.returnTypeMismatch(expected, actual));
        }
    }

    // ---- Visitor overrides: Literals ----

    @Override
    public ReLangType visitExprInt(ReLangParser.ExprIntContext ctx) {
        return ReLangType.IntType.INSTANCE;
    }

    @Override
    public ReLangType visitExprFloat(ReLangParser.ExprFloatContext ctx) {
        return ReLangType.FloatType.INSTANCE;
    }

    @Override
    public ReLangType visitExprTrue(ReLangParser.ExprTrueContext ctx) {
        return ReLangType.BoolType.INSTANCE;
    }

    @Override
    public ReLangType visitExprFalse(ReLangParser.ExprFalseContext ctx) {
        return ReLangType.BoolType.INSTANCE;
    }

    @Override
    public ReLangType visitExprString(ReLangParser.ExprStringContext ctx) {
        // Type-check any interpolated expressions inside the string
        var raw = ctx.STRING().getText();
        var inner = raw.substring(1, raw.length() - 1);
        checkInterpolations(inner, ctx);
        return ReLangType.StringType.INSTANCE;
    }

    @Override
    public ReLangType visitExprNone(ReLangParser.ExprNoneContext ctx) {
        return ReLangType.NoneType.INSTANCE;
    }

    @Override
    public ReLangType visitExprUnit(ReLangParser.ExprUnitContext ctx) {
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitExprDuration(ReLangParser.ExprDurationContext ctx) {
        return ReLangType.DurationType.INSTANCE;
    }

    @Override
    public ReLangType visitExprBytes(ReLangParser.ExprBytesContext ctx) {
        return ReLangType.BytesType.INSTANCE;
    }

    // ---- Visitor overrides: Unary ----

    @Override
    public ReLangType visitExprNegate(ReLangParser.ExprNegateContext ctx) {
        var operand = visit(ctx.expr());
        if (operand instanceof ReLangType.UnknownType) return ReLangType.UnknownType.INSTANCE;
        if (operand instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        if (operand instanceof ReLangType.FloatType) return ReLangType.FloatType.INSTANCE;
        addError(ctx, TypeDiagnostics.invalidNegationOperand(operand));
        return ReLangType.UnknownType.INSTANCE;
    }

    @Override
    public ReLangType visitExprNot(ReLangParser.ExprNotContext ctx) {
        var operand = visit(ctx.expr());
        if (operand instanceof ReLangType.UnknownType) return ReLangType.UnknownType.INSTANCE;
        if (!(operand instanceof ReLangType.BoolType)) {
            addError(ctx, TypeDiagnostics.invalidNotOperand(operand));
        }
        return ReLangType.BoolType.INSTANCE;
    }

    // ---- Visitor overrides: Binary ----

    @Override
    public ReLangType visitExprBinary(ReLangParser.ExprBinaryContext ctx) {
        var leftType = visit(ctx.left);
        var rightType = visit(ctx.right);

        // UnknownType propagation
        if (leftType instanceof ReLangType.UnknownType || rightType instanceof ReLangType.UnknownType) {
            return ReLangType.UnknownType.INSTANCE;
        }

        var op = ctx.op.getText();
        return switch (op) {
            case "+" -> checkAdd(leftType, rightType, ctx);
            case "-", "*", "/" -> checkArithmetic(op, leftType, rightType, ctx);
            case "%" -> checkModulo(leftType, rightType, ctx);
            case "<", "<=", ">", ">=" -> checkOrdering(op, leftType, rightType, ctx);
            case "==", "!=" -> checkEquality(op, leftType, rightType, ctx);
            default -> {
                addError(ctx, TypeDiagnostics.unknownOperator(op));
                yield ReLangType.UnknownType.INSTANCE;
            }
        };
    }

    private ReLangType checkAdd(ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.FloatType.INSTANCE;
        if (left.isNumeric() && right.isNumeric()) return ReLangType.FloatType.INSTANCE;
        if (left instanceof ReLangType.StringType && right instanceof ReLangType.StringType) return ReLangType.StringType.INSTANCE;
        addError(ctx, TypeDiagnostics.invalidBinaryOperator("+", left, right, "Use two numeric operands or two String operands."));
        return ReLangType.UnknownType.INSTANCE;
    }

    private ReLangType checkArithmetic(String op, ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.FloatType.INSTANCE;
        if (left.isNumeric() && right.isNumeric()) return ReLangType.FloatType.INSTANCE;
        addError(ctx, TypeDiagnostics.invalidBinaryOperator(op, left, right, "Use numeric operands for `" + op + "`."));
        return ReLangType.UnknownType.INSTANCE;
    }

    private ReLangType checkModulo(ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        addError(ctx, TypeDiagnostics.invalidModuloOperands(left, right));
        return ReLangType.UnknownType.INSTANCE;
    }

    private ReLangType checkOrdering(String op, ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.BoolType.INSTANCE;
        if (left.isNumeric() && right.isNumeric()) return ReLangType.BoolType.INSTANCE;
        addError(ctx, TypeDiagnostics.invalidBinaryOperator(op, left, right, "Use numeric operands for ordering comparisons."));
        return ReLangType.BoolType.INSTANCE;
    }

    private ReLangType checkEquality(String op, ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.BoolType.INSTANCE;
        if (left.isNumeric() && right.isNumeric()) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.BoolType && right instanceof ReLangType.BoolType) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.StringType && right instanceof ReLangType.StringType) return ReLangType.BoolType.INSTANCE;
        // Structural equality for user-defined record types (same type name)
        if (left instanceof ReLangType.UserType utL && right instanceof ReLangType.UserType utR
                && utL.name().equals(utR.name())) return ReLangType.BoolType.INSTANCE;
        // Product equality
        if (left instanceof ReLangType.ProductType && right instanceof ReLangType.ProductType) return ReLangType.BoolType.INSTANCE;
        addError(ctx, TypeDiagnostics.invalidBinaryOperator(op, left, right, "Compare values with compatible types."));
        return ReLangType.BoolType.INSTANCE;
    }

    // ---- Visitor overrides: Logical ----

    @Override
    public ReLangType visitExprAnd(ReLangParser.ExprAndContext ctx) {
        var left = visit(ctx.left);
        var right = visit(ctx.right);
        if (left instanceof ReLangType.UnknownType || right instanceof ReLangType.UnknownType) {
            return ReLangType.UnknownType.INSTANCE;
        }
        if (!(left instanceof ReLangType.BoolType)) {
            addError(ctx, TypeDiagnostics.logicalOperandMustBeBool("and", left));
        }
        if (!(right instanceof ReLangType.BoolType)) {
            addError(ctx, TypeDiagnostics.logicalOperandMustBeBool("and", right));
        }
        return ReLangType.BoolType.INSTANCE;
    }

    @Override
    public ReLangType visitExprOr(ReLangParser.ExprOrContext ctx) {
        var left = visit(ctx.left);
        var right = visit(ctx.right);
        if (left instanceof ReLangType.UnknownType || right instanceof ReLangType.UnknownType) {
            return ReLangType.UnknownType.INSTANCE;
        }
        if (!(left instanceof ReLangType.BoolType)) {
            addError(ctx, TypeDiagnostics.logicalOperandMustBeBool("or", left));
        }
        if (!(right instanceof ReLangType.BoolType)) {
            addError(ctx, TypeDiagnostics.logicalOperandMustBeBool("or", right));
        }
        return ReLangType.BoolType.INSTANCE;
    }

    // ---- Visitor overrides: Product ----

    @Override
    public ReLangType visitExprProduct(ReLangParser.ExprProductContext ctx) {
        var leftType = visit(ctx.left);
        var rightType = visit(ctx.right);
        // Flatten nested products
        var components = new ArrayList<ReLangType>();
        if (leftType instanceof ReLangType.ProductType lp) {
            components.addAll(lp.components());
        } else {
            components.add(leftType);
        }
        if (rightType instanceof ReLangType.ProductType rp) {
            components.addAll(rp.components());
        } else {
            components.add(rightType);
        }
        return new ReLangType.ProductType(components);
    }

    // ---- Visitor overrides: Variables ----

    @Override
    public ReLangType visitExprId(ReLangParser.ExprIdContext ctx) {
        var name = ctx.ID().getText();
        var type = currentScope.lookup(name);
        if (type == null) {
            addError(ctx, TypeDiagnostics.undefinedVariable(name));
            return ReLangType.UnknownType.INSTANCE;
        }
        return type;
    }

    @Override
    public ReLangType visitExprParen(ReLangParser.ExprParenContext ctx) {
        return visit(ctx.expr());
    }

    // ---- Visitor overrides: Function call ----

    @Override
    public ReLangType visitExprCall(ReLangParser.ExprCallContext ctx) {
        var funcName = ctx.ID().getText();
        var sig = functionSignatures.get(funcName);
        if (sig == null) {
            addError(ctx, TypeDiagnostics.unknownFunction(funcName));
            return ReLangType.UnknownType.INSTANCE;
        }

        // Count args
        int argCount = 0;
        if (ctx.callArguments() != null) {
            argCount = ctx.callArguments().callArg().size();
        }

        // Check arity
        if (argCount < sig.requiredCount()) {
            addError(
                    ctx,
                    TypeDiagnostics.arityAtLeast(funcName, sig.requiredCount(), argCount),
                    declarationNote("Function declared here", sig.declarationRange())
            );
        } else if (argCount > sig.params().size()) {
            addError(
                    ctx,
                    TypeDiagnostics.arityAtMost(funcName, sig.params().size(), argCount),
                    declarationNote("Function declared here", sig.declarationRange())
            );
        }

        // Check arg types (positional only for now - named args don't change this logic much)
        if (ctx.callArguments() != null) {
            var callArgs = ctx.callArguments().callArg();
            for (int i = 0; i < callArgs.size() && i < sig.params().size(); i++) {
                var argExpr = switch (callArgs.get(i)) {
                    case ReLangParser.CallArgPositionalContext pos -> pos.expr();
                    case ReLangParser.CallArgNamedContext named -> named.expr();
                    default -> null;
                };
                if (argExpr != null) {
                    var argType = visit(argExpr);
                    var paramType = sig.params().get(i).type();
                    if (!(paramType instanceof ReLangType.UnknownType) && !(argType instanceof ReLangType.UnknownType)) {
                        if (!argType.isAssignableTo(paramType)) {
                            var parameterRange = sig.params().get(i).declarationRange();
                            addError(
                                    ctx,
                                    TypeDiagnostics.argumentTypeMismatch(funcName, i + 1, paramType, argType),
                                    declarationNote("Parameter declared here", parameterRange)
                            );
                        }
                    }
                }
            }
        }

        // During return type inference, if the called function's return type is still pending
        // (null) and it's currently being inferred, we have a recursive call needing explicit annotation
        var returnType = sig.returnType();
        if (returnType == null) {
            if (inferring.contains(funcName)) {
                addError(ctx, TypeDiagnostics.recursiveReturnTypeInference(funcName));
            }
            return ReLangType.UnknownType.INSTANCE;
        }
        return returnType;
    }

    // ---- Visitor overrides: Await ----

    @Override
    public ReLangType visitExprAwait(ReLangParser.ExprAwaitContext ctx) {
        var inner = visit(ctx.expr());
        if (inner instanceof ReLangType.AwaitableType awaitable) {
            return awaitable.inner();
        }
        if (inner instanceof ReLangType.UnknownType) {
            return ReLangType.UnknownType.INSTANCE;
        }
        addError(ctx, TypeDiagnostics.invalidAwaitOperand(inner));
        return ReLangType.UnknownType.INSTANCE;
    }

    // ---- Visitor overrides: Field access and construction ----

    @Override
    public ReLangType visitExprFieldAccess(ReLangParser.ExprFieldAccessContext ctx) {
        var receiverType = visit(ctx.expr());
        var fieldName = ctx.ID().getText();

        if (receiverType instanceof ReLangType.UserType ut) {
            var typeInfo = userTypes.get(ut.name());
            if (typeInfo != null && typeInfo.fields().containsKey(fieldName)) {
                return typeInfo.fields().get(fieldName);
            }
            addError(
                    ctx,
                    TypeDiagnostics.unknownFieldInType(fieldName, ut.name()),
                    declarationNote("Type declared here", typeInfo != null ? typeInfo.declarationRange() : null)
            );
            return ReLangType.UnknownType.INSTANCE;
        }
        if (receiverType instanceof ReLangType.UnknownType) return ReLangType.UnknownType.INSTANCE;

        addError(ctx, TypeDiagnostics.unknownFieldOnType(fieldName, receiverType));
        return ReLangType.UnknownType.INSTANCE;
    }

    @Override
    public ReLangType visitExprConstruct(ReLangParser.ExprConstructContext ctx) {
        var typeName = ctx.ID().getText();
        var typeInfo = userTypes.get(typeName);
        if (typeInfo == null) {
            addError(ctx, TypeDiagnostics.unknownType(typeName));
            return ReLangType.UnknownType.INSTANCE;
        }

        // Check provided fields
        var providedFields = new HashSet<String>();
        for (var fi : ctx.fieldInit()) {
            var fieldName = fi.ID().getText();
            providedFields.add(fieldName);
            var exprType = visit(fi.expr());

            var expectedType = typeInfo.fields().get(fieldName);
            if (expectedType == null) {
                addError(
                        fi,
                        TypeDiagnostics.unknownFieldInConstruction(fieldName, typeName),
                        declarationNote("Type declared here", typeInfo.declarationRange())
                );
            } else if (!(exprType instanceof ReLangType.UnknownType) && !exprType.isAssignableTo(expectedType)) {
                addError(
                        fi,
                        TypeDiagnostics.fieldTypeMismatch(fieldName, expectedType, exprType),
                        declarationNote("Type declared here", typeInfo.declarationRange())
                );
            }
        }

        // Check all required fields are provided
        for (var requiredField : typeInfo.fields().keySet()) {
            if (!providedFields.contains(requiredField)) {
                addError(
                        ctx,
                        TypeDiagnostics.missingField(requiredField, typeName),
                        declarationNote("Type declared here", typeInfo.declarationRange())
                );
            }
        }

        return new ReLangType.UserType(typeName);
    }

    // ---- Visitor overrides: Statements ----

    @Override
    public ReLangType visitStatementLet(ReLangParser.StatementLetContext ctx) {
        var rhsType = visit(ctx.expr());
        var varName = ctx.ID().getText();

        if (ctx.typeRef() != null) {
            var declaredType = resolveType(ctx.typeRef());
            if (!(declaredType instanceof ReLangType.UnknownType) && !(rhsType instanceof ReLangType.UnknownType)) {
                if (!rhsType.isAssignableTo(declaredType)) {
                    addError(ctx, TypeDiagnostics.declaredAssignmentMismatch(declaredType, rhsType));
                }
            }
            currentScope.define(varName, declaredType, sourceRangeFromToken(ctx.ID().getSymbol(), varName));
        } else {
            currentScope.define(varName, rhsType, sourceRangeFromToken(ctx.ID().getSymbol(), varName));
        }
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementAssignment(ReLangParser.StatementAssignmentContext ctx) {
        var assign = ctx.assignment();
        var varName = assign.ID().getText();
        var rhsType = visit(assign.expr());

        var existingType = currentScope.lookup(varName);
        if (existingType == null) {
            addError(ctx, TypeDiagnostics.undefinedVariable(varName));
        } else {
            var declarationRange = currentScope.lookupDeclaration(varName);
            // Check parameter immutability
            if (currentScope.isParameter(varName)) {
                addError(
                        ctx,
                        TypeDiagnostics.immutableParameterReassignment(varName),
                        declarationNote("Parameter declared here", declarationRange)
                );
            }
            // Check type compatibility
            if (!(existingType instanceof ReLangType.UnknownType) && !(rhsType instanceof ReLangType.UnknownType)) {
                if (!rhsType.isAssignableTo(existingType)) {
                    addError(
                            ctx,
                            TypeDiagnostics.assignmentMismatch(existingType, rhsType),
                            declarationNote("Variable declared here", declarationRange)
                    );
                }
            }
        }
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementIf(ReLangParser.StatementIfContext ctx) {
        var condType = visit(ctx.expr());
        checkBoolCondition(condType, ctx, "if");
        visitBlock(ctx.block(0));
        if (ctx.block().size() > 1) {
            visitBlock(ctx.block(1));
        }
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementIfNoParens(ReLangParser.StatementIfNoParensContext ctx) {
        var condType = visit(ctx.expr());
        checkBoolCondition(condType, ctx, "if");
        visitBlock(ctx.block(0));
        if (ctx.block().size() > 1) {
            visitBlock(ctx.block(1));
        }
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementWhile(ReLangParser.StatementWhileContext ctx) {
        var condType = visit(ctx.expr());
        checkBoolCondition(condType, ctx, "while");
        var savedInsideLoop = insideLoop;
        insideLoop = true;
        visitBlock(ctx.block());
        insideLoop = savedInsideLoop;
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementWhileNoParens(ReLangParser.StatementWhileNoParensContext ctx) {
        var condType = visit(ctx.expr());
        checkBoolCondition(condType, ctx, "while");
        var savedInsideLoop = insideLoop;
        insideLoop = true;
        visitBlock(ctx.block());
        insideLoop = savedInsideLoop;
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementForRange(ReLangParser.StatementForRangeContext ctx) {
        var startType = visit(ctx.expr(0));
        var endType = visit(ctx.expr(1));
        checkIntBound(startType, ctx, "for-range start");
        checkIntBound(endType, ctx, "for-range end");

        var loopScope = new Scope(currentScope);
        loopScope.define(
                ctx.ID().getText(),
                ReLangType.IntType.INSTANCE,
                sourceRangeFromToken(ctx.ID().getSymbol(), ctx.ID().getText())
        );
        var savedScope = currentScope;
        var savedInsideLoop = insideLoop;
        currentScope = loopScope;
        insideLoop = true;
        visitBlock(ctx.block());
        currentScope = savedScope;
        insideLoop = savedInsideLoop;
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementForRangeInclusive(ReLangParser.StatementForRangeInclusiveContext ctx) {
        var startType = visit(ctx.expr(0));
        var endType = visit(ctx.expr(1));
        checkIntBound(startType, ctx, "for-range start");
        checkIntBound(endType, ctx, "for-range end");

        var loopScope = new Scope(currentScope);
        loopScope.define(
                ctx.ID().getText(),
                ReLangType.IntType.INSTANCE,
                sourceRangeFromToken(ctx.ID().getSymbol(), ctx.ID().getText())
        );
        var savedScope = currentScope;
        var savedInsideLoop = insideLoop;
        currentScope = loopScope;
        insideLoop = true;
        visitBlock(ctx.block());
        currentScope = savedScope;
        insideLoop = savedInsideLoop;
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementReturn(ReLangParser.StatementReturnContext ctx) {
        var exprType = visit(ctx.expr());
        if (currentFunctionReturnType != null) {
            checkReturnType(exprType, currentFunctionReturnType, ctx);
        }
        // During return type inference, track the return expression type
        if (inferredReturnFromStatements == null) {
            inferredReturnFromStatements = exprType;
        } else {
            inferredReturnFromStatements = unifyTypes(inferredReturnFromStatements, exprType);
        }
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementBreak(ReLangParser.StatementBreakContext ctx) {
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementContinue(ReLangParser.StatementContinueContext ctx) {
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementCheckpoint(ReLangParser.StatementCheckpointContext ctx) {
        return ReLangType.UnitType.INSTANCE;
    }

    @Override
    public ReLangType visitStatementExpr(ReLangParser.StatementExprContext ctx) {
        return visit(ctx.expr());
    }

    // ---- Visitor overrides: Block ----

    @Override
    public ReLangType visitBlock(ReLangParser.BlockContext ctx) {
        var blockScope = new Scope(currentScope);
        var savedScope = currentScope;
        currentScope = blockScope;

        ReLangType lastType = ReLangType.UnitType.INSTANCE;
        for (var stmt : ctx.statement()) {
            lastType = visit(stmt);
        }

        currentScope = savedScope;
        return lastType;
    }

    // ---- Visitor overrides: If-else expression ----

    @Override
    public ReLangType visitExprIfElse(ReLangParser.ExprIfElseContext ctx) {
        var condType = visit(ctx.expr());
        checkBoolCondition(condType, ctx, "if");
        var thenType = visitBlock(ctx.block(0));
        var elseType = visitBlock(ctx.block(1));
        return unifyTypes(thenType, elseType);
    }

    // ---- Visitor overrides: Match expressions ----

    @Override
    public ReLangType visitExprMatchSubject(ReLangParser.ExprMatchSubjectContext ctx) {
        var subjectType = visit(ctx.expr());

        ReLangType resultType = null;
        for (var arm : ctx.matchArm()) {
            // Check pattern type vs subject type
            var patternCtx = arm.matchPattern();
            if (patternCtx instanceof ReLangParser.PatternExprContext pec) {
                var patType = visit(pec.expr());
                if (!(subjectType instanceof ReLangType.UnknownType) && !(patType instanceof ReLangType.UnknownType)) {
                    if (!patType.isAssignableTo(subjectType) && !subjectType.isAssignableTo(patType)) {
                        // Only error on clearly incompatible types, but be lenient
                    }
                }
            }
            // Visit arm body
            var bodyType = visitMatchBody(arm.matchBody());
            resultType = (resultType == null) ? bodyType : unifyTypes(resultType, bodyType);
        }
        return resultType != null ? resultType : ReLangType.UnknownType.INSTANCE;
    }

    @Override
    public ReLangType visitExprMatchSubjectless(ReLangParser.ExprMatchSubjectlessContext ctx) {
        ReLangType resultType = null;
        for (var arm : ctx.matchArm()) {
            var patternCtx = arm.matchPattern();
            if (patternCtx instanceof ReLangParser.PatternExprContext pec) {
                var patType = visit(pec.expr());
                // In subjectless match, non-wildcard patterns should evaluate to Bool
                if (!(patType instanceof ReLangType.UnknownType) && !(patType instanceof ReLangType.BoolType)) {
                    addError(pec, TypeDiagnostics.invalidSubjectlessMatchPattern(patType));
                }
            }
            // Wildcard and none patterns are always ok
            var bodyType = visitMatchBody(arm.matchBody());
            resultType = (resultType == null) ? bodyType : unifyTypes(resultType, bodyType);
        }
        return resultType != null ? resultType : ReLangType.UnknownType.INSTANCE;
    }

    @Override
    public ReLangType visitMatchBody(ReLangParser.MatchBodyContext ctx) {
        if (ctx.block() != null) {
            return visitBlock(ctx.block());
        }
        return visit(ctx.expr());
    }

    // ---- Helpers ----

    private void checkBoolCondition(ReLangType condType, org.antlr.v4.runtime.ParserRuleContext ctx, String keyword) {
        if (condType instanceof ReLangType.UnknownType) return;
        if (!(condType instanceof ReLangType.BoolType)) {
            addError(ctx, TypeDiagnostics.invalidCondition(keyword, condType));
        }
    }

    private void checkIntBound(ReLangType boundType, org.antlr.v4.runtime.ParserRuleContext ctx, String label) {
        if (boundType instanceof ReLangType.UnknownType) return;
        if (!(boundType instanceof ReLangType.IntType)) {
            addError(ctx, TypeDiagnostics.invalidRangeBound(label, boundType));
        }
    }

    /**
     * Unify two types for branches (if-else, match arms).
     * Same type -> that type. T and None -> Optional(T). Otherwise Unknown.
     */
    private ReLangType unifyTypes(ReLangType a, ReLangType b) {
        if (a.equals(b)) return a;
        if (a instanceof ReLangType.UnknownType || b instanceof ReLangType.UnknownType) {
            return ReLangType.UnknownType.INSTANCE;
        }
        // T + None -> Optional(T)
        if (a instanceof ReLangType.NoneType) return new ReLangType.OptionalType(b);
        if (b instanceof ReLangType.NoneType) return new ReLangType.OptionalType(a);
        // Optional(T) + T -> Optional(T)
        if (a instanceof ReLangType.OptionalType optA && b.isAssignableTo(optA.inner())) return a;
        if (b instanceof ReLangType.OptionalType optB && a.isAssignableTo(optB.inner())) return b;
        // Can't unify - return Unknown rather than erroring
        return ReLangType.UnknownType.INSTANCE;
    }

    /**
     * Scan a string literal's inner content for ${...} interpolations and type-check each expression.
     */
    private void checkInterpolations(String inner, org.antlr.v4.runtime.ParserRuleContext parentCtx) {
        int i = 0;
        while (i < inner.length()) {
            char c = inner.charAt(i);
            if (c == '\\') {
                i += 2; // skip escaped character
            } else if (c == '$' && i + 1 < inner.length() && inner.charAt(i + 1) == '{') {
                // Find matching closing brace
                int braceDepth = 1;
                int start = i + 2;
                int j = start;
                while (j < inner.length() && braceDepth > 0) {
                    if (inner.charAt(j) == '{') braceDepth++;
                    else if (inner.charAt(j) == '}') braceDepth--;
                    if (braceDepth > 0) j++;
                }
                if (braceDepth != 0) {
                    addError(parentCtx, TypeDiagnostics.unclosedInterpolation());
                    return;
                }
                // Parse and type-check the expression
                var exprText = inner.substring(start, j);
                try {
                    var lexer = new ReLangLexer(org.antlr.v4.runtime.CharStreams.fromString(exprText));
                    var parser = new ReLangParser(new org.antlr.v4.runtime.CommonTokenStream(lexer));
                    var exprCtx = parser.expr();
                    visit(exprCtx);
                } catch (Exception e) {
                    addError(parentCtx, TypeDiagnostics.invalidInterpolationExpression(exprText));
                }
                i = j + 1;
            } else {
                i++;
            }
        }
    }

    private void addError(org.antlr.v4.runtime.ParserRuleContext ctx, TypeDiagnostics.Spec diagnostic) {
        addError(ctx, diagnostic.code(), diagnostic.message(), diagnostic.help());
    }

    private void addError(
            org.antlr.v4.runtime.ParserRuleContext ctx,
            TypeDiagnostics.Spec diagnostic,
            List<DiagnosticNote> notes
    ) {
        addError(ctx, diagnostic.code(), diagnostic.message(), diagnostic.help(), notes);
    }

    private void addError(org.antlr.v4.runtime.ParserRuleContext ctx, DiagnosticCode code, String message, String help) {
        addError(ctx, code, message, help, List.of());
    }

    private void addError(
            org.antlr.v4.runtime.ParserRuleContext ctx,
            DiagnosticCode code,
            String message,
            String help,
            List<DiagnosticNote> notes
    ) {
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        var snippet = ctx.getText();
        errors.add(new TypeError(code, line, col, message, snippet, help, notes));
    }

    private static List<DiagnosticNote> declarationNote(String message, SourceRange range) {
        if (range == null) {
            return List.of();
        }
        return List.of(new DiagnosticNote(message, range));
    }

    private static SourceRange sourceRangeFromToken(Token token, String snippet) {
        if (token == null) {
            return null;
        }
        return SourceRange.fromSnippet(token.getLine(), token.getCharPositionInLine(), snippet);
    }
}
