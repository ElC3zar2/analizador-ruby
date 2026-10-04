package com.umg.analizador.parser;

import com.umg.analizador.ast.*;
import com.umg.analizador.lexer.Token;
import com.umg.analizador.lexer.TokenType;
import com.umg.analizador.model.SourcePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import static com.umg.analizador.lexer.TokenType.*;

/** Parser descendente recursivo con precedencia y recuperacion entre sentencias. */
public final class Parser {
    private final List<Token> tokens;
    private int current;
    private final List<SyntaxError> errors = new ArrayList<>();

    public Parser(List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        if (this.tokens.isEmpty() || this.tokens.get(this.tokens.size() - 1).getType() != EOF) {
            throw new IllegalArgumentException("La lista de tokens debe terminar con EOF");
        }
        for (int i = 0; i < this.tokens.size() - 1; i++) {
            if (this.tokens.get(i).getType() == EOF) {
                throw new IllegalArgumentException("EOF debe aparecer una sola vez al final");
            }
        }
    }

    public ProgramNode parse() {
        current = 0;
        errors.clear();
        return new ProgramNode(new SourcePosition(1, 1), parseStatements(false));
    }

    public List<SyntaxError> getErrors() {
        return List.copyOf(errors);
    }

    private List<StatementNode> parseStatements(boolean insideBlock) {
        List<StatementNode> statements = new ArrayList<>();
        skipSeparators();
        while (!check(EOF) && !(insideBlock && isBlockEnd())) {
            int start = current;
            try {
                StatementNode statement = parseStatement();
                if (!isSeparator() && !check(EOF) && !(insideBlock && isBlockEnd())) {
                    throw error("Se esperaba un salto de linea o ';' entre sentencias");
                }
                statements.add(statement);
            } catch (ParseFailure failure) {
                synchronize(start);
            }
            skipSeparators();
        }
        return statements;
    }

    private StatementNode parseStatement() {
        if (isVariable(peek().getType()) && isAssignment(lookAhead().getType())) {
            Token variable = advance();
            Token operator = advance();
            return new AssignmentNode(variable, operator, parseExpression());
        }
        if (match(PUTS)) {
            return new PutsNode(previous().getPosition(), parseExpression());
        }
        if (match(IF)) {
            return parseIf(previous());
        }
        if (match(WHILE)) {
            return parseWhile(previous());
        }
        if (match(FOR)) {
            return parseFor(previous());
        }
        if (match(DEF)) {
            return parseMethod(previous());
        }
        if (match(RETURN)) {
            Token keyword = previous();
            ExpressionNode value = isSeparator() || isBlockEnd() || check(EOF) ? null : parseExpression();
            return new ReturnNode(keyword.getPosition(), value);
        }
        if (match(BREAK)) {
            return new BreakNode(previous().getPosition());
        }
        if (match(NEXT)) {
            return new NextNode(previous().getPosition());
        }
        return new ExpressionStatementNode(parseExpression());
    }

    private IfNode parseIf(Token keyword) {
        ExpressionNode condition = parseExpression();
        requireBlockSeparator();
        List<StatementNode> thenStatements = parseStatements(true);
        List<IfNode.Branch> elsifBranches = new ArrayList<>();
        while (match(ELSIF)) {
            Token elsif = previous();
            ExpressionNode elsifCondition = parseExpression();
            requireBlockSeparator();
            elsifBranches.add(new IfNode.Branch(elsif.getPosition(), elsifCondition, parseStatements(true)));
        }
        List<StatementNode> elseStatements = List.of();
        if (match(ELSE)) {
            requireBlockSeparator();
            elseStatements = parseStatements(true);
        }
        consume(END, "Se esperaba 'end' para cerrar 'if'");
        return new IfNode(keyword.getPosition(), condition, thenStatements, elsifBranches, elseStatements);
    }

    private WhileNode parseWhile(Token keyword) {
        ExpressionNode condition = parseExpression();
        match(DO);
        requireBlockSeparator();
        List<StatementNode> body = parseStatements(true);
        consume(END, "Se esperaba 'end' para cerrar 'while'");
        return new WhileNode(keyword.getPosition(), condition, body);
    }

    private ForNode parseFor(Token keyword) {
        Token variable = consume(IDENTIFIER, "Se esperaba un identificador despues de 'for'");
        consume(IN, "Se esperaba 'in' despues de la variable de 'for'");
        ExpressionNode iterable = parseExpression();
        match(DO);
        requireBlockSeparator();
        List<StatementNode> body = parseStatements(true);
        consume(END, "Se esperaba 'end' para cerrar 'for'");
        return new ForNode(keyword.getPosition(), variable, iterable, body);
    }

    private MethodDefinitionNode parseMethod(Token keyword) {
        Token name = consume(IDENTIFIER, "Se esperaba el nombre del metodo");
        List<Token> parameters = new ArrayList<>();
        if (match(LEFT_PAREN)) {
            if (!check(RIGHT_PAREN)) {
                parseParameters(parameters);
            }
            consume(RIGHT_PAREN, "Se esperaba ')' despues de los parametros");
        } else if (check(IDENTIFIER)) {
            parseParameters(parameters);
        }
        requireBlockSeparator();
        List<StatementNode> body = parseStatements(true);
        consume(END, "Se esperaba 'end' para cerrar 'def'");
        return new MethodDefinitionNode(keyword.getPosition(), name, parameters, body);
    }

    private void parseParameters(List<Token> parameters) {
        do {
            parameters.add(consume(IDENTIFIER, "Se esperaba un identificador como parametro"));
        } while (match(COMMA));
    }

