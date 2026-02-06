package com.relang.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Static type checker for ReLang, implemented as an ANTLR visitor.
 * <p>
 * Three-pass approach:
 * <ol>
 *   <li>Register builtins + collect all function signatures</li>
 *   <li>Check all function bodies</li>
 *   <li>Check top-level commands</li>
 * </ol>
 * <p>
 * When ANY operand is UnknownType, the operation silently succeeds and returns
 * UnknownType. This ensures existing untyped code continues to work.
 */
public class ReLangTypeChecker extends ReLangBaseVisitor<ReLangType> {

    private final List<TypeError> errors = new ArrayList<>();
    private final Map<String, FunctionSignature> functionSignatures = new HashMap<>();
    private Scope currentScope = new Scope(null);
    private ReLangType currentFunctionReturnType = null;
    private boolean insideLoop = false;

    // ---- Inner classes ----

    private static class Scope {
        private final Scope parent;
        private final Map<String, ReLangType> variables = new HashMap<>();

        Scope(Scope parent) {
            this.parent = parent;
        }

        void define(String name, ReLangType type) {
            variables.put(name, type);
        }

        ReLangType lookup(String name) {
            if (variables.containsKey(name)) return variables.get(name);
            if (parent != null) return parent.lookup(name);
            return null;
        }

        boolean isDefined(String name) {
            return lookup(name) != null;
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
            ReLangType returnType
    ) {}

    record ParamSignature(String name, ReLangType type) {}

    // ---- Public API ----

    /**
     * Run the type checker on a parsed source tree.
     * @return list of type errors (empty if code is well-typed)
     */
    public List<TypeError> check(ReLangParser.SourceContext tree) {
        // Pass 1: register builtins and collect function signatures
        registerBuiltins();
        for (var func : tree.function()) {
            collectFunctionSignature(func);
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
                List.of(new ParamSignature("value", ReLangType.UnknownType.INSTANCE)),
                1,
                new ReLangType.AwaitableType(ReLangType.UnknownType.INSTANCE)
        ));
        // pending() -> Awaitable<Unknown>
        functionSignatures.put("pending", new FunctionSignature(
                "pending",
                List.of(),
                0,
                new ReLangType.AwaitableType(ReLangType.UnknownType.INSTANCE)
        ));
    }

    private void collectFunctionSignature(ReLangParser.FunctionContext func) {
        switch (func) {
            case ReLangParser.FunctionBlockContext fb -> {
                var name = fb.ID().getText();
                var params = collectParams(fb.typedParameters());
                int requiredCount = countRequired(fb.typedParameters());
                var returnType = fb.typeRef() != null
                        ? ReLangType.fromTypeRef(fb.typeRef())
                        : ReLangType.UnknownType.INSTANCE;
                functionSignatures.put(name, new FunctionSignature(name, params, requiredCount, returnType));
            }
            case ReLangParser.FunctionExprContext fe -> {
                var name = fe.ID().getText();
                var params = collectParams(fe.typedParameters());
                int requiredCount = countRequired(fe.typedParameters());
                var returnType = fe.typeRef() != null
                        ? ReLangType.fromTypeRef(fe.typeRef())
                        : ReLangType.UnknownType.INSTANCE;
                functionSignatures.put(name, new FunctionSignature(name, params, requiredCount, returnType));
            }
            default -> {}
        }
    }

