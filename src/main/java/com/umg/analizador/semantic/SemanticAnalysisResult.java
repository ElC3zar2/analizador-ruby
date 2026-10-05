package com.umg.analizador.semantic;

import com.umg.analizador.parser.SyntaxAnalysisResult;
import java.util.List;
import java.util.Objects;

/** Resultado protegido que conserva diagnosticos previos y la tabla semantica final. */
public final class SemanticAnalysisResult {
    private final SyntaxAnalysisResult syntaxResult;
    private final List<SemanticError> errors;
    private final List<SemanticSymbol> symbols;

    public SemanticAnalysisResult(SyntaxAnalysisResult syntaxResult, List<SemanticError> errors,
            List<SemanticSymbol> symbols) {
        this.syntaxResult = Objects.requireNonNull(syntaxResult, "syntaxResult no debe ser null");
        this.errors = List.copyOf(errors);
        this.symbols = List.copyOf(symbols);
    }

    public SyntaxAnalysisResult getSyntaxResult() { return syntaxResult; }
    public List<SemanticError> getErrors() { return errors; }
    public List<SemanticSymbol> getSymbols() { return symbols; }
    public boolean hasErrors() { return syntaxResult.hasErrors() || hasSemanticErrors(); }
    public boolean hasSemanticErrors() { return !errors.isEmpty(); }
    public int getSemanticErrorCount() { return errors.size(); }
}
