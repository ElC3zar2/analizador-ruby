package com.umg.analizador.parser;

import com.umg.analizador.ast.ProgramNode;
import com.umg.analizador.lexer.LexicalError;
import java.util.List;
import java.util.Objects;

/** AST parcial o completo y diagnosticos de ambas etapas, con colecciones protegidas. */
public final class SyntaxAnalysisResult {
    private final ProgramNode program;
    private final List<SyntaxError> errors;
    private final List<LexicalError> lexicalErrors;

    public SyntaxAnalysisResult(ProgramNode program, List<SyntaxError> errors,
            List<LexicalError> lexicalErrors) {
        this.program = Objects.requireNonNull(program, "program no debe ser null");
        this.errors = List.copyOf(errors);
        this.lexicalErrors = List.copyOf(lexicalErrors);
    }

    public ProgramNode getProgram() {
        return program;
    }

    public List<SyntaxError> getErrors() {
        return errors;
    }

    public List<LexicalError> getLexicalErrors() {
        return lexicalErrors;
    }

    public boolean hasErrors() {
        return hasLexicalErrors() || hasSyntaxErrors();
    }

    public boolean hasLexicalErrors() {
        return !lexicalErrors.isEmpty();
    }

    public boolean hasSyntaxErrors() {
        return !errors.isEmpty();
    }

    public int getSyntaxErrorCount() {
        return errors.size();
    }
}
