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
import com.relang.nodes.ToStringNodeGen;
import com.relang.nodes.DurationLiteralNode;
import com.relang.nodes.BytesLiteralNode;
import com.relang.nodes.ReLangBuiltinNowNode;
import com.relang.nodes.ReLangBuiltinFailureNode;
import com.relang.nodes.ReLangBuiltinJsonNode;
import com.relang.nodes.ReLangFieldAccessNode;
import com.relang.nodes.ReLangConstructNode;
import com.relang.nodes.ReLangProductNode;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReLangTruffleParser {

    private static class ParseContext {
        final FrameDescriptor.Builder frameBuilder = FrameDescriptor.newBuilder();
        final Map<String, Integer> locals = new HashMap<>();
        final Source source;

        ParseContext(Source source) {
            this.source = source;
        }

        int getSlot(String name) {
            if (!locals.containsKey(name)) {
                int slot = frameBuilder.addSlot(com.oracle.truffle.api.frame.FrameSlotKind.Illegal, name, null);
                locals.put(name, slot);
            }
            return locals.get(name);
        }

        com.oracle.truffle.api.source.SourceSection sourceSection(org.antlr.v4.runtime.ParserRuleContext ctx) {
            if (source == null || ctx == null || ctx.getStart() == null) return null;
            int startIndex = ctx.getStart().getStartIndex();
            int stopIndex = ctx.getStop() != null ? ctx.getStop().getStopIndex() : startIndex;
            int length = stopIndex - startIndex + 1;
            if (startIndex < 0 || length <= 0 || startIndex + length > source.getLength()) return null;
            return source.createSection(startIndex, length);
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
        var errors = new ArrayList<SyntaxError>();
        var errorListener = new BaseErrorListener() {
            @Override
            public void syntaxError(
                    Recognizer<?, ?> recognizer,
                    Object offendingSymbol,
                    int line,
                    int charPositionInLine,
                    String msg,
                    RecognitionException e
            ) {
                var sourceSnippet = offendingSymbol instanceof Token token
                        && token.getText() != null
                        && !"<EOF>".equals(token.getText())
                        ? token.getText()
                        : null;
                var translated = SyntaxDiagnosticTranslator.translate(msg, sourceSnippet);
                errors.add(new SyntaxError(
                        translated.code(),
                        line,
                        charPositionInLine,
                        translated.message(),
                        sourceSnippet,
                        translated.help()
                ));
            }
        };

        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        var tree = parser.source();
        if (!errors.isEmpty()) {
            throw new ReLangSyntaxException(
                    errors,
                    source != null ? source.getName() : null,
                    source != null ? source.getCharacters().toString() : null
            );
        }
        return tree;
    }

    /**
     * Build Truffle nodes from a pre-parsed ANTLR tree.
     */
    public static Map<String, FunctionDescriptor> buildTruffleNodes(ReLang language, ReLangParser.SourceContext tree, Source source) {
        Map<String, FunctionDescriptor> functions = new HashMap<>();

        // Parse functions first (hoisting)
        for (var funcCtx : tree.function()) {
            switch (funcCtx) {
                case ReLangParser.FunctionBlockContext fb -> {
                    var functionName = fb.ID().getText();
                    var context = new ParseContext(source);
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
                    var context = new ParseContext(source);
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
        var mainContext = new ParseContext(source);
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

        var nowRoot = new ReLangBuiltinNowNode(language);
        functions.put("now", new FunctionDescriptor(nowRoot.getCallTarget(), List.of(), 0));

        var failureRoot = new ReLangBuiltinFailureNode(language);
        functions.put("Failure", new FunctionDescriptor(failureRoot.getCallTarget(), List.of("message"), 1));

        var jsonRoot = new ReLangBuiltinJsonNode(language);
        functions.put("json", new FunctionDescriptor(jsonRoot.getCallTarget(), List.of("text"), 1));

        return functions;
    }

    /**
     * Convenience method: ANTLR parse + build Truffle nodes in one call.
     */
    public static Map<String, FunctionDescriptor> parse(ReLang language, Source source) {
        var tree = parseAntlr(source);
        return buildTruffleNodes(language, tree, source);
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

    /** Set source section on a node and return it, for chaining. */
    private static ReLangNode withSourceSection(ParseContext context, org.antlr.v4.runtime.ParserRuleContext ruleCtx, ReLangNode node) {
        var ss = context.sourceSection(ruleCtx);
        if (ss != null) node.assignSourceSection(ss);
        return node;
    }

    private static ReLangNode parseBlock(ParseContext context, ReLangParser.BlockContext ctx) {
        List<ReLangNode> nodes = new ArrayList<>();
        for (var stmt : ctx.statement()) {
            nodes.add(parseStatement(context, stmt));
        }
        var node = new ReLangBlockNode(nodes.toArray(new ReLangNode[0]));
        return withSourceSection(context, ctx, node);
    }

    private static ReLangNode parseStatement(ParseContext context, ReLangParser.StatementContext ctx) {
        var node = switch (ctx) {
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
        return withSourceSection(context, ctx, node);
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
        var node = switch (ctx) {
            case ReLangParser.ExprBinaryContext bin -> parseBinaryExpr(context, bin);
            case ReLangParser.ExprIntContext intCtx -> new LongLiteralNode(Long.parseLong(intCtx.INT().getText().replace("_", "")));
            case ReLangParser.ExprFloatContext floatCtx -> new DoubleLiteralNode(Double.parseDouble(floatCtx.FLOAT().getText().replace("_", "")));
            case ReLangParser.ExprStringContext strCtx -> parseStringExpr(context, strCtx);
            case ReLangParser.ExprTrueContext ignored -> new BooleanLiteralNode(true);
            case ReLangParser.ExprFalseContext ignored -> new BooleanLiteralNode(false);
            case ReLangParser.ExprNoneContext ignored -> new NoneLiteralNode();
            case ReLangParser.ExprUnitContext ignored -> new UnitLiteralNode();
            case ReLangParser.ExprNegateContext neg -> NegateNodeGen.create(parseExpr(context, neg.expr()));
            case ReLangParser.ExprAwaitContext awaitCtx -> new ReLangAwaitNode(parseExpr(context, awaitCtx.expr()));
            case ReLangParser.ExprNotContext notCtx -> NotNodeGen.create(parseExpr(context, notCtx.expr()));
            case ReLangParser.ExprAndContext andCtx -> new LogicalAndNode(parseExpr(context, andCtx.left), parseExpr(context, andCtx.right));
            case ReLangParser.ExprOrContext orCtx -> new LogicalOrNode(parseExpr(context, orCtx.left), parseExpr(context, orCtx.right));
            case ReLangParser.ExprProductContext prod -> new ReLangProductNode(parseExpr(context, prod.left), parseExpr(context, prod.right));
            case ReLangParser.ExprParenContext paren -> parseExpr(context, paren.expr());
            case ReLangParser.ExprIdContext id -> ReLangReadLocalVarNodeGen.create(context.getSlot(id.ID().getText()));
            case ReLangParser.ExprCallContext call -> parseCallExpr(context, call);
            case ReLangParser.ExprFieldAccessContext fa -> new ReLangFieldAccessNode(parseExpr(context, fa.expr()), fa.ID().getText());
            case ReLangParser.ExprConstructContext con -> parseConstructExpr(context, con);
            case ReLangParser.ExprIfElseContext ifElse -> parseIfElseExpr(context, ifElse);
            case ReLangParser.ExprMatchSubjectContext matchCtx -> parseMatchSubject(context, matchCtx);
            case ReLangParser.ExprMatchSubjectlessContext matchCtx -> parseMatchSubjectless(context, matchCtx);
            case ReLangParser.ExprDurationContext durCtx -> parseDurationLiteral(durCtx);
            case ReLangParser.ExprBytesContext bytesCtx -> parseBytesLiteral(bytesCtx);
            case null -> throw new IllegalArgumentException("Expression context cannot be null");
            default -> throw new IllegalArgumentException("Unknown expr type: " + ctx.getText());
        };
        return withSourceSection(context, ctx, node);
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

    private static ReLangNode parseConstructExpr(ParseContext context, ReLangParser.ExprConstructContext con) {
        var typeName = con.ID().getText();
        var fieldNames = new ArrayList<String>();
        var fieldValues = new ArrayList<ReLangNode>();
        for (var fi : con.fieldInit()) {
            fieldNames.add(fi.ID().getText());
            fieldValues.add(parseExpr(context, fi.expr()));
        }
        return new ReLangConstructNode(typeName,
                fieldNames.toArray(new String[0]),
                fieldValues.toArray(new ReLangNode[0]));
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

    private static ReLangNode parseDurationLiteral(ReLangParser.ExprDurationContext ctx) {
        var text = ctx.DURATION().getText();
        long millis;
        if (text.endsWith("ms")) {
            millis = Long.parseLong(text.substring(0, text.length() - 2));
        } else if (text.endsWith("min")) {
            millis = Long.parseLong(text.substring(0, text.length() - 3)) * 60_000;
        } else if (text.endsWith("h")) {
            millis = Long.parseLong(text.substring(0, text.length() - 1)) * 3_600_000;
        } else if (text.endsWith("s")) {
            millis = Long.parseLong(text.substring(0, text.length() - 1)) * 1_000;
        } else {
            throw new RuntimeException("Invalid duration literal: " + text);
        }
        return new DurationLiteralNode(millis);
    }

    private static ReLangNode parseBytesLiteral(ReLangParser.ExprBytesContext ctx) {
        var raw = ctx.BYTES_LITERAL().getText();
        // Strip b" prefix and " suffix
        var inner = raw.substring(2, raw.length() - 1);
        var bytes = new java.io.ByteArrayOutputStream();
        int i = 0;
        while (i < inner.length()) {
            if (inner.charAt(i) == '\\' && i + 1 < inner.length()) {
                if (inner.charAt(i + 1) == 'x' && i + 3 < inner.length()) {
                    var hex = inner.substring(i + 2, i + 4);
                    bytes.write(Integer.parseInt(hex, 16));
                    i += 4;
                } else if (inner.charAt(i + 1) == '\\') {
                    bytes.write('\\');
                    i += 2;
                } else if (inner.charAt(i + 1) == '"') {
                    bytes.write('"');
                    i += 2;
                } else {
                    bytes.write(inner.charAt(i));
                    i++;
                }
            } else {
                bytes.write(inner.charAt(i));
                i++;
            }
        }
        return new BytesLiteralNode(bytes.toByteArray());
    }

    private static ReLangNode parseStringExpr(ParseContext context, ReLangParser.ExprStringContext ctx) {
        var raw = ctx.STRING().getText();
        // Remove surrounding quotes
        var inner = raw.substring(1, raw.length() - 1);

        // Check if there are any interpolation expressions (unescaped ${)
        if (!containsInterpolation(inner)) {
            return new StringLiteralNode(processStringLiteral(raw));
        }

        // Parse interpolation segments
        List<ReLangNode> segments = new ArrayList<>();
        var sb = new StringBuilder();
        int i = 0;
        while (i < inner.length()) {
            char c = inner.charAt(i);
            if (c == '\\' && i + 1 < inner.length()) {
                char next = inner.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i += 2; }
                    case 't' -> { sb.append('\t'); i += 2; }
                    case 'r' -> { sb.append('\r'); i += 2; }
                    case '\\' -> { sb.append('\\'); i += 2; }
                    case '"' -> { sb.append('"'); i += 2; }
                    case '$' -> { sb.append('$'); i += 2; }
                    default -> { sb.append(c); i++; }
                }
            } else if (c == '$' && i + 1 < inner.length() && inner.charAt(i + 1) == '{') {
                // Flush text segment
                if (!sb.isEmpty()) {
                    segments.add(new StringLiteralNode(sb.toString()));
                    sb.setLength(0);
                }
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
                    throw new RuntimeException("Unclosed interpolation in string at position " + i);
                }
                // Parse the expression inside ${}
                var exprText = inner.substring(start, j);
                var lexer = new ReLangLexer(CharStreams.fromString(exprText));
                var parser = new ReLangParser(new CommonTokenStream(lexer));
                var exprCtx = parser.expr();
                segments.add(parseExpr(context, exprCtx));
                i = j + 1; // skip closing brace
            } else {
                sb.append(c);
                i++;
            }
        }
        // Flush remaining text
        if (!sb.isEmpty()) {
            segments.add(new StringLiteralNode(sb.toString()));
        }

        if (segments.isEmpty()) {
            return new StringLiteralNode("");
        }
        if (segments.size() == 1) {
            return segments.get(0);
        }

        // Build concatenation tree with toString conversion
        ReLangNode result = ensureString(segments.get(0));
        for (int k = 1; k < segments.size(); k++) {
            result = AddNodeGen.create(result, ensureString(segments.get(k)));
        }
        return result;
    }

    private static boolean containsInterpolation(String inner) {
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '\\') {
                i++; // skip escaped character
            } else if (c == '$' && i + 1 < inner.length() && inner.charAt(i + 1) == '{') {
                return true;
            }
        }
        return false;
    }

    private static ReLangNode ensureString(ReLangNode node) {
        if (node instanceof StringLiteralNode) return node;
        return ToStringNodeGen.create(node);
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
