package com.umg.analizador;

import com.umg.analizador.ast.*;
import com.umg.analizador.lexer.Token;
import com.umg.analizador.lexer.TokenType;
import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.semantic.SemanticType;
import com.umg.analizador.semantic.SymbolKind;
import com.umg.analizador.service.AnalysisResult;
import com.umg.analizador.service.AnalysisService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pruebas end-to-end del contrato publico y de los archivos oficiales reales. */
public class Phase5IntegrationTest {
    private final AnalysisService service = new AnalysisService();

    @Test
    public void preservesLexicalResult() {
        AnalysisResult original = service.analyze("x = 1");
        AnalysisResult result = copy(original);
        assertSame(original.getLexicalResult(), result.getLexicalResult());
    }

    @Test
    public void preservesSyntaxResult() {
        AnalysisResult original = service.analyze("x = 1");
        assertSame(original.getSyntaxResult(), copy(original).getSyntaxResult());
    }

    @Test
    public void preservesSemanticResult() {
        AnalysisResult original = service.analyze("x = 1");
        assertSame(original.getSemanticResult(), copy(original).getSemanticResult());
    }

    @Test
    public void validProgramFlag() {
        assertTrue(service.analyze("x = 1\nputs x").isValid());
    }

    @Test
    public void lexicalErrorInvalidates() {
        AnalysisResult result = service.analyze("x = 10 ?");
        assertFalse(result.isValid());
        assertEquals(1, result.getLexicalErrorCount());
        assertTrue(result.getLexicalResult().getErrors().get(0).getMessage().contains("no reconocido"));
    }

    @Test
    public void syntaxErrorInvalidates() {
        AnalysisResult result = service.analyze("x =");
        assertFalse(result.isValid());
        assertEquals(0, result.getLexicalErrorCount());
        assertEquals(1, result.getSyntaxErrorCount());
        assertEquals(0, result.getSemanticErrorCount());
    }

    @Test
    public void semanticErrorInvalidates() {
        AnalysisResult result = service.analyze("puts variable_no_definida");
        assertFalse(result.isValid());
        assertEquals(0, result.getLexicalErrorCount());
        assertEquals(0, result.getSyntaxErrorCount());
        assertEquals(1, result.getSemanticErrorCount());
    }

    @Test
    public void lexicalErrorCount() {
        assertEquals(2, service.analyze("? &").getLexicalErrorCount());
    }

    @Test
    public void syntaxErrorCount() {
        assertEquals(2, service.analyze("x =\ny =").getSyntaxErrorCount());
    }

    @Test
    public void semanticErrorCount() {
        assertEquals(3, service.analyze("puts falta\nbreak\nreturn 10").getSemanticErrorCount());
    }

    @Test
    public void totalDoesNotDuplicatePreservedErrors() {
        AnalysisResult result = service.analyze("?");
        assertEquals(1, result.getLexicalErrorCount());
        assertEquals(1, result.getSyntaxErrorCount());
        assertEquals(0, result.getSemanticErrorCount());
        assertEquals(2, result.getTotalErrorCount());
    }

    @Test
    public void validTotalZero() {
        assertEquals(0, service.analyze("x = 1").getTotalErrorCount());
    }

    @Test
    public void totalIsSumOfCategories() {
        for (String source : Arrays.asList("", "?", "x =", "break\nnext")) {
            AnalysisResult result = service.analyze(source);
            assertEquals(result.getLexicalErrorCount() + result.getSyntaxErrorCount()
                    + result.getSemanticErrorCount(), result.getTotalErrorCount());
        }
    }

    @Test
    public void tokenCountIncludesEof() {
        AnalysisResult result = service.analyze("x = 1");
        assertEquals(4, result.getTokenCount());
        assertEquals(result.getLexicalResult().getTokens().size(), result.getTokenCount());
    }

    @Test
    public void symbolCountIsSemanticEntries() {
        AnalysisResult result = service.analyze("edad = 20");
        assertEquals(1, result.getSymbolCount());
        assertEquals(result.getSemanticResult().getSymbols().size(), result.getSymbolCount());
        assertTrue(result.getLexicalResult().getSymbols().size() > result.getSymbolCount());
    }

