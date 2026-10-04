package com.umg.analizador;

import com.umg.analizador.lexer.LexicalAnalyzer;
import com.umg.analizador.lexer.LexicalAnalysisResult;
import com.umg.analizador.lexer.LexicalError;
import com.umg.analizador.lexer.RubyLexerGenerated;
import com.umg.analizador.lexer.Token;
import com.umg.analizador.lexer.TokenType;
import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.model.SourceSymbol;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

public class Phase2LexerTest {
    private final LexicalAnalyzer analyzer = new LexicalAnalyzer();

    @Test
    public void emptyInput() {
        assertTokens("");
    }

    @Test
    public void allReservedWords() {
        assertTokens("if elsif else end while for in do def return break next true false nil puts and or not", TokenType.IF, TokenType.ELSIF, TokenType.ELSE, TokenType.END, TokenType.WHILE, TokenType.FOR, TokenType.IN, TokenType.DO, TokenType.DEF, TokenType.RETURN, TokenType.BREAK, TokenType.NEXT, TokenType.TRUE, TokenType.FALSE, TokenType.NIL, TokenType.PUTS, TokenType.AND, TokenType.OR, TokenType.NOT);
    }

    @Test
    public void identifiers() {
        assertTokens("edad contador nombre_usuario dato2 _ _dato ifdato puts2", TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.IDENTIFIER, TokenType.IDENTIFIER);
    }

    @Test
    public void unicodeIdentifiers() {
        assertTokens("año número", TokenType.IDENTIFIER, TokenType.IDENTIFIER);
    }

    @Test
    public void instanceVariables() {
        assertTokens("@nombre @_dato", TokenType.INSTANCE_VARIABLE, TokenType.INSTANCE_VARIABLE);
    }

    @Test
    public void classVariables() {
        assertTokens("@@contador @@_dato", TokenType.CLASS_VARIABLE, TokenType.CLASS_VARIABLE);
    }

    @Test
    public void globalVariables() {
        assertTokens("$total $_dato", TokenType.GLOBAL_VARIABLE, TokenType.GLOBAL_VARIABLE);
    }

    @Test
    public void integers() {
        assertTokens("0 25 1234", TokenType.INTEGER, TokenType.INTEGER, TokenType.INTEGER);
    }

    @Test
    public void floats() {
        assertTokens("3.14 0.5 25.75", TokenType.FLOAT, TokenType.FLOAT, TokenType.FLOAT);
    }

    @Test
    public void negativeNumber() {
        assertTokens("-25 -3.14", TokenType.MINUS, TokenType.INTEGER, TokenType.MINUS, TokenType.FLOAT);
    }

    @Test
    public void doubleQuotedString() {
        assertTokens("\"Hola Ruby\"", TokenType.STRING);
    }

    @Test
    public void singleQuotedString() {
        assertTokens("'Ruby'", TokenType.STRING);
    }

    @Test
    public void emptyStrings() {
        assertTokens("\"\" ''", TokenType.STRING, TokenType.STRING);
    }

    @Test
    public void escapedStrings() {
        assertTokens("\"a\\\"b\\\\c\\n\\t\\'\" 'a\\'b\\\\c\\n\\t\\\"'", TokenType.STRING, TokenType.STRING);
    }

    @Test
    public void interpolationPreserved() {
        assertTokens("\"Hola #{nombre}\"", TokenType.STRING);
    }

    @Test
    public void commentInsideString() {
        assertTokens("\"# comentario\"", TokenType.STRING);
    }

    @Test
    public void symbolLiterals() {
        assertTokens(":nombre :estado :activo :if", TokenType.SYMBOL_LITERAL, TokenType.SYMBOL_LITERAL, TokenType.SYMBOL_LITERAL, TokenType.SYMBOL_LITERAL);
    }

    @Test
    public void colonVersusSymbol() {
        assertTokens(": :nombre :  nombre", TokenType.COLON, TokenType.SYMBOL_LITERAL, TokenType.COLON, TokenType.IDENTIFIER);
    }

    @Test
    public void arithmeticOperators() {
        assertTokens("+ - * / % **", TokenType.PLUS, TokenType.MINUS, TokenType.MULTIPLY, TokenType.DIVIDE, TokenType.MODULO, TokenType.POWER);
    }