    private void requireBlockSeparator() {
        if (!isSeparator()) {
            throw error("Se esperaba un salto de linea o ';' antes del bloque");
        }
        skipSeparators();
    }

    private ExpressionNode parseExpression() {
        return parseOr();
    }

    private ExpressionNode parseOr() {
        return parseBinary(this::parseAnd, OR, LOGICAL_OR);
    }

    private ExpressionNode parseAnd() {
        return parseBinary(this::parseEquality, AND, LOGICAL_AND);
    }

    private ExpressionNode parseEquality() {
        return parseBinary(this::parseComparison, EQUAL, NOT_EQUAL);
    }

    private ExpressionNode parseComparison() {
        return parseBinary(this::parseRange, LESS_THAN, LESS_EQUAL, GREATER_THAN, GREATER_EQUAL);
    }

    private ExpressionNode parseRange() {
        return parseBinary(this::parseTerm, RANGE_INCLUSIVE, RANGE_EXCLUSIVE);
    }

    private ExpressionNode parseTerm() {
        return parseBinary(this::parseFactor, PLUS, MINUS);
    }

    private ExpressionNode parseFactor() {
        return parseBinary(this::parsePower, MULTIPLY, DIVIDE, MODULO);
    }

    private ExpressionNode parseBinary(Supplier<ExpressionNode> operand, TokenType... operators) {
        ExpressionNode expression = operand.get();
        while (match(operators)) {
            Token operator = previous();
            expression = new BinaryExpressionNode(expression, operator, operand.get());
        }
        return expression;
    }

    private ExpressionNode parsePower() {
        ExpressionNode left = parseUnary();
        if (match(POWER)) {
            Token operator = previous();
            return new BinaryExpressionNode(left, operator, parsePower());
        }
        return left;
    }

    private ExpressionNode parseUnary() {
        if (match(LOGICAL_NOT, NOT, MINUS)) {
            Token operator = previous();
            return new UnaryExpressionNode(operator, parseUnary());
        }
        return parsePrimary();
    }

    private ExpressionNode parsePrimary() {
        if (match(INTEGER, FLOAT, STRING, SYMBOL_LITERAL, TRUE, FALSE, NIL)) {
            return new LiteralNode(previous());
        }
        if (isVariable(peek().getType())) {
            Token variable = advance();
            if (variable.getType() == IDENTIFIER && match(LEFT_PAREN)) {
                return new CallExpressionNode(variable, parseElements(RIGHT_PAREN));
            }
            return new VariableNode(variable);
        }
        if (match(LEFT_PAREN)) {
            Token opening = previous();
            ExpressionNode expression = parseExpression();
            consume(RIGHT_PAREN, "Se esperaba ')' despues de la expresion");
            return new GroupingNode(opening.getPosition(), expression);
        }
        if (match(LEFT_BRACKET)) {
            Token opening = previous();
            return new ArrayLiteralNode(opening.getPosition(), parseElements(RIGHT_BRACKET));
        }
        throw error("Se esperaba una expresion; se encontro " + peek().getType());
    }

    private List<ExpressionNode> parseElements(TokenType closing) {
        List<ExpressionNode> elements = new ArrayList<>();
        skipNewlines();
        if (!check(closing)) {
            do {
                skipNewlines();
                elements.add(parseExpression());
                skipNewlines();
            } while (match(COMMA));
        }
        consume(closing, "Se esperaba " + closing + " para cerrar la lista");
        return elements;
    }

    private ParseFailure error(String message) {
        errors.add(new SyntaxError(message, peek()));
        return new ParseFailure();
    }

    private void synchronize(int start) {
        // Si nada se consumio, avanzar impide repetir indefinidamente el mismo error.
        if (current == start && !check(EOF)) {
            advance();
        }
        while (!check(EOF)) {
            if (isSeparator() || isBlockEnd() || isStatementStart()) {
                return;
            }
            advance();
        }
    }

    private boolean isStatementStart() {
        switch (peek().getType()) {
            case IF: case WHILE: case FOR: case DEF: case RETURN:
            case BREAK: case NEXT: case PUTS:
                return true;
            default:
                return false;
        }
    }

    private boolean isBlockEnd() {
        return check(ELSIF) || check(ELSE) || check(END);
    }

    private boolean isSeparator() {
        return check(NEWLINE) || check(SEMICOLON);
    }

    private void skipSeparators() {
        while (match(NEWLINE, SEMICOLON)) {
            // Separadores repetidos son validos.
        }
    }

    private void skipNewlines() {
        while (match(NEWLINE)) {
            // Dentro de arrays y argumentos pueden distribuirse elementos entre lineas.
        }
    }

    private static boolean isVariable(TokenType type) {
        return type == IDENTIFIER || type == INSTANCE_VARIABLE
                || type == CLASS_VARIABLE || type == GLOBAL_VARIABLE;
    }

    private static boolean isAssignment(TokenType type) {
        return type == ASSIGN || type == PLUS_ASSIGN || type == MINUS_ASSIGN
                || type == MULTIPLY_ASSIGN || type == DIVIDE_ASSIGN || type == MODULO_ASSIGN;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) {
            return advance();
        }
        throw error(message);
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        return peek().getType() == type;
    }

    private Token advance() {
        Token token = peek();
        if (!check(EOF)) {
            current++;
        }
        return token;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token lookAhead() {
        return tokens.get(Math.min(current + 1, tokens.size() - 1));
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    /** Control interno de recuperacion; los diagnosticos se guardan en errors. */
    private static final class ParseFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private ParseFailure() {
            super(null, null, false, false);
        }
    }
}
