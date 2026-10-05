package com.umg.analizador.service;

import com.umg.analizador.lexer.LexicalAnalysisResult;
import com.umg.analizador.lexer.LexicalAnalyzer;
import com.umg.analizador.parser.SyntaxAnalysisResult;
import com.umg.analizador.parser.SyntaxAnalyzer;
import com.umg.analizador.semantic.SemanticAnalysisResult;
import com.umg.analizador.semantic.SemanticAnalyzer;
import java.util.Objects;

/**
 * Entrada unica recomendada para consumir el backend desde Frontend.
 * Las fachadas existentes repiten etapas previas; para entradas pequenas se
 * conserva este contrato sencillo y estable en lugar de refactorizar las fases.
 */
public final class AnalysisService {
    public AnalysisResult analyze(String source) {
        Objects.requireNonNull(source, "source no debe ser null");
        LexicalAnalysisResult lexical = new LexicalAnalyzer().analyze(source);
        SyntaxAnalysisResult syntax = new SyntaxAnalyzer().analyze(source);
        SemanticAnalysisResult semantic = new SemanticAnalyzer().analyze(source);
        return new AnalysisResult(lexical, syntax, semantic);
    }
}