    private List<ParamSignature> collectParams(ReLangParser.TypedParametersContext ctx) {
        if (ctx == null) return List.of();
        var result = new ArrayList<ParamSignature>();
        for (var p : ctx.typedParam()) {
            var name = p.ID().getText();
            var type = p.typeRef() != null
                    ? ReLangType.fromTypeRef(p.typeRef())
                    : ReLangType.UnknownType.INSTANCE;
            result.add(new ParamSignature(name, type));
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

    // ---- Pass 2: Function body checking ----

    private void checkFunctionBody(ReLangParser.FunctionContext func) {
        switch (func) {
            case ReLangParser.FunctionBlockContext fb -> {
                var sig = functionSignatures.get(fb.ID().getText());
                var funcScope = new Scope(null);
                for (var p : sig.params()) {
                    funcScope.define(p.name(), p.type());
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
                    funcScope.define(p.name(), p.type());
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
        if (!actual.isAssignableTo(expected)) {
            addError(ctx, "Return type mismatch: expected " + expected.displayName()
                    + " but got " + actual.displayName());
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

    // ---- Visitor overrides: Unary ----

    @Override
    public ReLangType visitExprNegate(ReLangParser.ExprNegateContext ctx) {
        var operand = visit(ctx.expr());
        if (operand instanceof ReLangType.UnknownType) return ReLangType.UnknownType.INSTANCE;
        if (operand instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        if (operand instanceof ReLangType.FloatType) return ReLangType.FloatType.INSTANCE;
        addError(ctx, "Cannot negate type " + operand.displayName() + ", expected Int or Float");
        return ReLangType.UnknownType.INSTANCE;
    }

    @Override
    public ReLangType visitExprNot(ReLangParser.ExprNotContext ctx) {
        var operand = visit(ctx.expr());
        if (operand instanceof ReLangType.UnknownType) return ReLangType.UnknownType.INSTANCE;
        if (!(operand instanceof ReLangType.BoolType)) {
            addError(ctx, "Operator 'not' requires Bool, got " + operand.displayName());
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
                addError(ctx, "Unknown operator: " + op);
                yield ReLangType.UnknownType.INSTANCE;
            }
        };
    }

    private ReLangType checkAdd(ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.FloatType.INSTANCE;
        if (left instanceof ReLangType.StringType && right instanceof ReLangType.StringType) return ReLangType.StringType.INSTANCE;
        addError(ctx, "Operator '+' cannot be applied to " + left.displayName() + " and " + right.displayName());
        return ReLangType.UnknownType.INSTANCE;
    }

    private ReLangType checkArithmetic(String op, ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.FloatType.INSTANCE;
        addError(ctx, "Operator '" + op + "' cannot be applied to " + left.displayName() + " and " + right.displayName());
        return ReLangType.UnknownType.INSTANCE;
    }

    private ReLangType checkModulo(ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.IntType.INSTANCE;
        addError(ctx, "Operator '%' can only be applied to Int, got " + left.displayName() + " and " + right.displayName());
        return ReLangType.UnknownType.INSTANCE;
    }

    private ReLangType checkOrdering(String op, ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.BoolType.INSTANCE;
        addError(ctx, "Operator '" + op + "' cannot be applied to " + left.displayName() + " and " + right.displayName());
        return ReLangType.BoolType.INSTANCE;
    }

    private ReLangType checkEquality(String op, ReLangType left, ReLangType right, ReLangParser.ExprBinaryContext ctx) {
        if (left instanceof ReLangType.IntType && right instanceof ReLangType.IntType) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.FloatType && right instanceof ReLangType.FloatType) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.BoolType && right instanceof ReLangType.BoolType) return ReLangType.BoolType.INSTANCE;
        if (left instanceof ReLangType.StringType && right instanceof ReLangType.StringType) return ReLangType.BoolType.INSTANCE;
        addError(ctx, "Operator '" + op + "' cannot be applied to " + left.displayName() + " and " + right.displayName());
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
            addError(ctx, "Operator 'and' requires Bool operands, got " + left.displayName());
        }
        if (!(right instanceof ReLangType.BoolType)) {
            addError(ctx, "Operator 'and' requires Bool operands, got " + right.displayName());
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
            addError(ctx, "Operator 'or' requires Bool operands, got " + left.displayName());
        }
        if (!(right instanceof ReLangType.BoolType)) {
            addError(ctx, "Operator 'or' requires Bool operands, got " + right.displayName());
        }
        return ReLangType.BoolType.INSTANCE;
    }

    // ---- Visitor overrides: Variables ----

    @Override
    public ReLangType visitExprId(ReLangParser.ExprIdContext ctx) {
        var name = ctx.ID().getText();
        var type = currentScope.lookup(name);
        if (type == null) {
            // Untyped variable - don't error, just return Unknown for backward compat
            // The variable might be defined by a bare assignment (x = 10;)
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
            addError(ctx, "Unknown function: " + funcName);
            return ReLangType.UnknownType.INSTANCE;
        }

        // Count args
        int argCount = 0;
        if (ctx.callArguments() != null) {
            argCount = ctx.callArguments().callArg().size();
        }

        // Check arity
        if (argCount < sig.requiredCount()) {
            addError(ctx, "Function '" + funcName + "' requires at least " + sig.requiredCount()
                    + " arguments, got " + argCount);
        } else if (argCount > sig.params().size()) {
            addError(ctx, "Function '" + funcName + "' accepts at most " + sig.params().size()
                    + " arguments, got " + argCount);
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
                            addError(ctx, "Argument " + (i + 1) + " of '" + funcName + "': expected "
                                    + paramType.displayName() + " but got " + argType.displayName());
                        }
                    }
                }
            }
        }

        return sig.returnType();
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
        addError(ctx, "Cannot await non-awaitable type " + inner.displayName());
        return ReLangType.UnknownType.INSTANCE;
    }

    // ---- Visitor overrides: Statements ----

    @Override
    public ReLangType visitStatementLet(ReLangParser.StatementLetContext ctx) {
        var rhsType = visit(ctx.expr());
        var varName = ctx.ID().getText();
        // If initialized to none, widen to Unknown so reassignment to any type is allowed
        if (rhsType instanceof ReLangType.NoneType) {
            currentScope.define(varName, ReLangType.UnknownType.INSTANCE);
        } else {
            currentScope.define(varName, rhsType);
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
            // Implicit define (bare assignment like `x = 10;`) - backward compat
            currentScope.define(varName, rhsType);
        } else {
            // Check type compatibility
            if (!(existingType instanceof ReLangType.UnknownType) && !(rhsType instanceof ReLangType.UnknownType)) {
                if (!rhsType.isAssignableTo(existingType)) {
                    addError(ctx, "Cannot assign " + rhsType.displayName() + " to variable of type "
                            + existingType.displayName());
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
        loopScope.define(ctx.ID().getText(), ReLangType.IntType.INSTANCE);
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
        loopScope.define(ctx.ID().getText(), ReLangType.IntType.INSTANCE);
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
        // Trailing expression (expression blocks like { stmt*; expr })
        if (ctx.expr() != null) {
            lastType = visit(ctx.expr());
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
                        // (e.g., matching Optional against None is fine)
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
                    addError(pec, "Subjectless match pattern must be Bool, got " + patType.displayName());
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
            addError(ctx, "Condition must be Bool, got " + condType.displayName());
        }
    }

    private void checkIntBound(ReLangType boundType, org.antlr.v4.runtime.ParserRuleContext ctx, String label) {
        if (boundType instanceof ReLangType.UnknownType) return;
        if (!(boundType instanceof ReLangType.IntType)) {
            addError(ctx, label + " must be Int, got " + boundType.displayName());
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

    private void addError(org.antlr.v4.runtime.ParserRuleContext ctx, String message) {
        int line = ctx.getStart().getLine();
        int col = ctx.getStart().getCharPositionInLine();
        var snippet = ctx.getText();
        errors.add(new TypeError(line, col, message, snippet));
    }
}
