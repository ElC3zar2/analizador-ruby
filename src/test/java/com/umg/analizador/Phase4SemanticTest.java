package com.umg.analizador;

import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.parser.SyntaxAnalysisResult;
import com.umg.analizador.parser.SyntaxAnalyzer;
import com.umg.analizador.semantic.*;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/** Reglas semanticas del subconjunto dinamico y proteccion de resultados. */
public class Phase4SemanticTest {
    private final SemanticAnalyzer analyzer = new SemanticAnalyzer();

    @Test
    public void emptyProgram() {
        SemanticAnalysisResult result = valid("");
        assertTrue(result.getSymbols().isEmpty());
    }

    @Test
    public void definedVariable() {
        SemanticAnalysisResult result = valid("x = 10\nputs x");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void reassignment() {
        SemanticAnalysisResult result = valid("x = 10\nx = 20");
        assertEquals(1, result.getSymbols().size()); assertEquals(new SourcePosition(1, 1), symbol(result, "x", "GLOBAL").getPosition());
    }

    @Test
    public void dynamicTypeChange() {
        SemanticAnalysisResult result = valid("x = 10\nx = \"Ruby\"");
        assertEquals(SemanticType.STRING, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void compoundAddition() {
        SemanticAnalysisResult result = valid("x = 1\nx += 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void compoundSubtraction() {
        SemanticAnalysisResult result = valid("x = 3\nx -= 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void compoundMultiplication() {
        SemanticAnalysisResult result = valid("x = 3\nx *= 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void compoundDivision() {
        SemanticAnalysisResult result = valid("x = 3\nx /= 2");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void compoundModulo() {
        SemanticAnalysisResult result = valid("x = 3\nx %= 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void compoundStringAddition() {
        SemanticAnalysisResult result = valid("x = \"Ruby\"\nx += \"Java\"");
        assertEquals(SemanticType.STRING, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void unassignedInstanceReadAllowed() {
        SemanticAnalysisResult result = valid("x = @valor");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void unassignedGlobalReadAllowed() {
        SemanticAnalysisResult result = valid("x = $valor");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void assignedClassVariable() {
        SemanticAnalysisResult result = valid("@@valor = 10\nputs @@valor");
        assertEquals(SemanticType.INTEGER, symbol(result, "@@valor", "GLOBAL").getType());
    }

    @Test
    public void methodDefinition() {
        SemanticAnalysisResult result = valid("def calcular()\nreturn 10\nend");
        assertEquals(SymbolKind.METHOD, symbol(result, "calcular", "GLOBAL").getKind()); assertEquals(0, symbol(result, "calcular", "GLOBAL").getArity());
    }

    @Test
    public void validMethodCall() {
        SemanticAnalysisResult result = valid("def sumar(a, b)\nreturn a + b\nend\nsumar(1, 2)");
        assertEquals(2, symbol(result, "sumar", "GLOBAL").getArity());
    }

    @Test
    public void callBeforeDefinition() {
        SemanticAnalysisResult result = valid("saludar()\ndef saludar()\nputs \"Hola\"\nend");
        assertEquals(1, result.getSymbols().size());
    }

    @Test
    public void validParameters() {
        SemanticAnalysisResult result = valid("def suma(a, b)\nreturn a + b\nend");
        assertEquals(SymbolKind.PARAMETER, symbol(result, "a", "METHOD:suma").getKind()); assertEquals(SemanticType.UNKNOWN, symbol(result, "b", "METHOD:suma").getType());
    }

    @Test
    public void methodLocalScope() {
        SemanticAnalysisResult result = valid("x = 1\ndef metodo()\nx = 2\nputs x\nend\nputs x");
        assertEquals(3, result.getSymbols().size()); assertNotNull(symbol(result, "x", "METHOD:metodo"));
    }

    @Test
    public void globalPrefixNotLocal() {
        SemanticAnalysisResult result = valid("x = 1\n$x = 2\ndef metodo(x)\nputs x\nputs $x\nend");
        assertEquals(4, result.getSymbols().size()); assertEquals(SymbolKind.PARAMETER, symbol(result, "x", "METHOD:metodo").getKind());
    }

    @Test
    public void returnInsideMethod() {
        SemanticAnalysisResult result = valid("def metodo()\nreturn\nend");
    }

    @Test
    public void breakInsideWhile() {
        SemanticAnalysisResult result = valid("while true\nbreak\nend");
    }

    @Test
    public void nextInsideFor() {
        SemanticAnalysisResult result = valid("for elemento in []\nnext\nend");
        assertEquals(SymbolKind.LOOP_VARIABLE, symbol(result, "elemento", "GLOBAL").getKind());
    }

    @Test
    public void breakInsideIfInWhile() {
        SemanticAnalysisResult result = valid("while true\nif true\nbreak\nend\nend");
    }

    @Test
    public void nestedLoops() {
        SemanticAnalysisResult result = valid("while true\nfor i in 1..10\nnext\nbreak\nend\nbreak\nend");
    }

    @Test
    public void methodInternalLoop() {
        SemanticAnalysisResult result = valid("def metodo()\nwhile true\nbreak\nend\nreturn\nend");
    }

    @Test
    public void methodDoesNotEraseOuterLoop() {
        SemanticAnalysisResult result = valid("while true\ndef metodo()\nreturn\nend\nbreak\nend");
    }

    @Test
    public void forArray() {
        SemanticAnalysisResult result = valid("for x in [1, 2]\nputs x\nend");
    }

    @Test
    public void forRange() {
        SemanticAnalysisResult result = valid("for x in 1..10\nputs x\nend");
    }

    @Test
    public void forUnknownParameter() {
        SemanticAnalysisResult result = valid("def recorrer(datos)\nfor x in datos\nputs x\nend\nend");
        assertEquals(SymbolKind.LOOP_VARIABLE, symbol(result, "x", "METHOD:recorrer").getKind());
    }

    @Test
    public void forVariableRemainsVisible() {
        SemanticAnalysisResult result = valid("for x in []\nend\nputs x");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void ifTruthiness() {
        SemanticAnalysisResult result = valid("if 10\nx = 20\nend\nputs x");
        assertEquals("GLOBAL", symbol(result, "x", "GLOBAL").getScope());
    }

    @Test
    public void whileTruthiness() {
        SemanticAnalysisResult result = valid("while \"texto\"\nx = 1\nend\nputs x");
    }

    @Test
    public void elsifElseSameScope() {
        SemanticAnalysisResult result = valid("if true\nx = 1\nelsif 2\ny = 2\nelse\nz = 3\nend\nputs x\nputs y\nputs z");
        assertEquals(3, result.getSymbols().size());
    }

    @Test
    public void putsAnyType() {
        SemanticAnalysisResult result = valid("puts 1\nputs 1.5\nputs \"Ruby\"\nputs true\nputs false\nputs nil\nputs :activo\nputs []\nputs 1..10\nputs @sin_asignar");
    }

    @Test
    public void methodsSharePrefixedVariables() {
        SemanticAnalysisResult result = valid("@x = 1\n@@y = 2\n$z = 3\ndef metodo()\nputs @x\nputs @@y\nputs $z\n$z = 4.5\nend");
        assertEquals(SemanticType.FLOAT, symbol(result, "$z", "GLOBAL").getType());
    }

    @Test
    public void recursiveCall() {
        SemanticAnalysisResult result = valid("def recursivo(x)\nreturn recursivo(x)\nend");
    }

    @Test
    public void mutualCalls() {
        SemanticAnalysisResult result = valid("def a()\nreturn b()\nend\ndef b()\nreturn a()\nend");
    }

    @Test
    public void methodAndVariableSameName() {
        SemanticAnalysisResult result = valid("suma = 1\ndef suma()\nreturn 2\nend\nputs suma\nsuma()");
        assertEquals(2, result.getSymbols().stream().filter(s -> s.getName().equals("suma")).count());
    }

    @Test
    public void parameterReassignment() {
        SemanticAnalysisResult result = valid("def metodo(x)\nx = 2.5\nputs x\nend");
        assertEquals(SymbolKind.PARAMETER, symbol(result, "x", "METHOD:metodo").getKind()); assertEquals(SemanticType.FLOAT, symbol(result, "x", "METHOD:metodo").getType());
    }

    @Test
    public void loopVariableReassignment() {
        SemanticAnalysisResult result = valid("for x in []\nx = 1\nend");
        assertEquals(SymbolKind.LOOP_VARIABLE, symbol(result, "x", "GLOBAL").getKind()); assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void methodsInBlocksPredeclared() {
        SemanticAnalysisResult result = valid("metodo()\nif true\ndef metodo()\nreturn 1\nend\nend");
    }

    @Test
    public void nestedMethodScopeRestored() {
        SemanticAnalysisResult result = valid("def exterior(x)\ndef interior(y)\nreturn y\nend\nreturn x\nend");
        assertEquals("METHOD:interior", symbol(result, "y", "METHOD:interior").getScope());
    }

    @Test
    public void integerLiteral() {
        SemanticAnalysisResult result = valid("x = 25");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatLiteral() {
        SemanticAnalysisResult result = valid("x = 3.14");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void stringLiteral() {
        SemanticAnalysisResult result = valid("x = \"Ruby\"");
        assertEquals(SemanticType.STRING, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void trueLiteral() {
        SemanticAnalysisResult result = valid("x = true");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void falseLiteral() {
        SemanticAnalysisResult result = valid("x = false");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void nilLiteral() {
        SemanticAnalysisResult result = valid("x = nil");
        assertEquals(SemanticType.NIL, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void symbolLiteral() {
        SemanticAnalysisResult result = valid("x = :activo");
        assertEquals(SemanticType.SYMBOL, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void arrayLiteral() {
        SemanticAnalysisResult result = valid("x = [1, \"Ruby\", true, nil]");
        assertEquals(SemanticType.ARRAY, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void emptyArray() {
        SemanticAnalysisResult result = valid("x = []");
        assertEquals(SemanticType.ARRAY, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void integerAddition() {
        SemanticAnalysisResult result = valid("x = 1 + 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void integerFloatAddition() {
        SemanticAnalysisResult result = valid("x = 1 + 2.5");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatIntegerAddition() {
        SemanticAnalysisResult result = valid("x = 1.5 + 2");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatAddition() {
        SemanticAnalysisResult result = valid("x = 1.5 + 2.5");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void stringAddition() {
        SemanticAnalysisResult result = valid("x = \"Ruby\" + \"Java\"");
        assertEquals(SemanticType.STRING, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void numericSubtraction() {
        SemanticAnalysisResult result = valid("x = 10 - 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatSubtraction() {
        SemanticAnalysisResult result = valid("x = 10 - 2.5");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void numericMultiplication() {
        SemanticAnalysisResult result = valid("x = 10 * 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatMultiplication() {
        SemanticAnalysisResult result = valid("x = 10 * 2.5");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void numericDivision() {
        SemanticAnalysisResult result = valid("x = 10 / 2");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatDivision() {
        SemanticAnalysisResult result = valid("x = 10.5 / 2");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void numericModulo() {
        SemanticAnalysisResult result = valid("x = 10 % 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatModulo() {
        SemanticAnalysisResult result = valid("x = 10.5 % 2");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void numericPower() {
        SemanticAnalysisResult result = valid("x = 10 ** 2");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatPower() {
        SemanticAnalysisResult result = valid("x = 10 ** 2.5");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void crossTypeEquality() {
        SemanticAnalysisResult result = valid("x = 1 == \"1\"");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void crossTypeInequality() {
        SemanticAnalysisResult result = valid("x = nil != []");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void numericComparison() {
        SemanticAnalysisResult result = valid("x = 1 < 2.5");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void stringComparison() {
        SemanticAnalysisResult result = valid("x = \"Ruby\" >= \"Java\"");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void booleanLogical() {
        SemanticAnalysisResult result = valid("x = true && false");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void integerLogical() {
        SemanticAnalysisResult result = valid("x = 1 || 2");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void stringArrayWordLogical() {
        SemanticAnalysisResult result = valid("x = \"Ruby\" and [] or nil");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void logicalNot() {
        SemanticAnalysisResult result = valid("x = !10");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void wordNot() {
        SemanticAnalysisResult result = valid("x = not []");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void integerUnaryMinus() {
        SemanticAnalysisResult result = valid("x = -10");
        assertEquals(SemanticType.INTEGER, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatUnaryMinus() {
        SemanticAnalysisResult result = valid("x = -1.5");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void inclusiveRange() {
        SemanticAnalysisResult result = valid("x = 1..10");
        assertEquals(SemanticType.RANGE, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void exclusiveRange() {
        SemanticAnalysisResult result = valid("x = 1...10");
        assertEquals(SemanticType.RANGE, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void floatRange() {
        SemanticAnalysisResult result = valid("x = 1.5..10");
        assertEquals(SemanticType.RANGE, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void grouping() {
        SemanticAnalysisResult result = valid("x = (10 + 1.5)");
        assertEquals(SemanticType.FLOAT, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void unknownAddition() {
        SemanticAnalysisResult result = valid("x = @x + \"Ruby\"");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void unknownNumeric() {
        SemanticAnalysisResult result = valid("x = @x - \"Ruby\"");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void unknownComparison() {
        SemanticAnalysisResult result = valid("x = @x < nil");
        assertEquals(SemanticType.BOOLEAN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void unknownRange() {
        SemanticAnalysisResult result = valid("x = @x..\"Ruby\"");
        assertEquals(SemanticType.RANGE, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void unknownNegative() {
        SemanticAnalysisResult result = valid("x = -@x");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void undefinedLocal() {
        SemanticAnalysisResult result = invalid("puts x");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void missingCompoundVariable() {
        SemanticAnalysisResult result = invalid("x += 1");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Asignacion compuesta")));
    }

    @Test
    public void missingInstanceCompound() {
        SemanticAnalysisResult result = invalid("@x += 1");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Asignacion compuesta")));
    }

    @Test
    public void missingGlobalCompound() {
        SemanticAnalysisResult result = invalid("$x += 1");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Asignacion compuesta")));
    }

    @Test
    public void readDoesNotDefineForCompound() {
        SemanticAnalysisResult result = invalid("puts @x\n@x += 1");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Asignacion compuesta")));
    }

    @Test
    public void unassignedClassVariable() {
        SemanticAnalysisResult result = invalid("puts @@x");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void duplicateMethod() {
        SemanticAnalysisResult result = invalid("def suma(a, b)\nend\ndef suma(x, y)\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Metodo duplicado")));
    }

    @Test
    public void noOverloading() {
        SemanticAnalysisResult result = invalid("def suma()\nend\ndef suma(x)\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Metodo duplicado")));
    }

    @Test
    public void undefinedMethod() {
        SemanticAnalysisResult result = invalid("desconocido()");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Metodo no definido")));
    }

    @Test
    public void wrongArity() {
        SemanticAnalysisResult result = invalid("def suma(a, b)\nend\nsuma(1)");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Aridad incorrecta")));
    }

    @Test
    public void tooManyArguments() {
        SemanticAnalysisResult result = invalid("def suma()\nend\nsuma(1)");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Aridad incorrecta")));
    }

    @Test
    public void duplicateParameter() {
        SemanticAnalysisResult result = invalid("def suma(x, x)\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Parametro duplicado")));
    }

    @Test
    public void methodLocalNotGlobal() {
        SemanticAnalysisResult result = invalid("def metodo()\nz = 10\nend\nputs z");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void globalLocalNotVisibleInMethod() {
        SemanticAnalysisResult result = invalid("x = 10\ndef metodo()\nputs x\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void parameterNotGlobal() {
        SemanticAnalysisResult result = invalid("def metodo(x)\nend\nputs x");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void returnAtGlobal() {
        SemanticAnalysisResult result = invalid("return 10");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("return fuera")));
    }

    @Test
    public void breakOutsideLoop() {
        SemanticAnalysisResult result = invalid("break");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("break fuera")));
    }

    @Test
    public void nextOutsideLoop() {
        SemanticAnalysisResult result = invalid("next");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("next fuera")));
    }

    @Test
    public void breakInMethodInOuterLoop() {
        SemanticAnalysisResult result = invalid("while true\ndef prueba()\nbreak\nend\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("break fuera")));
    }

    @Test
    public void nextInMethodInOuterLoop() {
        SemanticAnalysisResult result = invalid("for x in []\ndef prueba()\nnext\nend\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("next fuera")));
    }

    @Test
    public void returnAfterMethodDoesNotLeak() {
        SemanticAnalysisResult result = invalid("def metodo()\nreturn\nend\nreturn 1");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("return fuera")));
    }

    @Test
    public void breakAfterLoopDoesNotLeak() {
        SemanticAnalysisResult result = invalid("while true\nbreak\nend\nbreak");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("break fuera")));
    }

    @Test
    public void nextAfterLoopDoesNotLeak() {
        SemanticAnalysisResult result = invalid("for x in []\nnext\nend\nnext");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("next fuera")));
    }

    @Test
    public void incompatibleAddition() {
        SemanticAnalysisResult result = invalid("x = 10 + \"Ruby\"");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleBooleanAddition() {
        SemanticAnalysisResult result = invalid("true + false");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleStringSubtraction() {
        SemanticAnalysisResult result = invalid("\"Ruby\" - \"Java\"");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleStringMultiplication() {
        SemanticAnalysisResult result = invalid("\"Ruby\" * 2");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleNilDivision() {
        SemanticAnalysisResult result = invalid("nil / 2");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleArrayModulo() {
        SemanticAnalysisResult result = invalid("[] % 2");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleSymbolPower() {
        SemanticAnalysisResult result = invalid(":activo ** 2");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleComparison() {
        SemanticAnalysisResult result = invalid("1 < \"Ruby\"");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleUnaryMinus() {
        SemanticAnalysisResult result = invalid("-\"Ruby\"");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleRange() {
        SemanticAnalysisResult result = invalid("\"hola\"..10");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void incompatibleStringRange() {
        SemanticAnalysisResult result = invalid("\"a\"..\"z\"");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void arrayElementError() {
        SemanticAnalysisResult result = invalid("[1, no_definida, 3]");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void forInteger() {
        SemanticAnalysisResult result = invalid("for x in 10\nputs x\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("for requiere")));
    }

    @Test
    public void forString() {
        SemanticAnalysisResult result = invalid("for x in \"Ruby\"\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("for requiere")));
    }

    @Test
    public void forNil() {
        SemanticAnalysisResult result = invalid("for x in nil\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("for requiere")));
    }

    @Test
    public void compoundIncompatible() {
        SemanticAnalysisResult result = invalid("x = \"Ruby\"\nx -= 1");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void compoundNumericStringIncompatible() {
        SemanticAnalysisResult result = invalid("x = 1\nx += \"Ruby\"");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("incompatible")));
    }

    @Test
    public void undefinedRhsBeforeDeclaration() {
        SemanticAnalysisResult result = invalid("x = x + 1");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void unknownPreventsCascades() {
        SemanticAnalysisResult result = invalid("x = falta + 1\nx * \"Ruby\"");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void callArgumentsAreAnalyzed() {
        SemanticAnalysisResult result = invalid("def f(x)\nend\nf(falta)");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void ifConditionIsAnalyzed() {
        SemanticAnalysisResult result = invalid("if falta\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void whileConditionIsAnalyzed() {
        SemanticAnalysisResult result = invalid("while falta\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void elsifConditionIsAnalyzed() {
        SemanticAnalysisResult result = invalid("if true\nelsif falta\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void returnExpressionIsAnalyzed() {
        SemanticAnalysisResult result = invalid("def f()\nreturn falta\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().toString(), result.getErrors().stream().anyMatch(error -> error.getMessage().contains("Variable no definida")));
    }

    @Test
    public void syntaxErrorsPreventDeepSemanticAnalysis() {
        SemanticAnalysisResult result = analyzer.analyze("x =\nputs desconocida\nbreak");
        assertTrue(result.hasErrors());
        assertTrue(result.getSyntaxResult().hasSyntaxErrors());
        assertFalse(result.hasSemanticErrors());
        assertTrue(result.getSymbols().isEmpty());
    }

    @Test
    public void lexicalErrorsPreventDeepSemanticAnalysis() {
        SemanticAnalysisResult result = analyzer.analyze("?\nputs desconocida");
        assertTrue(result.hasErrors());
        assertTrue(result.getSyntaxResult().hasLexicalErrors());
        assertFalse(result.hasSemanticErrors());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getSymbols().isEmpty());
        assertEquals(new SyntaxAnalyzer().analyze("?\nputs desconocida").getLexicalErrors(),
                result.getSyntaxResult().getLexicalErrors());
    }

    @Test
    public void syntaxDiagnosticsArePreserved() {
        String source = "def f(\nend";
        SemanticAnalysisResult result = analyzer.analyze(source);
        assertEquals(new SyntaxAnalyzer().analyze(source).getErrors(), result.getSyntaxResult().getErrors());
        assertEquals(0, result.getSemanticErrorCount());
    }

    @Test
    public void symbolTableContainsExpectedScopesKindsAndPositions() {
        SemanticAnalysisResult result = valid("x = 1\ndef suma(a, b)\ny = a + b\nreturn y\nend\nfor i in 1..10\nend");
        assertEquals(6, result.getSymbols().size());
        SemanticSymbol x = symbol(result, "x", "GLOBAL");
        assertEquals(SymbolKind.VARIABLE, x.getKind());
        assertEquals(SemanticType.INTEGER, x.getType());
        assertEquals(-1, x.getArity());
        assertEquals(new SourcePosition(1, 1), x.getPosition());
        SemanticSymbol method = symbol(result, "suma", "GLOBAL");
        assertEquals(SymbolKind.METHOD, method.getKind());
        assertEquals(2, method.getArity());
        assertEquals(new SourcePosition(2, 5), method.getPosition());
        assertEquals(SymbolKind.PARAMETER, symbol(result, "a", "METHOD:suma").getKind());
        assertEquals(new SourcePosition(2, 10), symbol(result, "a", "METHOD:suma").getPosition());
        assertEquals(SymbolKind.LOOP_VARIABLE, symbol(result, "i", "GLOBAL").getKind());
        assertEquals(new SourcePosition(6, 5), symbol(result, "i", "GLOBAL").getPosition());
    }

    @Test
    public void sameLocalNamesInDifferentMethodsAreIndependent() {
        SemanticAnalysisResult result = valid("def a(x)\ny = 1\nend\ndef b(x)\ny = \"Ruby\"\nend");
        assertEquals(SemanticType.INTEGER, symbol(result, "y", "METHOD:a").getType());
        assertEquals(SemanticType.STRING, symbol(result, "y", "METHOD:b").getType());
    }

    @Test
    public void compoundParameterUnknownDoesNotCascade() {
        SemanticAnalysisResult result = valid("def f(x)\nx += 1\nx -= 2\nx *= 3\nx /= 4\nx %= 5\nend");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "METHOD:f").getType());
    }

    @Test
    public void methodReturnTypeIsDeliberatelyUnknown() {
        SemanticAnalysisResult result = valid("def f()\nreturn 1\nend\nx = f()");
        assertEquals(SemanticType.UNKNOWN, symbol(result, "x", "GLOBAL").getType());
    }

    @Test
    public void globalAssignmentInsideMethodIsShared() {
        SemanticAnalysisResult result = valid("def f()\n$x = 1\nend\nputs $x");
        assertEquals("GLOBAL", symbol(result, "$x", "GLOBAL").getScope());
    }

    @Test
    public void returnInsideGlobalLoopStillInvalid() {
        SemanticAnalysisResult result = invalid("while true\nreturn 1\nend");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("return fuera"));
    }

    @Test
    public void undefinedLocalIsNotAnImplicitMethodCall() {
        SemanticAnalysisResult result = invalid("def f()\nend\nputs f");
        assertEquals(1, result.getSemanticErrorCount());
        assertTrue(result.getErrors().get(0).getMessage().contains("Variable no definida"));
    }

    @Test
    public void unknownRangeAvoidsSecondaryForError() {
        SemanticAnalysisResult result = invalid("for i in falta..10\nputs i\nend");
        assertEquals(1, result.getSemanticErrorCount());
    }

    @Test
    public void invalidForDoesNotAddUndefinedLoopVariableError() {
        SemanticAnalysisResult result = invalid("for i in 10\nputs i\nend\nputs i");
        assertEquals(1, result.getSemanticErrorCount());
        assertEquals(SymbolKind.LOOP_VARIABLE, symbol(result, "i", "GLOBAL").getKind());
    }

    @Test
    public void errorsInsideBothCallArgumentsAreRetained() {
        SemanticAnalysisResult result = invalid("def f(a, b)\nend\nf(x, y)");
        assertEquals(2, result.getSemanticErrorCount());
    }

    @Test
    public void semanticErrorsCarryAccuratePositions() {
        SemanticAnalysisResult result = invalid("\n  puts x\n  return 1\n  1 + \"Ruby\"");
        assertEquals(3, result.getSemanticErrorCount());
        assertEquals(new SourcePosition(2, 8), result.getErrors().get(0).getPosition());
        assertEquals(new SourcePosition(3, 3), result.getErrors().get(1).getPosition());
        assertEquals(new SourcePosition(4, 5), result.getErrors().get(2).getPosition());
    }

    @Test
    public void duplicateAndArityErrorPositions() {
        SemanticAnalysisResult result = invalid("def f(a)\nend\nf()\ndef f()\nend");
        assertEquals(2, result.getSemanticErrorCount());
        assertEquals(new SourcePosition(4, 5), result.getErrors().get(0).getPosition());
        assertEquals(new SourcePosition(3, 1), result.getErrors().get(1).getPosition());
    }

    @Test
    public void analyzerDoesNotLeakStateBetweenCalls() {
        invalid("break\nputs x");
        valid("x = 1");
        invalid("puts x");
        SemanticAnalysisResult result = valid("");
        assertTrue(result.getSymbols().isEmpty());
    }

    @Test
    public void nullSourceRejected() {
        assertThrows(NullPointerException.class, () -> analyzer.analyze(null));
    }

    @Test
    public void semanticErrorValueContract() {
        SourcePosition position = new SourcePosition(2, 3);
        SemanticError first = new SemanticError("Error", position);
        SemanticError second = new SemanticError("Error", new SourcePosition(2, 3));
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "otro tipo");
        assertNotEquals(first, new SemanticError("Otro error", position));
        assertNotEquals(first, new SemanticError("Error", new SourcePosition(3, 3)));
        assertEquals("Error", first.getMessage());
        assertEquals(position, first.getPosition());
        assertTrue(first.toString().contains("Error"));
        assertThrows(NullPointerException.class, () -> new SemanticError(null, position));
        assertThrows(NullPointerException.class, () -> new SemanticError("Error", null));
    }

    @Test
    public void semanticSymbolIsAnImmutableValue() {
        SourcePosition position = new SourcePosition(1, 1);
        SemanticSymbol first = new SemanticSymbol("x", SymbolKind.VARIABLE, SemanticType.INTEGER,
                position, "GLOBAL", -1);
        SemanticSymbol second = new SemanticSymbol("x", SymbolKind.VARIABLE, SemanticType.INTEGER,
                new SourcePosition(1, 1), "GLOBAL", -1);
        assertEquals(first, first);
        assertEquals(first, second);
        assertEquals(second, first);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, null);
        assertNotEquals(first, "otro tipo");
        SemanticSymbol updated = first.withType(SemanticType.STRING);
        assertNotEquals(first, updated);
        assertEquals(SemanticType.INTEGER, first.getType());
        assertEquals(SemanticType.STRING, updated.getType());
        assertEquals(position, updated.getPosition());
        assertEquals(first.getKind(), updated.getKind());
        assertEquals(first.getScope(), updated.getScope());
        assertEquals(first.getName(), updated.getName());
        assertTrue(first.toString().contains("INTEGER"));
    }

    @Test
    public void semanticSymbolArityValidation() {
        SourcePosition position = new SourcePosition(1, 1);
        assertThrows(IllegalArgumentException.class, () -> new SemanticSymbol("f", SymbolKind.METHOD,
                SemanticType.UNKNOWN, position, "GLOBAL", -1));
        assertThrows(IllegalArgumentException.class, () -> new SemanticSymbol("x", SymbolKind.VARIABLE,
                SemanticType.INTEGER, position, "GLOBAL", 0));
        assertThrows(NullPointerException.class, () -> new SemanticSymbol(null, SymbolKind.VARIABLE,
                SemanticType.INTEGER, position, "GLOBAL", -1));
    }

    @Test
    public void symbolTableSeparatesMethodsAndVariablesAndScopes() {
        SymbolTable table = new SymbolTable();
        SourcePosition position = new SourcePosition(1, 1);
        SemanticSymbol global = new SemanticSymbol("f", SymbolKind.VARIABLE, SemanticType.INTEGER,
                position, SymbolTable.GLOBAL, -1);
        SemanticSymbol method = new SemanticSymbol("f", SymbolKind.METHOD, SemanticType.UNKNOWN,
                position, SymbolTable.GLOBAL, 1);
        SemanticSymbol parameter = new SemanticSymbol("f", SymbolKind.PARAMETER, SemanticType.UNKNOWN,
                position, SymbolTable.methodScope("f"), -1);
        table.put(global);
        table.put(method);
        table.put(parameter);
        assertEquals(global, table.findVariable("f", "GLOBAL"));
        assertEquals(method, table.findMethod("f"));
        assertEquals(parameter, table.findVariable("f", "METHOD:f"));
        assertNull(table.findVariable("f", "METHOD:otro"));
        assertNull(table.findMethod("inexistente"));
        assertEquals(Arrays.asList(global, method, parameter), table.getSymbols());
        List<SemanticSymbol> snapshot = table.getSymbols();
        table.put(global.withType(SemanticType.STRING));
        assertEquals(SemanticType.INTEGER, snapshot.get(0).getType());
        assertEquals(SemanticType.STRING, table.getSymbols().get(0).getType());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    public void semanticResultDefensivelyCopiesCollections() {
        SyntaxAnalysisResult syntax = new SyntaxAnalyzer().analyze("");
        List<SemanticError> errors = new ArrayList<>(List.of(new SemanticError("Error", new SourcePosition(1, 1))));
        List<SemanticSymbol> symbols = new ArrayList<>(List.of(new SemanticSymbol("x", SymbolKind.VARIABLE,
                SemanticType.INTEGER, new SourcePosition(1, 1), "GLOBAL", -1)));
        SemanticAnalysisResult result = new SemanticAnalysisResult(syntax, errors, symbols);
        errors.clear();
        symbols.clear();
        assertEquals(1, result.getSemanticErrorCount());
        assertEquals(1, result.getSymbols().size());
        assertSame(syntax, result.getSyntaxResult());
        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.getSymbols().clear());
        assertTrue(result.hasErrors());
        assertTrue(result.hasSemanticErrors());
    }

    @Test
    public void validSemanticFixture() throws Exception {
        SemanticAnalysisResult result = valid(fixture("valid_semantic.rb"));
        assertEquals(16, result.getSymbols().size());
        assertEquals(SemanticType.STRING, symbol(result, "x", "GLOBAL").getType());
        assertEquals(SymbolKind.LOOP_VARIABLE, symbol(result, "i", "GLOBAL").getKind());
        System.out.println("valid_semantic.rb: simbolos = " + result.getSymbols().size());
    }

    @Test
    public void invalidSemanticFixture() throws Exception {
        SemanticAnalysisResult result = invalid(fixture("invalid_semantic.rb"));
        assertEquals(15, result.getSemanticErrorCount());
        String messages = result.getErrors().toString();
        for (String category : Arrays.asList("Variable no definida", "Asignacion compuesta", "break fuera",
                "next fuera", "return fuera", "Metodo no definido", "incompatible",
                "Negativo unario", "for requiere", "Parametro duplicado", "Aridad incorrecta", "Metodo duplicado")) {
            assertTrue(category + ": " + messages, messages.contains(category));
        }
        System.out.println("invalid_semantic.rb: " + result.getErrors());
    }

    private SemanticAnalysisResult valid(String source) {
        SemanticAnalysisResult result = analyzer.analyze(source);
        assertFalse(result.getSyntaxResult().getErrors().toString(), result.getSyntaxResult().hasErrors());
        assertFalse(result.getErrors().toString(), result.hasSemanticErrors());
        assertFalse(result.hasErrors());
        return result;
    }

    private SemanticAnalysisResult invalid(String source) {
        SemanticAnalysisResult result = analyzer.analyze(source);
        assertFalse(result.getSyntaxResult().getErrors().toString(), result.getSyntaxResult().hasErrors());
        assertTrue("Se esperaba error semantico: " + source, result.hasSemanticErrors());
        assertTrue(result.hasErrors());
        return result;
    }

    private SemanticSymbol symbol(SemanticAnalysisResult result, String name, String scope) {
        return result.getSymbols().stream().filter(symbol -> symbol.getName().equals(name)
                && symbol.getScope().equals(scope)).findFirst()
                .orElseThrow(() -> new AssertionError("Simbolo no encontrado: " + scope + " / " + name));
    }

    private String fixture(String name) throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/ruby/semantic/" + name)) {
            assertNotNull("Fixture no encontrado: " + name, stream);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}

