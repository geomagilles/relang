package com.relang.diagnostics;

import com.oracle.truffle.api.source.Source;
import com.relang.parser.ReLangSyntaxException;
import com.relang.parser.ReLangTruffleParser;
import com.relang.parser.ReLangTypeChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Diagnostic Scenario Matrix")
class DiagnosticScenarioMatrixTest {

    private static final List<Scenario> SCENARIOS = List.of(
            Scenario.exact("undefined variable", "x = 1;", "RL2002"),
            Scenario.exact("arity too few", "fn add(a: Int, b: Int): Int = a + b; add(1);", "RL2003"),
            Scenario.exact("arity too many", "fn add(a: Int, b: Int): Int = a + b; add(1, 2, 3);", "RL2003"),
            Scenario.exact("missing type annotation", "fn id(x): Int = x; id(1);", "RL2005"),
            Scenario.exact("unknown function", "oops(1);", "RL2004"),
            Scenario.exact("argument mismatch", "fn f(x: Int): Int = x; f(\"s\");", "RL2001"),
            Scenario.exact("return mismatch", "fn f(): Int = \"x\"; f();", "RL2001"),
            Scenario.exact("invalid negate", "fn f(): Int = -true; f();", "RL2006"),
            Scenario.exact("invalid not", "fn f(): Bool = not 1; f();", "RL2006"),
            Scenario.exact("invalid plus", "fn f(): Int = true + false; f();", "RL2006"),
            Scenario.exact("invalid minus", "fn f(): Int = \"a\" - \"b\"; f();", "RL2006"),
            Scenario.exact("invalid modulo", "fn f(): Int = 1.2 % 1.1; f();", "RL2006"),
            Scenario.exact("invalid ordering", "fn f(): Bool = true < 1; f();", "RL2006"),
            Scenario.exact("invalid equality", "fn f(): Bool = 1 == \"x\"; f();", "RL2006"),
            Scenario.exact("invalid and", "fn f(): Bool = 1 and true; f();", "RL2006"),
            Scenario.exact("invalid or", "fn f(): Bool = false or 1; f();", "RL2006"),
            Scenario.exact("invalid await", "await 1;", "RL2008"),
            Scenario.exact("unknown construct type", "User{name: \"a\"};", "RL2009"),
            Scenario.exact("unknown field access", "type User { name: String } User{name: \"a\"}.age;", "RL2010"),
            Scenario.exact("unknown field in constructor", "type User { name: String } User{age: 1};", "RL2010"),
            Scenario.exact("missing field in constructor", "type User { name: String, age: Int } User{name: \"a\"};", "RL2011"),
            Scenario.exact("field type mismatch", "type User { age: Int } User{age: \"x\"};", "RL2001"),
            Scenario.exact("declared assignment mismatch", "let x: Int = \"x\";", "RL2001"),
            Scenario.exact("assignment mismatch", "let x: Int = 1; x = \"x\";", "RL2001"),
            Scenario.exact("immutable parameter", "fn f(x: Int): Int { x = 2; return x; } f(1);", "RL2012"),
            Scenario.exact("invalid if condition", "if 1 { 1; }", "RL2013"),
            Scenario.exact("invalid while condition", "while \"x\" { break; }", "RL2013"),
            Scenario.exact("invalid subjectless match pattern", "match { 1 -> 1 };", "RL2015"),
            Scenario.exact("invalid for-range start", "for i in 1.0..2 { i; }", "RL2001"),
            Scenario.exact("invalid for-range end", "for i in 1..\"2\" { i; }", "RL2001"),
            Scenario.exact("unknown named argument", "fn add(a: Int, b: Int): Int = a + b; add(a: 1, c: 2);", "RL2016"),
            Scenario.exact("positional after named argument", "fn add(a: Int, b: Int): Int = a + b; add(a: 1, 2);", "RL2016"),
            Scenario.exact("break outside loop", "break;", "RL2017"),
            Scenario.exact("continue outside loop", "continue;", "RL2017"),
            Scenario.exact("out of scope variable", "if true { let scoped = 1; } scoped;", "RL2018"),
            Scenario.exact("unclosed interpolation", "let s = \"x ${1\";", "RL2014"),
            Scenario.prefix("syntax missing expression", "let x =", "RL10"),
            Scenario.prefix("syntax missing parenthesis", "fn f(a: Int { return a; }", "RL10"),
            Scenario.prefix("syntax missing rhs expression", "fn main(): Int = ;", "RL10")
    );

    private static Stream<Scenario> scenarios() {
        return SCENARIOS.stream();
    }

    private static ReLangDiagnostic firstDiagnostic(String sourceText) {
        var source = Source.newBuilder("relang", sourceText, "scenario.re").build();
        try {
            var tree = ReLangTruffleParser.parseAntlr(source);
            var errors = new ReLangTypeChecker().check(tree);
            assertFalse(errors.isEmpty(), "Expected at least one type diagnostic");
            return errors.getFirst().toDiagnostic();
        } catch (ReLangSyntaxException syntaxException) {
            var diagnostics = syntaxException.getDiagnostics();
            if (diagnostics.isEmpty()) {
                fail("Syntax exception without diagnostics");
            }
            return diagnostics.getFirst();
        }
    }

    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("scenarios")
    @DisplayName("first diagnostic matches expected code and includes help")
    void matrixCoverage(Scenario scenario) {
        var diagnostic = firstDiagnostic(scenario.source());
        if (scenario.exactMatch()) {
            assertEquals(diagnostic.code().value(), scenario.expectedCode(),
                    () -> "Expected " + scenario.expectedCode() + " but got " + diagnostic.code().value()
                            + " for source: " + scenario.name());
        } else {
            assertTrue(
                    diagnostic.code().value().startsWith(scenario.expectedCode()),
                    () -> "Expected prefix " + scenario.expectedCode() + " but got " + diagnostic.code().value()
                            + " for source: " + scenario.name()
            );
        }
        assertTrue(
                diagnostic.help() != null && !diagnostic.help().isBlank(),
                () -> "Expected help text for " + scenario.name() + " (" + diagnostic.code().value() + ")"
        );
    }

    @Test
    @DisplayName("matrix keeps at least 30 common error scenarios")
    void hasAtLeastThirtyScenarios() {
        assertTrue(SCENARIOS.size() >= 30, "Expected at least 30 scenarios, got: " + SCENARIOS.size());
    }

    @Test
    @DisplayName("help coverage is at least 80 percent for the scenario matrix")
    void helpCoverage() {
        var withHelp = SCENARIOS.stream()
                .map(Scenario::source)
                .map(DiagnosticScenarioMatrixTest::firstDiagnostic)
                .filter(diagnostic -> diagnostic.help() != null && !diagnostic.help().isBlank())
                .count();
        var coverage = (double) withHelp / SCENARIOS.size();
        assertTrue(coverage >= 0.80, "Expected help coverage >= 0.80, got: " + coverage);
    }

    private record Scenario(String name, String source, String expectedCode, boolean exactMatch) {
        static Scenario exact(String name, String source, String expectedCode) {
            return new Scenario(name, source, expectedCode, true);
        }

        static Scenario prefix(String name, String source, String codePrefix) {
            return new Scenario(name, source, codePrefix, false);
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
