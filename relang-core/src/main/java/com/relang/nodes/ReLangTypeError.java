package com.relang.nodes;

import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;
import com.relang.diagnostics.DiagnosticCode;
import com.relang.diagnostics.DiagnosticCodes;
import com.relang.diagnostics.InlineDiagnosticRenderer;
import com.relang.diagnostics.ReLangDiagnostic;
import com.relang.diagnostics.SourceRange;

public class ReLangTypeError extends AbstractTruffleException {

    private final ReLangDiagnostic diagnostic;

    public ReLangTypeError(Node node, String message) {
        this(node, DiagnosticCodes.RUNTIME_INVALID_OPERATION, message, null);
    }

    public ReLangTypeError(Node node, RuntimeDiagnostics.Spec spec) {
        this(node, spec.code(), spec.message(), spec.help());
    }

    public ReLangTypeError(Node node, DiagnosticCode code, String message, String help) {
        this(buildDiagnostic(node, code, message, help), node);
    }

    public ReLangDiagnostic getDiagnostic() {
        return diagnostic;
    }

    private ReLangTypeError(ReLangDiagnostic diagnostic, Node location) {
        super(InlineDiagnosticRenderer.render(diagnostic), location);
        this.diagnostic = diagnostic;
    }

    private static ReLangDiagnostic buildDiagnostic(Node node, DiagnosticCode code, String message, String help) {
        var diagnostic = ReLangDiagnostic.error(code, message, rangeFor(node));
        if (help != null && !help.isBlank()) {
            diagnostic = diagnostic.withHelp(help);
        }
        return diagnostic;
    }

    private static SourceRange rangeFor(Node node) {
        if (node == null) {
            return SourceRange.point(1, 0);
        }
        SourceSection section = node.getSourceSection();
        if (section == null || !section.isAvailable()) {
            return SourceRange.point(1, 0);
        }
        var startLine = Math.max(1, section.getStartLine());
        var endLine = Math.max(startLine, section.getEndLine());
        var startColumn = Math.max(0, section.getStartColumn() - 1);
        var endColumn = Math.max(startColumn + 1, section.getEndColumn());
        return new SourceRange(startLine, startColumn, endLine, endColumn);
    }
}
