package com.umg.analizador;

import com.umg.analizador.lexer.LexicalAnalysisResult;
import com.umg.analizador.lexer.LexicalError;
import com.umg.analizador.lexer.Token;
import com.umg.analizador.lexer.TokenType;
import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.model.SourceSymbol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pruebas del contrato comun de la Fase 1, sin ejecutar un lexer. */
public class Phase1Test {
    private final SourcePosition position = new SourcePosition(4, 8);
    private final Token token = new Token(TokenType.IDENTIFIER, "edad", position);
    private final LexicalError error = new LexicalError("Simbolo no reconocido", "@", position);
    private final SourceSymbol symbol = new SourceSymbol("e", position);

    @Test
    public void tokenCatalogContainsAllRequiredTypes() {
        String names = "IF ELSIF ELSE END WHILE FOR IN DO DEF RETURN BREAK NEXT TRUE FALSE NIL PUTS "
                + "AND OR NOT IDENTIFIER INSTANCE_VARIABLE CLASS_VARIABLE GLOBAL_VARIABLE "
                + "INTEGER FLOAT STRING SYMBOL_LITERAL PLUS MINUS MULTIPLY DIVIDE MODULO POWER "
                + "ASSIGN PLUS_ASSIGN MINUS_ASSIGN MULTIPLY_ASSIGN DIVIDE_ASSIGN MODULO_ASSIGN "
                + "EQUAL NOT_EQUAL LESS_THAN LESS_EQUAL GREATER_THAN GREATER_EQUAL "
                + "LOGICAL_AND LOGICAL_OR LOGICAL_NOT RANGE_INCLUSIVE RANGE_EXCLUSIVE "
                + "LEFT_PAREN RIGHT_PAREN LEFT_BRACKET RIGHT_BRACKET LEFT_BRACE RIGHT_BRACE "
                + "COMMA DOT COLON SEMICOLON NEWLINE EOF UNKNOWN";
        for (String name : names.split(" ")) {
            assertEquals(name, TokenType.valueOf(name).name());
        }
    }

    @Test
    public void positionPreservesPositiveCoordinates() {
        assertEquals(4, position.getLine());
        assertEquals(8, position.getColumn());
        SourcePosition first = new SourcePosition(1, 1);
        assertEquals(1, first.getLine());
        assertEquals(1, first.getColumn());
        SourcePosition largest = new SourcePosition(Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, largest.getLine());
        assertEquals(Integer.MAX_VALUE, largest.getColumn());
    }

    @Test
    public void positionRejectsZeroLine() {
        assertThrows(IllegalArgumentException.class, () -> new SourcePosition(0, 1));
    }

    @Test
    public void positionRejectsZeroColumn() {
        assertThrows(IllegalArgumentException.class, () -> new SourcePosition(1, 0));
    }

    @Test
    public void positionRejectsNegativeLine() {
        assertThrows(IllegalArgumentException.class, () -> new SourcePosition(-1, 1));
        assertThrows(IllegalArgumentException.class, () -> new SourcePosition(Integer.MIN_VALUE, 1));
    }

    @Test
    public void positionRejectsNegativeColumn() {
        assertThrows(IllegalArgumentException.class, () -> new SourcePosition(1, -1));
        assertThrows(IllegalArgumentException.class, () -> new SourcePosition(1, Integer.MIN_VALUE));
    }

    @Test
    public void positionEqualityIncludesBothCoordinates() {
        assertValueEquality(position, new SourcePosition(4, 8), new SourcePosition(4, 8));
        assertNotEquals(position, new SourcePosition(5, 8));
        assertNotEquals(position, new SourcePosition(4, 9));
    }

    @Test
    public void tokenPreservesFields() {
        assertEquals(TokenType.IDENTIFIER, token.getType());
        assertEquals("edad", token.getLexeme());
        assertEquals(position, token.getPosition());
    }

    @Test
    public void tokenRejectsNullType() {
        assertThrows(NullPointerException.class, () -> new Token(null, "edad", position));
    }

    @Test
    public void tokenRejectsNullLexeme() {
        assertThrows(NullPointerException.class, () -> new Token(TokenType.IDENTIFIER, null, position));
    }

