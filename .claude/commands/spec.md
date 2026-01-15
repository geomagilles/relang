# Spec Command

Create a detailed technical specification for a new feature in the ReLang programming language.

**Feature request:** $ARGUMENTS

## Process

1. **Review documentation** to understand project standards:
    - `/CLAUDE.md` - Project overview and architecture
    - Existing AST nodes in `src/main/java/com/relang/nodes/`
    - Grammar file at `src/main/antlr/com/relang/parser/ReLang.g4`

2. **Read the codebase** to understand existing patterns:
    - Explore similar node types in `nodes/` package
    - Check how resumability is implemented in existing nodes
    - Review parser in `ReLangTruffleParser.java`
    - Check existing tests in `src/test/java/com/relang/`

3. **Create specification with sensible defaults:**
    - **Node Pattern**: Extend `ReLangNode` with `executeGeneric(VirtualFrame, ResumableState)`
    - **Resumability**: Implement state capture in `FrameState` if node can suspend
    - **Type System**: Use `long` and `boolean` types consistently
    - **Testing**: Use JUnit 5 with GraalVM Polyglot Context

4. **Write specification to `/docs/specs/spec_[feature_name].md`** containing:
   ```markdown
   # Feature: [Name]

   ## Overview
   Brief description and purpose in the ReLang language context

   ## Language Integration

   ### Syntax
   - **Grammar addition**: ANTLR rule to add
   - **Example usage**:
   ```relang
   // Example code using the new feature
   ```

   ### AST Node
   - **Node Name**: `ReLang[Feature]Node`
   - **Location**: `src/main/java/com/relang/nodes/`
   - **Parent Class**: `ReLangNode` or appropriate base
   - **Resumability**: Does this node need checkpoint support?

   ## Implementation Files

   ### Grammar
   - `src/main/antlr/com/relang/parser/ReLang.g4` - Grammar changes

   ### Parser
   - `src/main/java/com/relang/parser/ReLangTruffleParser.java` - Visitor changes

   ### Nodes
   - `src/main/java/com/relang/nodes/ReLang[Feature]Node.java` - New node

   ### Tests
   - `src/test/java/com/relang/ReLang[Feature]Test.java` - Feature tests

   ## Node Implementation Details

   ### Execute Method
   ```java
   @Override
   public Object executeGeneric(VirtualFrame frame, ResumableState state) {
       // Implementation details
   }
   ```

   ### Resumability Support
   - Does this node need to save/restore state?
   - What state needs to be captured in `FrameState`?
   - How does unwinding/rewinding work for this node?

   ## Type Handling
   - Input types expected
   - Output type produced
   - Type coercion rules if applicable

   ## Testing Checklist
   - [ ] Basic functionality tests
   - [ ] Edge case tests
   - [ ] Checkpoint/resume tests (if resumable)
   - [ ] Error handling tests
   - [ ] Integration with existing features

   ## Open Questions
   - [Only if genuinely ambiguous]
   ```

5. **Present the specification** to the user with:
    - Summary of the proposed solution
    - Key decisions made (and why)
    - Any questions (only if truly needed)
    - Ask: "Should I proceed with implementation?"

## Best Practice Defaults

**When in doubt:**

- Extend `ReLangNode` and implement `executeGeneric(VirtualFrame, ResumableState)`
- Support resumability if the node can cause suspension (calls functions, loops, etc.)
- Use `@Child` annotation for child nodes, `@Children` for arrays
- Follow existing naming conventions: `ReLang[Feature]Node`
- Add grammar rules that integrate cleanly with existing expression/statement hierarchy
- Return `long` for numeric operations, `boolean` for comparisons
- Test with both regular execution and checkpoint/resume scenarios

**Only ask questions when:**

- Business logic is fundamentally ambiguous
- Multiple architectural approaches have significant tradeoffs
- The request conflicts with existing patterns
- The feature significantly changes the execution model
