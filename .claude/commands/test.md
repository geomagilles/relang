# Test Command

Please create comprehensive tests for: $ARGUMENTS

## ReLang Tests (JUnit 5)

**Location & Naming:**
- Location: `src/test/java/com/relang/`
- Run: `./gradlew test`
- Debug: `./gradlew test --tests "com.relang.YourTestClass" --info`

**Basic test structure:**
```java
package com.relang;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class YourFeatureTest {

    private Context context;

    @BeforeEach
    void setUp() {
        context = Context.newBuilder()
            .option("engine.WarnInterpreterOnly", "false")
            .build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void shouldExecuteSimpleExpression() {
        Value result = context.eval("relang", "1 + 2;");
        assertEquals(3L, result.asLong());
    }

    @Test
    void shouldHandleVariableAssignment() {
        Value result = context.eval("relang", "x = 10; y = 20; x + y;");
        assertEquals(30L, result.asLong());
    }
}
```

**Test categories:**
- Arithmetic: `ReLangTest.java` (add, sub, mul, div, precedence)
- Variables: `ReLangTest.java` (assignment, reassignment, scope)
- Control flow: `ReLangTest.java` (if/else, while loops)
- Functions: `ReLangTest.java` (declaration, calls, recursion)
- Resumability: `ReLangResumabilityTest.java` (checkpoint, suspend, resume)

---

## Checkpoint/Resume Testing

**Test checkpoint suspension:**
```java
@Test
void shouldSuspendAtCheckpoint() {
    String code = """
        x = 10;
        checkpoint;
        x = 20;
        x;
        """;

    Value result = context.eval("relang", code);

    // Result should be a SuspendedResult containing the state
    assertTrue(result.hasMembers());
    Value state = result.getMember("state");
    assertNotNull(state);
}
```

**Test resume from checkpoint:**
```java
@Test
void shouldResumeFromCheckpoint() {
    String code = """
        x = 10;
        checkpoint;
        x = x + 5;
        x;
        """;

    // First execution - suspends
    Value suspended = context.eval("relang", code);
    Value state = suspended.getMember("state");

    // Resume execution
    context.getBindings("relang").putMember("resumeState", state);
    Value result = context.eval("relang", code);

    assertEquals(15L, result.asLong());
}
```

**Test nested function checkpoint:**
```java
@Test
void shouldResumeAcrossFunctionCalls() {
    String code = """
        fn inner() {
            checkpoint;
            return 42;
        }
        fn outer() {
            x = inner();
            return x + 1;
        }
        outer();
        """;

    // First execution - suspends in inner()
    Value suspended = context.eval("relang", code);
    Value state = suspended.getMember("state");

    // Resume - should continue and return 43
    context.getBindings("relang").putMember("resumeState", state);
    Value result = context.eval("relang", code);

    assertEquals(43L, result.asLong());
}
```

---

## Helper Methods

**Create reusable test helpers:**
```java
private void assertEval(String code, long expected) {
    Value result = context.eval("relang", code);
    assertEquals(expected, result.asLong());
}

private void assertEval(String code, boolean expected) {
    Value result = context.eval("relang", code);
    assertEquals(expected, result.asBoolean());
}

private Value evalWithResume(String code) {
    Value result = context.eval("relang", code);
    if (result.hasMembers() && result.hasMember("state")) {
        // Suspended - resume
        context.getBindings("relang").putMember("resumeState", result.getMember("state"));
        return context.eval("relang", code);
    }
    return result;
}
```

---

## Best Practices

**Test structure:**
- Use descriptive names: `shouldDoSomethingWhenCondition`
- One assertion per test when practical
- Arrange-Act-Assert pattern
- Use `@BeforeEach` for context setup, `@AfterEach` for cleanup

**What to test:**
- Happy path execution
- Edge cases (empty blocks, zero values, max values)
- Error scenarios (division by zero, undefined variables)
- Checkpoint/resume state preservation
- Frame state across function boundaries

**What to avoid:**
- Testing ANTLR parser internals
- Testing Truffle framework behavior
- Brittle tests dependent on implementation details

**Test checklist:**
- [ ] Basic functionality works
- [ ] Edge cases handled
- [ ] Checkpoint suspends correctly
- [ ] Resume restores state correctly
- [ ] Nested function calls preserve state
- [ ] Loop iteration state preserved

**Run tests:**
```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "com.relang.ReLangTest"

# Specific test method
./gradlew test --tests "com.relang.ReLangTest.testArithmetic"

# With debug output
./gradlew test --tests "YourTest" --info --stacktrace
```