    @Test
    public void nullSourceRejected() {
        assertThrows(NullPointerException.class, () -> service.analyze(null));
    }

    @Test
    public void nullLexicalResultRejected() {
        AnalysisResult result = service.analyze("");
        assertThrows(NullPointerException.class, () -> new AnalysisResult(null,
                result.getSyntaxResult(), result.getSemanticResult()));
    }

    @Test
    public void nullSyntaxResultRejected() {
        AnalysisResult result = service.analyze("");
        assertThrows(NullPointerException.class, () -> new AnalysisResult(result.getLexicalResult(),
                null, result.getSemanticResult()));
    }

    @Test
    public void nullSemanticResultRejected() {
        AnalysisResult result = service.analyze("");
        assertThrows(NullPointerException.class, () -> new AnalysisResult(result.getLexicalResult(),
                result.getSyntaxResult(), null));
    }

    @Test
    public void emptyProgram() {
        AnalysisResult result = service.analyze("");
        assertTrue(result.isValid());
        assertEquals(1, result.getTokenCount());
        assertEquals(0, result.getSymbolCount());
        assertTrue(result.getSyntaxResult().getProgram().getStatements().isEmpty());
    }

    @Test
    public void minimalProgram() {
        AnalysisResult result = service.analyze("edad = 20");
        assertTrue(result.isValid());
        assertTrue(result.getSyntaxResult().getProgram().getStatements().get(0) instanceof AssignmentNode);
        assertEquals("edad", result.getSemanticResult().getSymbols().get(0).getName());
        assertEquals(SemanticType.INTEGER, result.getSemanticResult().getSymbols().get(0).getType());
    }

    @Test
    public void crlfNormalizedAcrossStages() {
        AnalysisResult result = service.analyze("x = 1\r\nputs x\r");
        assertTrue(result.isValid());
        assertEquals("x = 1\nputs x\n", result.getLexicalResult().getSymbols().stream()
                .map(symbol -> symbol.getValue()).collect(Collectors.joining()));
        assertEquals(new SourcePosition(2, 1), result.getSyntaxResult().getProgram().getStatements().get(1).getPosition());
    }

    @Test
    public void commentsIgnored() {
        AnalysisResult result = service.analyze("# ? @ & comentario\nx = 1 # mas ?\nputs x");
        assertTrue(result.isValid());
        assertEquals(2, result.getSyntaxResult().getProgram().getStatements().size());
        assertTrue(result.getLexicalResult().getAlphabet().contains("?"));
    }

    @Test
    public void unicodeStringPreserved() {
        String lexeme = "\"Hola, niño \uD83D\uDE00\"";
        AnalysisResult result = service.analyze("mensaje = " + lexeme + "\nputs mensaje");
        assertTrue(result.isValid());
        assertEquals(lexeme, result.getLexicalResult().getTokens().get(2).getLexeme());
        assertEquals(SemanticType.STRING, result.getSemanticResult().getSymbols().get(0).getType());
    }

    @Test
    public void eofIsUniqueAndLast() {
        AnalysisResult result = service.analyze("x = 1\nputs x");
        List<Token> tokens = result.getLexicalResult().getTokens();
        assertEquals(1L, tokens.stream().filter(token -> token.getType() == TokenType.EOF).count());
        assertEquals(TokenType.EOF, tokens.get(tokens.size() - 1).getType());
    }

    @Test
    public void positionsRetained() {
        AnalysisResult result = service.analyze(" \tx = 1\r\nputs x");
        assertTrue(result.isValid());
        assertEquals(new SourcePosition(1, 3), result.getLexicalResult().getTokens().get(0).getPosition());
        assertEquals(new SourcePosition(1, 3), result.getSyntaxResult().getProgram().getStatements().get(0).getPosition());
        assertEquals(new SourcePosition(1, 3), result.getSemanticResult().getSymbols().get(0).getPosition());
    }