    @Test
    public void powerVersusMultiply() {
        assertTokens("***", TokenType.POWER, TokenType.MULTIPLY);
    }

    @Test
    public void assignments() {
        assertTokens("= += -= *= /= %=", TokenType.ASSIGN, TokenType.PLUS_ASSIGN, TokenType.MINUS_ASSIGN, TokenType.MULTIPLY_ASSIGN, TokenType.DIVIDE_ASSIGN, TokenType.MODULO_ASSIGN);
    }

    @Test
    public void relationalOperators() {
        assertTokens("== != < <= > >=", TokenType.EQUAL, TokenType.NOT_EQUAL, TokenType.LESS_THAN, TokenType.LESS_EQUAL, TokenType.GREATER_THAN, TokenType.GREATER_EQUAL);
    }

    @Test
    public void greaterEqualVersusSeparateOperators() {
        assertTokens(">= > =", TokenType.GREATER_EQUAL, TokenType.GREATER_THAN, TokenType.ASSIGN);
    }

    @Test
    public void lessEqual() {
        assertTokens("<=", TokenType.LESS_EQUAL);
    }

    @Test
    public void equal() {
        assertTokens("==", TokenType.EQUAL);
    }

    @Test
    public void notEqual() {
        assertTokens("!=", TokenType.NOT_EQUAL);
    }

    @Test
    public void logicalOperators() {
        assertTokens("&& || !", TokenType.LOGICAL_AND, TokenType.LOGICAL_OR, TokenType.LOGICAL_NOT);
    }

    @Test
    public void logicalWords() {
        assertTokens("and or not", TokenType.AND, TokenType.OR, TokenType.NOT);
    }

    @Test
    public void inclusiveRange() {
        assertTokens("1..10", TokenType.INTEGER, TokenType.RANGE_INCLUSIVE, TokenType.INTEGER);
    }

    @Test
    public void exclusiveRange() {
        assertTokens("1...10", TokenType.INTEGER, TokenType.RANGE_EXCLUSIVE, TokenType.INTEGER);
    }

    @Test
    public void floatsAdjacentToRange() {
        assertTokens("1.5..2.5", TokenType.FLOAT, TokenType.RANGE_INCLUSIVE, TokenType.FLOAT);
    }

    @Test
    public void delimiters() {
        assertTokens("()[]{} , . : ;", TokenType.LEFT_PAREN, TokenType.RIGHT_PAREN, TokenType.LEFT_BRACKET, TokenType.RIGHT_BRACKET, TokenType.LEFT_BRACE, TokenType.RIGHT_BRACE, TokenType.COMMA, TokenType.DOT, TokenType.COLON, TokenType.SEMICOLON);
    }

    @Test
    public void spacesIgnored() {
        assertTokens("  edad   =  20  ", TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.INTEGER);
    }

    @Test
    public void tabsIgnored() {
        assertTokens("\tedad\t=\t20\t", TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.INTEGER);
    }

    @Test
    public void newlines() {
        assertTokens("\n\n", TokenType.NEWLINE, TokenType.NEWLINE);
    }

    @Test
    public void commentsIgnored() {
        assertTokens("# comentario ? @ \"");
    }

    @Test
    public void newlineAfterComment() {
        assertTokens("edad # comentario\nputs edad", TokenType.IDENTIFIER, TokenType.NEWLINE, TokenType.PUTS, TokenType.IDENTIFIER);
    }

    @Test
    public void crlfNormalized() {
        assertTokens("edad\r\nputs\r\n", TokenType.IDENTIFIER, TokenType.NEWLINE, TokenType.PUTS, TokenType.NEWLINE);
    }

    @Test
    public void crNormalized() {
        assertTokens("edad\rputs\r", TokenType.IDENTIFIER, TokenType.NEWLINE, TokenType.PUTS, TokenType.NEWLINE);
    }