    @Test
    public void tokenRejectsNullPosition() {
        assertThrows(NullPointerException.class, () -> new Token(TokenType.IDENTIFIER, "edad", null));
    }

    @Test
    public void tokenAllowsEmptyEofLexeme() {
        Token eof = new Token(TokenType.EOF, "", position);
        assertEquals(TokenType.EOF, eof.getType());
        assertEquals("", eof.getLexeme());
    }

    @Test
    public void tokenEqualityIncludesAllFields() {
        assertValueEquality(token, new Token(TokenType.IDENTIFIER, "edad", new SourcePosition(4, 8)),
                new Token(TokenType.IDENTIFIER, "edad", new SourcePosition(4, 8)));
        assertNotEquals(token, new Token(TokenType.STRING, "edad", position));
        assertNotEquals(token, new Token(TokenType.IDENTIFIER, "nombre", position));
        assertNotEquals(token, new Token(TokenType.IDENTIFIER, "edad", new SourcePosition(1, 1)));
    }

    @Test
    public void lexicalErrorPreservesFields() {
        assertEquals("Simbolo no reconocido", error.getMessage());
        assertEquals("@", error.getLexeme());
        assertEquals(position, error.getPosition());
    }

    @Test
    public void lexicalErrorRejectsNullFields() {
        assertThrows(NullPointerException.class, () -> new LexicalError(null, "@", position));
        assertThrows(NullPointerException.class, () -> new LexicalError("Error", null, position));
        assertThrows(NullPointerException.class, () -> new LexicalError("Error", "@", null));
    }

    @Test
    public void lexicalErrorEqualityIncludesAllFields() {
        assertValueEquality(error, new LexicalError("Simbolo no reconocido", "@", new SourcePosition(4, 8)),
                new LexicalError("Simbolo no reconocido", "@", new SourcePosition(4, 8)));
        assertNotEquals(error, new LexicalError("Otro error", "@", position));
        assertNotEquals(error, new LexicalError("Simbolo no reconocido", "?", position));
        assertNotEquals(error, new LexicalError("Simbolo no reconocido", "@", new SourcePosition(1, 1)));
    }

    @Test
    public void sourceSymbolPreservesFields() {
        assertEquals("e", symbol.getValue());
        assertEquals(position, symbol.getPosition());
    }

    @Test
    public void sourceSymbolRejectsNullFields() {
        assertThrows(NullPointerException.class, () -> new SourceSymbol(null, position));
        assertThrows(NullPointerException.class, () -> new SourceSymbol("e", null));
    }

    @Test
    public void sourceSymbolEqualityIncludesBothFields() {
        assertValueEquality(symbol, new SourceSymbol("e", new SourcePosition(4, 8)),
                new SourceSymbol("e", new SourcePosition(4, 8)));
        assertNotEquals(symbol, new SourceSymbol("d", position));
        assertNotEquals(symbol, new SourceSymbol("e", new SourcePosition(1, 1)));
    }

    @Test
    public void resultPreservesCollectionsAndTheirOrder() {
        Token secondToken = new Token(TokenType.INTEGER, "25", new SourcePosition(5, 1));
        SourceSymbol secondSymbol = new SourceSymbol("d", new SourcePosition(4, 9));
        List<Token> tokens = Arrays.asList(token, secondToken);
        List<LexicalError> errors = List.of(error);
        List<SourceSymbol> symbols = Arrays.asList(symbol, secondSymbol);
        Set<String> alphabet = new LinkedHashSet<>(Arrays.asList("e", "d"));
        LexicalAnalysisResult result = new LexicalAnalysisResult(tokens, errors, alphabet, symbols);
        assertEquals(tokens, result.getTokens());
        assertEquals(errors, result.getErrors());
        assertEquals(symbols, result.getSymbols());
        assertEquals(alphabet, result.getAlphabet());
        assertEquals(Arrays.asList("e", "d"), new ArrayList<>(result.getAlphabet()));
        assertEquals(2, result.getTokenCount());
        assertEquals(1, result.getErrorCount());
    }

    @Test
    public void resultWithoutErrorsReportsZeroErrors() {
        LexicalAnalysisResult result = new LexicalAnalysisResult(List.of(token), List.of(), Set.of("e"), List.of(symbol));
        assertFalse(result.hasErrors());
        assertEquals(0, result.getErrorCount());
        assertEquals(1, result.getTokenCount());
    }

