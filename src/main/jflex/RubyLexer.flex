package com.umg.analizador.lexer;

import com.umg.analizador.model.SourcePosition;
import java.util.ArrayList;
import java.util.List;

%%

%public
%final
%class RubyLexerGenerated
%unicode
%line
%column
%type Token
%function nextToken
%xstate DOUBLE_STRING SINGLE_STRING

%{
    private final List<LexicalError> errors = new ArrayList<>();
    private final StringBuilder stringLexeme = new StringBuilder();
    private SourcePosition stringPosition;
    private boolean eofReturned;

    public List<LexicalError> getErrors() {
        return List.copyOf(errors);
    }

    private SourcePosition position() {
        return new SourcePosition(yyline + 1, yycolumn + 1);
    }

    private Token token(TokenType type) {
        return new Token(type, yytext(), position());
    }

    private void startString(int state) {
        stringLexeme.setLength(0);
        stringLexeme.append(yytext());
        stringPosition = position();
        yybegin(state);
    }

    private Token finishString() {
        stringLexeme.append(yytext());
        yybegin(YYINITIAL);
        return new Token(TokenType.STRING, stringLexeme.toString(), stringPosition);
    }

    private Token unclosedString() {
        yybegin(YYINITIAL);
        String lexeme = stringLexeme.toString();
        errors.add(new LexicalError("Cadena sin cerrar", lexeme, stringPosition));
        return new Token(TokenType.UNKNOWN, lexeme, stringPosition);
    }
%}

Identifier = [\p{L}_][\p{L}0-9_]*
Integer = [0-9]+
Float = [0-9]+\.[0-9]+
Newline = \r\n|\r|\n

%%

<YYINITIAL> {
    "if"       { return token(TokenType.IF); }
    "elsif"    { return token(TokenType.ELSIF); }
    "else"     { return token(TokenType.ELSE); }
    "end"      { return token(TokenType.END); }
    "while"    { return token(TokenType.WHILE); }
    "for"      { return token(TokenType.FOR); }
    "in"       { return token(TokenType.IN); }
    "do"       { return token(TokenType.DO); }
    "def"      { return token(TokenType.DEF); }
    "return"   { return token(TokenType.RETURN); }
    "break"    { return token(TokenType.BREAK); }
    "next"     { return token(TokenType.NEXT); }
    "true"     { return token(TokenType.TRUE); }
    "false"    { return token(TokenType.FALSE); }
    "nil"      { return token(TokenType.NIL); }
    "puts"     { return token(TokenType.PUTS); }
    "and"      { return token(TokenType.AND); }
    "or"       { return token(TokenType.OR); }
    "not"      { return token(TokenType.NOT); }

    "@@"{Identifier} { return token(TokenType.CLASS_VARIABLE); }
    "@"{Identifier}  { return token(TokenType.INSTANCE_VARIABLE); }
    "$"{Identifier}  { return token(TokenType.GLOBAL_VARIABLE); }
    ":"{Identifier}  { return token(TokenType.SYMBOL_LITERAL); }
    {Identifier}     { return token(TokenType.IDENTIFIER); }
    {Float}          { return token(TokenType.FLOAT); }
    {Integer}        { return token(TokenType.INTEGER); }

    \" { startString(DOUBLE_STRING); }
    \' { startString(SINGLE_STRING); }

    "**" { return token(TokenType.POWER); }
    "+=" { return token(TokenType.PLUS_ASSIGN); }
    "-=" { return token(TokenType.MINUS_ASSIGN); }
    "*=" { return token(TokenType.MULTIPLY_ASSIGN); }
    "/=" { return token(TokenType.DIVIDE_ASSIGN); }
    "%=" { return token(TokenType.MODULO_ASSIGN); }
    "==" { return token(TokenType.EQUAL); }
    "!=" { return token(TokenType.NOT_EQUAL); }
    "<=" { return token(TokenType.LESS_EQUAL); }
    ">=" { return token(TokenType.GREATER_EQUAL); }
    "&&" { return token(TokenType.LOGICAL_AND); }
    "||" { return token(TokenType.LOGICAL_OR); }
    "..." { return token(TokenType.RANGE_EXCLUSIVE); }
    ".." { return token(TokenType.RANGE_INCLUSIVE); }
    "+" { return token(TokenType.PLUS); }
    "-" { return token(TokenType.MINUS); }
    "*" { return token(TokenType.MULTIPLY); }
    "/" { return token(TokenType.DIVIDE); }
    "%" { return token(TokenType.MODULO); }
    "=" { return token(TokenType.ASSIGN); }
    "<" { return token(TokenType.LESS_THAN); }
    ">" { return token(TokenType.GREATER_THAN); }
    "!" { return token(TokenType.LOGICAL_NOT); }
    "(" { return token(TokenType.LEFT_PAREN); }
    ")" { return token(TokenType.RIGHT_PAREN); }
    "[" { return token(TokenType.LEFT_BRACKET); }
    "]" { return token(TokenType.RIGHT_BRACKET); }
    "{" { return token(TokenType.LEFT_BRACE); }
    "}" { return token(TokenType.RIGHT_BRACE); }
    "," { return token(TokenType.COMMA); }
    "." { return token(TokenType.DOT); }
    ":" { return token(TokenType.COLON); }
    ";" { return token(TokenType.SEMICOLON); }

    [ \t]+         { /* Separadores sin token. */ }
    "#"[^\r\n]*   { /* El salto posterior se reconoce por separado. */ }
    {Newline}      { return token(TokenType.NEWLINE); }
    [^] {
        errors.add(new LexicalError("Símbolo no reconocido", yytext(), position()));
        return token(TokenType.UNKNOWN);
    }
}

<DOUBLE_STRING> {
    \"                { return finishString(); }
    [^\"\\\r\n]+     { stringLexeme.append(yytext()); }
}

<SINGLE_STRING> {
    \'                { return finishString(); }
    [^\'\\\r\n]+     { stringLexeme.append(yytext()); }
}

<DOUBLE_STRING,SINGLE_STRING> {
    \\[^\r\n] { stringLexeme.append(yytext()); }
    \\         { stringLexeme.append(yytext()); }
    {Newline}  {
        // Recuperar en la linea siguiente y conservar su token NEWLINE.
        yypushback(yylength());
        return unclosedString();
    }
    <<EOF>> { return unclosedString(); }
}

<YYINITIAL> <<EOF>> {
    if (eofReturned) {
        return null;
    }
    eofReturned = true;
    return new Token(TokenType.EOF, "", position());
}