    @Test
    public void mixedRuby() {
        assertTokens("if edad >= 20\n  puts \"adulto\"\nelse\n  puts :menor\nend", TokenType.IF, TokenType.IDENTIFIER, TokenType.GREATER_EQUAL, TokenType.INTEGER, TokenType.NEWLINE, TokenType.PUTS, TokenType.STRING, TokenType.NEWLINE, TokenType.ELSE, TokenType.NEWLINE, TokenType.PUTS, TokenType.SYMBOL_LITERAL, TokenType.NEWLINE, TokenType.END);
    }

    @Test
    public void nullSourceRejected() {
        assertThrows(NullPointerException.class, () -> analyzer.analyze(null));
    }

    @Test
    public void eofHasEndPositionAndAppearsOnce() {
        LexicalAnalysisResult result = assertTokens("edad = 20\n", TokenType.IDENTIFIER,
                TokenType.ASSIGN, TokenType.INTEGER, TokenType.NEWLINE);
        Token eof = result.getTokens().get(result.getTokenCount() - 1);
        assertEquals("", eof.getLexeme());
        assertEquals(new SourcePosition(2, 1), eof.getPosition());
    }

    @Test
    public void generatedLexerDoesNotRepeatEof() throws Exception {
        RubyLexerGenerated lexer = new RubyLexerGenerated(new StringReader(""));
        assertEquals(TokenType.EOF, lexer.nextToken().getType());
        assertNull(lexer.nextToken());
    }

    @Test
    public void lexemesPreservedExactly() {
        String[] lexemes = {"edad", "=", "20", "3.14", "\"Hola Ruby\"", "'Ruby'",
                "\"a\\\"b\\\\c\\n\\t\"", "'a\\'b'", ":nombre", "@nombre", "@@contador", "$total"};
        LexicalAnalysisResult result = analyzer.analyze(String.join(" ", lexemes));
        assertFalse(result.hasErrors());
        assertEquals(Arrays.asList(lexemes), result.getTokens().stream()
                .filter(token -> token.getType() != TokenType.EOF)
                .map(Token::getLexeme).collect(Collectors.toList()));
    }

