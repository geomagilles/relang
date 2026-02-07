package com.relang;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.ProvidedTags;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;
import com.relang.nodes.AwaitableTable;
import com.relang.nodes.ReLangMetaType;
import com.relang.nodes.ReLangSuspendException;
import com.relang.nodes.ResumableState;
import com.relang.nodes.SuspendedResult;
import com.relang.diagnostics.InlineDiagnosticRenderer;
import com.relang.parser.ReLangDiagnosticTruffleException;
import com.relang.parser.ReLangSyntaxException;
import com.relang.parser.ReLangTypeCheckException;
import com.relang.parser.TypeError;

import java.util.Map;

@TruffleLanguage.Registration(id = "relang", name = "ReLang", defaultMimeType = "application/x-relang", characterMimeTypes = "application/x-relang")
@ProvidedTags({StandardTags.ExpressionTag.class, StandardTags.StatementTag.class,
               StandardTags.RootBodyTag.class, StandardTags.RootTag.class,
               StandardTags.CallTag.class})
public final class ReLang extends TruffleLanguage<ReLangContext> {

    @Override
    protected ReLangContext createContext(Env env) {
        return new ReLangContext(this, env);
    }

    @Override
    protected Object getLanguageView(ReLangContext context, Object value) {
        if (value instanceof Long) return new ReLangLanguageView(value, ReLangMetaType.INT);
        if (value instanceof Double) return new ReLangLanguageView(value, ReLangMetaType.FLOAT);
        if (value instanceof Boolean) return new ReLangLanguageView(value, ReLangMetaType.BOOL);
        if (value instanceof String) return new ReLangLanguageView(value, ReLangMetaType.STRING);
        return value;
    }

    @Override
    protected CallTarget parse(ParsingRequest request) throws Exception {
        var lspRequest = LspDiagnosticsHelper.isLspRequest();
        var sourceCode = request.getSource().getCharacters().toString();
        var sourceHash = ResumableState.computeSourceHash(sourceCode);

        // Step 1: ANTLR parse
        final com.relang.parser.ReLangParser.SourceContext tree;
        try {
            tree = com.relang.parser.ReLangTruffleParser.parseAntlr(request.getSource());
        } catch (ReLangSyntaxException syntaxException) {
            if (lspRequest) {
                throw toTruffleSyntaxError(request, syntaxException);
            }
            throw syntaxException;
        }

        // Step 2: Type check
        var checker = new com.relang.parser.ReLangTypeChecker();
        var typeErrors = checker.check(tree);
        if (!typeErrors.isEmpty()) {
            if (lspRequest) {
                throw toTruffleTypeError(request, typeErrors);
            }
            throw new ReLangTypeCheckException(typeErrors, request.getSource().getName(), sourceCode);
        }

        // Step 3: Build Truffle nodes
        Map<String, FunctionDescriptor> functions = com.relang.parser.ReLangTruffleParser.buildTruffleNodes(this, tree, request.getSource());
        ReLangContext context = ReLangContext.get(null);
        context.getFunctionRegistry().putAll(functions);
        context.setCurrentSourceHash(sourceHash);

        // Verify source hash if resuming - fail fast if code changed
        ResumableState resumeState = context.getResumptionState();
        if (resumeState != null && resumeState.getSourceHash() != null) {
            resumeState.verifySourceHash(sourceCode);
        }

        // Get the entry point (main body or main function)
        FunctionDescriptor entryDesc = functions.containsKey("") ? functions.get("") : functions.get("main");

        // Wrap in a top-level node that catches ReLangSuspendException
        return new TopLevelRootNode(this, entryDesc.callTarget()).getCallTarget();
    }

    private static RuntimeException toTruffleSyntaxError(ParsingRequest request, ReLangSyntaxException syntaxException) {
        var firstError = syntaxException.getErrors().getFirst();
        var message = InlineDiagnosticRenderer.render(firstError.toDiagnostic());
        return toTruffleDiagnostic(request, firstError.line(), firstError.column(), firstError.sourceSnippet(), message);
    }

    private static RuntimeException toTruffleTypeError(ParsingRequest request, java.util.List<TypeError> typeErrors) {
        var firstError = typeErrors.getFirst();
        var message = InlineDiagnosticRenderer.render(firstError.toDiagnostic());
        return toTruffleDiagnostic(request, firstError.line(), firstError.column(), firstError.sourceSnippet(), message);
    }

    private static RuntimeException toTruffleDiagnostic(
            ParsingRequest request,
            int line,
            int column,
            String sourceSnippet,
            String message
    ) {
        var section = sourceSectionFor(request, line, column, sourceSnippet);
        return new ReLangDiagnosticTruffleException(message, new ErrorLocationNode(section));
    }

    private static SourceSection sourceSectionFor(ParsingRequest request, int line, int column, String sourceSnippet) {
        var source = request.getSource();
        var lineCount = source.getLineCount();
        if (lineCount <= 0) {
            return null;
        }

        var oneBasedLine = Math.max(1, Math.min(line, lineCount));
        var lineStart = source.getLineStartOffset(oneBasedLine);
        var lineLength = source.getLineLength(oneBasedLine);

        var zeroBasedColumn = Math.max(0, column);
        var startIndex = lineStart + Math.min(zeroBasedColumn, lineLength);
        var length = Math.max(1, sourceSnippet != null ? sourceSnippet.length() : 1);
        var maxLength = source.getLength() - startIndex;
        if (maxLength <= 0) {
            return null;
        }
        return source.createSection(startIndex, Math.min(length, maxLength));
    }

    private static final class ErrorLocationNode extends Node {
        private final SourceSection sourceSection;

        private ErrorLocationNode(SourceSection sourceSection) {
            this.sourceSection = sourceSection;
        }

        @Override
        public SourceSection getSourceSection() {
            return sourceSection;
        }
    }

    /**
     * Top-level wrapper that catches ReLangSuspendException and converts it to SuspendedResult.
     */
    private static class TopLevelRootNode extends RootNode {
        private final RootCallTarget entryPoint;

        TopLevelRootNode(ReLang language, RootCallTarget entryPoint) {
            super(language, FrameDescriptor.newBuilder().build());
            this.entryPoint = entryPoint;
        }

        @Override
        public Object execute(VirtualFrame frame) {
            ReLangContext context = ReLangContext.get(this);

            // On resume, restore the awaitable table from the saved state
            ResumableState resumeState = context.getResumptionState();
            if (resumeState != null && resumeState.getAwaitableTable() != null) {
                context.restoreAwaitableTable(resumeState.getAwaitableTable());
            }

            try {
                return entryPoint.call();
            } catch (ReLangSuspendException e) {
                // Set the source hash on the state for validation on resume
                e.getState().setSourceHash(context.getCurrentSourceHash());

                // Persist the awaitable table in the state
                AwaitableTable table = context.getAwaitableTable();
                if (!table.isEmpty()) {
                    e.getState().setAwaitableTable(table);
                }

                // Record which awaitable caused the suspension (if any)
                if (e.getAwaitedHandleId() != null) {
                    e.getState().setAwaitedHandleId(e.getAwaitedHandleId());
                }

                // Convert exception to SuspendedResult for the host
                return context.getEnv().asGuestValue(new SuspendedResult(e.getState()));
            }
        }
    }
}
