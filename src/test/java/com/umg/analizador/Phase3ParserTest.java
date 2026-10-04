package com.umg.analizador;

import com.umg.analizador.ast.*;
import com.umg.analizador.lexer.*;
import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.parser.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import static org.junit.Assert.*;

/** Pruebas de sintaxis, estructura del AST y recuperacion sin analisis semantico. */
public class Phase3ParserTest {
    private final SyntaxAnalyzer analyzer = new SyntaxAnalyzer();

    @Test
    public void integerAssignment() {
        AssignmentNode node = statement("edad = 20", AssignmentNode.class);
        assertEquals(TokenType.INTEGER, ((LiteralNode) node.getExpression()).getToken().getType());
    }

    @Test
    public void floatAssignment() {
        AssignmentNode node = statement("x = 3.14", AssignmentNode.class);
        assertEquals("3.14", ((LiteralNode) node.getExpression()).getToken().getLexeme());
    }

    @Test
    public void doubleStringAssignment() {
        AssignmentNode node = statement("nombre = \"Ruby\"", AssignmentNode.class);
        assertEquals("\"Ruby\"", ((LiteralNode) node.getExpression()).getToken().getLexeme());
    }

    @Test
    public void singleStringAssignment() {
        AssignmentNode node = statement("nombre = 'Ruby'", AssignmentNode.class);
        assertEquals("'Ruby'", ((LiteralNode) node.getExpression()).getToken().getLexeme());
    }

    @Test
    public void instanceVariable() {
        AssignmentNode node = statement("@x = 1", AssignmentNode.class);
        assertEquals(TokenType.INSTANCE_VARIABLE, node.getVariable().getType());
    }

    @Test
    public void classVariable() {
        AssignmentNode node = statement("@@x = 1", AssignmentNode.class);
        assertEquals(TokenType.CLASS_VARIABLE, node.getVariable().getType());
    }

    @Test
    public void globalVariable() {
        AssignmentNode node = statement("$x = 1", AssignmentNode.class);
        assertEquals(TokenType.GLOBAL_VARIABLE, node.getVariable().getType());
    }

    @Test
    public void plusAssignment() {
        AssignmentNode node = statement("x += 1", AssignmentNode.class);
        assertEquals(TokenType.PLUS_ASSIGN, node.getOperator().getType());
    }

    @Test
    public void minusAssignment() {
        AssignmentNode node = statement("x -= 1", AssignmentNode.class);
        assertEquals(TokenType.MINUS_ASSIGN, node.getOperator().getType());
    }

    @Test
    public void multiplyAssignment() {
        AssignmentNode node = statement("x *= 2", AssignmentNode.class);
        assertEquals(TokenType.MULTIPLY_ASSIGN, node.getOperator().getType());
    }

    @Test
    public void divideAssignment() {
        AssignmentNode node = statement("x /= 2", AssignmentNode.class);
        assertEquals(TokenType.DIVIDE_ASSIGN, node.getOperator().getType());
    }

    @Test
    public void moduloAssignment() {
        AssignmentNode node = statement("x %= 2", AssignmentNode.class);
        assertEquals(TokenType.MODULO_ASSIGN, node.getOperator().getType());
    }

