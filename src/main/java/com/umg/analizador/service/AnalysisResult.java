package com.umg.analizador.service;

import com.umg.analizador.lexer.LexicalAnalysisResult;
import com.umg.analizador.parser.SyntaxAnalysisResult;
import com.umg.analizador.semantic.SemanticAnalysisResult;
import java.util.Objects;

/** Resultado integrado inmutable; conserva las etapas sin copiar sus datos. */
public final class AnalysisResult {
    private final LexicalAnalysisResult lexicalResult;
    private final SyntaxAnalysisResult syntaxResult;
    private final SemanticAnalysisResult semanticResult;

    public AnalysisResult(LexicalAnalysisResult lexicalResult, SyntaxAnalysisResult syntaxResult,
            SemanticAnalysisResult semanticResult) {
        this.lexicalResult = Objects.requireNonNull(lexicalResult, "lexicalResult no debe ser null");
        this.syntaxResult = Objects.requireNonNull(syntaxResult, "syntaxResult no debe ser null");
        this.semanticResult = Objects.requireNonNull(semanticResult, "semanticResult no debe ser null");
    }

    public LexicalAnalysisResult getLexicalResult() { return lexicalResult; }
    public SyntaxAnalysisResult getSyntaxResult() { return syntaxResult; }
    public SemanticAnalysisResult getSemanticResult() { return semanticResult; }

    public boolean isValid() {
        return !lexicalResult.hasErrors() && !syntaxResult.hasErrors() && !semanticResult.hasErrors();
    }

    public int getLexicalErrorCount() { return lexicalResult.getErrorCount(); }
    public int getSyntaxErrorCount() { return syntaxResult.getSyntaxErrorCount(); }
    public int getSemanticErrorCount() { return semanticResult.getSemanticErrorCount(); }

    /** Cuenta cada categoria una vez, aunque las etapas posteriores conserven errores previos. */
    public int getTotalErrorCount() {
        return getLexicalErrorCount() + getSyntaxErrorCount() + getSemanticErrorCount();
    }

    /** Incluye NEWLINE y el unico EOF entregado por el lexer. */
    public int getTokenCount() { return lexicalResult.getTokenCount(); }

    /** Cuenta entradas semanticas, no caracteres del codigo fuente. */
    public int getSymbolCount() { return semanticResult.getSymbols().size(); }
}
