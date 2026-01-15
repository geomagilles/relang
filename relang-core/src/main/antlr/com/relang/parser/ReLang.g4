grammar ReLang;

@header {
package com.relang.parser;
}

// Parser Rules
source
    : function* command* EOF
    ;

function
    : 'fn' ID '(' parameters? ')' block
    ;

parameters
    : ID (',' ID)*
    ;

command
    : statement
    ;

statement
    : assignment ';'                  # StatementAssignment
    | 'if' '(' expr ')' block ('else' block)?  # StatementIf
    | 'while' '(' expr ')' block      # StatementWhile
    | 'return' expr ';'               # StatementReturn
    | 'checkpoint' ';'                # StatementCheckpoint
    | expr ';'                        # StatementExpr
    ;

block
    : '{' statement* '}'
    ;

assignment
    : ID '=' expr
    ;

expr
    : left=expr op=('*'|'/') right=expr  # ExprBinary
    | left=expr op=('+'|'-') right=expr  # ExprBinary
    | left=expr op=('<'|'==') right=expr # ExprBinary
    | ID '(' arguments? ')'              # ExprCall
    | ID                                 # ExprId
    | INT                                # ExprInt
    | '(' expr ')'                       # ExprDid
    ;

arguments
    : expr (',' expr)*
    ;

// Lexer Rules
ID  : [a-zA-Z_] [a-zA-Z0-9_]* ;
INT : [0-9]+ ;
WS  : [ \t\r\n]+ -> skip ;