    @Test
    public void addition() {
        ExpressionStatementNode node = statement("1 + 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.PLUS);
    }

    @Test
    public void subtraction() {
        ExpressionStatementNode node = statement("1 - 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.MINUS);
    }

    @Test
    public void multiplication() {
        ExpressionStatementNode node = statement("1 * 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.MULTIPLY);
    }

    @Test
    public void division() {
        ExpressionStatementNode node = statement("1 / 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.DIVIDE);
    }

    @Test
    public void modulo() {
        ExpressionStatementNode node = statement("1 % 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.MODULO);
    }

    @Test
    public void power() {
        ExpressionStatementNode node = statement("2 ** 3", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.POWER);
    }

    @Test
    public void grouping() {
        ExpressionStatementNode node = statement("(1 + 2) * 3", ExpressionStatementNode.class);
        BinaryExpressionNode binary = assertBinary(node.getExpression(), TokenType.MULTIPLY); assertTrue(binary.getLeft() instanceof GroupingNode);
    }

    @Test
    public void lessThan() {
        ExpressionStatementNode node = statement("x < 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.LESS_THAN);
    }

    @Test
    public void lessEqual() {
        ExpressionStatementNode node = statement("x <= 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.LESS_EQUAL);
    }

    @Test
    public void greaterThan() {
        ExpressionStatementNode node = statement("x > 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.GREATER_THAN);
    }

    @Test
    public void greaterEqual() {
        ExpressionStatementNode node = statement("x >= 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.GREATER_EQUAL);
    }

    @Test
    public void equality() {
        ExpressionStatementNode node = statement("x == 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.EQUAL);
    }

    @Test
    public void inequality() {
        ExpressionStatementNode node = statement("x != 2", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.NOT_EQUAL);
    }

    @Test
    public void inclusiveRange() {
        ExpressionStatementNode node = statement("1..10", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.RANGE_INCLUSIVE);
    }

    @Test
    public void exclusiveRange() {
        ExpressionStatementNode node = statement("1...10", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.RANGE_EXCLUSIVE);
    }

    @Test
    public void logicalNot() {
        ExpressionStatementNode node = statement("!true", ExpressionStatementNode.class);
        assertEquals(TokenType.LOGICAL_NOT, ((UnaryExpressionNode) node.getExpression()).getOperator().getType());
    }

    @Test
    public void wordNot() {
        ExpressionStatementNode node = statement("not false", ExpressionStatementNode.class);
        assertEquals(TokenType.NOT, ((UnaryExpressionNode) node.getExpression()).getOperator().getType());
    }

    @Test
    public void unaryMinus() {
        ExpressionStatementNode node = statement("-25", ExpressionStatementNode.class);
        assertEquals(TokenType.MINUS, ((UnaryExpressionNode) node.getExpression()).getOperator().getType());
    }

    @Test
    public void puts() {
        PutsNode node = statement("puts 20", PutsNode.class);
        assertTrue(node.getExpression() instanceof LiteralNode);
    }

    @Test
    public void putsWithParentheses() {
        PutsNode node = statement("puts(20)", PutsNode.class);
        assertTrue(node.getExpression() instanceof GroupingNode);
    }

    @Test
    public void simpleIf() {
        IfNode node = statement("if true\nputs 1\nend", IfNode.class);
        assertEquals(1, node.getThenStatements().size()); assertTrue(node.getElseStatements().isEmpty());
    }

    @Test
    public void ifElse() {
        IfNode node = statement("if true\nputs 1\nelse\nputs 2\nend", IfNode.class);
        assertEquals(1, node.getElseStatements().size());
    }

    @Test
    public void ifElsifElse() {
        IfNode node = statement("if x < 1\nputs 1\nelsif x == 1\nputs 2\nelsif x == 2\nputs 3\nelse\nputs 4\nend", IfNode.class);
        assertEquals(2, node.getElsifBranches().size()); assertEquals(1, node.getElseStatements().size());
    }

    @Test
    public void nestedIf() {
        IfNode node = statement("if true\nif false\nputs 1\nend\nelse\nputs 2\nend", IfNode.class);
        assertTrue(node.getThenStatements().get(0) instanceof IfNode);
    }

    @Test
    public void whileStatement() {
        WhileNode node = statement("while x < 10\nx += 1\nend", WhileNode.class);
        assertEquals(1, node.getBody().size()); assertTrue(node.getBody().get(0) instanceof AssignmentNode);
    }

    @Test
    public void whileDo() {
        WhileNode node = statement("while true do\nbreak\nend", WhileNode.class);
        assertTrue(node.getBody().get(0) instanceof BreakNode);
    }

    @Test
    public void forStatement() {
        ForNode node = statement("for elemento in 1..10\nputs elemento\nend", ForNode.class);
        assertEquals("elemento", node.getVariable().getLexeme()); assertBinary(node.getIterable(), TokenType.RANGE_INCLUSIVE);
    }

    @Test
    public void forDo() {
        ForNode node = statement("for elemento in [1, 2] do\nnext\nend", ForNode.class);
        assertTrue(node.getIterable() instanceof ArrayLiteralNode); assertTrue(node.getBody().get(0) instanceof NextNode);
    }

    @Test
    public void methodWithoutParameters() {
        MethodDefinitionNode node = statement("def calcular\nreturn 1\nend", MethodDefinitionNode.class);
        assertEquals("calcular", node.getName().getLexeme()); assertTrue(node.getParameters().isEmpty());
    }

    @Test
    public void methodWithParameters() {
        MethodDefinitionNode node = statement("def calcular(a, b)\nreturn a + b\nend", MethodDefinitionNode.class);
        assertEquals(Arrays.asList("a", "b"), node.getParameters().stream().map(Token::getLexeme).collect(Collectors.toList()));
    }

    @Test
    public void methodWithBareParameters() {
        MethodDefinitionNode node = statement("def calcular a, b\nreturn a\nend", MethodDefinitionNode.class);
        assertEquals(2, node.getParameters().size());
    }

    @Test
    public void methodWithEmptyParentheses() {
        MethodDefinitionNode node = statement("def calcular()\nreturn\nend", MethodDefinitionNode.class);
        assertTrue(node.getParameters().isEmpty());
    }

    @Test
    public void emptyReturn() {
        ReturnNode node = statement("return", ReturnNode.class);
        assertNull(node.getExpression());
    }

    @Test
    public void returnExpression() {
        ReturnNode node = statement("return x + 1", ReturnNode.class);
        assertBinary(node.getExpression(), TokenType.PLUS);
    }

    @Test
    public void breakOutsideLoopDeferredToSemantics() {
        BreakNode node = statement("break", BreakNode.class);
    }

    @Test
    public void nextOutsideLoopDeferredToSemantics() {
        NextNode node = statement("next", NextNode.class);
    }

    @Test
    public void emptyArray() {
        ExpressionStatementNode node = statement("[]", ExpressionStatementNode.class);
        assertTrue(((ArrayLiteralNode) node.getExpression()).getElements().isEmpty());
    }

    @Test
    public void arrayWithElements() {
        ExpressionStatementNode node = statement("[1, 2, \"Ruby\", x]", ExpressionStatementNode.class);
        assertEquals(4, ((ArrayLiteralNode) node.getExpression()).getElements().size());
    }

    @Test
    public void nestedArrays() {
        ExpressionStatementNode node = statement("[[1], []]", ExpressionStatementNode.class);
        assertTrue(((ArrayLiteralNode) node.getExpression()).getElements().get(0) instanceof ArrayLiteralNode);
    }

    @Test
    public void multilineArray() {
        ExpressionStatementNode node = statement("[\n1,\n2\n]", ExpressionStatementNode.class);
        assertEquals(2, ((ArrayLiteralNode) node.getExpression()).getElements().size());
    }

    @Test
    public void callWithoutArguments() {
        ExpressionStatementNode node = statement("calcular()", ExpressionStatementNode.class);
        CallExpressionNode call = (CallExpressionNode) node.getExpression(); assertEquals("calcular", call.getCallee().getLexeme()); assertTrue(call.getArguments().isEmpty());
    }

    @Test
    public void callWithArguments() {
        ExpressionStatementNode node = statement("calcular(1, 2)", ExpressionStatementNode.class);
        assertEquals(2, ((CallExpressionNode) node.getExpression()).getArguments().size());
    }

    @Test
    public void callWithExpressionArguments() {
        ExpressionStatementNode node = statement("calcular(x + 1, saludar(nombre), [1, 2])", ExpressionStatementNode.class);
        assertEquals(3, ((CallExpressionNode) node.getExpression()).getArguments().size());
    }

    @Test
    public void multilineCall() {
        ExpressionStatementNode node = statement("calcular(\n1,\n2\n)", ExpressionStatementNode.class);
        assertEquals(2, ((CallExpressionNode) node.getExpression()).getArguments().size());
    }

    @Test
    public void symbolLiteral() {
        ExpressionStatementNode node = statement(":activo", ExpressionStatementNode.class);
        assertEquals(TokenType.SYMBOL_LITERAL, ((LiteralNode) node.getExpression()).getToken().getType());
    }

    @Test
    public void trueLiteral() {
        ExpressionStatementNode node = statement("true", ExpressionStatementNode.class);
        assertEquals(TokenType.TRUE, ((LiteralNode) node.getExpression()).getToken().getType());
    }

    @Test
    public void falseLiteral() {
        ExpressionStatementNode node = statement("false", ExpressionStatementNode.class);
        assertEquals(TokenType.FALSE, ((LiteralNode) node.getExpression()).getToken().getType());
    }

    @Test
    public void nilLiteral() {
        ExpressionStatementNode node = statement("nil", ExpressionStatementNode.class);
        assertEquals(TokenType.NIL, ((LiteralNode) node.getExpression()).getToken().getType());
    }

    @Test
    public void variableExpression() {
        ExpressionStatementNode node = statement("@x + @@y + $z", ExpressionStatementNode.class);
        assertBinary(node.getExpression(), TokenType.PLUS);
    }

    @Test
    public void semicolonBlocks() {
        IfNode node = statement("if true; x = 1; else; x = 2; end", IfNode.class);
        assertEquals(1, node.getThenStatements().size()); assertEquals(1, node.getElseStatements().size());
    }

    @Test
    public void emptyBlock() {
        WhileNode node = statement("while true\nend", WhileNode.class);
        assertTrue(node.getBody().isEmpty());
    }

    @Test
    public void emptyProgram() {
        SyntaxAnalysisResult result = valid("");
        assertTrue(result.getProgram().getStatements().isEmpty());
        assertEquals(new SourcePosition(1, 1), result.getProgram().getPosition());
    }

    @Test
    public void multipleStatements() {
        assertEquals(3, valid("x = 1\ny = 2\nputs x + y").getProgram().getStatements().size());
    }

    @Test
    public void semicolonSeparators() {
        assertEquals(2, valid("x = 1; y = 2;").getProgram().getStatements().size());
    }

    @Test
    public void repeatedSeparatorsAndBlankLines() {
        assertEquals(2, valid("\n\n;;x = 1;\n\n;y = 2\n\n").getProgram().getStatements().size());
    }

    @Test
    public void arithmeticPrecedence() {
        ExpressionNode expression = expression("1 + 2 * 3 ** 4");
        BinaryExpressionNode plus = assertBinary(expression, TokenType.PLUS);
        BinaryExpressionNode multiply = assertBinary(plus.getRight(), TokenType.MULTIPLY);
        assertBinary(multiply.getRight(), TokenType.POWER);
    }

    @Test
    public void logicalPrecedence() {
        BinaryExpressionNode or = assertBinary(expression("true or false and true"), TokenType.OR);
        assertBinary(or.getRight(), TokenType.AND);
        BinaryExpressionNode symbolicOr = assertBinary(expression("true || false && true"), TokenType.LOGICAL_OR);
        assertBinary(symbolicOr.getRight(), TokenType.LOGICAL_AND);
    }

    @Test
    public void equalityComparisonRangeAndTermPrecedence() {
        BinaryExpressionNode equality = assertBinary(expression("x < 1 + 2..10 == true"), TokenType.EQUAL);
        BinaryExpressionNode comparison = assertBinary(equality.getLeft(), TokenType.LESS_THAN);
        BinaryExpressionNode range = assertBinary(comparison.getRight(), TokenType.RANGE_INCLUSIVE);
        assertBinary(range.getLeft(), TokenType.PLUS);
    }

    @Test
    public void powerIsRightAssociative() {
        BinaryExpressionNode power = assertBinary(expression("2 ** 3 ** 4"), TokenType.POWER);
        assertTrue(power.getLeft() instanceof LiteralNode);
        assertBinary(power.getRight(), TokenType.POWER);
    }

    @Test
    public void subtractionIsLeftAssociative() {
        BinaryExpressionNode minus = assertBinary(expression("10 - 3 - 2"), TokenType.MINUS);
        assertBinary(minus.getLeft(), TokenType.MINUS);
        assertTrue(minus.getRight() instanceof LiteralNode);
    }

    @Test
    public void unaryHasRecommendedAcademicPrecedence() {
        BinaryExpressionNode power = assertBinary(expression("-2 ** 2"), TokenType.POWER);
        assertTrue(power.getLeft() instanceof UnaryExpressionNode);
        assertTrue(expression("!not -x") instanceof UnaryExpressionNode);
    }

    @Test
    public void nodePositionsRetainSourceLocations() {
        SyntaxAnalysisResult result = valid("\n  x = -(1 + 2)\n  puts x\n");
        AssignmentNode assignment = (AssignmentNode) result.getProgram().getStatements().get(0);
        assertEquals(new SourcePosition(2, 3), assignment.getPosition());
        assertEquals(new SourcePosition(2, 5), assignment.getOperator().getPosition());
        UnaryExpressionNode unary = (UnaryExpressionNode) assignment.getExpression();
        assertEquals(new SourcePosition(2, 7), unary.getPosition());
        GroupingNode grouping = (GroupingNode) unary.getExpression();
        assertEquals(new SourcePosition(2, 8), grouping.getPosition());
        assertEquals(new SourcePosition(2, 9), grouping.getExpression().getPosition());
        assertEquals(new SourcePosition(3, 3), result.getProgram().getStatements().get(1).getPosition());
    }

    @Test
    public void nullSourceRejected() {
        assertThrows(NullPointerException.class, () -> analyzer.analyze(null));
    }

    @Test
    public void incompleteExpression() {
        SyntaxAnalysisResult result = invalid("x =");
        assertEquals(1, result.getSyntaxErrorCount());
        assertEquals(TokenType.EOF, result.getErrors().get(0).getToken().getType());
    }

    @Test
    public void missingEnd() {
        SyntaxAnalysisResult result = invalid("if x > 10\nputs \"hola\"\n");
        assertTrue(result.getErrors().get(0).getMessage().contains("'end'"));
        assertEquals(new SourcePosition(3, 1), result.getErrors().get(0).getToken().getPosition());
    }

    @Test
    public void missingCondition() {
        SyntaxAnalysisResult result = invalid("if\nputs 1\nend");
        assertEquals(TokenType.NEWLINE, result.getErrors().get(0).getToken().getType());
    }

    @Test
    public void malformedMethodParameters() {
        SyntaxAnalysisResult result = invalid("def metodo(\nend");
        assertTrue(result.getErrors().get(0).getMessage().contains("parametro"));
    }

    @Test
    public void missingClosingGroupingParenthesis() {
        invalid("x = (1 + 2");
    }

    @Test
    public void missingClosingArrayBracket() {
        invalid("x = [1, 2");
    }

    @Test
    public void missingClosingCallParenthesis() {
        invalid("calcular(1, 2");
    }

    @Test
    public void emptyGroupingRejected() {
        invalid("()");
    }

    @Test
    public void invalidAssignmentTarget() {
        invalid("20 = 3");
    }

    @Test
    public void missingStatementSeparator() {
        invalid("x = 1 y = 2");
    }

    @Test
    public void missingBlockSeparator() {
        invalid("if true puts 1 end");
    }

    @Test
    public void forRequiresIdentifier() {
        invalid("for @x in 1..10\nend");
    }

    @Test
    public void forRequiresIn() {
        invalid("for x 1..10\nend");
    }

    @Test
    public void putsRequiresOneArgument() {
        invalid("puts");
        invalid("puts()");
        invalid("puts(1, 2)");
    }

    @Test
    public void breakAndNextDoNotAcceptArguments() {
        invalid("break 1");
        invalid("next 2");
    }

    @Test
    public void unsupportedMethodFormsRejected() {
        invalid("def nombre(a = 1)\nend");
        invalid("def nombre(*a)\nend");
        invalid("def objeto.nombre()\nend");
    }

    @Test
    public void bareCallsDeliberatelyRejected() {
        invalid("saludar nombre");
    }

    @Test
    public void hashesIndexingAndMemberCallsRejected() {
        invalid("{x: 1}");
        invalid("a[0]");
        invalid("obj.metodo()");
    }

    @Test
    public void trailingCommasRejected() {
        invalid("[1, ]");
        invalid("calcular(1, )");
        invalid("def calcular(a,)\nend");
    }

    @Test(timeout = 2000)
    public void recoveryAfterIncompleteAssignment() {
        SyntaxAnalysisResult result = invalid("x =\ny = 2\nputs y");
        assertEquals(1, result.getSyntaxErrorCount());
        assertEquals(2, result.getProgram().getStatements().size());
        assertTrue(result.getProgram().getStatements().get(0) instanceof AssignmentNode);
        assertTrue(result.getProgram().getStatements().get(1) instanceof PutsNode);
    }

    @Test(timeout = 2000)
    public void recoveryAtSemicolon() {
        SyntaxAnalysisResult result = invalid("x = ; y = 2; puts y");
        assertEquals(2, result.getProgram().getStatements().size());
    }

    @Test(timeout = 2000)
    public void recoveryAtStatementKeyword() {
        SyntaxAnalysisResult result = invalid("x = puts 20\nreturn");
        assertEquals(2, result.getProgram().getStatements().size());
        assertTrue(result.getProgram().getStatements().get(0) instanceof PutsNode);
    }

    @Test(timeout = 2000)
    public void recoveryInsideBlockPreservesFollowingStatements() {
        IfNode node = (IfNode) invalid("if true\nx =\nputs 2\nelse\nputs 3\nend")
                .getProgram().getStatements().get(0);
        assertEquals(1, node.getThenStatements().size());
        assertEquals(1, node.getElseStatements().size());
    }

    @Test(timeout = 2000)
    public void unexpectedClosingKeywordsDoNotLoop() {
        SyntaxAnalysisResult result = invalid("end; else; elsif; end;\nputs 1");
        assertEquals(4, result.getSyntaxErrorCount());
        assertEquals(1, result.getProgram().getStatements().size());
    }

    @Test
    public void lexicalErrorsPreservedAndParsingContinues() {
        SyntaxAnalysisResult result = analyzer.analyze("?\nputs 20");
        assertTrue(result.hasErrors());
        assertTrue(result.hasSyntaxErrors());
        assertTrue(result.hasLexicalErrors());
        LexicalAnalysisResult lexical = new LexicalAnalyzer().analyze("?\nputs 20");
        assertEquals(lexical.getErrors(), result.getLexicalErrors());
        assertEquals(TokenType.UNKNOWN, result.getErrors().get(0).getToken().getType());
        assertEquals(1, result.getProgram().getStatements().size());
    }

    @Test
    public void syntaxErrorTokenPositionsAndNormalization() {
        SyntaxAnalysisResult result = invalid("x = 1\r\n  y =\r\nputs 20");
        SyntaxError error = result.getErrors().get(0);
        assertEquals(TokenType.NEWLINE, error.getToken().getType());
        assertEquals("\n", error.getToken().getLexeme());
        assertEquals(new SourcePosition(2, 6), error.getToken().getPosition());
    }

    @Test
    public void syntaxErrorEqualityHashAndDescription() {
        Token token = new Token(TokenType.EOF, "", new SourcePosition(1, 1));
        SyntaxError first = new SyntaxError("Falta expresion", token);
        SyntaxError second = new SyntaxError("Falta expresion",
                new Token(TokenType.EOF, "", new SourcePosition(1, 1)));
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "otro tipo");
        assertNotEquals(first, new SyntaxError("Otro mensaje", token));
        assertNotEquals(first, new SyntaxError("Falta expresion",
                new Token(TokenType.NEWLINE, "\n", new SourcePosition(1, 1))));
        assertEquals("Falta expresion", first.getMessage());
        assertEquals(token, first.getToken());
        assertTrue(first.toString().contains("Falta expresion"));
        assertThrows(NullPointerException.class, () -> new SyntaxError(null, token));
        assertThrows(NullPointerException.class, () -> new SyntaxError("Error", null));
    }

    @Test
    public void resultCopiesAndProtectsDiagnosticCollections() {
        ProgramNode program = valid("").getProgram();
        Token token = new Token(TokenType.UNKNOWN, "?", new SourcePosition(1, 1));
        SyntaxError syntax = new SyntaxError("Error", token);
        LexicalError lexical = new LexicalError("Error", "?", token.getPosition());
        List<SyntaxError> syntaxErrors = new ArrayList<>(List.of(syntax));
        List<LexicalError> lexicalErrors = new ArrayList<>(List.of(lexical));
        SyntaxAnalysisResult result = new SyntaxAnalysisResult(program, syntaxErrors, lexicalErrors);
        syntaxErrors.clear();
        lexicalErrors.clear();
        assertEquals(List.of(syntax), result.getErrors());
        assertEquals(List.of(lexical), result.getLexicalErrors());
        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getLexicalErrors().clear());
        assertEquals(1, result.getSyntaxErrorCount());
        assertSame(program, result.getProgram());
    }

    @Test
    public void resultFlagsCoverEachErrorCombination() {
        ProgramNode program = valid("").getProgram();
        Token token = new Token(TokenType.UNKNOWN, "?", new SourcePosition(1, 1));
        List<SyntaxError> syntax = List.of(new SyntaxError("Error", token));
        List<LexicalError> lexical = List.of(new LexicalError("Error", "?", token.getPosition()));
        SyntaxAnalysisResult onlyLexical = new SyntaxAnalysisResult(program, List.of(), lexical);
        assertTrue(onlyLexical.hasErrors());
        assertTrue(onlyLexical.hasLexicalErrors());
        assertFalse(onlyLexical.hasSyntaxErrors());
        SyntaxAnalysisResult onlySyntax = new SyntaxAnalysisResult(program, syntax, List.of());
        assertTrue(onlySyntax.hasErrors());
        assertTrue(onlySyntax.hasSyntaxErrors());
        assertFalse(onlySyntax.hasLexicalErrors());
        assertFalse(valid("").hasErrors());
    }

    @Test
    public void astListsDefensivelyCopiedAndProtected() {
        SourcePosition position = new SourcePosition(1, 1);
        List<StatementNode> statements = new ArrayList<>(List.of(new BreakNode(position)));
        ProgramNode program = new ProgramNode(position, statements);
        WhileNode loop = new WhileNode(position, expression("true"), statements);
        ForNode forNode = new ForNode(position, new Token(TokenType.IDENTIFIER, "x", position),
                expression("[]"), statements);
        IfNode.Branch branch = new IfNode.Branch(position, expression("true"), statements);
        List<IfNode.Branch> branches = new ArrayList<>(List.of(branch));
        IfNode conditional = new IfNode(position, expression("true"), statements, branches, statements);
        List<Token> parameters = new ArrayList<>(List.of(new Token(TokenType.IDENTIFIER, "a", position)));
        MethodDefinitionNode method = new MethodDefinitionNode(position,
                new Token(TokenType.IDENTIFIER, "f", position), parameters, statements);
        List<ExpressionNode> values = new ArrayList<>(List.of(expression("1")));
        ArrayLiteralNode array = new ArrayLiteralNode(position, values);
        CallExpressionNode call = new CallExpressionNode(
                new Token(TokenType.IDENTIFIER, "f", position), values);
        statements.clear();
        branches.clear();
        parameters.clear();
        values.clear();
        List<List<?>> lists = Arrays.asList(program.getStatements(), loop.getBody(), forNode.getBody(),
                branch.getStatements(), conditional.getThenStatements(), conditional.getElseStatements(),
                conditional.getElsifBranches(), method.getParameters(), method.getBody(),
                array.getElements(), call.getArguments());
        for (List<?> list : lists) {
            assertEquals(1, list.size());
            assertThrows(UnsupportedOperationException.class, list::clear);
        }
    }

    @Test
    public void parserInputAndErrorsAreEncapsulatedAndRepeatable() {
        List<Token> tokens = new ArrayList<>(new LexicalAnalyzer().analyze("x =").getTokens());
        Parser parser = new Parser(tokens);
        tokens.clear();
        parser.parse();
        assertEquals(1, parser.getErrors().size());
        List<SyntaxError> snapshot = parser.getErrors();
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        parser.parse();
        assertEquals(snapshot, parser.getErrors());
    }

    @Test
    public void parserRejectsMalformedTokenStreams() {
        assertThrows(IllegalArgumentException.class, () -> new Parser(List.of()));
        Token eof = new Token(TokenType.EOF, "", new SourcePosition(1, 1));
        assertThrows(IllegalArgumentException.class, () -> new Parser(List.of(eof, eof)));
        assertThrows(IllegalArgumentException.class, () -> new Parser(List.of(
                new Token(TokenType.INTEGER, "1", new SourcePosition(1, 1)))));
    }

    @Test
    public void analyzerCallsAreIndependent() {
        invalid("x =");
        assertEquals(1, valid("x = 2").getProgram().getStatements().size());
    }

    @Test
    public void validParserFixture() throws Exception {
        SyntaxAnalysisResult result = valid(fixture("valid_parser.rb"));
        assertEquals(11, result.getProgram().getStatements().size());
        assertTrue(result.getProgram().getStatements().get(0) instanceof MethodDefinitionNode);
        assertTrue(result.getProgram().getStatements().stream().anyMatch(node -> node instanceof WhileNode));
        assertTrue(result.getProgram().getStatements().stream().anyMatch(node -> node instanceof ForNode));
    }

    @Test(timeout = 2000)
    public void invalidParserFixture() throws Exception {
        SyntaxAnalysisResult result = invalid(fixture("invalid_parser.rb"));
        assertEquals(7, result.getSyntaxErrorCount());
        assertEquals(3, result.getProgram().getStatements().stream()
                .filter(node -> node instanceof PutsNode).count());
        assertEquals(TokenType.EOF, result.getErrors().get(6).getToken().getType());
        System.out.println("invalid_parser.rb: " + result.getErrors());
    }

    private SyntaxAnalysisResult valid(String source) {
        SyntaxAnalysisResult result = analyzer.analyze(source);
        assertFalse(result.getLexicalErrors().toString() + result.getErrors(), result.hasErrors());
        assertNotNull(result.getProgram());
        return result;
    }

    private SyntaxAnalysisResult invalid(String source) {
        SyntaxAnalysisResult result = analyzer.analyze(source);
        assertFalse(result.getLexicalErrors().toString(), result.hasLexicalErrors());
        assertTrue("Se esperaba error sintactico: " + source, result.hasSyntaxErrors());
        assertNotNull(result.getProgram());
        return result;
    }

    private <T extends StatementNode> T statement(String source, Class<T> type) {
        List<StatementNode> statements = valid(source).getProgram().getStatements();
        assertEquals(1, statements.size());
        assertTrue(statements.get(0).getClass().getName(), type.isInstance(statements.get(0)));
        return type.cast(statements.get(0));
    }

    private ExpressionNode expression(String source) {
        return statement(source, ExpressionStatementNode.class).getExpression();
    }

    private static BinaryExpressionNode assertBinary(ExpressionNode expression, TokenType type) {
        assertTrue(expression instanceof BinaryExpressionNode);
        BinaryExpressionNode node = (BinaryExpressionNode) expression;
        assertEquals(type, node.getOperator().getType());
        return node;
    }

    private String fixture(String name) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/ruby/parser/" + name)) {
            assertNotNull("Fixture no encontrado: " + name, stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