    @Test
    public void tokenPositionsCountTabsAsOneColumn() {
        LexicalAnalysisResult result = assertTokens(" \tedad = 20\n\tputs edad",
                TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.INTEGER, TokenType.NEWLINE,
                TokenType.PUTS, TokenType.IDENTIFIER);
        int[][] expected = {{1, 3}, {1, 8}, {1, 10}, {1, 12}, {2, 2}, {2, 7}, {2, 11}};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(new SourcePosition(expected[i][0], expected[i][1]),
                    result.getTokens().get(i).getPosition());
        }
    }

    @Test
    public void invalidCharacterProducesErrorAndUnknown() {
        LexicalAnalysisResult result = analyzer.analyze("edad = 20 ?");
        assertEquals(Arrays.asList(TokenType.IDENTIFIER, TokenType.ASSIGN, TokenType.INTEGER,
                TokenType.UNKNOWN, TokenType.EOF), types(result));
        assertEquals(1, result.getErrorCount());
        LexicalError error = result.getErrors().get(0);
        assertEquals("Símbolo no reconocido", error.getMessage());
        assertEquals("?", error.getLexeme());
        assertEquals(new SourcePosition(1, 11), error.getPosition());
        assertEquals(error.getPosition(), result.getTokens().get(3).getPosition());
    }

    @Test
    public void analysisContinuesAfterInvalidCharacters() {
        LexicalAnalysisResult result = analyzer.analyze("? edad = 20 &\nputs edad");
        assertEquals(Arrays.asList(TokenType.UNKNOWN, TokenType.IDENTIFIER, TokenType.ASSIGN,
                TokenType.INTEGER, TokenType.UNKNOWN, TokenType.NEWLINE, TokenType.PUTS,
                TokenType.IDENTIFIER, TokenType.EOF), types(result));
        assertEquals(2, result.getErrorCount());
        assertEquals(new SourcePosition(1, 1), result.getErrors().get(0).getPosition());
        assertEquals(new SourcePosition(1, 13), result.getErrors().get(1).getPosition());
    }

    @Test
    public void incompleteVariablePrefixesProduceControlledErrors() {
        LexicalAnalysisResult result = analyzer.analyze("@ @@ $");
        assertEquals(4, result.getErrorCount());
        assertEquals(Arrays.asList("@", "@", "@", "$"), result.getErrors().stream()
                .map(LexicalError::getLexeme).collect(Collectors.toList()));
        assertEquals(TokenType.EOF, result.getTokens().get(4).getType());
    }

    @Test
    public void unclosedDoubleStringAtEofIsOneError() {
        assertUnclosed("nombre = \"Ruby", "\"Ruby", new SourcePosition(1, 10));
    }

    @Test
    public void unclosedSingleStringAtEofIsOneError() {
        assertUnclosed("nombre = 'Ruby", "'Ruby", new SourcePosition(1, 10));
    }

    @Test
    public void danglingEscapeAtEofIsControlled() {
        assertUnclosed("\"Ruby\\", "\"Ruby\\", new SourcePosition(1, 1));
    }

    @Test
    public void unclosedStringRecoversAtNewline() {
        LexicalAnalysisResult result = analyzer.analyze("  \"Ruby ? @\nputs 'bien'");
        assertEquals(Arrays.asList(TokenType.UNKNOWN, TokenType.NEWLINE, TokenType.PUTS,
                TokenType.STRING, TokenType.EOF), types(result));
        assertEquals(1, result.getErrorCount());
        assertEquals("\"Ruby ? @", result.getErrors().get(0).getLexeme());
        assertEquals(new SourcePosition(1, 3), result.getErrors().get(0).getPosition());
        assertEquals(new SourcePosition(1, 12), result.getTokens().get(1).getPosition());
        assertEquals(new SourcePosition(2, 1), result.getTokens().get(2).getPosition());
    }

    @Test
    public void danglingEscapeBeforeNewlineRecovers() {
        LexicalAnalysisResult result = analyzer.analyze("'Ruby\\\nputs 25");
        assertEquals(1, result.getErrorCount());
        assertEquals("'Ruby\\", result.getErrors().get(0).getLexeme());
        assertEquals(Arrays.asList(TokenType.UNKNOWN, TokenType.NEWLINE, TokenType.PUTS,
                TokenType.INTEGER, TokenType.EOF), types(result));
    }

    @Test
    public void alphabetPreservesFirstAppearanceIncludingWhitespace() {
        LexicalAnalysisResult result = analyzer.analyze("aba \t\n#?");
        assertEquals(Arrays.asList("a", "b", " ", "\t", "\n", "#", "?"),
                new ArrayList<>(result.getAlphabet()));
        assertFalse(result.hasErrors());
    }

    @Test
    public void symbolsIncludeWhitespaceCommentsAndInvalidCharacters() {
        String source = "a \t\n#?\n?";
        LexicalAnalysisResult result = analyzer.analyze(source);
        assertEquals(source.length(), result.getSymbols().size());
        assertEquals(source, result.getSymbols().stream()
                .map(SourceSymbol::getValue).collect(Collectors.joining()));
        int[][] expected = {{1, 1}, {1, 2}, {1, 3}, {1, 4}, {2, 1}, {2, 2}, {2, 3}, {3, 1}};
        for (int i = 0; i < expected.length; i++) {
            assertEquals(new SourcePosition(expected[i][0], expected[i][1]),
                    result.getSymbols().get(i).getPosition());
        }
    }

    @Test
    public void normalizedInputSharedByLexerAndSymbols() {
        LexicalAnalysisResult result = assertTokens("a\r\nb\rc\n", TokenType.IDENTIFIER,
                TokenType.NEWLINE, TokenType.IDENTIFIER, TokenType.NEWLINE,
                TokenType.IDENTIFIER, TokenType.NEWLINE);
        assertEquals("a\nb\nc\n", result.getSymbols().stream()
                .map(SourceSymbol::getValue).collect(Collectors.joining()));
        assertEquals(Arrays.asList("a", "\n", "b", "c"), new ArrayList<>(result.getAlphabet()));
        for (int i = 0; i < 6; i++) {
            assertEquals(result.getSymbols().get(i).getPosition(),
                    result.getTokens().get(i).getPosition());
        }
        assertEquals(new SourcePosition(4, 1), result.getTokens().get(6).getPosition());
    }

    @Test
    public void supplementaryUnicodeSymbolAndTokenPositionsAgree() {
        LexicalAnalysisResult result = analyzer.analyze("\uD83D\uDE00 edad");
        assertEquals(1, result.getErrorCount());
        assertEquals("\uD83D\uDE00", result.getErrors().get(0).getLexeme());
        assertEquals("\uD83D\uDE00", result.getSymbols().get(0).getValue());
        assertEquals(new SourcePosition(1, 4), result.getTokens().get(1).getPosition());
        assertEquals(result.getSymbols().get(2).getPosition(), result.getTokens().get(1).getPosition());
    }

    @Test
    public void repeatedCallsDoNotShareErrorsOrSymbols() {
        LexicalAnalysisResult invalid = analyzer.analyze("?");
        LexicalAnalysisResult valid = assertTokens("edad", TokenType.IDENTIFIER);
        assertEquals(1, invalid.getErrorCount());
        assertFalse(valid.getAlphabet().contains("?"));
        assertEquals(4, valid.getSymbols().size());
    }

    @Test
    public void validRubyFixture() throws Exception {
        String source = fixture("valid_lexer.rb");
        LexicalAnalysisResult result = analyzer.analyze(source);
        assertFalse(result.getErrors().toString(), result.hasErrors());
        assertTrue(result.getTokenCount() > 100);
        assertEquals(1L, result.getTokens().stream().filter(t -> t.getType() == TokenType.EOF).count());
        assertEquals(source.replace("\r\n", "\n").replace('\r', '\n'), result.getSymbols().stream()
                .map(SourceSymbol::getValue).collect(Collectors.joining()));
        System.out.println("valid_lexer.rb: tokens incluyendo EOF = " + result.getTokenCount());
    }

    @Test
    public void invalidRubyFixture() throws Exception {
        LexicalAnalysisResult result = analyzer.analyze(fixture("invalid_lexer.rb"));
        assertEquals(9, result.getErrorCount());
        assertEquals(Arrays.asList("?", "@", "@", "@", "$", "&", "|",
                "\"Ruby sin cerrar", "'cadena sin cerrar"), result.getErrors().stream()
                .map(LexicalError::getLexeme).collect(Collectors.toList()));
        assertEquals(2L, result.getErrors().stream()
                .filter(error -> error.getMessage().equals("Cadena sin cerrar")).count());
        assertEquals(2L, result.getTokens().stream().filter(t -> t.getType() == TokenType.PUTS).count());
        assertEquals(1L, result.getTokens().stream().filter(t -> t.getType() == TokenType.EOF).count());
        System.out.println("invalid_lexer.rb: " + result.getErrors());
    }

    private LexicalAnalysisResult assertTokens(String source, TokenType... expected) {
        LexicalAnalysisResult result = analyzer.analyze(source);
        assertFalse(result.getErrors().toString(), result.hasErrors());
        List<TokenType> expectedTypes = new ArrayList<>(Arrays.asList(expected));
        expectedTypes.add(TokenType.EOF);
        assertEquals(expectedTypes, types(result));
        return result;
    }

    private void assertUnclosed(String source, String lexeme, SourcePosition position) {
        LexicalAnalysisResult result = analyzer.analyze(source);
        assertEquals(1, result.getErrorCount());
        assertEquals("Cadena sin cerrar", result.getErrors().get(0).getMessage());
        assertEquals(lexeme, result.getErrors().get(0).getLexeme());
        assertEquals(position, result.getErrors().get(0).getPosition());
        Token unknown = result.getTokens().get(result.getTokenCount() - 2);
        assertEquals(TokenType.UNKNOWN, unknown.getType());
        assertEquals(lexeme, unknown.getLexeme());
        assertEquals(position, unknown.getPosition());
        assertEquals(1L, result.getTokens().stream().filter(t -> t.getType() == TokenType.EOF).count());
    }

    private static List<TokenType> types(LexicalAnalysisResult result) {
        return result.getTokens().stream().map(Token::getType).collect(Collectors.toList());
    }

    private String fixture(String name) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/ruby/lexer/" + name)) {
            assertNotNull("Fixture no encontrado: " + name, stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
