package com.umg.analizador.lexer;

import com.umg.analizador.model.SourcePosition;
import com.umg.analizador.model.SourceSymbol;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Fachada del lexer; cada llamada analiza una entrada independiente normalizada. */
public final class LexicalAnalyzer {
    public LexicalAnalysisResult analyze(String source) {
        Objects.requireNonNull(source, "source no debe ser null");
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n');
        List<SourceSymbol> symbols = new ArrayList<>();
        Set<String> alphabet = new LinkedHashSet<>();
        int line = 1;
        int column = 1;
        for (int offset = 0; offset < normalized.length();) {
            int codePoint = normalized.codePointAt(offset);
            String value = new String(Character.toChars(codePoint));
            symbols.add(new SourceSymbol(value, new SourcePosition(line, column)));
            alphabet.add(value);
            int width = Character.charCount(codePoint);
            offset += width;
            if (codePoint == '\n') {
                line++;
                column = 1;
            } else {
                // JFlex cuenta columnas en unidades UTF-16; un tabulador ocupa una.
                column += width;
            }
        }

        RubyLexerGenerated lexer = new RubyLexerGenerated(new StringReader(normalized));
        List<Token> tokens = new ArrayList<>();
        try {
            Token token;
            do {
                token = lexer.nextToken();
                tokens.add(token);
            } while (token.getType() != TokenType.EOF);
        } catch (IOException exception) {
            // StringReader no realiza E/S externa; esto no es un error del codigo Ruby.
            throw new UncheckedIOException("No se pudo leer el codigo fuente", exception);
        }
        return new LexicalAnalysisResult(tokens, lexer.getErrors(), alphabet, symbols);
    }
}
