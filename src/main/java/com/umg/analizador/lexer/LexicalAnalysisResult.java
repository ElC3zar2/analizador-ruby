package com.umg.analizador.lexer;

import com.umg.analizador.model.SourceSymbol;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resultado inmutable del analisis lexico. Los simbolos son las unidades
 * individuales observadas; el alfabeto contiene sus valores distintos.
 * Una palabra o lexema es una secuencia de simbolos, disponible mediante
 * {@link Token#getLexeme()}; {@link TokenType} indica su clasificacion.
 */
public final class LexicalAnalysisResult {
    private final List<Token> tokens;
    private final List<LexicalError> errors;
    private final Set<String> alphabet;
    private final List<SourceSymbol> symbols;

    public LexicalAnalysisResult(List<Token> tokens, List<LexicalError> errors,
            Set<String> alphabet, List<SourceSymbol> symbols) {
        this.tokens = List.copyOf(Objects.requireNonNull(tokens, "tokens no debe ser null"));
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors no debe ser null"));
        Set<String> alphabetCopy = new LinkedHashSet<>(
                Objects.requireNonNull(alphabet, "alphabet no debe ser null"));
        for (String value : alphabetCopy) {
            Objects.requireNonNull(value, "alphabet no debe contener null");
        }
        this.alphabet = Collections.unmodifiableSet(alphabetCopy);
        this.symbols = List.copyOf(Objects.requireNonNull(symbols, "symbols no debe ser null"));
    }

    public List<Token> getTokens() {
        return tokens;
    }

    public List<LexicalError> getErrors() {
        return errors;
    }

    public Set<String> getAlphabet() {
        return alphabet;
    }

    public List<SourceSymbol> getSymbols() {
        return symbols;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public int getTokenCount() {
        return tokens.size();
    }

    public int getErrorCount() {
        return errors.size();
    }
}
