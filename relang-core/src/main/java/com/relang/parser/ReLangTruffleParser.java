package com.relang.parser;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.relang.FunctionDescriptor;
import com.relang.ReLang;
import com.relang.ReLangRootNode;
import com.relang.nodes.ReLangNode;
import com.relang.nodes.ReLangAwaitNode;
import com.relang.nodes.ReLangBlockNode;
import com.relang.nodes.ReLangBuiltinResolvedNode;
import com.relang.nodes.ReLangBuiltinPendingNode;
import com.relang.nodes.LongLiteralNode;
import com.relang.nodes.DoubleLiteralNode;
import com.relang.nodes.BooleanLiteralNode;
import com.relang.nodes.StringLiteralNode;
import com.relang.nodes.NoneLiteralNode;
import com.relang.nodes.UnitLiteralNode;
import com.relang.nodes.AddNodeGen;
import com.relang.nodes.SubNodeGen;
import com.relang.nodes.MulNodeGen;
import com.relang.nodes.DivNodeGen;
import com.relang.nodes.ModNodeGen;
import com.relang.nodes.NegateNodeGen;
import com.relang.nodes.NotNodeGen;
import com.relang.nodes.LogicalAndNode;
import com.relang.nodes.LogicalOrNode;
import com.relang.nodes.LessThanNodeGen;
import com.relang.nodes.LessOrEqualNodeGen;
import com.relang.nodes.GreaterThanNodeGen;
import com.relang.nodes.GreaterOrEqualNodeGen;
import com.relang.nodes.EqualsNodeGen;
import com.relang.nodes.NotEqualsNodeGen;
import com.relang.nodes.ReLangIfNode;
import com.relang.nodes.ReLangWhileNode;
import com.relang.nodes.ReLangReadLocalVarNodeGen;
import com.relang.nodes.ReLangWriteLocalVarNodeGen;
import com.relang.nodes.ReLangReturnNode;
import com.relang.nodes.ReLangInvokeNode;
import com.relang.nodes.ReLangReadArgumentNode;
import com.relang.nodes.ReLangBreakNode;
import com.relang.nodes.ReLangContinueNode;
import com.relang.nodes.ReLangForRangeNode;
import com.relang.nodes.ReLangMatchNode;
import com.relang.nodes.ReLangMatchSubjectlessNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReLangTruffleParser {

    private static class ParseContext {
        final FrameDescriptor.Builder frameBuilder = FrameDescriptor.newBuilder();
        final Map<String, Integer> locals = new HashMap<>();

        int getSlot(String name) {
            if (!locals.containsKey(name)) {
                int slot = frameBuilder.addSlot(com.oracle.truffle.api.frame.FrameSlotKind.Illegal, name, null);
                locals.put(name, slot);
            }
            return locals.get(name);
        }
    }

    private record ParamInfo(List<String> names, List<ReLangNode> setupNodes, int requiredCount) {}

    /**
     * Parse source text into an ANTLR parse tree (no Truffle nodes).
     * Useful for static analysis passes before Truffle node construction.
     */
    public static ReLangParser.SourceContext parseAntlr(Source source) {
        var lexer = new ReLangLexer(CharStreams.fromString(source.getCharacters().toString()));
        var parser = new ReLangParser(new CommonTokenStream(lexer));
        return parser.source();
    }

    /**
     * Build Truffle nodes from a pre-parsed ANTLR tree.
     */
    public static Map<String, FunctionDescriptor> buildTruffleNodes(ReLang language, ReLangParser.SourceContext tree) {
        Map<String, FunctionDescriptor> functions = new HashMap<>();

        // Parse functions first (hoisting)
        for (var funcCtx : tree.function()) {
            switch (funcCtx) {
                case ReLangParser.FunctionBlockContext fb -> {
                    var functionName = fb.ID().getText();
                    var context = new ParseContext();
                    var paramInfo = parseTypedParams(context, fb.typedParameters());
                    var body = parseBlock(context, fb.block());

                    ReLangNode fullBody;
                    if (!paramInfo.setupNodes.isEmpty()) {
                        var allNodes = new ArrayList<>(paramInfo.setupNodes);
                        allNodes.add(body);
                        fullBody = new ReLangBlockNode(allNodes.toArray(new ReLangNode[0]));
                    } else {
                        fullBody = body;
                    }

                    var rootNode = new ReLangRootNode(language, context.frameBuilder.build(), fullBody);
                    functions.put(functionName, new FunctionDescriptor(rootNode.getCallTarget(), paramInfo.names, paramInfo.requiredCount));
                }
                case ReLangParser.FunctionExprContext fe -> {
                    var functionName = fe.ID().getText();
                    var context = new ParseContext();
                    var paramInfo = parseTypedParams(context, fe.typedParameters());
                    var bodyExpr = parseExpr(context, fe.expr());

                    // Wrap expression body - parameter setup + expression as single block
                    List<ReLangNode> bodyNodes = new ArrayList<>(paramInfo.setupNodes);
                    bodyNodes.add(bodyExpr);
                    var fullBody = new ReLangBlockNode(bodyNodes.toArray(new ReLangNode[0]));

                    var rootNode = new ReLangRootNode(language, context.frameBuilder.build(), fullBody);
                    functions.put(functionName, new FunctionDescriptor(rootNode.getCallTarget(), paramInfo.names, paramInfo.requiredCount));
                }
                default -> throw new IllegalArgumentException("Unknown function type: " + funcCtx.getClass().getSimpleName());
            }
        }

        // Parse main body (top-level statements)
        var mainContext = new ParseContext();
        List<ReLangNode> mainNodes = new ArrayList<>();
        if (tree.command().isEmpty()) {
            // If no main code, minimal dummy
            mainNodes.add(new LongLiteralNode(0));
        } else {
            for (var cmd : tree.command()) {
                mainNodes.add(parseStatement(mainContext, cmd.statement()));
            }
        }

        var mainBody = new ReLangBlockNode(mainNodes.toArray(new ReLangNode[0]));
        var mainRoot = new ReLangRootNode(language, mainContext.frameBuilder.build(), mainBody);

        functions.put("", new FunctionDescriptor(mainRoot.getCallTarget(), List.of(), 0));

        // Register built-in functions for awaitables
        var resolvedRoot = new ReLangBuiltinResolvedNode(language);
        functions.put("resolved", new FunctionDescriptor(resolvedRoot.getCallTarget(), List.of("value"), 1));

        var pendingRoot = new ReLangBuiltinPendingNode(language);
        functions.put("pending", new FunctionDescriptor(pendingRoot.getCallTarget(), List.of(), 0));

        return functions;
    }

    /**
     * Convenience method: ANTLR parse + build Truffle nodes in one call.
     */
    public static Map<String, FunctionDescriptor> parse(ReLang language, Source source) {
        var tree = parseAntlr(source);
        return buildTruffleNodes(language, tree);
    }

    private static ParamInfo parseTypedParams(ParseContext context, ReLangParser.TypedParametersContext paramsCtx) {
        List<String> names = new ArrayList<>();
        List<ReLangNode> setupNodes = new ArrayList<>();
        int requiredCount = 0;
        boolean seenDefault = false;

        if (paramsCtx != null) {
            int argIndex = 0;
            for (var param : paramsCtx.typedParam()) {
                var paramName = param.ID().getText();
                names.add(paramName);
                int slot = context.getSlot(paramName);

                ReLangNode defaultNode = null;
                if (param.expr() != null) {
                    defaultNode = parseExpr(context, param.expr());
                    seenDefault = true;
                } else {
                    if (seenDefault) {
                        throw new RuntimeException("Non-default parameter '" + paramName + "' follows default parameter");
                    }
                    requiredCount++;
                }

                ReLangNode readArg = new ReLangReadArgumentNode(argIndex, defaultNode);
                ReLangNode writeArg = ReLangWriteLocalVarNodeGen.create(readArg, slot);
                setupNodes.add(writeArg);
                argIndex++;
            }
        }

        return new ParamInfo(names, setupNodes, requiredCount);
    }

    private static ReLangNode parseBlock(ParseContext context, ReLangParser.BlockContext ctx) {
        List<ReLangNode> nodes = new ArrayList<>();
        for (var stmt : ctx.statement()) {
            nodes.add(parseStatement(context, stmt));
        }
        // Handle trailing expression without semicolon (expression blocks like { 1 })
        if (ctx.expr() != null) {
            nodes.add(parseExpr(context, ctx.expr()));
        }
        return new ReLangBlockNode(nodes.toArray(new ReLangNode[0]));
    }

    private static ReLangNode parseStatement(ParseContext context, ReLangParser.StatementContext ctx) {
        return switch (ctx) {
            case ReLangParser.StatementExprContext s -> parseExpr(context, s.expr());
            case ReLangParser.StatementAssignmentContext s -> parseAssignment(context, s.assignment());
            case ReLangParser.StatementLetContext s -> parseLet(context, s);
            case ReLangParser.StatementIfContext s -> parseIf(context, s);
            case ReLangParser.StatementIfNoParensContext s -> parseIfNoParens(context, s);
            case ReLangParser.StatementWhileContext s -> parseWhile(context, s);
            case ReLangParser.StatementWhileNoParensContext s -> parseWhileNoParens(context, s);
            case ReLangParser.StatementForRangeContext s -> parseForRange(context, s);
            case ReLangParser.StatementForRangeInclusiveContext s -> parseForRangeInclusive(context, s);
            case ReLangParser.StatementReturnContext s -> new ReLangReturnNode(parseExpr(context, s.expr()));
            case ReLangParser.StatementBreakContext ignored -> new ReLangBreakNode();
            case ReLangParser.StatementContinueContext ignored -> new ReLangContinueNode();
            case ReLangParser.StatementCheckpointContext ignored -> new com.relang.nodes.ReLangCheckpointNode();
            case null -> throw new IllegalArgumentException("Statement context cannot be null");
            default -> throw new IllegalArgumentException("Unknown statement type: " + ctx.getClass().getSimpleName());
        };
    }

    private static ReLangNode parseLet(ParseContext context, ReLangParser.StatementLetContext ctx) {
        var varName = ctx.ID().getText();
        var valueNode = parseExpr(context, ctx.expr());
        int slot = context.getSlot(varName);
        return ReLangWriteLocalVarNodeGen.create(valueNode, slot);
    }

    private static ReLangNode parseIf(ParseContext context, ReLangParser.StatementIfContext ctx) {
        var condition = parseExpr(context, ctx.expr());
        var thenPart = parseBlock(context, ctx.block(0));
        ReLangNode elsePart = null;
        if (ctx.block().size() > 1) {
            elsePart = parseBlock(context, ctx.block(1));
        }
        return new ReLangIfNode(condition, thenPart, elsePart);
    }

    private static ReLangNode parseWhile(ParseContext context, ReLangParser.StatementWhileContext ctx) {
        var condition = parseExpr(context, ctx.expr());
        var body = parseBlock(context, ctx.block());
        return new ReLangWhileNode(condition, body);
    }

    private static ReLangNode parseAssignment(ParseContext context, ReLangParser.AssignmentContext ctx) {
        var varName = ctx.ID().getText();
        var valueNode = parseExpr(context, ctx.expr());
        int slot = context.getSlot(varName);
        return ReLangWriteLocalVarNodeGen.create(valueNode, slot);
    }

    private static ReLangNode parseExpr(ParseContext context, ReLangParser.ExprContext ctx) {
        return switch (ctx) {
            case ReLangParser.ExprBinaryContext bin -> parseBinaryExpr(context, bin);
            case ReLangParser.ExprIntContext intCtx -> new LongLiteralNode(Long.parseLong(intCtx.INT().getText().replace("_", "")));
            case ReLangParser.ExprFloatContext floatCtx -> new DoubleLiteralNode(Double.parseDouble(floatCtx.FLOAT().getText().replace("_", "")));
            case ReLangParser.ExprStringContext strCtx -> new StringLiteralNode(processStringLiteral(strCtx.STRING().getText()));
            case ReLangParser.ExprTrueContext ignored -> new BooleanLiteralNode(true);
            case ReLangParser.ExprFalseContext ignored -> new BooleanLiteralNode(false);
            case ReLangParser.ExprNoneContext ignored -> new NoneLiteralNode();
            case ReLangParser.ExprUnitContext ignored -> new UnitLiteralNode();
            case ReLangParser.ExprNegateContext neg -> NegateNodeGen.create(parseExpr(context, neg.expr()));
            case ReLangParser.ExprAwaitContext awaitCtx -> new ReLangAwaitNode(parseExpr(context, awaitCtx.expr()));
            case ReLangParser.ExprNotContext notCtx -> NotNodeGen.create(parseExpr(context, notCtx.expr()));
            case ReLangParser.ExprAndContext andCtx -> new LogicalAndNode(parseExpr(context, andCtx.left), parseExpr(context, andCtx.right));
            case ReLangParser.ExprOrContext orCtx -> new LogicalOrNode(parseExpr(context, orCtx.left), parseExpr(context, orCtx.right));
            case ReLangParser.ExprParenContext paren -> parseExpr(context, paren.expr());
            case ReLangParser.ExprIdContext id -> ReLangReadLocalVarNodeGen.create(context.getSlot(id.ID().getText()));
            case ReLangParser.ExprCallContext call -> parseCallExpr(context, call);
            case ReLangParser.ExprIfElseContext ifElse -> parseIfElseExpr(context, ifElse);
            case ReLangParser.ExprMatchSubjectContext matchCtx -> parseMatchSubject(context, matchCtx);
            case ReLangParser.ExprMatchSubjectlessContext matchCtx -> parseMatchSubjectless(context, matchCtx);
            case null -> throw new IllegalArgumentException("Expression context cannot be null");
            default -> throw new IllegalArgumentException("Unknown expr type: " + ctx.getText());
        };
    }

    private static ReLangNode parseBinaryExpr(ParseContext context, ReLangParser.ExprBinaryContext bin) {
        var left = parseExpr(context, bin.left);
        var right = parseExpr(context, bin.right);

        return switch (bin.op.getText()) {
            case "+" -> AddNodeGen.create(left, right);
            case "-" -> SubNodeGen.create(left, right);
            case "*" -> MulNodeGen.create(left, right);
            case "/" -> DivNodeGen.create(left, right);
            case "%" -> ModNodeGen.create(left, right);
            case "<" -> LessThanNodeGen.create(left, right);
            case "<=" -> LessOrEqualNodeGen.create(left, right);
            case ">" -> GreaterThanNodeGen.create(left, right);
            case ">=" -> GreaterOrEqualNodeGen.create(left, right);
            case "==" -> EqualsNodeGen.create(left, right);
            case "!=" -> NotEqualsNodeGen.create(left, right);
            default -> throw new IllegalArgumentException("Unknown operator: " + bin.op.getText());
        };
    }

    private static ReLangNode parseCallExpr(ParseContext context, ReLangParser.ExprCallContext call) {
        var funcName = call.ID().getText();
        List<ReLangNode> args = new ArrayList<>();
        List<String> argNames = new ArrayList<>();

        if (call.callArguments() != null) {
            for (var callArg : call.callArguments().callArg()) {
                switch (callArg) {
                    case ReLangParser.CallArgPositionalContext pos -> {
                        args.add(parseExpr(context, pos.expr()));
                        argNames.add(null);
                    }
                    case ReLangParser.CallArgNamedContext named -> {
                        args.add(parseExpr(context, named.expr()));
                        argNames.add(named.ID().getText());
                    }
                    default -> throw new IllegalArgumentException("Unknown call arg type: " + callArg.getClass().getSimpleName());
                }
            }
        }
        return new ReLangInvokeNode(funcName, args.toArray(new ReLangNode[0]), argNames.toArray(new String[0]));
    }

    private static ReLangNode parseIfNoParens(ParseContext context, ReLangParser.StatementIfNoParensContext ctx) {
        var condition = parseExpr(context, ctx.expr());
        var thenPart = parseBlock(context, ctx.block(0));
        ReLangNode elsePart = null;
        if (ctx.block().size() > 1) {
            elsePart = parseBlock(context, ctx.block(1));
        }
        return new ReLangIfNode(condition, thenPart, elsePart);
    }

    private static ReLangNode parseWhileNoParens(ParseContext context, ReLangParser.StatementWhileNoParensContext ctx) {
        var condition = parseExpr(context, ctx.expr());
        var body = parseBlock(context, ctx.block());
        return new ReLangWhileNode(condition, body);
    }

    private static ReLangNode parseIfElseExpr(ParseContext context, ReLangParser.ExprIfElseContext ctx) {
        var condition = parseExpr(context, ctx.expr());
        var thenPart = parseBlock(context, ctx.block(0));
        var elsePart = parseBlock(context, ctx.block(1));
        return new ReLangIfNode(condition, thenPart, elsePart);
    }

    private static ReLangNode parseForRange(ParseContext context, ReLangParser.StatementForRangeContext ctx) {
        var varName = ctx.ID().getText();
        int slot = context.getSlot(varName);
        var startNode = parseExpr(context, ctx.expr(0));
        var endNode = parseExpr(context, ctx.expr(1));
        var body = parseBlock(context, ctx.block());
        return new ReLangForRangeNode(startNode, endNode, body, slot, false);
    }

    private static ReLangNode parseForRangeInclusive(ParseContext context, ReLangParser.StatementForRangeInclusiveContext ctx) {
        var varName = ctx.ID().getText();
        int slot = context.getSlot(varName);
        var startNode = parseExpr(context, ctx.expr(0));
        var endNode = parseExpr(context, ctx.expr(1));
        var body = parseBlock(context, ctx.block());
        return new ReLangForRangeNode(startNode, endNode, body, slot, true);
    }

    private static ReLangNode parseMatchSubject(ParseContext context, ReLangParser.ExprMatchSubjectContext ctx) {
        var subject = parseExpr(context, ctx.expr());
        var arms = new ArrayList<ReLangMatchNode.MatchArmNode>();
        for (var armCtx : ctx.matchArm()) {
            arms.add(parseMatchArm(context, armCtx));
        }
        return new ReLangMatchNode(subject, arms.toArray(new ReLangMatchNode.MatchArmNode[0]));
    }

    private static ReLangMatchNode.MatchArmNode parseMatchArm(ParseContext context, ReLangParser.MatchArmContext armCtx) {
        var patternCtx = armCtx.matchPattern();
        var bodyNode = parseMatchBody(context, armCtx.matchBody());

        return switch (patternCtx) {
            case ReLangParser.PatternWildcardContext ignored ->
                new ReLangMatchNode.MatchArmNode(ReLangMatchNode.MatchArmNode.MatchPatternKind.WILDCARD, null, bodyNode);
            case ReLangParser.PatternNoneContext ignored ->
                new ReLangMatchNode.MatchArmNode(ReLangMatchNode.MatchArmNode.MatchPatternKind.NONE, null, bodyNode);
            case ReLangParser.PatternExprContext exprCtx ->
                new ReLangMatchNode.MatchArmNode(ReLangMatchNode.MatchArmNode.MatchPatternKind.LITERAL, parseExpr(context, exprCtx.expr()), bodyNode);
            case null -> throw new IllegalArgumentException("Match pattern cannot be null");
            default -> throw new IllegalArgumentException("Unknown match pattern: " + patternCtx.getClass().getSimpleName());
        };
    }

    private static ReLangNode parseMatchSubjectless(ParseContext context, ReLangParser.ExprMatchSubjectlessContext ctx) {
        var arms = new ArrayList<ReLangMatchSubjectlessNode.SubjectlessArmNode>();
        for (var armCtx : ctx.matchArm()) {
            arms.add(parseSubjectlessArm(context, armCtx));
        }
        return new ReLangMatchSubjectlessNode(arms.toArray(new ReLangMatchSubjectlessNode.SubjectlessArmNode[0]));
    }

    private static ReLangMatchSubjectlessNode.SubjectlessArmNode parseSubjectlessArm(ParseContext context, ReLangParser.MatchArmContext armCtx) {
        var patternCtx = armCtx.matchPattern();
        var bodyNode = parseMatchBody(context, armCtx.matchBody());

        return switch (patternCtx) {
            case ReLangParser.PatternWildcardContext ignored ->
                new ReLangMatchSubjectlessNode.SubjectlessArmNode(true, null, bodyNode);
            case ReLangParser.PatternExprContext exprCtx ->
                new ReLangMatchSubjectlessNode.SubjectlessArmNode(false, parseExpr(context, exprCtx.expr()), bodyNode);
            case ReLangParser.PatternNoneContext ignored ->
                new ReLangMatchSubjectlessNode.SubjectlessArmNode(true, null, bodyNode); // none in subjectless treated as wildcard
            case null -> throw new IllegalArgumentException("Match pattern cannot be null");
            default -> throw new IllegalArgumentException("Unknown match pattern: " + patternCtx.getClass().getSimpleName());
        };
    }

    private static ReLangNode parseMatchBody(ParseContext context, ReLangParser.MatchBodyContext bodyCtx) {
        if (bodyCtx.block() != null) {
            return parseBlock(context, bodyCtx.block());
        } else {
            return parseExpr(context, bodyCtx.expr());
        }
    }

    private static String processStringLiteral(String raw) {
        // raw includes surrounding quotes
        var inner = raw.substring(1, raw.length() - 1);
        var sb = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '\\' && i + 1 < inner.length()) {
                char next = inner.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case '$' -> { sb.append('$'); i++; }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
