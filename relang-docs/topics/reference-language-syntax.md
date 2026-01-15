# Language Syntax Reference

Complete syntax reference for the ReLang programming language.

## Lexical Elements

### Comments

ReLang currently does not support comments.

### Identifiers

Identifiers start with a letter or underscore, followed by letters, digits, or underscores:

```
x
myVariable
_private
counter1
```

### Keywords

```
fn       if       else     while    return   checkpoint
true     false
```

### Literals

#### Integer Literals

Decimal integers:

```
0
42
1000000
```

#### Boolean Literals

```
true
false
```

## Types

ReLang has two primitive types:

| Type | Description | Examples |
|------|-------------|----------|
| `long` | 64-bit signed integer | `0`, `42`, `-1` |
| `boolean` | Boolean value | `true`, `false` |

## Expressions

### Arithmetic Operators

| Operator | Description | Example | Result |
|----------|-------------|---------|--------|
| `+` | Addition | `3 + 2` | `5` |
| `-` | Subtraction | `3 - 2` | `1` |
| `*` | Multiplication | `3 * 2` | `6` |
| `/` | Division | `7 / 2` | `3` |

Division is integer division (truncates toward zero).

### Comparison Operators

| Operator | Description | Example | Result |
|----------|-------------|---------|--------|
| `<` | Less than | `3 < 5` | `true` |
| `==` | Equal to | `3 == 3` | `true` |

### Operator Precedence

From highest to lowest:

1. `*`, `/` (multiplicative)
2. `+`, `-` (additive)
3. `<`, `==` (comparison)

Use parentheses to override:

```
(1 + 2) * 3    // 9, not 7
```

### Variable References

```
x
myVar
```

Variables must be assigned before use.

### Function Calls

```
functionName(arg1, arg2, ...)
```

Examples:

```
double(21)
add(1, 2)
process()
```

### Parenthesized Expressions

```
(expression)
```

## Statements

### Assignment

```
variable = expression;
```

Creates a new variable or updates an existing one:

```
x = 42;
y = x + 1;
x = x * 2;
```

### Expression Statement

Any expression followed by semicolon:

```
functionCall();
x + y;  // Result discarded
```

### If Statement

```
if (condition) {
    statements
}
```

With else:

```
if (condition) {
    statements
} else {
    statements
}
```

Example:

```
if (x < 10) {
    y = 1;
} else {
    y = 2;
}
```

### While Loop

```
while (condition) {
    statements
}
```

Example:

```
i = 0;
while (i < 10) {
    i = i + 1;
}
```

### Return Statement

```
return expression;
```

Returns a value from a function:

```
fn double(n) {
    return n * 2;
}
```

### Checkpoint Statement

```
checkpoint;
```

Suspends execution and captures state. See [How Resumability Works](explanation-resumability.md).

## Functions

### Function Definition

```
fn name(parameters) {
    statements
}
```

Parameters are comma-separated identifiers:

```
fn add(a, b) {
    return a + b;
}

fn greet() {
    return 42;
}
```

### Function Calls

```
name(arguments)
```

Arguments are comma-separated expressions:

```
add(1, 2)
double(x + 1)
greet()
```

## Program Structure

A ReLang program consists of:

1. **Function definitions** (optional)
2. **Top-level statements** (the main program)

```
// Function definitions
fn helper(x) {
    return x * 2;
}

fn compute(n) {
    return helper(n) + 1;
}

// Main program (top-level statements)
result = compute(10);
result
```

The value of the last expression is the program's result.

## Grammar (EBNF)

```ebnf
program     = { function } { statement } ;

function    = "fn" IDENTIFIER "(" [ parameters ] ")" block ;
parameters  = IDENTIFIER { "," IDENTIFIER } ;

block       = "{" { statement } "}" ;

statement   = assignment
            | ifStmt
            | whileStmt
            | returnStmt
            | checkpointStmt
            | exprStmt ;

assignment  = IDENTIFIER "=" expr ";" ;
ifStmt      = "if" "(" expr ")" block [ "else" block ] ;
whileStmt   = "while" "(" expr ")" block ;
returnStmt  = "return" expr ";" ;
checkpointStmt = "checkpoint" ";" ;
exprStmt    = expr ";" ;

expr        = term { ( "+" | "-" | "<" | "==" ) term } ;
term        = factor { ( "*" | "/" ) factor } ;
factor      = INTEGER
            | "true" | "false"
            | IDENTIFIER
            | IDENTIFIER "(" [ arguments ] ")"
            | "(" expr ")" ;

arguments   = expr { "," expr } ;

IDENTIFIER  = ( letter | "_" ) { letter | digit | "_" } ;
INTEGER     = digit { digit } ;
```

## See Also

- [Getting Started](tutorial-getting-started.md)
- [How Resumability Works](explanation-resumability.md)