    @Test
    public void errorPositionsAvailable() {
        AnalysisResult lexical = service.analyze("\n  ?");
        assertEquals(new SourcePosition(2, 3), lexical.getLexicalResult().getErrors().get(0).getPosition());
        AnalysisResult syntax = service.analyze("\n  x =");
        assertEquals(new SourcePosition(2, 6), syntax.getSyntaxResult().getErrors().get(0).getToken().getPosition());
        AnalysisResult semantic = service.analyze("\n  puts falta");
        assertEquals(new SourcePosition(2, 8), semantic.getSemanticResult().getErrors().get(0).getPosition());
    }

    @Test
    public void stageDiagnosticsAreConsistent() {
        AnalysisResult result = service.analyze("?\nx =");
        assertEquals(result.getLexicalResult().getErrors(), result.getSyntaxResult().getLexicalErrors());
        assertEquals(result.getSyntaxResult().getErrors(), result.getSemanticResult().getSyntaxResult().getErrors());
        assertEquals(result.getSyntaxResult().getLexicalErrors(),
                result.getSemanticResult().getSyntaxResult().getLexicalErrors());
    }

    @Test
    public void priorErrorsSuppressSemantics() {
        for (String source : Arrays.asList("?\nputs falta", "x =\nbreak")) {
            AnalysisResult result = service.analyze(source);
            assertFalse(result.isValid());
            assertEquals(0, result.getSemanticErrorCount());
            assertEquals(0, result.getSymbolCount());
        }
    }

    @Test
    public void immutableIntegratedCollections() {
        AnalysisResult result = service.analyze("x = 1");
        assertThrows(UnsupportedOperationException.class, () -> result.getLexicalResult().getTokens().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getLexicalResult().getAlphabet().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getSyntaxResult().getProgram().getStatements().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getSemanticResult().getSymbols().clear());
    }

    @Test
    public void errorCollectionsProtected() {
        AnalysisResult lexical = service.analyze("?");
        AnalysisResult semantic = service.analyze("break");
        assertThrows(UnsupportedOperationException.class, () -> lexical.getLexicalResult().getErrors().clear());
        assertThrows(UnsupportedOperationException.class, () -> lexical.getSyntaxResult().getErrors().clear());
        assertThrows(UnsupportedOperationException.class, () -> semantic.getSemanticResult().getErrors().clear());
    }

    @Test
    public void independentServiceCalls() {
        AnalysisResult first = service.analyze("x = 1");
        AnalysisResult invalid = service.analyze("puts x");
        assertTrue(first.isValid());
        assertFalse(invalid.isValid());
        assertTrue(service.analyze("").isValid());
        assertEquals(1, first.getSymbolCount());
    }

    @Test
    public void completeValidProgram() {
        String source = "def sumar(a, b)\nreturn a + b\nend\n"
                + "datos = [1, 2, 3]\ntotal = 0\nfor dato in datos\n"
                + "total += dato\nend\nresultado = sumar(total, 10)\n"
                + "if resultado > 10\nputs resultado\nelse\nputs 0\nend";
        AnalysisResult result = service.analyze(source);
        assertTrue(result.getSemanticResult().getErrors().toString(), result.isValid());
        assertTrue(has(result, TokenType.DEF));
        assertTrue(has(result, TokenType.FOR));
        assertTrue(has(result, TokenType.IF));
        assertTrue(result.getSymbolCount() >= 6);
    }

    @Test
    public void lexicalStringErrorIsControlled() {
        AnalysisResult result = service.analyze("nombre = \"Ruby\nputs 20");
        assertFalse(result.isValid());
        assertEquals(1, result.getLexicalErrorCount());
        assertEquals("Cadena sin cerrar", result.getLexicalResult().getErrors().get(0).getMessage());
        assertEquals(0, result.getSemanticErrorCount());
    }

    @Test
    public void bajaExistsAndIsNotEmpty() throws Exception {
        assertFalse(readOfficial("baja.rb").trim().isEmpty());
    }

    @Test
    public void bajaIsValid() throws Exception {
        assertValidOfficial("baja.rb");
    }

