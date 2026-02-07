package com.relang.diagnostics;

import com.oracle.truffle.api.source.Source;
import com.relang.parser.ReLangTruffleParser;
import com.relang.parser.ReLangTypeChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DisplayName("Primitive Diagnostic Snapshots")
class PrimitiveDiagnosticSnapshotsTest {

    @Test
    @DisplayName("Snapshot: declared assignment type mismatch")
    void declaredAssignmentTypeMismatchSnapshot() {
        var rendered = renderFirstTypeDiagnostic("let x: Int = \"oops\";");
        assertEquals("""
                error[RL2001]: Cannot assign String to variable of declared type Int
                 --> snapshot.re:1:1
                help: Use a value compatible with Int.""", rendered);
    }

    @Test
    @DisplayName("Snapshot: non-bool condition")
    void nonBoolConditionSnapshot() {
        var rendered = renderFirstTypeDiagnostic("if 1 { 1; }");
        assertEquals("""
                error[RL2013]: Condition must be Bool, got Int
                 --> snapshot.re:1:1
                help: Change the `if` condition to a Bool expression.""", rendered);
    }

    @Test
    @DisplayName("Snapshot: rejected primitive operator coercion")
    void invalidPrimitiveOperatorSnapshot() {
        var rendered = renderFirstTypeDiagnostic("fn f(): Int = \"a\" + 1; f();");
        assertEquals("""
                error[RL2006]: Operator '+' cannot be applied to String and Int
                 --> snapshot.re:1:15
                help: Use two numeric operands or two String operands.""", rendered);
    }

    private static String renderFirstTypeDiagnostic(String sourceText) {
        var source = Source.newBuilder("relang", sourceText, "snapshot.re").build();
        var tree = ReLangTruffleParser.parseAntlr(source);
        var errors = new ReLangTypeChecker().check(tree);
        assertFalse(errors.isEmpty(), "Expected at least one type diagnostic");
        return CliDiagnosticRenderer.renderSingle(errors.getFirst().toDiagnostic(), "snapshot.re", null);
    }
}
