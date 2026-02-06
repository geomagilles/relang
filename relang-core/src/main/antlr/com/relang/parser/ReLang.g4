grammar ReLang;

@header {
package com.relang.parser;
}

// Parser Rules
source
    : typeDecl* function* command* EOF
    ;

typeDecl
    : 'sealed' ID                                              # DeclSealed
    | 'type' ID (':' ID)? '{' fieldDecl (',' fieldDecl)* ','? '}'  # DeclType
    ;

fieldDecl
    : ID ':' typeRef
    ;

function
    : 'fn' ID '(' typedParameters? ')' (':' typeRef)? block           # FunctionBlock
    | 'fn' ID '(' typedParameters? ')' (':' typeRef)? '=' expr ';'?   # FunctionExpr
    ;

typedParameters
    : typedParam (',' typedParam)*
    ;

typedParam
    : ID (':' typeRef)? ('=' expr)?
    ;

typeRef
    : typeRefAtom '&' typeRefAtom ('&' typeRefAtom)*           # TypeRefProduct
    | typeRefAtom '|' typeRefAtom ('|' typeRefAtom)*           # TypeRefUnion
    | typeRefAtom                                               # TypeRefSimple
    ;

typeRefAtom
    : ID '?'?
    ;

command
    : statement
    ;

statement
    : 'let' ID (':' typeRef)? '=' expr ';'?                    # StatementLet
    | assignment ';'?                                            # StatementAssignment
    | 'if' '(' expr ')' block ('else' block)?                   # StatementIf
    | 'if' expr block ('else' block)?                            # StatementIfNoParens
    | 'while' '(' expr ')' block                                # StatementWhile
    | 'while' expr block                                         # StatementWhileNoParens
    | 'for' ID 'in' expr '..' expr block                        # StatementForRange
    | 'for' ID 'in' expr '..=' expr block                       # StatementForRangeInclusive
    | 'return' expr ';'?                                         # StatementReturn
    | 'break' ';'?                                               # StatementBreak
    | 'continue' ';'?                                            # StatementContinue
    | 'checkpoint' ';'?                                          # StatementCheckpoint
    | expr ';'?                                                  # StatementExpr
    ;

block
    : '{' statement* '}'
    ;

assignment
    : ID '=' expr
    ;

expr
    : 'await' expr                                               # ExprAwait
    | 'not' expr                                                 # ExprNot
    | '-' expr                                                   # ExprNegate
    | expr '.' ID                                                # ExprFieldAccess
    | left=expr op=('*'|'/'|'%') right=expr                     # ExprBinary
    | left=expr op=('+'|'-') right=expr                          # ExprBinary
    | left=expr op=('<'|'<='|'>'|'>='|'=='|'!=') right=expr     # ExprBinary
    | left=expr 'and' right=expr                                 # ExprAnd
    | left=expr 'or' right=expr                                  # ExprOr
    | left=expr '&' right=expr                                   # ExprProduct
    | 'if' expr block 'else' block                               # ExprIfElse
    | 'match' expr '{' matchArm (',' matchArm)* ','? '}'         # ExprMatchSubject
    | 'match' '{' matchArm (',' matchArm)* ','? '}'              # ExprMatchSubjectless
    | ID '(' callArguments? ')'                                   # ExprCall
    | ID '{' fieldInit (',' fieldInit)* ','? '}'                 # ExprConstruct
    | ID                                                          # ExprId
    | INT                                                         # ExprInt
    | FLOAT                                                       # ExprFloat
    | STRING                                                      # ExprString
    | 'true'                                                      # ExprTrue
    | 'false'                                                     # ExprFalse
    | 'none'                                                      # ExprNone
    | 'unit'                                                      # ExprUnit
    | DURATION                                                      # ExprDuration
    | BYTES_LITERAL                                                 # ExprBytes
    | '(' expr ')'                                                # ExprParen
    ;

matchArm
    : matchPattern '->' matchBody
    ;

matchPattern
    : '_'                                                         # PatternWildcard
    | 'none'                                                      # PatternNone
    | expr                                                        # PatternExpr
    ;

matchBody
    : block
    | expr
    ;

callArguments
    : callArg (',' callArg)*
    ;

callArg
    : ID ':' expr                                                 # CallArgNamed
    | expr                                                        # CallArgPositional
    ;

fieldInit
    : ID ':' expr
    ;

// Lexer Rules
FLOAT : [0-9] ([0-9_]* [0-9])? '.' [0-9] ([0-9_]* [0-9])? ([eE] [+-]? [0-9]+)?
      | [0-9] ([0-9_]* [0-9])? [eE] [+-]? [0-9]+
      ;
DURATION : [0-9]+ ('ms' | 's' | 'min' | 'h') ;
BYTES_LITERAL : 'b"' (BYTES_ESC | ~["\\])* '"' ;
fragment BYTES_ESC : '\\' [\\"] | '\\x' [0-9a-fA-F] [0-9a-fA-F] ;
INT   : [0-9] ([0-9_]* [0-9])? ;
STRING : '"' (ESC | ~["\\])* '"' ;
fragment ESC : '\\' [nrt\\"$] ;
ID    : [a-zA-Z_] [a-zA-Z0-9_]* ;
WS    : [ \t\r\n]+ -> skip ;
LINE_COMMENT : '//' ~[\r\n]* -> skip ;