    @Test
    public void bajaHasLexicalData() throws Exception {
        AnalysisResult result = assertValidOfficial("baja.rb");
        assertTrue(result.getTokenCount() > 20);
        assertFalse(result.getLexicalResult().getAlphabet().isEmpty());
        assertFalse(result.getLexicalResult().getSymbols().isEmpty());
        assertEquals(1L, result.getLexicalResult().getTokens().stream().filter(token -> token.getType() == TokenType.EOF).count());
    }

    @Test
    public void bajaHasSemanticSymbols() throws Exception {
        AnalysisResult result = assertValidOfficial("baja.rb");
        assertTrue(result.getSymbolCount() >= 3);
    }

    @Test
    public void mediaExistsAndIsNotEmpty() throws Exception {
        assertFalse(readOfficial("media.rb").trim().isEmpty());
    }

    @Test
    public void mediaIsValid() throws Exception {
        assertValidOfficial("media.rb");
    }

    @Test
    public void mediaHasLexicalData() throws Exception {
        AnalysisResult result = assertValidOfficial("media.rb");
        assertTrue(result.getTokenCount() > 50);
        assertFalse(result.getLexicalResult().getAlphabet().isEmpty());
        assertFalse(result.getLexicalResult().getSymbols().isEmpty());
        assertEquals(1L, result.getLexicalResult().getTokens().stream().filter(token -> token.getType() == TokenType.EOF).count());
    }

    @Test
    public void mediaHasSemanticSymbols() throws Exception {
        AnalysisResult result = assertValidOfficial("media.rb");
        assertTrue(result.getSymbolCount() >= 6);
        assertTrue(result.getSemanticResult().getSymbols().stream().anyMatch(symbol -> symbol.getKind() == SymbolKind.METHOD));
        assertTrue(result.getSemanticResult().getSymbols().stream().anyMatch(symbol -> symbol.getKind() == SymbolKind.PARAMETER));
        assertTrue(result.getSemanticResult().getSymbols().stream().anyMatch(symbol -> symbol.getKind() == SymbolKind.LOOP_VARIABLE));
    }

    @Test
    public void altaExistsAndIsNotEmpty() throws Exception {
        assertFalse(readOfficial("alta.rb").trim().isEmpty());
    }

    @Test
    public void altaIsValid() throws Exception {
        assertValidOfficial("alta.rb");
    }

    @Test
    public void altaHasLexicalData() throws Exception {
        AnalysisResult result = assertValidOfficial("alta.rb");
        assertTrue(result.getTokenCount() > 100);
        assertFalse(result.getLexicalResult().getAlphabet().isEmpty());
        assertFalse(result.getLexicalResult().getSymbols().isEmpty());
        assertEquals(1L, result.getLexicalResult().getTokens().stream().filter(token -> token.getType() == TokenType.EOF).count());
    }

    @Test
    public void altaHasSemanticSymbols() throws Exception {
        AnalysisResult result = assertValidOfficial("alta.rb");
        assertTrue(result.getSymbolCount() >= 6);
        assertTrue(result.getSemanticResult().getSymbols().stream().anyMatch(symbol -> symbol.getKind() == SymbolKind.METHOD));
        assertTrue(result.getSemanticResult().getSymbols().stream().anyMatch(symbol -> symbol.getKind() == SymbolKind.PARAMETER));
        assertTrue(result.getSemanticResult().getSymbols().stream().anyMatch(symbol -> symbol.getKind() == SymbolKind.LOOP_VARIABLE));
    }

    @Test
    public void bajaContainsItsRequiredConstructions() throws Exception {
        AnalysisResult result = assertValidOfficial("baja.rb");
        for (TokenType type : Arrays.asList(TokenType.INTEGER, TokenType.STRING, TokenType.ASSIGN,
                TokenType.PLUS, TokenType.GREATER_EQUAL, TokenType.PUTS, TokenType.IF, TokenType.ELSE)) {
            assertTrue(type.toString(), has(result, type));
        }
        assertTrue(result.getSyntaxResult().getProgram().getStatements().stream().anyMatch(node -> node instanceof IfNode));
    }

