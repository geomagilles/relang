package com.relang.parser;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.relang.ReLang;
import com.relang.ReLangRootNode;
import com.relang.nodes.ReLangNode;
import com.relang.nodes.ReLangBlockNode;
import com.relang.nodes.LongLiteralNode;
import com.relang.nodes.AddNodeGen;
import com.relang.nodes.SubNodeGen;
import com.relang.nodes.MulNodeGen;
import com.relang.nodes.DivNodeGen;
import com.relang.nodes.LessThanNodeGen;
import com.relang.nodes.EqualsNodeGen;
import com.relang.nodes.ReLangIfNode;
import com.relang.nodes.ReLangWhileNode;
import com.relang.nodes.ReLangReadLocalVarNodeGen;
import com.relang.nodes.ReLangWriteLocalVarNodeGen;
import com.relang.nodes.ReLangReturnNode;
import com.relang.nodes.ReLangInvokeNode;
import com.relang.nodes.ReLangReadArgumentNode;
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

    public static Map<String, RootCallTarget> parse(ReLang language, Source source) {
        ReLangLexer lexer = new ReLangLexer(CharStreams.fromString(source.getCharacters().toString()));
        ReLangParser parser = new ReLangParser(new CommonTokenStream(lexer));

        ReLangParser.SourceContext tree = parser.source();

        Map<String, RootCallTarget> functions = new HashMap<>();

        // Parse functions first
        for (ReLangParser.FunctionContext funcCtx : tree.function()) {
            String functionName = funcCtx.ID().getText();
            ParseContext context = new ParseContext();

            List<ReLangNode> setupNodes = new ArrayList<>();
            // Handle parameters
            if (funcCtx.parameters() != null) {
                int argIndex = 0;
                for (org.antlr.v4.runtime.tree.TerminalNode paramId : funcCtx.parameters().ID()) {
                    String paramName = paramId.getText();
                    int slot = context.getSlot(paramName);

                    ReLangNode readArg = new ReLangReadArgumentNode(argIndex);
                    ReLangNode writeArg = ReLangWriteLocalVarNodeGen.create(readArg, slot);
                    setupNodes.add(writeArg);
                    argIndex++;
                }
            }

            ReLangNode body = parseBlock(context, funcCtx.block());

            ReLangNode fullBody;
            if (!setupNodes.isEmpty()) {
                List<ReLangNode> allNodes = new ArrayList<>(setupNodes);
                allNodes.add(body);
                fullBody = new ReLangBlockNode(allNodes.toArray(new ReLangNode[0]));
            } else {
                fullBody = body;
            }

            ReLangRootNode rootNode = new ReLangRootNode(language, context.frameBuilder.build(), fullBody);
            functions.put(functionName, rootNode.getCallTarget());
        }

        // Parse main body (top-level statements)
        ParseContext mainContext = new ParseContext();
        List<ReLangNode> mainNodes = new ArrayList<>();
        if (tree.command().isEmpty()) {
            // If no main code, minimal dummy
            mainNodes.add(new LongLiteralNode(0));
        } else {
            for (ReLangParser.CommandContext cmd : tree.command()) {
                mainNodes.add(parseStatement(mainContext, cmd.statement()));
            }
        }

        ReLangNode mainBody = new ReLangBlockNode(mainNodes.toArray(new ReLangNode[0]));
        ReLangRootNode mainRoot = new ReLangRootNode(language, mainContext.frameBuilder.build(), mainBody);

        functions.put("main", mainRoot.getCallTarget());
        return functions;
    }

    private static ReLangNode parseBlock(ParseContext context, ReLangParser.BlockContext ctx) {
        List<ReLangNode> nodes = new ArrayList<>();
        for (ReLangParser.StatementContext stmt : ctx.statement()) {
            nodes.add(parseStatement(context, stmt));
        }
        return new ReLangBlockNode(nodes.toArray(new ReLangNode[0]));
    }

    private static ReLangNode parseStatement(ParseContext context, ReLangParser.StatementContext ctx) {
        if (ctx instanceof ReLangParser.StatementExprContext) {
            return parseExpr(context, ((ReLangParser.StatementExprContext) ctx).expr());
        } else if (ctx instanceof ReLangParser.StatementAssignmentContext) {
            return parseAssignment(context, ((ReLangParser.StatementAssignmentContext) ctx).assignment());
        } else if (ctx instanceof ReLangParser.StatementIfContext) {
            return parseIf(context, (ReLangParser.StatementIfContext) ctx);
        } else if (ctx instanceof ReLangParser.StatementWhileContext) {
            return parseWhile(context, (ReLangParser.StatementWhileContext) ctx);
        } else if (ctx instanceof ReLangParser.StatementReturnContext) {
            return new ReLangReturnNode(parseExpr(context, ((ReLangParser.StatementReturnContext) ctx).expr()));
        }
        throw new RuntimeException("Unknown statement type: " + ctx.getClass().getSimpleName());
    }

    private static ReLangNode parseIf(ParseContext context, ReLangParser.StatementIfContext ctx) {
        ReLangNode condition = parseExpr(context, ctx.expr());
        ReLangNode thenPart = parseBlock(context, ctx.block(0));
        ReLangNode elsePart = null;
        if (ctx.block().size() > 1) {
            elsePart = parseBlock(context, ctx.block(1));
        }
        return new ReLangIfNode(condition, thenPart, elsePart);
    }

    private static ReLangNode parseWhile(ParseContext context, ReLangParser.StatementWhileContext ctx) {
        ReLangNode condition = parseExpr(context, ctx.expr());
        ReLangNode body = parseBlock(context, ctx.block());
        return new ReLangWhileNode(condition, body);
    }

    private static ReLangNode parseAssignment(ParseContext context, ReLangParser.AssignmentContext ctx) {
        String varName = ctx.ID().getText();
        ReLangNode valueNode = parseExpr(context, ctx.expr());
        int slot = context.getSlot(varName);
        return ReLangWriteLocalVarNodeGen.create(valueNode, slot);
    }

    private static ReLangNode parseExpr(ParseContext context, ReLangParser.ExprContext ctx) {
        if (ctx instanceof ReLangParser.ExprBinaryContext) {
            ReLangParser.ExprBinaryContext bin = (ReLangParser.ExprBinaryContext) ctx;
            ReLangNode left = parseExpr(context, bin.left);
            ReLangNode right = parseExpr(context, bin.right);

            String op = bin.op.getText();
            if (op.equals("+")) {
                return AddNodeGen.create(left, right);
            } else if (op.equals("-")) {
                return SubNodeGen.create(left, right);
            } else if (op.equals("*")) {
                return MulNodeGen.create(left, right);
            } else if (op.equals("/")) {
                return DivNodeGen.create(left, right);
            } else if (op.equals("<")) {
                return LessThanNodeGen.create(left, right);
            } else if (op.equals("==")) {
                return EqualsNodeGen.create(left, right);
            }
        } else if (ctx instanceof ReLangParser.ExprIntContext) {
            long val = Long.parseLong(((ReLangParser.ExprIntContext) ctx).INT().getText());
            return new LongLiteralNode(val);
        } else if (ctx instanceof ReLangParser.ExprDidContext) {
            return parseExpr(context, ((ReLangParser.ExprDidContext) ctx).expr());
        } else if (ctx instanceof ReLangParser.ExprIdContext) {
            String varName = ((ReLangParser.ExprIdContext) ctx).ID().getText();
            int slot = context.getSlot(varName);
            return ReLangReadLocalVarNodeGen.create(slot);
        } else if (ctx instanceof ReLangParser.ExprCallContext) {
            ReLangParser.ExprCallContext call = (ReLangParser.ExprCallContext) ctx;
            String funcName = call.ID().getText();
            List<ReLangNode> args = new ArrayList<>();
            if (call.arguments() != null) {
                for (ReLangParser.ExprContext argExpr : call.arguments().expr()) {
                    args.add(parseExpr(context, argExpr));
                }
            }
            return new ReLangInvokeNode(funcName, args.toArray(new ReLangNode[0]));
        }

        throw new RuntimeException("Unknown expr type: " + ctx.getText());
    }
}
