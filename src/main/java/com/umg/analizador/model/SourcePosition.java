package com.umg.analizador.model;

import java.util.Objects;

/** Posicion inmutable del codigo fuente; lineas y columnas comienzan en 1. */
public final class SourcePosition {
    private final int line;
    private final int column;

    public SourcePosition(int line, int column) {
        if (line < 1 || column < 1) {
            throw new IllegalArgumentException("La linea y la columna deben ser mayores que cero");
        }
        this.line = line;
        this.column = column;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @Override
    public String toString() {
        return "SourcePosition{line=" + line + ", column=" + column + '}';
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof SourcePosition)) {
            return false;
        }
        SourcePosition other = (SourcePosition) object;
        return line == other.line && column == other.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(line, column);
    }
}
