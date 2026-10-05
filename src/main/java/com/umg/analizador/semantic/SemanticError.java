package com.umg.analizador.semantic;

import com.umg.analizador.model.SourcePosition;
import java.util.Objects;

/** Diagnostico semantico inmutable en la posicion de la construccion incorrecta. */
public final class SemanticError {
    private final String message;
    private final SourcePosition position;

    public SemanticError(String message, SourcePosition position) {
        this.message = Objects.requireNonNull(message, "message no debe ser null");
        this.position = Objects.requireNonNull(position, "position no debe ser null");
    }

    public String getMessage() { return message; }
    public SourcePosition getPosition() { return position; }

    @Override
    public boolean equals(Object object) {
        if (this == object) { return true; }
        if (!(object instanceof SemanticError)) { return false; }
        SemanticError other = (SemanticError) object;
        return message.equals(other.message) && position.equals(other.position);
    }

    @Override
    public int hashCode() { return Objects.hash(message, position); }

    @Override
    public String toString() {
        return "SemanticError{message='" + message + "', position=" + position + '}';
    }
}