    @Test
    public void resultWithErrorsReportsTheirCount() {
        LexicalError second = new LexicalError("Otro error", "?", new SourcePosition(5, 1));
        LexicalAnalysisResult result = new LexicalAnalysisResult(List.of(), List.of(error, second), Set.of(), List.of());
        assertTrue(result.hasErrors());
        assertEquals(2, result.getErrorCount());
        assertEquals(0, result.getTokenCount());
    }

    @Test
    public void emptyResultHasEmptyCollections() {
        LexicalAnalysisResult result = new LexicalAnalysisResult(List.of(), List.of(), Set.of(), List.of());
        assertTrue(result.getTokens().isEmpty());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getAlphabet().isEmpty());
        assertTrue(result.getSymbols().isEmpty());
        assertFalse(result.hasErrors());
        assertEquals(0, result.getTokenCount());
        assertEquals(0, result.getErrorCount());
    }

    @Test
    public void resultCopiesEveryInputCollection() {
        List<Token> tokens = new ArrayList<>(List.of(token));
        List<LexicalError> errors = new ArrayList<>(List.of(error));
        Set<String> alphabet = new LinkedHashSet<>(Set.of("e"));
        List<SourceSymbol> symbols = new ArrayList<>(List.of(symbol));
        LexicalAnalysisResult result = new LexicalAnalysisResult(tokens, errors, alphabet, symbols);
        tokens.clear();
        errors.clear();
        alphabet.clear();
        symbols.clear();
        assertEquals(List.of(token), result.getTokens());
        assertEquals(List.of(error), result.getErrors());
        assertEquals(Set.of("e"), result.getAlphabet());
        assertEquals(List.of(symbol), result.getSymbols());
        assertEquals(1, result.getTokenCount());
        assertEquals(1, result.getErrorCount());
        assertTrue(result.hasErrors());
    }

    @Test
    public void resultTokensCannotBeModified() {
        List<Token> tokens = resultWithAllCollections().getTokens();
        assertThrows(UnsupportedOperationException.class, () -> tokens.add(token));
        assertThrows(UnsupportedOperationException.class, () -> tokens.set(0, token));
        assertThrows(UnsupportedOperationException.class, () -> tokens.remove(0));
        assertEquals(List.of(token), tokens);
    }

    @Test
    public void resultErrorsCannotBeModified() {
        List<LexicalError> errors = resultWithAllCollections().getErrors();
        assertThrows(UnsupportedOperationException.class, () -> errors.add(error));
        assertThrows(UnsupportedOperationException.class, () -> errors.set(0, error));
        assertThrows(UnsupportedOperationException.class, () -> errors.remove(0));
        assertEquals(List.of(error), errors);
    }

    @Test
    public void resultSymbolsCannotBeModified() {
        List<SourceSymbol> symbols = resultWithAllCollections().getSymbols();
        assertThrows(UnsupportedOperationException.class, () -> symbols.add(symbol));
        assertThrows(UnsupportedOperationException.class, () -> symbols.set(0, symbol));
        assertThrows(UnsupportedOperationException.class, () -> symbols.remove(0));
        assertEquals(List.of(symbol), symbols);
    }

    @Test
    public void resultAlphabetCannotBeModified() {
        Set<String> alphabet = resultWithAllCollections().getAlphabet();
        assertThrows(UnsupportedOperationException.class, () -> alphabet.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> alphabet.remove("e"));
        assertThrows(UnsupportedOperationException.class, alphabet::clear);
        assertEquals(Set.of("e"), alphabet);
    }

    private LexicalAnalysisResult resultWithAllCollections() {
        return new LexicalAnalysisResult(List.of(token), List.of(error), Set.of("e"), List.of(symbol));
    }

    private static void assertValueEquality(Object first, Object second, Object third) {
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(second, third);
        assertEquals(first, third);
        assertEquals(first.hashCode(), second.hashCode());
        assertEquals(second.hashCode(), third.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "objeto de otro tipo");
        Set<Object> distinct = new HashSet<>(Arrays.asList(first, second, third));
        assertEquals(1, distinct.size());
    }
}
