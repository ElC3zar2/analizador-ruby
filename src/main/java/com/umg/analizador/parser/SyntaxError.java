package com.umg.analizador.parser;

import com.umg.analizador.lexer.Token;
import java.util.Objects;

/** Diagnostico sintactico inmutable asociado al token donde se detecto el problema. */
public final class SyntaxError {
    private final String message;
    private final Token token;

    public SyntaxError(String message, Token token) {
        this.message = Objects.requireNonNull(message, "message no debe ser null");
        this.token = Objects.requireNonNull(token, "token no debe ser null");
    }

    public String getMessage() {
        return message;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SyntaxError)) {
            return false;
        }
        SyntaxError other = (SyntaxError) object;
        return message.equals(other.message) && token.equals(other.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, token);
    }

    @Override
    public String toString() {
        return "SyntaxError{message='" + message + "', token=" + token + '}';
    }
}
