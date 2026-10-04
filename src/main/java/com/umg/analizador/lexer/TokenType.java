package com.umg.analizador.lexer;

/** Clasificaciones de los lexemas del subconjunto de Ruby admitido. */
public enum TokenType {
    // Palabras reservadas y palabras logicas.
    IF, ELSIF, ELSE, END, WHILE, FOR, IN, DO, DEF, RETURN,
    BREAK, NEXT, TRUE, FALSE, NIL, PUTS, AND, OR, NOT,

    // Identificadores y variables.
    IDENTIFIER, INSTANCE_VARIABLE, CLASS_VARIABLE, GLOBAL_VARIABLE,

    // Literales.
    INTEGER, FLOAT, STRING, SYMBOL_LITERAL,

    // Operadores aritmeticos.
    PLUS, MINUS, MULTIPLY, DIVIDE, MODULO, POWER,

    // Asignacion.
    ASSIGN, PLUS_ASSIGN, MINUS_ASSIGN, MULTIPLY_ASSIGN,
    DIVIDE_ASSIGN, MODULO_ASSIGN,

    // Operadores relacionales y logicos.
    EQUAL, NOT_EQUAL, LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL,
    LOGICAL_AND, LOGICAL_OR, LOGICAL_NOT,

    // Rangos y delimitadores.
    RANGE_INCLUSIVE, RANGE_EXCLUSIVE,
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACKET, RIGHT_BRACKET,
    LEFT_BRACE, RIGHT_BRACE, COMMA, DOT, COLON, SEMICOLON,

    // Control de linea y del analizador.
    NEWLINE, EOF, UNKNOWN
}
