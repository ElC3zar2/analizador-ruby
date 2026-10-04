package com.umg.analizador.lexer;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Diagnostico inmutable de un problema lexico y del fragmento que lo origina. */
public final class LexicalError {
    private final String message;
    private final String lexeme;
    private final SourcePosition position;

    public LexicalError(String message, String lexeme, SourcePosition position) {
        this.message = Objects.requireNonNull(message, "message no debe ser null");
        this.lexeme = Objects.requireNonNull(lexeme, "lexeme no debe ser null");
        this.position = Objects.requireNonNull(position, "position no debe ser null");
    }

    public String getMessage() {
        return message;
    }

    public String getLexeme() {
        return lexeme;
    }

    public SourcePosition getPosition() {
        return position;
    }

    @Override
    public String toString() {
        return "LexicalError{message='" + message + "', lexeme='" + lexeme
                + "', position=" + position + '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof LexicalError)) {
            return false;
        }
        LexicalError other = (LexicalError) object;
        return message.equals(other.message) && lexeme.equals(other.lexeme)
                && position.equals(other.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, lexeme, position);
    }
}
