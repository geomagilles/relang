# Language Syntax Reference (v0.1)

This reference summarizes the canonical v0.1 syntax from `/Users/gilles/dev/relang/specs/v0.1/relang-spec-v0.1-canonique.md`.

## Keywords

`import`, `from`, `type`, `sealed`, `fn`, `let`, `if`, `else`, `match`, `for`, `in`, `while`, `break`, `continue`, `return`, `await`, `spawn`

## Core Declarations

```relang
import PaymentsClient from "payments"

type Order { id: String, total: Int }
sealed PaymentState

fn process(order: Order): Receipt | Failure {
  let charged = await charge(order)
  match charged {
    r: Receipt -> r
    f: Failure -> f
  }
}
```

Rules:

- declarations are top-level
- functions are top-level (`fn`)
- no executable top-level statements

## Function Forms

```relang
fn add(a: Int, b: Int): Int = a + b

fn normalize(value: Int): Int {
  if value < 0 { -value } else { value }
}
```

## Statements

- `let Pattern = Expr`
- `LValue = Expr`
- `return Expr`
- expression statements
- `if` / `match`
- `for` / `while`
- `break` / `continue`

## Expressions

- `await Expr` and `await Expr!`
- `spawn CallExpr`
- arithmetic/comparison/logical operators
- coordination operators (`and`, `or`) for awaitables
- product (`&`) and union (`|`) type/value forms
- list literals (`[a, b]`)

## Type Forms

- simple: `Int`, `String`, `Failure`, ...
- optional: `T?`
- list: `[T]`
- map: `Map<K, V>`
- product: `A & B`
- union: `A | B`

## Canonical Minimal EBNF

```ebnf
Program        = { Decl } ;
Decl           = TypeDecl | SealedDecl | FnDecl | ImportDecl ;

ImportDecl      = "import" ImportSpec "from" ModuleRef ;
ImportSpec      = "*" | "*" "as" Ident | Ident { "," Ident } ;

TypeDecl        = "type" Ident "{" { FieldDecl } "}" ;
SealedDecl      = "sealed" Ident ;
FieldDecl       = Ident ":" Type [ "=" Expr ] ;

FnDecl          = { Annotation } "fn" Ident "(" [ Params ] ")" [ ":" Type ] FnBody ;
Params          = Param { "," Param } ;
Param           = Ident ":" Type [ "=" Expr ] ;
FnBody          = Block | "=" Expr ;

Block           = "{" { Stmt } [ Expr ] "}" ;
Stmt            = LetStmt | AssignStmt | ReturnStmt | ExprStmt | IfStmt | MatchStmt
                | ForStmt | WhileStmt | BreakStmt | ContinueStmt ;

LetStmt         = "let" Pattern "=" Expr ;
AssignStmt      = LValue "=" Expr ;
ReturnStmt      = "return" Expr ;
ExprStmt        = Expr ;

IfStmt          = "if" Expr Block [ "else" Block ] ;
MatchStmt       = "match" Expr "{" { MatchArm } "}" ;
MatchArm        = Pattern "->" (Block | Expr) ;

ForStmt         = "for" Pattern "in" Expr Block ;
WhileStmt       = "while" Expr Block ;

Expr            = AwaitExpr | SpawnExpr | BinaryExpr | UnaryExpr | PrimaryExpr ;
AwaitExpr       = "await" Expr [ "!" ] ;
SpawnExpr       = "spawn" CallExpr ;
CallExpr        = PrimaryExpr "(" [ Args ] ")" ;
Args            = Arg { "," Arg } ;
Arg             = [ Ident ":" ] Expr ;

PrimaryExpr     = Literal | Ident | "(" Expr ")" | TupleExpr | ListExpr | MatchExpr | IfExpr ;
TupleExpr       = "(" Expr "&" Expr { "&" Expr } ")" ;
ListExpr        = "[" [ Expr { "," Expr } ] "]" ;

Type            = SimpleType | OptionalType | ListType | MapType | ProductType | UnionType ;
SimpleType      = Ident ;
OptionalType    = Type "?" ;
ListType        = "[" Type "]" ;
MapType         = "Map" "<" Type "," Type ">" ;
ProductType     = Type "&" Type { "&" Type } ;
UnionType       = Type "|" Type { "|" Type } ;
```

## Not in v0.1 Core

- `checkpoint;` keyword
- language-level `retry` construct
- language-level `timeout(...)` primitive
- first-class function types

## See Also

- [Failure Model Tutorial](tutorial-failures.md)
- [Awaitables Tutorial](tutorial-awaitables.md)
