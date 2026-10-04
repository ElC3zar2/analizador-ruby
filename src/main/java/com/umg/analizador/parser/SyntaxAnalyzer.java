package com.umg.analizador.parser;

import com.umg.analizador.ast.ProgramNode;
import com.umg.analizador.lexer.LexicalAnalysisResult;
import com.umg.analizador.lexer.LexicalAnalyzer;
import java.util.Objects;

/** Fachada publica que combina el lexer y el parser manual del subconjunto Ruby. */
public final class SyntaxAnalyzer {
    public SyntaxAnalysisResult analyze(String source) {
        Objects.requireNonNull(source, "source no debe ser null");
        LexicalAnalysisResult lexical = new LexicalAnalyzer().analyze(source);
        Parser parser = new Parser(lexical.getTokens());
        ProgramNode program = parser.parse();
        return new SyntaxAnalysisResult(program, parser.getErrors(), lexical.getErrors());
    }
}
