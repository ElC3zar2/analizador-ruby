package com.umg.analizador.lexer;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Lexema reconocido, su clasificacion y su posicion inicial en el codigo fuente. */
public final class Token {
    private final TokenType type;
    private final String lexeme;
    private final SourcePosition position;

    public Token(TokenType type, String lexeme, SourcePosition position) {
        this.type = Objects.requireNonNull(type, "type no debe ser null");
        this.lexeme = Objects.requireNonNull(lexeme, "lexeme no debe ser null");
        this.position = Objects.requireNonNull(position, "position no debe ser null");
    }

    public TokenType getType() {
        return type;
    }

    public String getLexeme() {
        return lexeme;
    }

    public SourcePosition getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "Token{type=" + type + ", lexeme='" + lexeme + "', position=" + position + '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Token)) {
            return false;
        }
        Token other = (Token) object;
        return type == other.type && lexeme.equals(other.lexeme) && position.equals(other.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, lexeme, position);
    }
}