    @Test
    public void mediaContainsItsRequiredConstructions() throws Exception {
        AnalysisResult result = assertValidOfficial("media.rb");
        for (TokenType type : Arrays.asList(TokenType.DEF, TokenType.RETURN, TokenType.LEFT_BRACKET,
                TokenType.FOR, TokenType.PLUS_ASSIGN, TokenType.IF, TokenType.ELSE)) {
            assertTrue(type.toString(), has(result, type));
        }
        assertTrue(result.getSyntaxResult().getProgram().getStatements().stream()
                .anyMatch(node -> node instanceof ForNode));
    }

    @Test
    public void altaContainsNestedLoopsAndConditions() throws Exception {
        AnalysisResult result = assertValidOfficial("alta.rb");
        MethodDefinitionNode method = (MethodDefinitionNode) result.getSyntaxResult().getProgram().getStatements().get(0);
        ForNode forLoop = (ForNode) method.getBody().stream().filter(node -> node instanceof ForNode).findFirst().get();
        WhileNode whileLoop = (WhileNode) method.getBody().stream().filter(node -> node instanceof WhileNode).findFirst().get();
        assertTrue(forLoop.getBody().get(0) instanceof IfNode);
        assertTrue(whileLoop.getBody().get(0) instanceof IfNode);
        for (TokenType type : Arrays.asList(TokenType.DEF, TokenType.FOR, TokenType.WHILE, TokenType.IF,
                TokenType.ELSIF, TokenType.ELSE, TokenType.MODULO, TokenType.POWER,
                TokenType.AND, TokenType.LOGICAL_OR, TokenType.RETURN)) {
            assertTrue(type.toString(), has(result, type));
        }
    }

    @Test
    public void officialComplexityProgressionIsVisible() throws Exception {
        AnalysisResult low = assertValidOfficial("baja.rb");
        AnalysisResult medium = assertValidOfficial("media.rb");
        AnalysisResult high = assertValidOfficial("alta.rb");
        assertTrue(low.getTokenCount() < medium.getTokenCount());
        assertTrue(medium.getTokenCount() < high.getTokenCount());
        assertFalse(has(low, TokenType.DEF));
        assertFalse(has(low, TokenType.FOR));
        assertTrue(has(medium, TokenType.DEF));
        assertTrue(has(medium, TokenType.FOR));
        assertFalse(has(medium, TokenType.WHILE));
        assertTrue(has(high, TokenType.WHILE));
        assertTrue(has(high, TokenType.ELSIF));
    }

    @Test
    public void officialFileMetrics() throws Exception {
        for (String name : Arrays.asList("baja.rb", "media.rb", "alta.rb")) {
            AnalysisResult result = assertValidOfficial(name);
            System.out.println(name + ": tokens=" + result.getTokenCount()
                    + ", simbolos=" + result.getSymbolCount()
                    + ", erroresLexicos=" + result.getLexicalErrorCount()
                    + ", erroresSintacticos=" + result.getSyntaxErrorCount()
                    + ", erroresSemanticos=" + result.getSemanticErrorCount());
        }
    }

    private AnalysisResult copy(AnalysisResult result) {
        return new AnalysisResult(result.getLexicalResult(), result.getSyntaxResult(), result.getSemanticResult());
    }

    private String readOfficial(String name) throws Exception {
        Path path = Paths.get("archivos-prueba", name);
        assertTrue("Archivo oficial no encontrado: " + path.toAbsolutePath(), Files.isRegularFile(path));
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private AnalysisResult assertValidOfficial(String name) throws Exception {
        AnalysisResult result = service.analyze(readOfficial(name));
        assertTrue(result.getLexicalResult().getErrors().toString()
                + result.getSyntaxResult().getErrors() + result.getSemanticResult().getErrors(), result.isValid());
        assertEquals(0, result.getLexicalErrorCount());
        assertEquals(0, result.getSyntaxErrorCount());
        assertEquals(0, result.getSemanticErrorCount());
        assertEquals(0, result.getTotalErrorCount());
        return result;
    }

    private static boolean has(AnalysisResult result, TokenType type) {
        return result.getLexicalResult().getTokens().stream().anyMatch(token -> token.getType() == type);
    }
}

